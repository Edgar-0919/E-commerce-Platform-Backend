package com.ecommerce.payment.feign;

import com.ecommerce.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 订单服务 Feign 客户端 — 支付回调后更新订单状态
 * <p>
 * 通过 Nacos 服务发现调用 order-service 的内部接口，
 * FeignHeaderInterceptor 自动透传用户上下文。
 */
@FeignClient(name = "order-service", path = "/api/internal/order")
public interface OrderFeignClient {

    /** 更新订单状态（支付成功→已支付，退款成功→已退款） */
    @PutMapping("/{id}/status")
    Result<Void> updateStatus(@PathVariable("id") Long orderId, @RequestBody Map<String, Object> body);
}