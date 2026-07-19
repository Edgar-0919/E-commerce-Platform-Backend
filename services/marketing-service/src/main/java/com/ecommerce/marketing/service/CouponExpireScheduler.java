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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券过期定时任务
 * 每小时扫描一次，将已过期但状态仍为"未使用"的优惠券标记为"已过期"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireScheduler {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void expireCoupons() {
        log.info("开始执行优惠券过期扫描...");
        // 查询所有未使用的优惠券
        List<UserCoupon> unusedCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getStatus, 0));
        int expiredCount = 0;
        for (UserCoupon uc : unusedCoupons) {
            CouponTemplate template = couponTemplateMapper.selectById(uc.getTemplateId());
            if (template != null && template.getEndTime().isBefore(LocalDateTime.now())) {
                uc.setStatus(2); // 已过期
                userCouponMapper.updateById(uc);
                expiredCount++;
            }
        }
        log.info("优惠券过期扫描完成，处理 {} 张过期券", expiredCount);
    }
}