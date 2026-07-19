package com.ecommerce.order.payment.channel.impl;

import com.ecommerce.order.payment.channel.CallbackResult;
import com.ecommerce.order.payment.channel.PaymentChannel;
import com.ecommerce.order.payment.channel.RefundResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付渠道 — 对接支付宝开放平台 API
 * <p>
 * 当前为骨架实现，待配置商户凭证后替换 MockPaymentChannel。
 */
@Slf4j
// @Component
// @ConditionalOnProperty(name = "payment.channel", havingValue = "alipay")
public class AlipayPaymentChannel implements PaymentChannel {

    @Override
    public String createPaymentUrl(String orderNo, BigDecimal amount, String subject) {
        log.warn("[Alipay] createPaymentUrl 未接入真实 API: orderNo={}, amount={}", orderNo, amount);
        throw new UnsupportedOperationException("支付宝支付未接入，请配置商户凭证后启用");
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> params) {
        log.warn("[Alipay] handleCallback 未接入真实 API");
        throw new UnsupportedOperationException("支付宝回调验证未接入");
    }

    @Override
    public RefundResult refund(String paymentNo, BigDecimal amount, String reason) {
        log.warn("[Alipay] refund 未接入真实 API: paymentNo={}, amount={}", paymentNo, amount);
        throw new UnsupportedOperationException("支付宝退款未接入");
    }

    @Override
    public String channelName() {
        return "alipay";
    }
}