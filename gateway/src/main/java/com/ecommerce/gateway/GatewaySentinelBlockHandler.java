package com.ecommerce.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sentinel 网关限流/熔断自定义响应处理器
 * 当请求触发 Sentinel 流控或熔断规则时，返回 JSON 格式的错误响应，
 * 而非默认的错误页面。配合 GatewayApplication 中的 GatewayCallbackManager 使用。
 */
public class GatewaySentinelBlockHandler implements BlockRequestHandler {

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        String message;
        int code;
        
        if (ex instanceof DegradeException) {
            message = "系统维护中，请稍后再试";
            code = 503;
        } else {
            message = "请求过于频繁，请稍后再试";
            code = 429;
        }
        
        return ServerResponse.status(HttpStatus.valueOf(code))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"code\":" + code + ",\"message\":\"" + message + "\",\"timestamp\":" +
                        System.currentTimeMillis() + "}");
    }
}