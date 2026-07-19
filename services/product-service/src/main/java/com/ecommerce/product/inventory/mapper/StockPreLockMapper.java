package com.ecommerce.product.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.product.inventory.model.entity.StockPreLock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockPreLockMapper extends BaseMapper<StockPreLock> {
}