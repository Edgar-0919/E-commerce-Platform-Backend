package com.ecommerce.core.constant;

import lombok.Getter;

/**
 * 支付状态枚举
 * 支付状态流转：
 * PENDING → PAID → REFUNDING → REFUNDED
 * PENDING → CLOSED
 */
@Getter
public enum PaymentStatusEnum {

    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDING(2, "退款中"),
    REFUNDED(3, "已退款"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    PaymentStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
