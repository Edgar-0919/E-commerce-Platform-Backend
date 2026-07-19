package com.ecommerce.product.inventory.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存视图对象 — 包含商品名称，方便管理端直接展示
 */
@Data
public class StockVO {

    private Long id;
    private Long skuId;
    private Long productId;
    /** 商品名称（从 product-service 批量查询） */
    private String productName;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
    private Integer safetyStock;
    private LocalDateTime updateTime;
}