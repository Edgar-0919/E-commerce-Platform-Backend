package com.ecommerce.inventory.service;

import com.ecommerce.inventory.model.dto.StockOperationDTO;

import java.util.List;

public interface InventoryService {

    Integer getStock(Long skuId);

    void lockStock(List<StockOperationDTO> items);

    void releaseStock(List<StockOperationDTO> items);

    void deductStock(List<StockOperationDTO> items);

    void syncStockToRedis(Long skuId);

    List<Long> getLowStockSkus();
}
