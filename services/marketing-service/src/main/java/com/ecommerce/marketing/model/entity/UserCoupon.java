package com.ecommerce.marketing.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long templateId;
    private Integer status;
    private Long orderId;
    private LocalDateTime usedTime;
    private LocalDateTime createTime;
}
