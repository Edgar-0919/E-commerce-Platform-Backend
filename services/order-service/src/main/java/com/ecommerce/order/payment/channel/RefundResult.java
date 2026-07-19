package com.ecommerce.order.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 退款结果
 */
@Data
@AllArgsConstructor
public class RefundResult {

    /** 是否退款成功 */
    private boolean success;
    /** 第三方退款流水号 */
    private String refundTransactionNo;
    /** 失败原因 */
    private String failReason;

    public static RefundResult ok(String refundTransactionNo) {
        return new RefundResult(true, refundTransactionNo, null);
    }

    public static RefundResult fail(String reason) {
        return new RefundResult(false, null, reason);
    }
}