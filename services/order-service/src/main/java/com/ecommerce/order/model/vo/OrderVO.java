package com.ecommerce.order.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String statusDesc;
    private String receiverName;
    private String phone;
    private String address;
    private String remark;
    private List<OrderItemVO> items;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

@Data
class OrderItemVO {
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
