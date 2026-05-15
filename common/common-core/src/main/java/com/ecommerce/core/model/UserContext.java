package com.ecommerce.core.model;

import lombok.Data;
import java.util.List;

/**
 * 用户上下文类
 * 用于在请求链路中传递当前用户信息
 * 使用InheritableThreadLocal确保子线程也能继承上下文
 */
@Data
public class UserContext {

    private Long userId;
    private String username;
    private String nickname;
    private List<String> roles;

    // InheritableThreadLocal 确保子线程（如 @Async 异步方法）也能继承父线程的上下文
    private static final ThreadLocal<UserContext> CONTEXT = new InheritableThreadLocal<>();

    public static void set(UserContext ctx) {
        CONTEXT.set(ctx);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long currentUserId() {
        UserContext ctx = get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String currentUsername() {
        UserContext ctx = get();
        return ctx != null ? ctx.getUsername() : null;
    }
}
