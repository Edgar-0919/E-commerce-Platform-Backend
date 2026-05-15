package com.ecommerce.order.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateDTO {

    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemDTO> items;

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    private String remark;
    private Long couponId;
}

