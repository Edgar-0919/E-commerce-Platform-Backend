package com.ecommerce.marketing.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券视图对象 — 前端展示用
 * <p>
 * 将 UserCoupon 与 CouponTemplate 关联后的扁平化数据结构，
 * 解决后端实体字段（如 status=0/1/2）与前端期望字段（如 status=1/2/3）不一致的问题。
 * <p>
 * 状态映射规则：
 * - 后端 0（未使用） → 前端 1（可使用）
 * - 后端 1（已使用） → 前端 2（已使用）
 * - 后端 2（已过期） → 前端 3（已过期）
 * - 若优惠券虽状态为 0 但模板 endTime 已过，自动映射为 3（已过期）
 */
@Data
public class UserCouponVO {

    /** 用户优惠券 ID */
    private Long id;

    /** 优惠券模板 ID */
    private Long templateId;

    /** 用户 ID */
    private Long userId;

    /** 前端展示状态：1=可使用, 2=已使用, 3=已过期 */
    private Integer status;

    /** 优惠券名称（来自 CouponTemplate.name） */
    private String name;

    /** 优惠类型：1=满减, 2=折扣（来自 CouponTemplate.type） */
    private Integer discountType;

    /** 优惠面额/折扣值（来自 CouponTemplate.amount） */
    private BigDecimal discountValue;

    /** 最低使用金额（来自 CouponTemplate.threshold） */
    private BigDecimal minAmount;

    /** 有效期截止时间（来自 CouponTemplate.endTime） */
    private LocalDateTime expireTime;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 关联订单 ID */
    private Long orderId;

    /** 领取时间 */
    private LocalDateTime createTime;
}