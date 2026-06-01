package com.ecommerce.cart.mq;

import com.ecommerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 订单创建消息消费者 — 下单成功后清除购物车中已提交的商品
 * <p>
 * 监听 destination: order-created，消费者组: cart-service
 * 实现下单后购物车自动清除的最终一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final CartService cartService;

    @Bean
    public Consumer<Map<String, Object>> orderCreated() {
        return message -> {
            Long userId = toLong(message.get("userId"));
            if (userId == null) {
                log.error("订单创建事件缺少userId: {}", message);
                return;
            }

            try {
                cartService.clearSelected(userId);
                log.info("购物车已清除选中商品: userId={}", userId);
            } catch (Exception e) {
                log.error("清除购物车选中商品失败: userId={}, error={}", userId, e.getMessage());
                throw new RuntimeException("清除购物车失败, userId=" + userId, e);
            }
        };
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}