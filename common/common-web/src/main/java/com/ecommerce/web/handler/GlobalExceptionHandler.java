package com.ecommerce.web.handler;

import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.AuthException;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理应用中抛出的各类异常，确保返回统一格式的错误响应
 * 异常处理优先级：
 * 1. BusinessException - 业务异常，返回具体错误码和消息
 * 2. AuthException - 认证异常，返回401状态码
 * 3. MethodArgumentNotValidException - 参数校验异常
 * 4. Exception - 兜底处理，隐藏内部细节
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} — code={}, message={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthException(AuthException e, HttpServletRequest request) {
        log.warn("[认证异常] {} {} — {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("[参数校验] {} {} — {}", request.getMethod(), request.getRequestURI(), msg);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), msg);
    }

    // 兜底处理：对外隐藏内部异常细节，返回通用错误信息
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {} {} —", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
