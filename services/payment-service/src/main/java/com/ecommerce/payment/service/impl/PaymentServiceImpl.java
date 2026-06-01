package com.ecommerce.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.PaymentStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.util.IdGenerator;
import com.ecommerce.payment.mapper.PaymentLogMapper;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.model.entity.Payment;
import com.ecommerce.payment.model.entity.PaymentLog;
import com.ecommerce.payment.model.entity.Refund;
import com.ecommerce.payment.model.event.PaymentEvent;
import com.ecommerce.payment.model.event.RefundEvent;
import com.ecommerce.payment.mq.PaymentProducer;
import com.ecommerce.payment.channel.PaymentChannel;
import com.ecommerce.payment.channel.CallbackResult;
import com.ecommerce.payment.channel.RefundResult;
import com.ecommerce.payment.service.PaymentService;
import com.ecommerce.payment.feign.OrderFeignClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OrderFeignClient orderFeignClient;
    private final PaymentProducer paymentProducer;
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

        // 通过 Feign 调用 order-service 更新订单状态为"已支付"
        try {
            orderFeignClient.updateStatus(payment.getOrderId(),
                    Map.of("status", 2, "operator", "PAYMENT_CALLBACK"));
            log.info("订单状态已更新: orderId={}, status=PAID", payment.getOrderId());
        } catch (Exception e) {
            log.error("Feign调用-更新订单状态失败: orderId={}", payment.getOrderId(), e);
            // 支付已记录成功，订单状态更新失败通过补偿任务重试
        }

        // 发送 Spring Cloud Stream 消息 payment-success → inventory-service 扣减库存
        PaymentEvent event = new PaymentEvent(payment.getOrderId(), payment.getPaymentNo());
        paymentProducer.sendPaymentSuccess(event);
    }

    @Override
    @Transactional
    public void refund(Long userId, Long orderId, BigDecimal amount, String reason) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_NOT_EXIST);
        }
        if (payment.getStatus() != PaymentStatusEnum.PAID.getCode()) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_AMOUNT_ERROR);
        }

        payment.setStatus(PaymentStatusEnum.REFUNDING.getCode());
        paymentMapper.updateById(payment);

        Refund refund = new Refund();
        refund.setRefundNo(IdGenerator.refundNo());
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setStatus(0); // processing
        refundMapper.insert(refund);

        // 通过支付渠道发起退款
        PaymentChannel paymentChannel = channelMap.getOrDefault(payment.getChannel(), channelMap.get("mock"));
        RefundResult refundResult = paymentChannel.refund(payment.getPaymentNo(), amount, reason);
        if (!refundResult.isSuccess()) {
            log.error("渠道退款失败: refundNo={}, reason={}", refund.getRefundNo(), refundResult.getFailReason());
            refund.setStatus(2); // rejected
            refundMapper.updateById(refund);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "退款失败: " + refundResult.getFailReason());
        }

        refund.setStatus(1); // refunded
        refundMapper.updateById(refund);

        payment.setStatus(PaymentStatusEnum.REFUNDED.getCode());
        paymentMapper.updateById(payment);

        log.info("退款处理成功: refundNo={}, orderId={}, amount={}", refund.getRefundNo(), orderId, amount);

        // 通过 Feign 调用 order-service 更新订单状态为"已退款"
        try {
            orderFeignClient.updateStatus(orderId,
                    Map.of("status", 7, "operator", "REFUND_CALLBACK"));
            log.info("订单状态已更新: orderId={}, status=REFUNDED", orderId);
        } catch (Exception e) {
            log.error("Feign调用-更新退款状态失败: orderId={}", orderId, e);
        }

        // 发送 Spring Cloud Stream 消息 refund-success → order-service
        paymentProducer.sendRefundSuccess(new RefundEvent(orderId, refund.getRefundNo(), payment.getPaymentNo()));
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
