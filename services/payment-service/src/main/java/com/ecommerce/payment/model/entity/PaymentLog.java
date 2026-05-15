package com.ecommerce.payment.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_payment_log")
public class PaymentLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long paymentId;
    private String type;
    private String request;
    private String response;
    private LocalDateTime createTime;
}
