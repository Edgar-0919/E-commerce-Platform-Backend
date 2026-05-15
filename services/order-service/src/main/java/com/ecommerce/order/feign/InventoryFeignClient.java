package com.ecommerce.order.feign;

import com.ecommerce.inventory.model.dto.StockOperationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// 通过 Nacos 服务发现调用 inventory-service，FeignHeaderInterceptor 自动透传用户上下文
@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryFeignClient {

    @PostMapping("/lock")
    void lockStock(@RequestBody List<StockOperationDTO> items);

    @PostMapping("/release")
    void releaseStock(@RequestBody List<StockOperationDTO> items);
}
