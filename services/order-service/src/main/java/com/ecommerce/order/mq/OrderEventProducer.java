package com.ecommerce.order.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import com.ecommerce.order.cart.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单事件生产者 — 基于 Spring Cloud Stream StreamBridge 发送订单事件
 * <p>
 * - orderCreated: 下单后本地调用 CartService 清除已购商品（cart 已合并到 order-service）
 * - orderTimeout: 下单后延迟30分钟检查超时未支付，触发自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer extends AbstractMqProducer {

    private final CartService cartService;

    /** 订单创建成功后，本地调用购物车服务清除已下单商品 */
    public void sendOrderCreated(Long userId, Long orderId, String orderNo) {
        try {
            cartService.clearSelected(userId);
            log.info("订单创建后清除购物车: userId={}, orderId={}", userId, orderId);
        } catch (Exception e) {
            log.error("清除购物车失败: userId={}, orderId={}", userId, orderId, e);
        }
    }

    /**
     * 发送订单超时检查延迟消息 —— 基于 RabbitMQ 原生 TTL + DLX，不依赖任何插件。
     * <p>
     * 链路：设置 expiration=TTL 毫秒 → 投递到 order-timeout-delay 交换机 → 进入 order-timeout.delay 队列（无消费者）
     * → TTL 到期经 DLX 转发到 order-timeout 业务交换机 → OrderTimeoutConsumer 消费，
     * 若订单未支付则自动取消并释放库存。默认延迟 30 分钟（1800000 ms）。
     *
     * @param orderId 订单ID
     * @param delayMs 延迟毫秒数
     */
    public void sendOrderTimeoutCheck(Long orderId, long delayMs) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        // 发往延迟 binding：TTL 在 delay 队列到期后 DLX 转发到真正的业务交换机
        sendDelay("orderTimeoutDelay-out-0", payload, delayMs);
    }
}