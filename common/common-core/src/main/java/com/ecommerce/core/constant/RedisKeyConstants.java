package com.ecommerce.core.constant;

/**
 * Redis Key常量接口
 * 统一管理Redis缓存键名，按业务域分类：
 * - 用户域（token、用户信息）
 * - 库存域（可用库存、锁定库存）
 * - 幂等控制（订单、支付）
 * - 业务缓存（购物车）
 * - 营销域（优惠券、秒杀）
 */
public interface RedisKeyConstants {

    // ---- 用户域 ----
    String USER_TOKEN_PREFIX = "user:token:";
    String USER_INFO_PREFIX = "user:info:";
    // ---- 库存域 ----
    // 可用库存（由Lua脚本原子操作）
    String STOCK_PREFIX = "stock:";
    // 锁定库存（下单锁定、支付后扣除、取消后释放）
    String STOCK_LOCK_PREFIX = "stock:lock:";
    // ---- 幂等控制 ----
    String ORDER_IDEMPOTENT_PREFIX = "order:idempotent:";
    String PAYMENT_IDEMPOTENT_PREFIX = "payment:idempotent:";
    // ---- 业务缓存 ----
    String CART_PREFIX = "cart:";
    // ---- 营销域 ----
    String COUPON_STOCK_PREFIX = "coupon:stock:";

    static String userTokenKey(Long userId) {
        return USER_TOKEN_PREFIX + userId;
    }

    static String userInfoKey(Long userId) {
        return USER_INFO_PREFIX + userId;
    }

    static String stockKey(Long skuId) {
        return STOCK_PREFIX + skuId;
    }

    static String orderIdempotentKey(String orderNo) {
        return ORDER_IDEMPOTENT_PREFIX + orderNo;
    }

    static String paymentIdempotentKey(String paymentNo) {
        return PAYMENT_IDEMPOTENT_PREFIX + paymentNo;
    }

    static String cartKey(Long userId) {
        return CART_PREFIX + userId;
    }
}
