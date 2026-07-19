package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.model.dto.UserAddressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端 — 获取用户收货地址
 */
@FeignClient(name = "user-service", contextId = "order-user-address", path = "/api/user/address")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<UserAddressDTO> getAddressById(@PathVariable("id") Long addressId);
}