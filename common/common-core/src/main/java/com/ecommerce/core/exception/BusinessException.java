package com.ecommerce.core.exception;

import com.ecommerce.core.constant.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常情况，配合GlobalExceptionHandler统一处理
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
