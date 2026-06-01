package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.util.IdGenerator;
import com.ecommerce.order.feign.InventoryFeignClient;
import com.ecommerce.order.feign.ProductFeignClient;
import com.ecommerce.order.feign.UserFeignClient;
import com.ecommerce.order.mq.OrderEventProducer;
import com.ecommerce.order.model.dto.StockOperationDTO;
import com.ecommerce.order.model.vo.SkuVO;
import com.ecommerce.order.model.dto.UserAddressDTO;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderLogMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.dto.OrderCreateDTO;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import com.ecommerce.order.model.entity.OrderLog;
import com.ecommerce.order.model.vo.OrderItemVO;
import com.ecommerce.order.model.vo.OrderVO;
import com.ecommerce.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;
    private final OrderEventProducer orderEventProducer;

    /**
     * 创建订单 — 电商核心流程
     * <p>
     * @GlobalTransactional 协调 Seata AT 模式分布式事务：
     * order-service（订单+订单项） + inventory-service（库存锁定） + marketing-service（优惠券核销）
     * 任意一个环节失败则全部回滚
     */
    @Override
    @Transactional
    @GlobalTransactional(timeoutMills = 300000, name = "ecommerce-createOrder")
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        // 1. 通过 Feign 获取所有 SKU 信息，构建 skuId -> SKU 的映射
        Map<Long, SkuVO> skuMap = new HashMap<>();
        for (var item : dto.getItems()) {
            try {
                var skuResult = productFeignClient.getSkuById(item.getSkuId());
                if (skuResult != null && skuResult.getData() != null) {
                    skuMap.put(item.getSkuId(), skuResult.getData());
                } else {
                    log.error("SKU不存在: skuId={}", item.getSkuId());
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "商品SKU不存在: " + item.getSkuId());
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Feign获取SKU信息失败: skuId={}", item.getSkuId(), e);
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "获取商品信息失败");
            }
        }

        // 2. Calculate total amount - 使用 SKU 真实价格
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var item : dto.getItems()) {
            SkuVO sku = skuMap.get(item.getSkuId());
            BigDecimal itemAmount = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }

        // 3. Create order
        Order order = new Order();
        order.setOrderNo(IdGenerator.orderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
        // 通过 Feign 查询用户收货地址，填充到订单
        try {
            Result<UserAddressDTO> result = userFeignClient.getAddressById(dto.getAddressId());
            if (result.getCode() != 200 || result.getData() == null) {
                log.error("获取收货地址失败，addressId={}，code={}，message={}",
                        dto.getAddressId(), result.getCode(), result.getMessage());
                throw new BusinessException(
                        result.getCode() != 0 ? result.getCode() : ResultCodeEnum.PARAM_ERROR.getCode(),
                        result.getMessage() != null ? result.getMessage() : "收货地址不存在");
            }
            UserAddressDTO addr = result.getData();
            order.setReceiverName(addr.getReceiverName());
            order.setPhone(addr.getPhone());
            order.setAddress(addr.getProvince() + addr.getCity() + addr.getDistrict() + addr.getDetail());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询收货地址失败: addressId={}", dto.getAddressId(), e);
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收货地址不存在");
        }
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        // 4. Save order items — 通过 Feign 动态获取商品名称、规格、图片等真实数据
        List<StockOperationDTO> stockLocks = new ArrayList<>();
        for (var itemDTO : dto.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(itemDTO.getSkuId());
            
            // 使用 SKU 真实价格
            SkuVO sku = skuMap.get(itemDTO.getSkuId());
            item.setPrice(sku.getPrice());
            item.setQuantity(itemDTO.getQuantity());
            item.setAmount(sku.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            // 填充商品信息
            item.setProductName(sku.getProductName());
            item.setProductId(sku.getProductId());
            item.setImage(sku.getImage());
            // 将 SKU 规格 Map 转为可读字符串作为规格描述
            if (sku.getSpecValues() != null && !sku.getSpecValues().isEmpty()) {
                item.setSpecDesc(String.join("; ", sku.getSpecValues().values()));
            }
            
            orderItemMapper.insert(item);

            StockOperationDTO stockDTO = new StockOperationDTO();
            stockDTO.setOrderId(order.getId());
            stockDTO.setSkuId(itemDTO.getSkuId());
            stockDTO.setQuantity(itemDTO.getQuantity());
            stockLocks.add(stockDTO);
        }

        // 5. 通过 Feign 远程锁定库存（跨服务事务由 Seata AT 模式管理）
        // 锁库失败时抛出异常，Spring 事务回滚第2、3步的订单和订单项
        try {
            inventoryFeignClient.lockStock(stockLocks);
        } catch (Exception e) {
            log.error("库存锁定失败，订单创建回滚: orderNo={}", order.getOrderNo(), e);
            throw new BusinessException(ResultCodeEnum.STOCK_LOCK_FAILED);
        }

        // 6. Record log
        saveOrderLog(order.getId(), null, order.getStatus(), "用户下单", "SYSTEM");

        // 7. 发送订单创建事件通知购物车服务清除已下单商品
        try {
            orderEventProducer.sendOrderCreated(userId, order.getId(), order.getOrderNo());
        } catch (Exception e) {
            log.error("发送订单创建事件失败: orderNo={}, userId={}", order.getOrderNo(), userId, e);
            // MQ发送失败不阻断下单流程，购物车数据由定时任务兜底清理
        }

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
        List<Order> orders = p.getRecords();

        // 批量加载订单项，避免 N+1 查询：先收集所有订单 ID，一次性查询全部订单项
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderItem>> itemMap = Collections.emptyMap();
        if (!orderIds.isEmpty()) {
            List<OrderItem> allItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
            itemMap = allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        }
        final Map<Long, List<OrderItem>> finalItemMap = itemMap;

        List<OrderVO> records = orders.stream()
                .map(o -> toVO(o, finalItemMap.getOrDefault(o.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
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

    /**
     * 将 Order 实体转换为 OrderVO。
     * <p>
     * 会单独查询该订单的订单项（适用单条查询场景如 getById、createOrder）。
     * 批量场景请使用 {@link #toVO(Order, List)} 避免 N+1 查询。
     */
    private OrderVO toVO(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        return toVO(order, items);
    }

    /**
     * 将 Order 实体转换为 OrderVO，使用调用方预先批量加载的订单项。
     * <p>
     * 批量查询场景（如分页列表）应在外部一次性加载所有订单项，
     * 通过此方法传入，避免每个订单单独查询造成 N+1 问题。
     *
     * @param order 订单实体
     * @param items 该订单的订单项列表（由调用方批量加载后分组传入）
     */
    private OrderVO toVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");

        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);
        return vo;
    }
}
