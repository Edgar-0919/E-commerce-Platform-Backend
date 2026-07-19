package com.ecommerce.order.mq;

import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 订单超时取消消费者
 * <p>
 * 消费延迟消息 order-timeout，检查订单是否已支付，
 * 若仍为待支付状态则自动取消并释放库存。
 * 消费失败时抛出异常触发 MQ 重试（max-attempts）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Bean
    public Consumer<Map<String, Object>> orderTimeout() {
        return message -> {
            Long orderId = toLong(message.get("orderId"));
            if (orderId == null) {
                log.warn("超时检查消息缺少orderId: {}", message);
                return;
            }

            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.warn("超时检查-订单不存在: orderId={}", orderId);
                return;
            }

            // 仅待支付状态执行取消，已支付/已取消则跳过
            if (order.getStatus() != OrderStatusEnum.PENDING_PAY.getCode()) {
                log.info("订单状态已变更，跳过超时取消: orderId={}, status={}", orderId, order.getStatus());
                return;
            }

            try {
                orderService.cancel(order.getUserId(), orderId);
                log.info("订单超时自动取消成功: orderId={}", orderId);
            } catch (Exception e) {
                log.error("订单超时自动取消失败: orderId={}", orderId, e);
                // 抛出异常触发 MQ 重试机制
                throw new RuntimeException("订单超时自动取消失败, orderId=" + orderId, e);
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