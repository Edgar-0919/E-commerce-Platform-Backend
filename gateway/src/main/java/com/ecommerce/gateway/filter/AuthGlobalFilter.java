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

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher matcher = new AntPathMatcher();

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/product/page",
            "/api/product/*",
            "/api/product/category/tree",
            "/api/product/sku/**",
            "/api/product/brand/**",
            "/api/search/**",
            "/api/marketing/banner/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MERCHANT = "ROLE_MERCHANT";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

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

        if (isAdminPath(path)) {
            List<String> roles = payload.getRoles();
            if (roles == null) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            if (isAdminOnlyPath(path)) {
                if (!roles.contains(ROLE_ADMIN)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            } else {
                if (!roles.contains(ROLE_ADMIN) && !roles.contains(ROLE_MERCHANT)) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(GlobalConstants.USER_ID_HEADER, String.valueOf(payload.getUserId()))
                .header(GlobalConstants.USERNAME_HEADER, payload.getUsername())
                .header(GlobalConstants.USER_ROLES_HEADER,
                        payload.getRoles() != null ? String.join(",", payload.getRoles()) : "")
                .header(GlobalConstants.MERCHANT_ID_HEADER,
                        payload.getMerchantId() != null ? String.valueOf(payload.getMerchantId()) : "")
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/api/admin/");
    }

    private boolean isAdminOnlyPath(String path) {
        if (path.startsWith("/api/admin/categories")) {
            return false;
        }
        return path.startsWith("/api/admin/users") || 
               path.startsWith("/api/admin/brands") ||
               path.startsWith("/api/admin/merchants") ||
               path.startsWith("/api/admin/merchant-applications");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}