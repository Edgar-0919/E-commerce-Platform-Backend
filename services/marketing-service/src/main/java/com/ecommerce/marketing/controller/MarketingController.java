package com.ecommerce.marketing.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.marketing.model.entity.*;
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
     * 领取优惠券 — 秒杀/抢券场景流量尖峰，需要限流和热点参数保护
     */
    @PostMapping("/coupon/receive/{templateId}")
    @Operation(summary = "领取优惠券")
    @SentinelResource(value = "receiveCoupon", blockHandler = "receiveBlockHandler", fallback = "receiveFallback")
    public Result<Void> receiveCoupon(@PathVariable Long templateId) {
        marketingService.receiveCoupon(UserContext.currentUserId(), templateId);
        return Result.success();
    }

    public Result<Void> receiveBlockHandler(Long templateId, BlockException ex) {
        log.warn("[营销] 领券被Sentinel限流 — templateId={}", templateId);
        return Result.fail(429, "领取人数过多，请稍后再试");
    }

    public Result<Void> receiveFallback(Long templateId, Throwable ex) {
        log.error("[营销] 领券服务降级 —", ex);
        return Result.fail(503, "领券服务暂时不可用");
    }

    @GetMapping("/coupon/my")
    @Operation(summary = "我的优惠券")
    public Result<List<UserCouponVO>> myCoupon(@RequestParam(required = false) Integer status) {
        return Result.success(marketingService.getUserCoupons(UserContext.currentUserId(), status));
    }

    @GetMapping("/points")
    @Operation(summary = "我的积分")
    public Result<UserPoints> myPoints() {
        return Result.success(marketingService.getPoints(UserContext.currentUserId()));
    }

    @PostMapping("/points")
    @Operation(summary = "积分变动")
    public Result<Void> changePoints(@RequestParam Integer points,
                                      @RequestParam String remark) {
        // 通过 JWT 令牌解析当前登录用户，确保用户只能修改自己的积分，防止未授权篡改
        Long userId = UserContext.currentUserId();
        if (points > 0) {
            marketingService.addPoints(userId, points, remark);
        } else {
            marketingService.deductPoints(userId, -points, remark);
        }
        return Result.success();
    }

    @GetMapping("/promotion/{type}")
    @Operation(summary = "促销活动列表")
    public Result<List<Promotion>> promotions(@PathVariable String type) {
        return Result.success(marketingService.getPromotions(type));
    }

    @GetMapping("/banner")
    @Operation(summary = "轮播图列表")
    public Result<List<BannerVO>> banners(@RequestParam(defaultValue = "home") String position) {
        return Result.success(marketingService.getBanners(position));
    }
}
