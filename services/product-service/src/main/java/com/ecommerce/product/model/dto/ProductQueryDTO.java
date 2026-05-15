package com.ecommerce.product.model.dto;

import lombok.Data;

@Data
public class ProductQueryDTO {

    private String keyword;
    private Long categoryId;
    private Long brandId;
    private Integer status;
    private Integer page = 1;
    private Integer size = 20;
}
