package com.ecommerce.order.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order_log")
public class OrderLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
