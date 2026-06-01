package com.ecommerce.payment.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.payment.feign.fallback.OrderFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 订单服务 Feign 客户端 — 支付回调后更新订单状态
 * <p>
 * fallbackFactory：调用失败时触发降级，支付状态同步失败将通过 MQ 异步重试
 */
@FeignClient(name = "order-service", path = "/api/internal/order",
        fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {

    @PutMapping("/{id}/status")
    Result<Void> updateStatus(@PathVariable("id") Long orderId, @RequestBody Map<String, Object> body);
}