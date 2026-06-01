package com.ecommerce.order.model.dto;

import lombok.Data;

@Data
public class StockOperationDTO {

    private Long orderId;
    private Long skuId;
    private Integer quantity;
}