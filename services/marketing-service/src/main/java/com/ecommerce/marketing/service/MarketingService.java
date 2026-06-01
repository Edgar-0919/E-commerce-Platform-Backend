package com.ecommerce.marketing.service;

import com.ecommerce.marketing.model.entity.*;
import com.ecommerce.marketing.model.vo.BannerVO;
import com.ecommerce.marketing.model.vo.UserCouponVO;

import java.util.List;

public interface MarketingService {

    // Coupon
    List<CouponTemplate> getAvailableCoupons();

    void receiveCoupon(Long userId, Long templateId);

    /** 获取用户优惠券列表，返回关联 CouponTemplate 的扁平化 VO */
    List<UserCouponVO> getUserCoupons(Long userId, Integer status);

    void useCoupon(Long userCouponId, Long orderId);

    // Points
    UserPoints getPoints(Long userId);

    void addPoints(Long userId, Integer points, String remark);

    void deductPoints(Long userId, Integer points, String remark);

    // Promotion
    List<Promotion> getPromotions(String type);

    // Banner
    List<BannerVO> getBanners(String position);
}
