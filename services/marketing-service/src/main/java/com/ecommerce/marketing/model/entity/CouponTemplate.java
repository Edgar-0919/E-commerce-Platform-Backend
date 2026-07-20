package com.ecommerce.marketing.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private Integer totalCount;
    private Integer issuedCount;
    private Integer perUserLimit;
    private Long merchantId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
