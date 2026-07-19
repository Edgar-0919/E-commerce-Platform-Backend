package com.ecommerce.order.cart.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class SkuVO {

    private Long id;
    private Long productId;
    private String skuCode;
    private Long merchantId;
    private Map<String, String> specValues;
    private BigDecimal price;
    private BigDecimal marketPrice;
    private BigDecimal weight;
    private String image;
    private Integer stock;
    private Integer status;
    private String productName;
}