package com.ecommerce.marketing.service;

import com.ecommerce.marketing.model.entity.CouponTemplate;
import com.ecommerce.marketing.model.entity.UserCoupon;
import com.ecommerce.marketing.model.vo.BannerVO;
import com.ecommerce.marketing.model.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

public interface MarketingService {

    List<CouponTemplate> getAvailableCoupons();

    void receiveCoupon(Long userId, Long templateId);

    List<UserCouponVO> getUserCoupons(Long userId, Integer status);

    BigDecimal lockCoupon(Long userId, Long userCouponId, BigDecimal orderAmount);

    void confirmCoupon(Long userCouponId, Long orderId);

    void releaseCoupon(Long userId, Long userCouponId);

    List<BannerVO> getBanners(String position);
}
