package com.ecommerce.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private String categoryPath;
    private Integer sort;
    private String icon;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
