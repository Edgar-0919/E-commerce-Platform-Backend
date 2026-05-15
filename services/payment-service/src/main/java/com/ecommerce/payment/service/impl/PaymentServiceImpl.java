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
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PaymentLogMapper paymentLogMapper;

    @Override
    @Transactional
    public String createPayment(Long userId, Long orderId, String orderNo, BigDecimal amount, String channel) {
        // 幂等校验：同一订单重复创建支付单时直接返回已有的支付单号
        Payment existing = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (existing != null) {
            return existing.getPaymentNo();
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

        log.info("支付单创建: paymentNo={}, orderNo={}, channel={}, amount={}",
                payment.getPaymentNo(), orderNo, channel, amount);

        // TODO: 对接第三方支付API（支付宝/微信支付），目前为模拟实现

        return payment.getPaymentNo();
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

        payment.setStatus(PaymentStatusEnum.PAID.getCode());
        payment.setPaidTime(LocalDateTime.now());
        payment.setTransactionNo(transactionNo);
        paymentMapper.updateById(payment);

        // Record log
        PaymentLog logEntry = new PaymentLog();
        logEntry.setPaymentId(payment.getId());
        logEntry.setType("CALLBACK");
        logEntry.setResponse("channel=" + channel + ", transactionNo=" + transactionNo);
        logEntry.setCreateTime(LocalDateTime.now());
        paymentLogMapper.insert(logEntry);

        log.info("支付回调处理成功: paymentNo={}, channel={}, transactionNo={}", paymentNo, channel, transactionNo);

        // TODO: Send RocketMQ message payment-success → inventory/order service
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

        // TODO: 对接第三方退款API，目前为模拟退款成功
        refund.setStatus(1); // refunded
        refundMapper.updateById(refund);

        payment.setStatus(PaymentStatusEnum.REFUNDED.getCode());
        paymentMapper.updateById(payment);

        log.info("退款处理成功: refundNo={}, orderId={}, amount={}", refund.getRefundNo(), orderId, amount);

        // TODO: Send RocketMQ message refund-success → order service
    }

    @Override
    public Integer getStatus(String orderNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo));
        return payment != null ? payment.getStatus() : null;
    }
}
