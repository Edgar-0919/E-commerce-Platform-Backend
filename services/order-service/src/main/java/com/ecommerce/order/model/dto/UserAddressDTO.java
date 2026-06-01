package com.ecommerce.order.model.dto;

import lombok.Data;

@Data
public class UserAddressDTO {

    private Long id;
    private Long userId;
    private String receiverName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}