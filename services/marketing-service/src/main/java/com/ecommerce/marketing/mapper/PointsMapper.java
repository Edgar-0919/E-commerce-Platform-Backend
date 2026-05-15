package com.ecommerce.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.marketing.model.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointsMapper extends BaseMapper<UserPoints> {
}
