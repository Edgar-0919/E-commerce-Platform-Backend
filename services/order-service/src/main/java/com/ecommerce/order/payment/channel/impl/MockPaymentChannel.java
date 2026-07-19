package com.ecommerce.order.payment.channel.impl;

import com.ecommerce.order.payment.channel.CallbackResult;
import com.ecommerce.order.payment.channel.PaymentChannel;
import com.ecommerce.order.payment.channel.RefundResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 模拟支付渠道 — 开发/测试环境使用，所有支付操作直接成功
 */
@Slf4j
@Component
public class MockPaymentChannel implements PaymentChannel {

    @Override
    public String createPaymentUrl(String orderNo, BigDecimal amount, String subject) {
        log.info("[Mock] 创建支付: orderNo={}, amount={}, subject={}", orderNo, amount, subject);
        return "/mock-pay?orderNo=" + orderNo + "&amount=" + amount.toPlainString();
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> params) {
        String orderNo = params.getOrDefault("orderNo", "unknown");
        String transactionNo = "MOCK_TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[Mock] 支付回调: orderNo={}, transactionNo={}", orderNo, transactionNo);
        return CallbackResult.ok(transactionNo);
    }

    @Override
    public RefundResult refund(String paymentNo, BigDecimal amount, String reason) {
        String refundTransactionNo = "MOCK_REFUND_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[Mock] 退款: paymentNo={}, amount={}, reason={}, refundTransactionNo={}",
                paymentNo, amount, reason, refundTransactionNo);
        return RefundResult.ok(refundTransactionNo);
    }

    @Override
    public String channelName() {
        return "mock";
    }
}