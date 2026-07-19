package com.ecommerce.product.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String mainImage;
    private List<String> images;
    private String description;
    private String unit;
    private Integer status;
    /** 最低 SKU 价格 — 商品列表展示用 */
    private BigDecimal price;
    /** 总库存 — 所有 SKU 库存累加 */
    private Integer stock;
    private List<SkuVO> skus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
