package com.ecommerce.marketing.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.marketing.model.entity.*;
import com.ecommerce.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/coupon/receive/{templateId}")
    @Operation(summary = "领取优惠券")
    public Result<Void> receiveCoupon(@PathVariable Long templateId) {
        marketingService.receiveCoupon(UserContext.currentUserId(), templateId);
        return Result.success();
    }

    @GetMapping("/coupon/my")
    @Operation(summary = "我的优惠券")
    public Result<List<UserCoupon>> myCoupon(@RequestParam(required = false) Integer status) {
        return Result.success(marketingService.getUserCoupons(UserContext.currentUserId(), status));
    }

    @GetMapping("/points")
    @Operation(summary = "我的积分")
    public Result<UserPoints> myPoints() {
        return Result.success(marketingService.getPoints(UserContext.currentUserId()));
    }

    @PostMapping("/points")
    @Operation(summary = "积分变动（内部调用）")
    public Result<Void> changePoints(@RequestParam Long userId,
                                      @RequestParam Integer points,
                                      @RequestParam String remark) {
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
}
