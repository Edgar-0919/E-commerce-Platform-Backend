package com.ecommerce.order.model.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项视图对象
 * <p>
 * 用于 OrderVO 中携带订单商品列表，供前端订单详情页展示商品名称、规格、图片等。
 */
@Data
public class OrderItemVO {
    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String specDesc;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private String image;
}
