package com.ecommerce.payment.service;

import java.math.BigDecimal;

public interface PaymentService {

    String createPayment(Long userId, Long orderId, String orderNo, BigDecimal amount, String channel);

    void handleCallback(String channel, String paymentNo, String transactionNo);

    void refund(Long userId, Long orderId, BigDecimal amount, String reason);

    Integer getStatus(String orderNo);
}
