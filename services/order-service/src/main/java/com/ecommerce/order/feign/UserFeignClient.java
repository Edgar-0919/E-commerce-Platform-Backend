package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.feign.fallback.UserFeignFallbackFactory;
import com.ecommerce.order.model.dto.UserAddressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端 — 获取用户收货地址
 * <p>
 * fallbackFactory：调用失败时触发降级
 */
@FeignClient(name = "user-service", path = "/api/user/address",
        fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserAddressDTO> getAddressById(@PathVariable("id") Long addressId);
}