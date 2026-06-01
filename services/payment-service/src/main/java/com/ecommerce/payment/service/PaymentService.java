package com.ecommerce.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    /** 创建支付单并获取支付链接，返回 {paymentNo, payUrl} */
    Map<String, String> createPayment(Long userId, Long orderId, String orderNo, BigDecimal amount, String channel);

    void handleCallback(String channel, String paymentNo, String transactionNo);

    /** 模拟支付成功（用于前端测试） */
    void simulatePayment(String orderNo);

    void refund(Long userId, Long orderId, BigDecimal amount, String reason);

    Integer getStatus(String orderNo);
}
