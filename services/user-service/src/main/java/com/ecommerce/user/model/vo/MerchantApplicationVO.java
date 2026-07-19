package com.ecommerce.user.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantApplicationVO {

    private Long id;
    private Long userId;
    private String username;
    private String merchantName;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String businessLicense;
    private Integer status;
    private String statusText;
    private String reviewRemark;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}