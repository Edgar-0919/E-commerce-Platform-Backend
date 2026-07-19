package com.ecommerce.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantApplicationDTO {

    @NotBlank(message = "商户名称不能为空")
    private String merchantName;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private String email;

    private String address;

    private String businessLicense;
}