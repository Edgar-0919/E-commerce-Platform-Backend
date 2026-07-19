package com.ecommerce.product.inventory.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockOperationDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;
}