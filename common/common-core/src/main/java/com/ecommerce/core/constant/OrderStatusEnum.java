package com.ecommerce.core.constant;

import lombok.Getter;

/**
 * 订单状态枚举
 * 订单状态流转：
 * PENDING_PAY → PENDING_DELIVER → DELIVERED → RECEIVED
 * PENDING_PAY → CANCELLED
 * PAID → REFUNDING → REFUNDED
 */
@Getter
public enum OrderStatusEnum {

    PENDING_PAY(0, "待支付"),
    PENDING_DELIVER(1, "待发货"),
    DELIVERED(2, "已发货"),
    RECEIVED(3, "已收货"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatusEnum of(int code) {
        for (OrderStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
