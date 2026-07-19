package com.ecommerce.user.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewApplicationDTO {

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String reviewRemark;
}