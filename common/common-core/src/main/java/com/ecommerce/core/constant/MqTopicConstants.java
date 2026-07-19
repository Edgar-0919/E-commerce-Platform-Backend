package com.ecommerce.core.constant;

/**
 * Spring Cloud Stream Binding 常量接口
 * 统一管理消息 binding 和 ConsumerGroup：
 * 事件 binding：
 * - payment-success: 支付成功事件
 * - refund-success: 退款成功事件
 * - order-created: 订单创建事件
 * - order-cancelled: 订单取消事件
 * - product-status-change: 商品状态变更事件
 * 消费者分组：按服务划分，每个服务独立消费组
 */
public interface MqTopicConstants {

    // ---- 事件 Topic ----
    String PAYMENT_SUCCESS = "payment-success";       // inventory-service, order-service 消费
    String REFUND_SUCCESS = "refund-success";         // order-service 消费
    String ORDER_CREATED = "order-created";           // cart-service 消费（清空已购）
    String ORDER_CANCELLED = "order-cancelled";       // inventory-service 消费（释放库存）
    String PRODUCT_STATUS_CHANGE = "product-status-change"; // search-service 消费（同步ES）
    String STOCK_DEDUCT = "stock-deduct";
    String COUPON_USED = "coupon-used";

    // ---- 消费组 ----
    String CONSUMER_GROUP_INVENTORY = "inventory-consumer-group";
    String CONSUMER_GROUP_ORDER = "order-consumer-group";
    String CONSUMER_GROUP_CART = "cart-consumer-group";
    String CONSUMER_GROUP_SEARCH = "search-consumer-group";
}
