package com.ecommerce.payment.feign.fallback;

import com.ecommerce.core.model.Result;
import com.ecommerce.payment.feign.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单服务 Feign 降级工厂 — 当 order-service 不可用时不阻塞支付流程，
 * 支付状态变更将通过 MQ 消息异步重试
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {

    @Override
    public OrderFeignClient create(Throwable cause) {
        log.error("[支付] 订单服务Feign调用失败 — {}", cause.getMessage());
        return (orderId, body) -> {
            log.warn("[支付] 订单状态更新降级 — orderId={}, 将通过MQ异步重试", orderId);
            return Result.fail(503, "订单状态同步失败，将通过消息队列重试");
        };
    }
}