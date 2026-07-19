package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.order.model.dto.OrderCreateDTO;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.vo.OrderVO;

public interface OrderService {

    OrderVO createOrder(Long userId, OrderCreateDTO dto);

    PageResult<OrderVO> page(Long userId, Integer page, Integer size, Integer status);

    OrderVO getById(Long id);

    OrderVO getByOrderNo(String orderNo);

    void cancel(Long userId, Long id);

    void confirmReceive(Long userId, Long id);

    void updateStatus(Long id, Integer status, String operator);

    Order getOrderEntity(Long id);
}
