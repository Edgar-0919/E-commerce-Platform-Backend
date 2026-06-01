package com.ecommerce.payment.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付成功事件 — 通过 Spring Cloud Stream 发送，触发下游库存扣减和订单状态同步
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    /** 订单ID */
    private Long orderId;
    /** 支付单号 */
    private String paymentNo;
}