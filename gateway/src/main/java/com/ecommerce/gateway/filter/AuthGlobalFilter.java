package com.ecommerce.gateway.filter;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.TokenPayload;
import com.ecommerce.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 网关认证全局过滤器
 * 职责：
 * 1. 拦截所有请求，对白名单路径直接放行
 * 2. 解析JWT Token验证用户身份
 * 3. 将用户信息透传到下游微服务
 * 执行顺序：-100（优先于业务过滤器执行）
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher matcher = new AntPathMatcher();

    // 白名单路径：无需认证即可访问
    // /api/product/** 和 /api/search/** 为B端/C端共享浏览路径
    // Knife4j 文档和 Swagger 资源也需要放行
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/product/**",
            "/api/search/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(GlobalConstants.TOKEN_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(GlobalConstants.TOKEN_PREFIX.length());
        TokenPayload payload = JwtUtils.parseToken(token);
        if (payload == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 透传用户信息到下游微服务：避免每个服务重复解析JWT
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(GlobalConstants.USER_ID_HEADER, String.valueOf(payload.getUserId()))
                .header(GlobalConstants.USERNAME_HEADER, payload.getUsername())
                .header(GlobalConstants.USER_ROLES_HEADER,
                        payload.getRoles() != null ? String.join(",", payload.getRoles()) : "")
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        // -100 确保在所有业务过滤器之前执行认证
        return -100;
    }
}
