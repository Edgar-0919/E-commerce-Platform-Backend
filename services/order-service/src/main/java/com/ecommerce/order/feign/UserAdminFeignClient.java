package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 管理后台专用 Feign 客户端 —— 批量查询用户名，供订单管理页面显示"用户"列。
 */
@FeignClient(name = "user-service", contextId = "order-user-admin", path = "/api/admin/users")
public interface UserAdminFeignClient {

    @GetMapping("/batch/usernames")
    Result<Map<Long, String>> getUsernames(@RequestParam("ids") List<Long> userIds);
}
