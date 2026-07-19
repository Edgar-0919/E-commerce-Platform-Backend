package com.ecommerce.product.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预扣流水实体 — 本地消息表
 * <p>
 * 记录预扣→实扣→回滚全生命周期，用于超时对账补偿。
 * status: 0=预扣中, 1=已实扣, 2=已回滚
 */
@Data
@TableName("t_stock_pre_lock")
public class StockPreLock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long skuId;
    private Long merchantId;
    private Integer quantity;
    private Integer status;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}