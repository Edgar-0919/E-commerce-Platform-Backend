package com.ecommerce.product.inventory.feign;

import com.ecommerce.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 订单服务 Feign 客户端 — 对账任务查询订单状态
 */
@FeignClient(name = "order-service", path = "/api/order")
public interface OrderFeignClient {

    /** 查询订单详情（用于对账任务判断订单是否已支付/已取消） */
    @GetMapping("/{id}")
    Result<Map<String, Object>> getById(@PathVariable("id") Long id);
}