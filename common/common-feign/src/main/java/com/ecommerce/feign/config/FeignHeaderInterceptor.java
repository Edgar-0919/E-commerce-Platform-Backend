package com.ecommerce.feign.config;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign请求头拦截器
 * 自动将当前用户上下文信息透传到下游服务
 * 透传的信息：
 * - X-User-Id: 用户ID
 * - X-Username: 用户名
 * - X-User-Roles: 用户角色列表
 * <p>配合UserContextInterceptor使用，实现服务间用户身份传递
 */
public class FeignHeaderInterceptor implements RequestInterceptor {

    // 在所有 Feign 调用中自动透传当前用户上下文，确保服务间调用链路保持用户身份
    @Override
    public void apply(RequestTemplate template) {
        UserContext ctx = UserContext.get();
        if (ctx != null) {
            template.header(GlobalConstants.USER_ID_HEADER, String.valueOf(ctx.getUserId()));
            if (ctx.getUsername() != null) {
                template.header(GlobalConstants.USERNAME_HEADER, ctx.getUsername());
            }
            if (ctx.getRoles() != null && !ctx.getRoles().isEmpty()) {
                template.header(GlobalConstants.USER_ROLES_HEADER,
                        String.join(",", ctx.getRoles()));
            }
        }
    }
}
