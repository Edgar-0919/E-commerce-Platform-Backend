package com.ecommerce.order.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_refund")
public class Refund {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String refundNo;
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String reason;
    private Integer refundType;
    private String refundReason;
    private String refundImages;
    private Integer returnStatus;
    private String returnLogisticsNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}