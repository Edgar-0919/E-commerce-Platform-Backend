package com.ecommerce.payment.channel;

import java.math.BigDecimal;

/**
 * 支付渠道抽象接口 — 策略模式解耦支付渠道（支付宝/微信支付）
 * <p>
 * 各渠道实现类负责与第三方支付 API 交互：
 * - 统一下单（获取支付链接/二维码）
 * - 支付回调验签与解析
 * - 退款发起
 * <p>
 * 接入真实渠道时（如 Alipay SDK），只需新增一个实现类并注册为 Spring Bean，
 * 无需修改 PaymentServiceImpl 的业务逻辑。
 */
public interface PaymentChannel {

    /**
     * 统一下单，返回支付跳转链接（web端返回支付页URL, 移动端返回唤起链接）
     *
     * @param orderNo  订单编号
     * @param amount   支付金额（元）
     * @param subject  商品描述
     * @return 支付链接，调用方重定向到该链接完成支付
     */
    String createPaymentUrl(String orderNo, BigDecimal amount, String subject);

    /**
     * 支付回调验签与解析
     *
     * @param params 第三方回调参数 Map（支付宝为 request params，微信为 XML/JSON body）
     * @return 回调结果，包含验签结果和第三方交易流水号
     */
    CallbackResult handleCallback(java.util.Map<String, String> params);

    /**
     * 发起退款
     *
     * @param paymentNo 我方支付单号
     * @param amount    退款金额（元）
     * @param reason    退款原因
     * @return 退款结果
     */
    RefundResult refund(String paymentNo, BigDecimal amount, String reason);

    /** 渠道标识，如 "alipay"、"wechat" */
    String channelName();
}