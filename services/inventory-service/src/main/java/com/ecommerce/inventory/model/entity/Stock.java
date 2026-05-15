package com.ecommerce.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock")
public class Stock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long skuId;
    private Long productId;
    // totalStock = lockedStock + availableStock（冗余字段，方便库存查询）
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
    // 低于此值时触发预警
    private Integer safetyStock;
    // MyBatis-Plus 乐观锁：更新时自动检查 version，防止并发覆盖
    @Version
    private Integer version;
    private LocalDateTime updateTime;
}
