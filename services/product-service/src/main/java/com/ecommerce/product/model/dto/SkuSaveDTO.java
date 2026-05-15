package com.ecommerce.product.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuSaveDTO {

    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    @NotBlank(message = "规格值不能为空")
    private Map<String, String> specValues;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal marketPrice;
    private BigDecimal weight;
    private String image;
    private Integer stock;
}
