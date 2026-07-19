package com.ecommerce.order.payment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.constant.PaymentStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.util.IdGenerator;
import com.ecommerce.order.payment.mapper.PaymentLogMapper;
import com.ecommerce.order.payment.mapper.PaymentMapper;
import com.ecommerce.order.payment.mapper.RefundMapper;
import com.ecommerce.order.payment.model.entity.Payment;
import com.ecommerce.order.payment.model.entity.PaymentLog;
import com.ecommerce.order.payment.model.entity.Refund;
import com.ecommerce.order.payment.model.event.PaymentEvent;
import com.ecommerce.order.payment.model.event.RefundEvent;
import com.ecommerce.order.payment.mq.PaymentProducer;
import com.ecommerce.order.payment.channel.PaymentChannel;
import com.ecommerce.order.payment.channel.CallbackResult;
import com.ecommerce.order.payment.channel.RefundResult;
import com.ecommerce.order.payment.PaymentService;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.feign.MarketingFeignClient;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final OrderService orderService;
    private final PaymentProducer paymentProducer;
    private final MarketingFeignClient marketingFeignClient;
    private final OrderItemMapper orderItemMapper;
    private final List<PaymentChannel> channels;
    /** channelName → PaymentChannel 映射，O(1) 路由 */
    private Map<String, PaymentChannel> channelMap;

    @PostConstruct
    void initChannels() {
        channelMap = channels.stream()
                .collect(Collectors.toMap(PaymentChannel::channelName, c -> c));
        log.info("已加载支付渠道: {}", channelMap.keySet());
    }

    @Override
    @Transactional
    public Map<String, String> createPayment(Long userId, Long orderId, String orderNo, BigDecimal amount, String channel) {
        // 幂等校验：同一订单重复创建支付单时直接返回已有的支付单号
        Payment existing = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (existing != null) {
            return Map.of("paymentNo", existing.getPaymentNo(), "payUrl", "");
        }

        Payment payment = new Payment();
        payment.setPaymentNo(IdGenerator.paymentNo());
        payment.setOrderId(orderId);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setChannel(channel);
        payment.setStatus(PaymentStatusEnum.PENDING.getCode());
        paymentMapper.insert(payment);

        // 通过支付渠道获取支付链接（支付宝返回收银台HTML表单/微信返回二维码链接）
        PaymentChannel paymentChannel = channelMap.getOrDefault(channel, channelMap.get("mock"));
        String payUrl = paymentChannel.createPaymentUrl(orderNo, amount,
                "订单-" + orderNo.substring(0, Math.min(8, orderNo.length())));

        log.info("支付单创建: paymentNo={}, orderNo={}, channel={}, amount={}",
                payment.getPaymentNo(), orderNo, channel, amount);

        return Map.of("paymentNo", payment.getPaymentNo(), "payUrl", payUrl);
    }

    @Override
    @Transactional
    public void handleCallback(String channel, String paymentNo, String transactionNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }

        // 非待支付状态的支付单不处理回调（防止重复通知导致重复发货）
        if (payment.getStatus() != PaymentStatusEnum.PENDING.getCode()) {
            log.warn("重复支付回调: paymentNo={}", paymentNo);
            return;
        }

        // 通过支付渠道验签（生产环境需验签通过后才能信任回调数据）
        PaymentChannel paymentChannel = channelMap.getOrDefault(channel, channelMap.get("mock"));
        CallbackResult cbResult = paymentChannel.handleCallback(
                Map.of("paymentNo", paymentNo, "transactionNo", transactionNo));
        if (!cbResult.isSuccess()) {
            log.error("支付回调验签失败: channel={}, reason={}", channel, cbResult.getFailReason());
            throw new BusinessException(ResultCodeEnum.PAYMENT_AMOUNT_ERROR);
        }

        String verifiedTransactionNo = cbResult.getTransactionNo();
        payment.setStatus(PaymentStatusEnum.PAID.getCode());
        payment.setPaidTime(LocalDateTime.now());
        payment.setTransactionNo(verifiedTransactionNo);
        paymentMapper.updateById(payment);

        // Record log
        PaymentLog logEntry = new PaymentLog();
        logEntry.setPaymentId(payment.getId());
        logEntry.setType("CALLBACK");
        logEntry.setResponse("channel=" + channel + ", transactionNo=" + verifiedTransactionNo);
        logEntry.setCreateTime(LocalDateTime.now());
        paymentLogMapper.insert(logEntry);

        log.info("支付回调处理成功: paymentNo={}, channel={}, transactionNo={}", paymentNo, channel, verifiedTransactionNo);

        // 本地调用 orderService 更新订单状态为"待发货"（同一服务内，无需 Feign）
        try {
            orderService.updateStatus(payment.getOrderId(),
                    OrderStatusEnum.PENDING_DELIVER.getCode(), "PAYMENT_CALLBACK");
            log.info("订单状态已更新: orderId={}, status=PENDING_DELIVER", payment.getOrderId());
        } catch (Exception e) {
            log.error("更新订单状态失败: orderId={}", payment.getOrderId(), e);
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, payment.getOrderId()));
        List<PaymentEvent.OrderItemDTO> itemDTOs = orderItems.stream()
                .map(item -> new PaymentEvent.OrderItemDTO(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());

        final PaymentEvent event = new PaymentEvent(payment.getOrderId(), payment.getPaymentNo(), itemDTOs);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    paymentProducer.sendPaymentSuccess(event);
                    log.info("支付成功事件已发送: orderId={}", event.getOrderId());
                } catch (Exception e) {
                    log.error("支付成功事件发送失败(需补偿): orderId={}, paymentNo={}",
                            event.getOrderId(), event.getPaymentNo(), e);
                }

                try {
                    Order order = orderService.getOrderEntity(payment.getOrderId());
                    if (order != null && order.getCouponId() != null) {
                        marketingFeignClient.confirmCoupon(order.getCouponId(), order.getId());
                        log.info("优惠券确认成功: orderId={}, couponId={}", order.getId(), order.getCouponId());
                    }
                } catch (Exception e) {
                    log.error("优惠券确认失败(需补偿): orderId={}", payment.getOrderId(), e);
                }
            }
        });
    }

    // ==================== 退款 ====================

    /**
     * 用户提交退款申请（仅创建退款记录，不调用支付渠道）
     * <p>
     * 状态变更：订单 → 退款中(5)、支付 → 退款中(2)、退款单 → 处理中(0)
     * 后续由管理员在 B 端审核通过后调用 {@link #processRefund(Long)} 执行实际退款。
     */
    @Override
    @Transactional
    public void requestRefund(Long userId, Long orderId, BigDecimal amount, String reason) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }
        // 仅已支付状态可申请退款
        if (payment.getStatus() != PaymentStatusEnum.PAID.getCode()) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_STATUS_INVALID.getCode(),
                    "当前支付状态不支持退款");
        }

        // 1. 创建退款单，状态=处理中(0)
        Refund refund = new Refund();
        refund.setRefundNo(IdGenerator.refundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setAmount(amount);
        refund.setReason(reason != null ? reason : "用户申请退款");
        refund.setStatus(0);
        refundMapper.insert(refund);

        // 2. 支付状态 → 退款中(2)
        payment.setStatus(PaymentStatusEnum.REFUNDING.getCode());
        paymentMapper.updateById(payment);

        // 3. 订单状态 → 退款中(5)（本地调用，替代原 Feign 调用）
        try {
            orderService.updateStatus(orderId,
                    OrderStatusEnum.REFUNDING.getCode(), "USER_REFUND_REQUEST");
        } catch (Exception e) {
            log.error("更新订单为退款中失败: orderId={}", orderId, e);
        }

        log.info("退款申请已提交: refundNo={}, orderId={}, amount={}", refund.getRefundNo(), orderId, amount);
    }

    /**
     * 用户提交退款/退货退款申请（完整参数版）
     * 支持退款类型、原因分类、凭证图片
     */
    @Override
    @Transactional
    public Long requestRefund(Long userId, Long orderId, BigDecimal amount, String reason,
                              Integer refundType, String refundReason, String refundImages) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }
        if (payment.getStatus() != PaymentStatusEnum.PAID.getCode()) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_STATUS_INVALID.getCode(),
                    "当前支付状态不支持退款");
        }

        Refund refund = new Refund();
        refund.setRefundNo(IdGenerator.refundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setAmount(amount);
        refund.setReason(reason != null ? reason : "用户申请退款");
        refund.setRefundType(refundType != null ? refundType : 1);
        refund.setRefundReason(refundReason);
        refund.setRefundImages(refundImages);
        refund.setStatus(0);
        // 退货退款：初始状态=待退货
        if (refundType != null && refundType == 2) {
            refund.setReturnStatus(1);
        }
        refundMapper.insert(refund);

        payment.setStatus(PaymentStatusEnum.REFUNDING.getCode());
        paymentMapper.updateById(payment);

        try {
            orderService.updateStatus(orderId,
                    OrderStatusEnum.REFUNDING.getCode(), "USER_REFUND_REQUEST");
        } catch (Exception e) {
            log.error("更新订单为退款中失败: orderId={}", orderId, e);
        }

        log.info("退款申请已提交: refundNo={}, orderId={}, amount={}, refundType={}",
                refund.getRefundNo(), orderId, amount, refundType);
        return refund.getId();
    }

    @Override
    public void submitReturnLogistics(Long refundId, String logisticsNo) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "退款单不存在");
        }
        if (refund.getRefundType() == null || refund.getRefundType() != 2) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "非退货退款单，无需填写物流");
        }
        refund.setReturnLogisticsNo(logisticsNo);
        refund.setReturnStatus(2); // 已退货
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);
        log.info("退货物流已填写: refundId={}, logisticsNo={}", refundId, logisticsNo);
    }

    @Override
    public void confirmReturnReceived(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "退款单不存在");
        }
        if (refund.getRefundType() == null || refund.getRefundType() != 2) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "非退货退款单");
        }
        refund.setReturnStatus(3); // 已收货
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);
        log.info("管理员确认收到退货: refundId={}, refundNo={}", refundId, refund.getRefundNo());
    }

    @Override
    public Map<String, Object> getRefundInfo(Long orderId) {
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, orderId)
                .orderByDesc(Refund::getCreateTime)
                .last("LIMIT 1"));
        if (refund == null) return Map.of();
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("refundId", refund.getId());
        map.put("refundNo", refund.getRefundNo());
        map.put("amount", refund.getAmount());
        map.put("reason", refund.getReason());
        map.put("refundType", refund.getRefundType());
        map.put("refundReason", refund.getRefundReason());
        map.put("refundImages", refund.getRefundImages());
        map.put("returnStatus", refund.getReturnStatus());
        map.put("returnLogisticsNo", refund.getReturnLogisticsNo());
        map.put("status", refund.getStatus());
        map.put("createTime", refund.getCreateTime());
        return map;
    }

    /**
     * 管理员审核通过 → 执行实际退款
     * <p>
     * 调用支付渠道退款 → 成功则退款单=已退款(1)、支付=已退款(3)、订单=已退款(6)
     * → 失败则退款单=已拒绝(2)、支付/订单回退
     */
    @Override
    @Transactional
    public void processRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该退款单已处理，不能重复操作");
        }

        Payment payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }

        // 调用支付渠道退款
        PaymentChannel paymentChannel = channelMap.getOrDefault(payment.getChannel(), channelMap.get("mock"));
        RefundResult refundResult = paymentChannel.refund(payment.getPaymentNo(), refund.getAmount(), refund.getReason());
        if (!refundResult.isSuccess()) {
            log.error("渠道退款失败: refundNo={}, reason={}", refund.getRefundNo(), refundResult.getFailReason());
            refund.setStatus(2); // 已拒绝
            refund.setUpdateTime(LocalDateTime.now());
            refundMapper.updateById(refund);

            // 回退支付状态 → 已支付(1)
            payment.setStatus(PaymentStatusEnum.PAID.getCode());
            paymentMapper.updateById(payment);

            // 回退订单状态 → 已收货(3)（本地调用）
            try {
                orderService.updateStatus(refund.getOrderId(),
                        OrderStatusEnum.RECEIVED.getCode(), "REFUND_FAILED");
            } catch (Exception e) {
                log.error("回退订单状态失败: orderId={}", refund.getOrderId(), e);
            }
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "退款失败: " + refundResult.getFailReason());
        }

        // 退款成功 → 更新所有状态
        refund.setStatus(1); // 已退款
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        payment.setStatus(PaymentStatusEnum.REFUNDED.getCode());
        paymentMapper.updateById(payment);

        // 更新订单状态 → 已退款(6)（本地调用，替代原 Feign + MQ 双重通知）
        try {
            orderService.updateStatus(refund.getOrderId(),
                    OrderStatusEnum.REFUNDED.getCode(), "REFUND_APPROVED");
        } catch (Exception e) {
            log.error("更新订单为已退款失败: orderId={}", refund.getOrderId(), e);
        }

        // 事务提交后发送 MQ 通知 product-service 恢复库存 + 优惠券回退
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, refund.getOrderId()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    RefundEvent refundEvent = new RefundEvent();
                    refundEvent.setOrderId(refund.getOrderId());
                    refundEvent.setRefundNo(refund.getRefundNo());
                    refundEvent.setPaymentNo(payment.getPaymentNo());
                    // 携带订单项列表，用于 product-service 恢复库存
                    List<Map<String, Object>> items = orderItems.stream().map(item -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("skuId", item.getSkuId());
                        map.put("quantity", item.getQuantity());
                        return map;
                    }).collect(Collectors.toList());
                    refundEvent.setOrderItems(items);
                    paymentProducer.sendRefundSuccess(refundEvent);
                    log.info("退款成功MQ已发送: orderId={}, refundNo={}", refund.getOrderId(), refund.getRefundNo());
                } catch (Exception e) {
                    log.error("退款成功MQ发送失败: orderId={}, error={}", refund.getOrderId(), e.getMessage());
                }
            }
        });

        log.info("退款处理成功: refundNo={}, orderId={}, amount={}", refund.getRefundNo(), refund.getOrderId(), refund.getAmount());
    }

    /**
     * 管理员拒绝退款 → 回退状态
     * <p>
     * 退款单=已拒绝(2)、支付=已支付(1)、订单=已收货(3)
     */
    @Override
    @Transactional
    public void declineRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "退款单不存在");
        }
        if (refund.getStatus() != 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该退款单已处理，不能重复操作");
        }

        refund.setStatus(2); // 已拒绝
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        // 回退支付状态 → 已支付(1)
        Payment payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment != null) {
            payment.setStatus(PaymentStatusEnum.PAID.getCode());
            paymentMapper.updateById(payment);
        }

        // 回退订单状态 → 已收货(3)（本地调用）
        try {
            orderService.updateStatus(refund.getOrderId(),
                    OrderStatusEnum.RECEIVED.getCode(), "REFUND_DECLINED");
        } catch (Exception e) {
            log.error("回退订单状态失败: orderId={}", refund.getOrderId(), e);
        }

        log.info("退款申请已拒绝: refundNo={}", refund.getRefundNo());
    }

    @Override
    public PageResult<Refund> refundPage(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>()
                .eq(status != null, Refund::getStatus, status)
                .orderByDesc(Refund::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Refund> p =
                refundMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords());
    }

    @Override
    public Integer getStatus(String orderNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo));
        return payment != null ? payment.getStatus() : null;
    }

    @Override
    @Transactional
    public void simulatePayment(String orderNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }
        if (payment.getStatus() != PaymentStatusEnum.PENDING.getCode()) {
            log.warn("订单已支付或已退款: orderNo={}", orderNo);
            return;
        }

        String transactionNo = "SIM_TXN_" + System.currentTimeMillis();
        handleCallback(payment.getChannel(), payment.getPaymentNo(), transactionNo);
        log.info("模拟支付成功: orderNo={}, paymentNo={}", orderNo, payment.getPaymentNo());
    }
}