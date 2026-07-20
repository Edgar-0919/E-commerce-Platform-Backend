package com.ecommerce.product.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_spec_param")
public class SpecParam {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long groupId;
    private String name;
    @TableField("`values`")
    private String values;
    private Integer sort;
}
