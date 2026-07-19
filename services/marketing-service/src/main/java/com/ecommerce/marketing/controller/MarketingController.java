package com.ecommerce.marketing.controller;


import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.marketing.model.entity.CouponTemplate;
import com.ecommerce.marketing.model.entity.UserCoupon;
import com.ecommerce.marketing.model.vo.BannerVO;
import com.ecommerce.marketing.model.vo.UserCouponVO;
import com.ecommerce.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
@Tag(name = "营销服务", description = "优惠券、积分、促销活动")
public class MarketingController {

    private final MarketingService marketingService;

    @GetMapping("/coupon/page")
    @Operation(summary = "可用优惠券列表")
    public Result<List<CouponTemplate>> couponList() {
        return Result.success(marketingService.getAvailableCoupons());
    }

    /**
     * 领取优惠券 — 抢券场景流量尖峰，需要限流和热点参数保护
     */
    @PostMapping("/coupon/receive/{templateId}")
    @Operation(summary = "领取优惠券")
    public Result<Void> receiveCoupon(@PathVariable("templateId") Long templateId) {
        marketingService.receiveCoupon(UserContext.currentUserId(), templateId);
        return Result.success();
    }

    @GetMapping("/coupon/my")
    @Operation(summary = "我的优惠券")
    public Result<List<UserCouponVO>> myCoupon(@RequestParam(value = "status", required = false) Integer status) {
        return Result.success(marketingService.getUserCoupons(UserContext.currentUserId(), status));
    }

    @GetMapping("/banner")
    @Operation(summary = "轮播图列表")
    public Result<List<BannerVO>> banners(@RequestParam(value = "position", defaultValue = "home") String position) {
        return Result.success(marketingService.getBanners(position));
    }

    @PostMapping("/coupon/lock/{userCouponId}")
    @Operation(summary = "锁定优惠券（下单时调用，校验并锁定）")
    public Result<java.math.BigDecimal> lockCoupon(@PathVariable("userCouponId") Long userCouponId,
                                                    @RequestParam("orderAmount") java.math.BigDecimal orderAmount) {
        return Result.success(marketingService.lockCoupon(UserContext.currentUserId(), userCouponId, orderAmount));
    }

    @PostMapping("/coupon/confirm/{userCouponId}")
    @Operation(summary = "确认优惠券（支付成功后调用，关联订单）")
    public Result<Void> confirmCoupon(@PathVariable("userCouponId") Long userCouponId,
                                       @RequestParam("orderId") Long orderId) {
        marketingService.confirmCoupon(userCouponId, orderId);
        return Result.success();
    }

    @PostMapping("/coupon/release/{userCouponId}")
    @Operation(summary = "释放优惠券（订单取消/退款时调用）")
    public Result<Void> releaseCoupon(@PathVariable("userCouponId") Long userCouponId) {
        marketingService.releaseCoupon(UserContext.currentUserId(), userCouponId);
        return Result.success();
    }
}
