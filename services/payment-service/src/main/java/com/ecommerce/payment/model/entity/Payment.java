package com.ecommerce.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String channel;
    private Integer status;
    private LocalDateTime paidTime;
    private String transactionNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
