package com.ecommerce.product.inventory;

import com.ecommerce.product.inventory.model.dto.StockOperationDTO;

import java.util.List;

public interface InventoryService {

    Integer getStock(Long skuId);

    void lockStock(List<StockOperationDTO> items);

    void releaseStock(List<StockOperationDTO> items);

    void deductStock(List<StockOperationDTO> items);

    /** 退款成功后恢复库存（DB 增加可用库存 + 同步 Redis） */
    void increaseStock(List<StockOperationDTO> items);

    void syncStockToRedis(Long skuId);

    List<Long> getLowStockSkus();
}