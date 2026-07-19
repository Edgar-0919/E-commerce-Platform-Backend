package com.ecommerce.core.constant;

/**
 * 全局常量接口
 * 定义系统级常量：
 * - 请求头常量（认证、链路追踪）
 * - Token相关常量
 * - 时间常量（过期时间等）
 */
public interface GlobalConstants {

    // 链路追踪ID
    String TRACE_ID_HEADER = "X-Trace-Id";
    // Gateway认证后通过请求头透传用户身份，下游微服务不再解析JWT
    String USER_ID_HEADER = "X-User-Id";
    String USERNAME_HEADER = "X-Username";
    String USER_ROLES_HEADER = "X-User-Roles";
    // 商户隔离：ROLE_ADMIN 可传 ALL 跳过租户过滤
    String MERCHANT_ID_HEADER = "X-Merchant-Id";
    String MERCHANT_ID_ALL = "ALL";

    String TOKEN_PREFIX = "Bearer ";
    String AUTHORIZATION_HEADER = "Authorization";

    long TOKEN_EXPIRE_SECONDS = 7200L;
    long REFRESH_TOKEN_EXPIRE_SECONDS = 604800L;
}
