package com.ecommerce.order.feign;

import com.ecommerce.order.model.dto.StockOperationDTO;
import com.ecommerce.order.feign.fallback.InventoryFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 库存服务 Feign 客户端 — 库存锁定与释放
 * <p>
 * fallbackFactory：调用失败时触发降级，避免订单创建链路中断
 */
@FeignClient(name = "inventory-service", path = "/api/inventory",
        fallbackFactory = InventoryFeignFallbackFactory.class)
public interface InventoryFeignClient {

    @PostMapping("/lock")
    void lockStock(@RequestBody List<StockOperationDTO> items);

    @PostMapping("/release")
    void releaseStock(@RequestBody List<StockOperationDTO> items);
}
