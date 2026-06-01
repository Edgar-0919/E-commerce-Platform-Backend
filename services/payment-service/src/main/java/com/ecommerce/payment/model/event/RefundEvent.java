package com.ecommerce.payment.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款成功事件 — 通过 Spring Cloud Stream 发送，通知 order-service 更新订单状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundEvent {

    /** 订单ID */
    private Long orderId;
    /** 退款单号 */
    private String refundNo;
    /** 关联支付单号 */
    private String paymentNo;
}