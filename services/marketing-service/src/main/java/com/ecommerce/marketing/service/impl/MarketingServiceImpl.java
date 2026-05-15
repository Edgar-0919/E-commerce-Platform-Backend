package com.ecommerce.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.marketing.mapper.*;
import com.ecommerce.marketing.model.entity.*;
import com.ecommerce.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingServiceImpl implements MarketingService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final PromotionMapper promotionMapper;
    private final PointsMapper pointsMapper;
    private final PointsLogMapper pointsLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== Coupon ====================

    @Override
    public List<CouponTemplate> getAvailableCoupons() {
        return couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .lt(CouponTemplate::getStartTime, LocalDateTime.now())
                .gt(CouponTemplate::getEndTime, LocalDateTime.now())
                .apply("issued_count < total_count"));
    }

    // 领券：校验模板有效性 → 检查库存 → 检查每人限领数量 → 扣减模板库存 → 插入用户券
    // 注意：并发领券时模板库存（issued_count）存在超发风险，生产环境应使用 Redis 原子扣减
    @Override
    @Transactional
    public void receiveCoupon(Long userId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() == 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
        }
        if (template.getIssuedCount() >= template.getTotalCount()) {
            throw new BusinessException(ResultCodeEnum.COUPON_STOCK_INSUFFICIENT);
        }

        // Check per-user limit
        Long userCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId));
        if (userCount >= template.getPerUserLimit()) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE.getCode(), "已达到每人限领数量");
        }

        template.setIssuedCount(template.getIssuedCount() + 1);
        couponTemplateMapper.updateById(template);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);

        log.info("优惠券领取成功: userId={}, templateId={}, couponId={}", userId, templateId, userCoupon.getId());
    }

    @Override
    public List<UserCoupon> getUserCoupons(Long userId, Integer status) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status));
    }

    @Override
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
        }

        // 使用券时一并校验模板有效期，过期券自动标记为已过期（status=2）
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        if (template.getEndTime().isBefore(LocalDateTime.now())) {
            userCoupon.setStatus(2);
            userCouponMapper.updateById(userCoupon);
            throw new BusinessException(ResultCodeEnum.COUPON_EXPIRED);
        }

        userCoupon.setStatus(1);
        userCoupon.setOrderId(orderId);
        userCoupon.setUsedTime(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);

        log.info("优惠券使用成功: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    // ==================== Points ====================

    // 首次查询时自动创建积分账户（懒初始化，避免注册时创建无用数据）
    @Override
    public UserPoints getPoints(Long userId) {
        UserPoints points = pointsMapper.selectOne(new LambdaQueryWrapper<UserPoints>()
                .eq(UserPoints::getUserId, userId));
        if (points == null) {
            points = new UserPoints();
            points.setUserId(userId);
            points.setTotalPoints(0);
            points.setFrozenPoints(0);
            pointsMapper.insert(points);
        }
        return points;
    }

    @Override
    @Transactional
    public void addPoints(Long userId, Integer points, String remark) {
        UserPoints up = getPoints(userId);
        up.setTotalPoints(up.getTotalPoints() + points);
        pointsMapper.updateById(up);

        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(userId);
        logEntry.setChangeType("earn");
        logEntry.setPoints(points);
        logEntry.setRemark(remark);
        logEntry.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        log.info("积分增加: userId={}, points={}, remark={}", userId, points, remark);
    }

    @Override
    @Transactional
    public void deductPoints(Long userId, Integer points, String remark) {
        UserPoints up = getPoints(userId);
        if (up.getTotalPoints() < points) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE.getCode(), "积分不足");
        }
        up.setTotalPoints(up.getTotalPoints() - points);
        pointsMapper.updateById(up);

        PointsLog logEntry = new PointsLog();
        logEntry.setUserId(userId);
        logEntry.setChangeType("consume");
        logEntry.setPoints(-points);
        logEntry.setRemark(remark);
        logEntry.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        log.info("积分扣减: userId={}, points={}, remark={}", userId, points, remark);
    }

    // ==================== Promotion ====================

    @Override
    public List<Promotion> getPromotions(String type) {
        return promotionMapper.selectList(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getType, type)
                .eq(Promotion::getStatus, 1)
                .le(Promotion::getStartTime, LocalDateTime.now())
                .ge(Promotion::getEndTime, LocalDateTime.now()));
    }
}
