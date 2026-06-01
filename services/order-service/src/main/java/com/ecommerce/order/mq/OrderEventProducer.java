package com.ecommerce.order.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单事件生产者 — 基于 Spring Cloud Stream StreamBridge 发送订单创建事件
 * <p>
 * 发送 order-created 事件给 cart-service，触发购物车清除。
 * 事件在订单创建成功后由 OrderServiceImpl 调用。
 */
@Slf4j
@Component
public class OrderEventProducer extends AbstractMqProducer {

    /** 订单创建成功后，通知购物车服务清除已下单商品 */
    public void sendOrderCreated(Long userId, Long orderId, String orderNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("orderId", orderId);
        payload.put("orderNo", orderNo);
        send("orderCreated-out-0", payload);
    }
}