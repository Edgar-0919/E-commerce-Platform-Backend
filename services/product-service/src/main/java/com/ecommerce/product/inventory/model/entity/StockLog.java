package com.ecommerce.product.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_log")
public class StockLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long skuId;
    private Long merchantId;
    private Long orderId;
    private String type;
    private Integer quantity;
    private Integer beforeQty;
    private Integer afterQty;
    private String remark;
    private LocalDateTime createTime;
}