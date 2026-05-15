package com.ecommerce.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_spec_group")
public class SpecGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Long categoryId;
    private Integer sort;
}
