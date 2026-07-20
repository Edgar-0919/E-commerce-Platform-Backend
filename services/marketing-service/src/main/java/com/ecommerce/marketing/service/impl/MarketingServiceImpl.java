package com.ecommerce.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.marketing.mapper.BannerMapper;
import com.ecommerce.marketing.mapper.CouponTemplateMapper;
import com.ecommerce.marketing.mapper.UserCouponMapper;
import com.ecommerce.marketing.model.entity.Banner;
import com.ecommerce.marketing.model.entity.CouponTemplate;
import com.ecommerce.marketing.model.entity.UserCoupon;
import com.ecommerce.marketing.model.vo.BannerVO;
import com.ecommerce.marketing.model.vo.UserCouponVO;
import com.ecommerce.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingServiceImpl implements MarketingService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final BannerMapper bannerMapper;
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
    public List<UserCouponVO> getUserCoupons(Long userId, Integer status) {
        // 查询用户持有的所有优惠券
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId));

        // 关联 CouponTemplate 转换为前端需要的 VO
        // 状态映射：后端 0/1/2 → 前端 1/2/3（可使用/已使用/已过期）
        return userCoupons.stream()
                .map(uc -> {
                    // 查询关联的优惠券模板以获取名称、面额、门槛、有效期等
                    CouponTemplate template = couponTemplateMapper.selectById(uc.getTemplateId());
                    UserCouponVO vo = new UserCouponVO();
                    vo.setId(uc.getId());
                    vo.setTemplateId(uc.getTemplateId());
                    vo.setUserId(uc.getUserId());
                    vo.setUsedTime(uc.getUsedTime());
                    vo.setOrderId(uc.getOrderId());
                    vo.setCreateTime(uc.getCreateTime());

                    if (template != null) {
                        vo.setName(template.getName());
                        vo.setDiscountType(template.getType());
                        vo.setDiscountValue(template.getAmount());
                        vo.setMinAmount(template.getThreshold());
                        vo.setExpireTime(template.getEndTime());
                    }

                    // 状态映射：后端状态（0=未使用, 1=已使用, 2=已过期）→ 前端状态（1=可使用, 2=已使用, 3=已过期）
                    // 未使用但模板已过期的优惠券自动标记为已过期
                    if (uc.getStatus() == 1) {
                        vo.setStatus(2); // 已使用 → 前端 2
                    } else if (uc.getStatus() == 2) {
                        vo.setStatus(3); // 已过期 → 前端 3
                    } else if (template != null && template.getEndTime().isBefore(LocalDateTime.now())) {
                        vo.setStatus(3); // 模板已过期但券未标记 → 前端 3
                    } else {
                        vo.setStatus(1); // 未使用且在有效期内 → 前端 1
                    }
                    return vo;
                })
                // 如果前端传了 status 筛选，在后端按映射后的前端状态码过滤
                .filter(vo -> status == null || vo.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    private static final String COUPON_LOCK_KEY_PREFIX = "coupon:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 300;

    @Override
    @Transactional
    public BigDecimal lockCoupon(Long userId, Long userCouponId, BigDecimal orderAmount) {
        String lockKey = COUPON_LOCK_KEY_PREFIX + userCouponId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, userId.toString(), LOCK_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE.getCode(), "优惠券正在使用中，请稍后重试");
        }

        try {
            UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
            if (userCoupon == null || userCoupon.getStatus() != 0) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
            }
            if (!userCoupon.getUserId().equals(userId)) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
            }

            CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
            if (template == null || template.getStatus() != 1) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
            }
            if (template.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ResultCodeEnum.COUPON_EXPIRED);
            }
            if (template.getStartTime() != null && template.getStartTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE.getCode(), "优惠券尚未生效");
            }

            if (orderAmount.compareTo(template.getThreshold()) < 0) {
                throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE.getCode(),
                        "订单金额未达到优惠券使用门槛");
            }

            BigDecimal discountAmount = BigDecimal.ZERO;
            if (template.getType() == 1 || template.getType() == 3) {
                discountAmount = template.getAmount();
            } else if (template.getType() == 2) {
                discountAmount = orderAmount.multiply(
                        BigDecimal.ONE.subtract(template.getAmount().divide(new BigDecimal("100"))));
            }
            if (discountAmount.compareTo(orderAmount) > 0) {
                discountAmount = orderAmount;
            }

            userCoupon.setStatus(1);
            userCoupon.setUsedTime(LocalDateTime.now());
            userCouponMapper.updateById(userCoupon);

            log.info("优惠券锁定成功: userCouponId={}, userId={}, discountAmount={}", userCouponId, userId, discountAmount);
            return discountAmount;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public void confirmCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BusinessException(ResultCodeEnum.COUPON_NOT_AVAILABLE);
        }

        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        if (template != null && template.getEndTime().isBefore(LocalDateTime.now())) {
            userCoupon.setStatus(2);
            userCouponMapper.updateById(userCoupon);
            throw new BusinessException(ResultCodeEnum.COUPON_EXPIRED);
        }

        if (userCoupon.getStatus() == 1) {
            if (userCoupon.getOrderId() == null) {
                userCoupon.setOrderId(orderId);
                userCouponMapper.updateById(userCoupon);
                log.info("优惠券确认成功: userCouponId={}, orderId={}", userCouponId, orderId);
            } else if (!userCoupon.getOrderId().equals(orderId)) {
                log.warn("优惠券已关联其他订单: userCouponId={}, existingOrderId={}, newOrderId={}",
                        userCouponId, userCoupon.getOrderId(), orderId);
            } else {
                log.info("优惠券已确认过: userCouponId={}, orderId={}", userCouponId, orderId);
            }
            return;
        }

        if (userCoupon.getStatus() == 0) {
            userCoupon.setStatus(1);
            userCoupon.setOrderId(orderId);
            userCoupon.setUsedTime(LocalDateTime.now());
            userCouponMapper.updateById(userCoupon);
            log.info("优惠券确认并标记使用: userCouponId={}, orderId={}", userCouponId, orderId);
            return;
        }

        log.warn("优惠券状态异常，无法确认: userCouponId={}, status={}", userCouponId, userCoupon.getStatus());
    }

    @Override
    public void releaseCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != 1) {
            return; // 已释放或未使用，无需处理
        }
        userCoupon.setStatus(0); // 回退为未使用
        userCoupon.setOrderId(null);
        userCoupon.setUsedTime(null);
        userCouponMapper.updateById(userCoupon);
        log.info("优惠券已释放: userCouponId={}, userId={}", userCouponId, userId);
    }

    // ==================== Banner ====================

    @Override
    public List<BannerVO> getBanners(String position) {
        LocalDateTime now = LocalDateTime.now();
        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getPosition, position)
                .eq(Banner::getStatus, 1)
                .and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
                .and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
                .orderByAsc(Banner::getSort));
        
        return banners.stream()
                .map(banner -> {
                    BannerVO vo = new BannerVO();
                    vo.setId(banner.getId());
                    vo.setImage(banner.getImage());
                    vo.setTitle(banner.getTitle());
                    vo.setDescription(banner.getDescription());
                    vo.setLinkUrl(banner.getLinkUrl());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
