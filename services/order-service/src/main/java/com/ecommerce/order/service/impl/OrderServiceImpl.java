package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.util.IdGenerator;
import com.ecommerce.order.feign.InventoryFeignClient;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderLogMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.dto.OrderCreateDTO;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import com.ecommerce.order.model.entity.OrderLog;
import com.ecommerce.order.model.vo.OrderVO;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * 核心业务：
 * 1. 创建订单（含库存锁定）
 * 2. 查询订单列表
 * 3. 取消订单（含库存释放）
 * 4. 订单状态更新
 * 事务策略：
 * - 使用@Transactional管理本地事务
 * - 跨服务调用通过Seata AT模式保证一致性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final InventoryFeignClient inventoryFeignClient;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        // 1. Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var item : dto.getItems()) {
            BigDecimal itemAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        // 2. Create order
        Order order = new Order();
        order.setOrderNo(IdGenerator.orderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setReceiverName("待完善"); // In real code, fetch from address
        order.setPhone("待完善");
        order.setAddress("待完善");
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        // 3. Save order items
        List<StockOperationDTO> stockLocks = new ArrayList<>();
        for (var itemDTO : dto.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(itemDTO.getSkuId());
            item.setPrice(itemDTO.getPrice());
            item.setQuantity(itemDTO.getQuantity());
            item.setAmount(itemDTO.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            item.setProductName("商品名称"); // In real code, fetch from product-service
            orderItemMapper.insert(item);

            StockOperationDTO stockDTO = new StockOperationDTO();
            stockDTO.setOrderId(order.getId());
            stockDTO.setSkuId(itemDTO.getSkuId());
            stockDTO.setQuantity(itemDTO.getQuantity());
            stockLocks.add(stockDTO);
        }

        // 4. 通过 Feign 远程锁定库存（跨服务事务由 Seata AT 模式管理）
        // 锁库失败时抛出异常，Spring 事务回滚第2、3步的订单和订单项
        try {
            inventoryFeignClient.lockStock(stockLocks);
        } catch (Exception e) {
            log.error("库存锁定失败，订单创建回滚: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException(ResultCodeEnum.STOCK_LOCK_FAILED);
        }

        // 5. Record log
        saveOrderLog(order.getId(), null, order.getStatus(), "用户下单", "SYSTEM");

        log.info("订单创建成功: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, totalAmount);
        return toVO(order);
    }

    @Override
    public PageResult<OrderVO> page(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);

        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<OrderVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), records);
    }

    @Override
    public OrderVO getById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        return toVO(order);
    }

    @Override
    public OrderVO getByOrderNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        return toVO(order);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "无权操作此订单");
        }
        // 仅待支付状态允许取消
        if (order.getStatus() != OrderStatusEnum.PENDING_PAY.getCode()) {
            throw new BusinessException(ResultCodeEnum.ORDER_CANNOT_CANCEL);
        }

        // 释放已锁定的库存（即使释放失败也不阻塞取消流程，避免订单卡死）
        // Release inventory
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        List<StockOperationDTO> releases = items.stream().map(item -> {
            StockOperationDTO dto = new StockOperationDTO();
            dto.setOrderId(order.getId());
            dto.setSkuId(item.getSkuId());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        try {
            inventoryFeignClient.releaseStock(releases);
        } catch (Exception e) {
            log.error("库存释放失败: orderId={}", order.getId(), e);
        }

        int fromStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED.getCode());
        orderMapper.updateById(order);

        saveOrderLog(order.getId(), fromStatus, order.getStatus(), "用户取消订单", "USER");
        log.info("订单取消成功: orderNo={}", order.getOrderNo());
    }

    // 供支付回调、退款回调等事件驱动的状态变更，非用户直接调用的接口
    @Override
    public void updateStatus(Long id, Integer status, String operator) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        int fromStatus = order.getStatus();
        order.setStatus(status);
        if (status == OrderStatusEnum.PENDING_DELIVER.getCode()) {
            order.setPayTime(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        saveOrderLog(order.getId(), fromStatus, status,
                OrderStatusEnum.of(status) != null ? OrderStatusEnum.of(status).getDesc() : "状态变更", operator);
    }

    private void saveOrderLog(Long orderId, Integer fromStatus, Integer toStatus, String remark, String operator) {
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setOperator(operator);
        logEntry.setRemark(remark);
        logEntry.setCreateTime(LocalDateTime.now());
        orderLogMapper.insert(logEntry);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        // Convert items to VO (simplified, in real code use MapStruct)
        return vo;
    }
}
