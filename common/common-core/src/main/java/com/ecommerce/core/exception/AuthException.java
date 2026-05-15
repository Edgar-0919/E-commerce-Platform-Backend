package com.ecommerce.core.exception;

import com.ecommerce.core.constant.ResultCodeEnum;

/**
 * 认证异常类
 * 用于封装认证相关的异常，如Token过期、Token无效等
 * 默认HTTP状态码：401 Unauthorized
 */
public class AuthException extends BusinessException {

    public AuthException(ResultCodeEnum resultCode) {
        super(resultCode);
    }

    public AuthException(String message) {
        super(ResultCodeEnum.UNAUTHORIZED.getCode(), message);
    }
}
