package com.ecommerce.product.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ProductSaveDTO {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String mainImage;
    private List<String> images;
    private String description;
    private String unit;

    private List<SkuSaveDTO> skus;
}
