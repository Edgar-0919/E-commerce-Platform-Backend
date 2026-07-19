package com.ecommerce.web.filter;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用户上下文过滤器
 * 从请求头中提取用户信息并设置到ThreadLocal
 * 
 * 架构说明：
 * - Gateway层解析JWT，将userId/username/roles/merchantId写入请求头
 * - 此过滤器从请求头读取这些信息，设置到UserContext ThreadLocal
 * - 下游Service通过UserContext.currentUserId()/currentMerchantId()获取当前用户/商户
 * 
 * 请求头格式：
 * - X-User-Id: 用户ID
 * - X-Username: 用户名
 * - X-User-Roles: 角色列表（逗号分隔）
 * - X-Merchant-Id: 商户ID（ROLE_ADMIN可传ALL跳过租户过滤）
 */
@Slf4j
@Component
@Order(1)
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            UserContext ctx = extractFromRequest(httpRequest);
            if (ctx != null) {
                UserContext.set(ctx);
                log.debug("UserContext set: userId={}, username={}, merchantId={}",
                        ctx.getUserId(), ctx.getUsername(), ctx.getMerchantId());
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 从Gateway透传的请求头中提取用户信息，不再重复解析JWT
     * 内联实现，避免跨模块依赖 common-security 导致 ClassNotFoundException
     */
    private UserContext extractFromRequest(HttpServletRequest request) {
        String userId = request.getHeader(GlobalConstants.USER_ID_HEADER);
        String username = request.getHeader(GlobalConstants.USERNAME_HEADER);
        String rolesHeader = request.getHeader(GlobalConstants.USER_ROLES_HEADER);
        String merchantId = request.getHeader(GlobalConstants.MERCHANT_ID_HEADER);

        if (userId == null || userId.trim().isEmpty()) return null;

        try {
            UserContext ctx = new UserContext();
            ctx.setUserId(Long.parseLong(userId.trim()));
            ctx.setUsername(username);
            List<String> roles = (rolesHeader != null && !rolesHeader.isEmpty())
                    ? Arrays.asList(rolesHeader.split(","))
                    : Collections.emptyList();
            ctx.setRoles(roles);
            // 商户ID：ROLE_ADMIN 且 header 传 ALL 时设为 null（跳过租户过滤）
            if (merchantId != null && !merchantId.isEmpty()) {
                if (!GlobalConstants.MERCHANT_ID_ALL.equals(merchantId)) {
                    ctx.setMerchantId(Long.parseLong(merchantId));
                } else {
                    log.debug("Merchant ID is ALL, skipping tenant filter for admin user: {}", username);
                }
            } else {
                log.debug("Merchant ID header is empty, tenant context not set for user: {}", username);
            }
            return ctx;
        } catch (NumberFormatException e) {
            log.warn("Invalid user id or merchant id format in header: userId={}, merchantId={}", userId, merchantId);
            return null;
        }
    }
}