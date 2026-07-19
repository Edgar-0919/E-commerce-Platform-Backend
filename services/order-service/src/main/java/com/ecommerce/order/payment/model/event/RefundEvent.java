package com.ecommerce.order.payment.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 退款成功事件 — 退款成功后在 order-service 内部本地调用更新订单状态
 * 同时通过 MQ 通知 product-service 恢复库存
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
    /** 订单项列表（用于 product-service 恢复库存） */
    private List<Map<String, Object>> orderItems;
}