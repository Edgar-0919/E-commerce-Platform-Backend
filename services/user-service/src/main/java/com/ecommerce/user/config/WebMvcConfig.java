package com.ecommerce.user.config;

import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user/login", "/api/user/register");
    }

    private static class UserContextInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            UserContext ctx = extractFromRequest(request);
            if (ctx != null) {
                UserContext.set(ctx);
            }
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            UserContext.clear();
        }

        private static UserContext extractFromRequest(HttpServletRequest request) {
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
}
