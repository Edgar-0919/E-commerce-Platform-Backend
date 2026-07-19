package com.ecommerce.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    // orderNo 对外暴露，id 内部使用（防止恶意遍历）
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long couponId;
    // 商品原价合计
    private BigDecimal totalAmount;
    // 优惠金额（优惠券+满减+积分抵扣）
    private BigDecimal discountAmount;
    // 实付金额 = totalAmount - discountAmount
    private BigDecimal payAmount;
    // 状态见 OrderStatusEnum
    private Integer status;
    private String receiverName;
    private String phone;
    private String address;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime deliverTime;
    private LocalDateTime receiveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
