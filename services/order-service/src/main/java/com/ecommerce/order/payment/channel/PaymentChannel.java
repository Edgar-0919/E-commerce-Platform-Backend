package com.ecommerce.order.payment.channel;

import java.math.BigDecimal;

/**
 * 支付渠道抽象接口 — 策略模式解耦支付渠道（支付宝/微信支付）
 * <p>
 * 各渠道实现类负责与第三方支付 API 交互：
 * - 统一下单（获取支付链接/二维码）
 * - 支付回调验签与解析
 * - 退款发起
 */
public interface PaymentChannel {

    /**
     * 统一下单，返回支付跳转链接（web端返回支付页URL, 移动端返回唤起链接）
     */
    String createPaymentUrl(String orderNo, BigDecimal amount, String subject);

    /**
     * 支付回调验签与解析
     */
    CallbackResult handleCallback(java.util.Map<String, String> params);

    /**
     * 发起退款
     */
    RefundResult refund(String paymentNo, BigDecimal amount, String reason);

    /** 渠道标识，如 "alipay"、"wechat" */
    String channelName();
}