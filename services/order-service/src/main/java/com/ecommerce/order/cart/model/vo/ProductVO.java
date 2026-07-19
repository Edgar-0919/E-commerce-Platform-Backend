package com.ecommerce.order.cart.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String mainImage;
    private List<String> images;
    private String description;
    private String unit;
    private Integer status;
    private List<SkuVO> skus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}