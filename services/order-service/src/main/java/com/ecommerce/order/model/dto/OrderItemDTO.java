package com.ecommerce.order.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    @NotNull
    private Long skuId;
    @NotNull
    private Integer quantity;
    private BigDecimal price;
}
