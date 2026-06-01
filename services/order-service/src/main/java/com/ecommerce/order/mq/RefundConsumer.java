package com.ecommerce.order.mq;

import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 订单消息消费者 — 接收退款成功事件，更新订单状态
 * <p>
 * 支付回调时通过 Feign 同步更新状态作为主路径，
 * 此消费者作为补偿/重试机制，保证退款状态最终一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundConsumer {

    private final OrderService orderService;

    @Bean
    public Consumer<Map<String, Object>> refundSuccess() {
        return message -> {
            Long orderId = toLong(message.get("orderId"));
            String refundNo = (String) message.get("refundNo");
            log.info("收到退款成功事件: orderId={}, refundNo={}", orderId, refundNo);

            if (orderId == null) {
                log.error("退款事件缺少orderId: {}", message);
                return;
            }

            // 幂等更新：仅未退款状态下才更新，防止重复操作
            try {
                orderService.updateStatus(orderId,
                        OrderStatusEnum.REFUNDED.getCode(), "MQ_REFUND_CALLBACK");
                log.info("退款状态已同步: orderId={}, status=REFUNDED", orderId);
            } catch (Exception e) {
                log.error("退款状态同步失败: orderId={}, error={}", orderId, e.getMessage());
                // 抛出异常触发 MQ 重试
                throw new RuntimeException("退款状态更新失败, orderId=" + orderId, e);
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