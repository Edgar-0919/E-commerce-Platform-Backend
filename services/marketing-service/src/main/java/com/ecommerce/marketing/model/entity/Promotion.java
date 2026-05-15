package com.ecommerce.marketing.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_promotion")
public class Promotion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String type;
    private Long productId;
    private Long skuId;
    private String rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
