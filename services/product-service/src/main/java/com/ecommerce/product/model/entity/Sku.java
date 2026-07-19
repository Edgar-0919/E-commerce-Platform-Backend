package com.ecommerce.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_sku")
public class Sku {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long productId;
    private String skuCode;
    private Long merchantId;
    private String specValues;
    private BigDecimal price;
    private BigDecimal marketPrice;
    private BigDecimal weight;
    private String image;
    private Integer stock;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
