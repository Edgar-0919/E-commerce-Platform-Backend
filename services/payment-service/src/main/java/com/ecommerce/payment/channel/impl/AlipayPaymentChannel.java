package com.ecommerce.payment.channel.impl;

import com.ecommerce.payment.channel.CallbackResult;
import com.ecommerce.payment.channel.PaymentChannel;
import com.ecommerce.payment.channel.RefundResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付渠道 — 对接支付宝开放平台 API
 * <p>
 * 接入步骤：
 * 1. 引入 alipay-sdk-java 依赖
 * 2. 配置 AlipayConfig（appId, privateKey, alipayPublicKey, gatewayUrl）
 * 3. 实现 createPaymentUrl → AlipayTradePagePayRequest（PC 网页支付）
 * 4. 实现 handleCallback → 验签 + 解析 notify 参数
 * 5. 实现 refund → AlipayTradeRefundRequest
 * <p>
 * 当前为骨架实现，待配置商户凭证后替换 MockPaymentChannel。
 * 激活方式：application.yml 中设置 payment.channel=alipay 或通过 @ConditionalOnProperty 切换。
 */
@Slf4j
// @Component
// @ConditionalOnProperty(name = "payment.channel", havingValue = "alipay")
public class AlipayPaymentChannel implements PaymentChannel {

    @Override
    public String createPaymentUrl(String orderNo, BigDecimal amount, String subject) {
        /*
         * 接入示例（需引入 alipay-sdk-java）：
         *
         * AlipayClient alipayClient = new DefaultAlipayClient(
         *     alipayConfig.getGatewayUrl(),
         *     alipayConfig.getAppId(),
         *     alipayConfig.getPrivateKey(),
         *     "json", "UTF-8",
         *     alipayConfig.getAlipayPublicKey(), "RSA2"
         * );
         *
         * AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
         * request.setNotifyUrl(alipayConfig.getNotifyUrl());
         * request.setReturnUrl(alipayConfig.getReturnUrl());
         *
         * JSONObject bizContent = new JSONObject();
         * bizContent.put("out_trade_no", orderNo);
         * bizContent.put("total_amount", amount.toPlainString());
         * bizContent.put("subject", subject);
         * bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
         * request.setBizContent(bizContent.toString());
         *
         * String form = alipayClient.pageExecute(request).getBody();
         * return form; // 完整的 HTML form，前端渲染后跳转支付宝收银台
         */

        log.warn("[Alipay] createPaymentUrl 未接入真实 API: orderNo={}, amount={}", orderNo, amount);
        throw new UnsupportedOperationException("支付宝支付未接入，请配置商户凭证后启用");
    }

    @Override
    public CallbackResult handleCallback(Map<String, String> params) {
        /*
         * 接入示例：
         * boolean verified = AlipaySignature.rsaCheckV1(
         *     params, alipayConfig.getAlipayPublicKey(), "UTF-8", "RSA2"
         * );
         * if (!verified) return CallbackResult.fail("验签失败");
         *
         * String tradeStatus = params.get("trade_status");
         * if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
         *     return CallbackResult.fail("交易未完成: " + tradeStatus);
         * }
         * return CallbackResult.ok(params.get("trade_no"));
         */

        log.warn("[Alipay] handleCallback 未接入真实 API");
        throw new UnsupportedOperationException("支付宝回调验证未接入");
    }

    @Override
    public RefundResult refund(String paymentNo, BigDecimal amount, String reason) {
        /*
         * 接入示例：
         * AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
         * JSONObject bizContent = new JSONObject();
         * bizContent.put("out_trade_no", paymentNo);
         * bizContent.put("refund_amount", amount.toPlainString());
         * bizContent.put("refund_reason", reason);
         * request.setBizContent(bizContent.toString());
         *
         * AlipayTradeRefundResponse response = alipayClient.execute(request);
         * if (response.isSuccess()) {
         *     return RefundResult.ok(response.getTradeNo());
         * }
         * return RefundResult.fail(response.getSubMsg());
         */

        log.warn("[Alipay] refund 未接入真实 API: paymentNo={}, amount={}", paymentNo, amount);
        throw new UnsupportedOperationException("支付宝退款未接入");
    }

    @Override
    public String channelName() {
        return "alipay";
    }
}