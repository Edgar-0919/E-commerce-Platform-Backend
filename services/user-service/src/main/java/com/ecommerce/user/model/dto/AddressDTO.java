package com.ecommerce.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {

    @NotBlank(message = "收件人不能为空")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private Boolean isDefault;
}
