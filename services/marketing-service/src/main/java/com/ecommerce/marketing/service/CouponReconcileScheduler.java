package com.ecommerce.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.marketing.mapper.CouponTemplateMapper;
import com.ecommerce.marketing.mapper.UserCouponMapper;
import com.ecommerce.marketing.model.entity.CouponTemplate;
import com.ecommerce.marketing.model.entity.UserCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReconcileScheduler {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    @Scheduled(cron = "0 0 */1 * * ?")
    @Transactional
    public void reconcileCouponStatus() {
        log.info("开始优惠券状态对账任务");

        List<UserCoupon> usedCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getStatus, 1)
                .isNull(UserCoupon::getOrderId));

        if (usedCoupons.isEmpty()) {
            log.info("优惠券对账完成，无异常数据");
            return;
        }

        log.warn("发现 {} 张已使用但未关联订单的优惠券，开始处理", usedCoupons.size());

        for (UserCoupon coupon : usedCoupons) {
            try {
                CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
                if (template != null && template.getEndTime().isBefore(LocalDateTime.now())) {
                    coupon.setStatus(2);
                    userCouponMapper.updateById(coupon);
                    log.info("优惠券过期，标记为已过期: userCouponId={}", coupon.getId());
                } else {
                    coupon.setStatus(0);
                    coupon.setUsedTime(null);
                    userCouponMapper.updateById(coupon);
                    log.info("优惠券未关联订单，恢复为可用: userCouponId={}", coupon.getId());
                }
            } catch (Exception e) {
                log.error("处理异常优惠券失败: userCouponId={}, error={}", coupon.getId(), e.getMessage(), e);
            }
        }

        log.info("优惠券状态对账任务完成");
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void expireCoupons() {
        log.info("开始优惠券过期处理任务");

        List<UserCoupon> validCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getStatus, 0));

        int expiredCount = 0;
        for (UserCoupon coupon : validCoupons) {
            try {
                CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
                if (template != null && template.getEndTime().isBefore(LocalDateTime.now())) {
                    coupon.setStatus(2);
                    userCouponMapper.updateById(coupon);
                    expiredCount++;
                }
            } catch (Exception e) {
                log.error("处理优惠券过期失败: userCouponId={}, error={}", coupon.getId(), e.getMessage(), e);
            }
        }

        log.info("优惠券过期处理任务完成，共过期 {} 张", expiredCount);
    }
}