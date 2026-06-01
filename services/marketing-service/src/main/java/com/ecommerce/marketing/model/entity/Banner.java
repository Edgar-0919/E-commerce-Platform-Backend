package com.ecommerce.marketing.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_banner")
public class Banner {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String image;
    private String title;
    private String description;
    private String position;
    private String linkUrl;
    private Integer sort;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}