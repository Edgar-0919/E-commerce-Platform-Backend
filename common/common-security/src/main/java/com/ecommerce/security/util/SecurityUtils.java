package com.ecommerce.security.util;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SecurityUtils {

    // 从Gateway透传的请求头中提取用户信息，不再重复解析JWT
    public static UserContext extractFromRequest(HttpServletRequest request) {
        String userId = request.getHeader(GlobalConstants.USER_ID_HEADER);
        String username = request.getHeader(GlobalConstants.USERNAME_HEADER);
        String rolesHeader = request.getHeader(GlobalConstants.USER_ROLES_HEADER);

        if (userId == null) return null;

        UserContext ctx = new UserContext();
        ctx.setUserId(Long.parseLong(userId));
        ctx.setUsername(username);
        List<String> roles = (rolesHeader != null && !rolesHeader.isEmpty())
                ? Arrays.asList(rolesHeader.split(","))
                : Collections.emptyList();
        ctx.setRoles(roles);
        return ctx;
    }
}
