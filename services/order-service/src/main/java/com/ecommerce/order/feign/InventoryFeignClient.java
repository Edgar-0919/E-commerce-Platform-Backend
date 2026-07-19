package com.ecommerce.order.feign;

import com.ecommerce.order.model.dto.StockOperationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 库存服务 Feign 客户端 — 库存锁定与释放
 * 注意：inventory-service 已合并到 product-service，服务名指向 product-service
 */
@FeignClient(name = "product-service", contextId = "order-inventory", path = "/api/inventory")
public interface InventoryFeignClient {

    @PostMapping("/lock")
    void lockStock(@RequestBody List<StockOperationDTO> items);

    @PostMapping("/release")
    void releaseStock(@RequestBody List<StockOperationDTO> items);
}
