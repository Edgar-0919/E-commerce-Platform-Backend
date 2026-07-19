package com.ecommerce.order.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 支付回调结果
 */
@Data
@AllArgsConstructor
public class CallbackResult {

    /** 是否验证通过 */
    private boolean success;
    /** 第三方交易流水号 */
    private String transactionNo;
    /** 失败原因（验签失败时非空） */
    private String failReason;

    public static CallbackResult ok(String transactionNo) {
        return new CallbackResult(true, transactionNo, null);
    }

    public static CallbackResult fail(String reason) {
        return new CallbackResult(false, null, reason);
    }
}