package com.ecommerce.product.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductQueryDTO {

    private String keyword;
    private Long categoryId;
    private Integer status;
    private Integer page = 1;
    private Integer size = 20;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
}
