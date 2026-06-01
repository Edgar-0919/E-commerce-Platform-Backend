package com.ecommerce.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.ecommerce.core.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel WebMVC 全局 BlockException 处理器
 * <p>
 * 通过声明 BlockExceptionHandler Bean 替代默认处理器，
 * 当请求触发 Sentinel 流控、降级、热点、授权或系统规则时，
 * 返回统一格式的 Result JSON 响应。
 * <p>
 * 仅在 Sentinel 相关类存在于 classpath 时生效。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(BlockExceptionHandler.class)
public class SentinelBlockConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        return (request, response, e) -> {
            int httpStatus = 429;
            String message;

            if (e instanceof FlowException) {
                message = "请求过于频繁，请稍后再试";
                httpStatus = 429;
            } else if (e instanceof DegradeException) {
                message = "服务降级中，请稍后再试";
                httpStatus = 503;
            } else if (e instanceof ParamFlowException) {
                message = "热点参数访问受限";
                httpStatus = 429;
            } else if (e instanceof AuthorityException) {
                message = "访问被拒绝";
                httpStatus = 403;
            } else if (e instanceof SystemBlockException) {
                message = "系统负载过高，请稍后再试";
                httpStatus = 429;
            } else {
                message = "服务被限流";
                httpStatus = 429;
            }

            log.warn("[Sentinel拦截] type={}, ip={}",
                    e.getClass().getSimpleName(), request.getRemoteAddr());

            response.setStatus(httpStatus);
            response.setContentType("application/json;charset=UTF-8");
            Result<?> result = Result.fail(httpStatus, message);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        };
    }
}