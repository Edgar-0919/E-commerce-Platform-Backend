package com.ecommerce.mybatis.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有数据库实体的基类，提供通用字段和行为
 * 包含字段：
 * - id: 雪花算法生成的分布式ID
 * - createTime: 创建时间（自动填充）
 * - updateTime: 更新时间（自动填充）
 * - deleted: 逻辑删除标记（MyBatis-Plus自动处理）
 */
@Data
public class BaseEntity implements Serializable {

    // 雪花算法分布式ID，避免分库分表后ID冲突
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // MyBatis-Plus 逻辑删除：查询/更新时自动拼接 deleted=0
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
