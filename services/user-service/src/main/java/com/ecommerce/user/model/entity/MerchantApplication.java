package com.ecommerce.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_merchant_application")
public class MerchantApplication {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String merchantName;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String businessLicense;
    private Integer status;
    private String reviewRemark;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}