package com.ecommerce.core.exception;

import com.ecommerce.core.constant.ResultCodeEnum;

/**
 * Feign调用异常类
 * 用于封装服务间调用失败的异常
 * 包含服务名信息，便于定位问题
 */
public class FeignCallException extends BusinessException {

    public FeignCallException(String service, String message) {
        super(ResultCodeEnum.SYSTEM_ERROR.getCode(), "服务调用失败[" + service + "]: " + message);
    }
}
