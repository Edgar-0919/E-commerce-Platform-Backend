package com.ecommerce.marketing.service;

import com.ecommerce.marketing.model.entity.*;

import java.util.List;

public interface MarketingService {

    // Coupon
    List<CouponTemplate> getAvailableCoupons();

    void receiveCoupon(Long userId, Long templateId);

    List<UserCoupon> getUserCoupons(Long userId, Integer status);

    void useCoupon(Long userCouponId, Long orderId);

    // Points
    UserPoints getPoints(Long userId);

    void addPoints(Long userId, Integer points, String remark);

    void deductPoints(Long userId, Integer points, String remark);

    // Promotion
    List<Promotion> getPromotions(String type);
}
