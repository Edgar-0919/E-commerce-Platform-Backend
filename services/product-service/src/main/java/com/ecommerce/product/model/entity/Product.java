package com.ecommerce.product.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product")
public class Product extends BaseEntity {

    private String name;
    private Long categoryId;
    private String categoryPath;
    private String categoryName;
    private Long merchantId;
    private String mainImage;
    private String images;
    private String description;
    private String unit;
    private Integer status;
    private Integer salesCount;
}
