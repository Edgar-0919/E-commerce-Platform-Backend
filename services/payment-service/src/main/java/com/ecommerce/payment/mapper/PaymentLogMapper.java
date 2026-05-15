package com.ecommerce.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.payment.model.entity.PaymentLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentLogMapper extends BaseMapper<PaymentLog> {
}
