package com.ecommerce.order.feign.fallback;

import com.ecommerce.order.model.dto.StockOperationDTO;
import com.ecommerce.order.feign.InventoryFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存服务 Feign 降级工厂 — 当 inventory-service 不可用时抛出异常回滚事务
 */
@Slf4j
@Component
public class InventoryFeignFallbackFactory implements FallbackFactory<InventoryFeignClient> {

    @Override
    public InventoryFeignClient create(Throwable cause) {
        log.error("[订单] 库存服务Feign调用失败 — {}", cause.getMessage());
        return new InventoryFeignClient() {
            @Override
            public void lockStock(List<StockOperationDTO> items) {
                throw new RuntimeException("库存服务不可用，无法锁定库存", cause);
            }

            @Override
            public void releaseStock(List<StockOperationDTO> items) {
                log.warn("[订单] 库存释放降级（库存服务不可用）— items={}", items.size());
            }
        };
    }
}