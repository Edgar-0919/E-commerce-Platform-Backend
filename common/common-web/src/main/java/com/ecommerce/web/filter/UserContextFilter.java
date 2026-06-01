package com.ecommerce.web.filter;

import com.ecommerce.core.model.UserContext;
import com.ecommerce.security.util.SecurityUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 从请求头中提取用户信息并设置到ThreadLocal
 * 
 * 架构说明：
 * - Gateway层解析JWT，将userId/username/roles写入请求头
 * - 此过滤器从请求头读取这些信息，设置到UserContext ThreadLocal
 * - 下游Service通过UserContext.currentUserId()获取当前用户
 * 
 * 请求头格式：
 * - X-User-Id: 用户ID
 * - X-Username: 用户名
 * - X-User-Roles: 角色列表（逗号分隔）
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
            UserContext ctx = SecurityUtils.extractFromRequest(httpRequest);
            if (ctx != null) {
                UserContext.set(ctx);
                log.debug("UserContext set: userId={}, username={}", 
                        ctx.getUserId(), ctx.getUsername());
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}