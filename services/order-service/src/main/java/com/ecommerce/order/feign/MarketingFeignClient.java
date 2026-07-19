package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "marketing-service")
public interface MarketingFeignClient {

    @PostMapping("/api/marketing/coupon/lock/{userCouponId}")
    Result<BigDecimal> lockCoupon(@PathVariable("userCouponId") Long userCouponId,
                                   @RequestParam("orderAmount") BigDecimal orderAmount);

    @PostMapping("/api/marketing/coupon/confirm/{userCouponId}")
    Result<Void> confirmCoupon(@PathVariable("userCouponId") Long userCouponId,
                               @RequestParam("orderId") Long orderId);

    @PostMapping("/api/marketing/coupon/release/{userCouponId}")
    Result<Void> releaseCoupon(@PathVariable("userCouponId") Long userCouponId);
}