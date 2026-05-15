package com.ecommerce.core.constant;

import lombok.Getter;

/**
 * 错误码枚举类
 * 采用分段管理策略，各业务模块独立分配区间，便于问题定位和扩展
 * 
 * 错误码分段规则：
 * <ul>
 *   <li>200 - 成功</li>
 *   <li>400-599 - HTTP状态码映射</li>
 *   <li>10000-19999 - 用户模块</li>
 *   <li>20000-29999 - 商品模块</li>
 *   <li>30000-39999 - 订单模块</li>
 *   <li>40000-49999 - 支付模块</li>
 *   <li>50000-59999 - 库存模块</li>
 *   <li>60000-69999 - 购物车模块</li>
 *   <li>80000-89999 - 营销模块</li>
 * </ul>
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数错误"),
    SYSTEM_ERROR(500, "系统异常"),

    // 用户相关 10000-19999
    USER_NOT_EXIST(10001, "用户不存在"),
    PASSWORD_ERROR(10002, "密码错误"),
    USERNAME_EXIST(10003, "用户名已存在"),
    PHONE_EXIST(10004, "手机号已注册"),
    TOKEN_EXPIRED(10005, "Token已过期"),
    TOKEN_INVALID(10006, "Token无效"),

    // 商品相关 20000-29999
    PRODUCT_NOT_EXIST(20001, "商品不存在"),
    CATEGORY_NOT_EXIST(20002, "分类不存在"),
    SKU_NOT_EXIST(20003, "SKU不存在"),
    BRAND_NOT_EXIST(20004, "品牌不存在"),

    // 订单相关 30000-39999
    ORDER_NOT_EXIST(30001, "订单不存在"),
    ORDER_STATUS_ERROR(30002, "订单状态异常"),
    ORDER_CANNOT_CANCEL(30003, "订单无法取消"),

    // 支付相关 40000-49999
    PAYMENT_NOT_EXIST(40001, "支付记录不存在"),
    PAYMENT_AMOUNT_ERROR(40002, "支付金额不匹配"),
    REFUND_AMOUNT_ERROR(40003, "退款金额超出"),

    // 库存相关 50000-59999
    STOCK_INSUFFICIENT(50001, "库存不足"),
    STOCK_LOCK_FAILED(50002, "库存锁定失败"),

    // 购物车相关 60000-69999
    CART_ITEM_NOT_EXIST(60001, "购物车项不存在"),

    // 营销相关 80000-89999
    COUPON_EXPIRED(80001, "优惠券已过期"),
    COUPON_NOT_AVAILABLE(80002, "优惠券不可用"),
    COUPON_STOCK_INSUFFICIENT(80003, "优惠券已领完"),
    SECKILL_NOT_START(80004, "秒杀未开始"),
    SECKILL_SOLD_OUT(80005, "秒杀已售罄");

    private final int code;
    private final String message;

    ResultCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
