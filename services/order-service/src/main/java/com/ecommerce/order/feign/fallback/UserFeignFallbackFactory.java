package com.ecommerce.order.feign.fallback;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.feign.UserFeignClient;
import com.ecommerce.order.model.dto.UserAddressDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 降级工厂 — 当 user-service 不可用时返回空地址
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("[订单] 用户服务Feign调用失败 — {}", cause.getMessage());
        return addressId -> {
            log.warn("[订单] 获取地址降级 — addressId={}", addressId);
            return Result.fail(503, "收货地址服务不可用");
        };
    }
}