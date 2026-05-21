package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.order.feign.InventoryFeignClient;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderLogMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import com.ecommerce.order.model.entity.OrderLog;
import com.ecommerce.order.model.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "管理端-订单管理", description = "管理员订单列表、发货、取消")
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final InventoryFeignClient inventoryFeignClient;

    @GetMapping
    @Operation(summary = "管理端订单分页")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(orderNo != null && !orderNo.isBlank(), Order::getOrderNo, orderNo)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);

        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(Order::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(Order::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        Page<Order> p = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<OrderVO> records = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), records));
    }

    @GetMapping("/{id}")
    @Operation(summary = "管理端订单详情")
    public Result<OrderVO> getById(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        return Result.success(toVO(order));
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "管理端发货")
    @Transactional
    public Result<Void> ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String trackingNo = body.get("trackingNo");
        String logisticsCompany = body.get("logisticsCompany");

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (order.getStatus() != OrderStatusEnum.PENDING_DELIVER.getCode()) {
            throw new BusinessException(ResultCodeEnum.ORDER_CANNOT_CANCEL);
        }

        int fromStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.DELIVERED.getCode());
        order.setDeliverTime(LocalDateTime.now());
        orderMapper.updateById(order);

        String remark = "管理员发货 - " + logisticsCompany + ": " + trackingNo;
        saveOrderLog(order.getId(), fromStatus, order.getStatus(), remark, "ADMIN");

        log.info("管理员发货: orderNo={}, trackingNo={}", order.getOrderNo(), trackingNo);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "管理端取消订单")
    @Transactional
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        int currentStatus = order.getStatus();
        if (currentStatus != OrderStatusEnum.PENDING_PAY.getCode()
                && currentStatus != OrderStatusEnum.PENDING_DELIVER.getCode()) {
            throw new BusinessException(ResultCodeEnum.ORDER_CANNOT_CANCEL);
        }

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
            log.error("管理端取消订单-库存释放失败: orderId={}", order.getId(), e);
        }

        int fromStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED.getCode());
        orderMapper.updateById(order);

        saveOrderLog(order.getId(), fromStatus, order.getStatus(), "管理员取消 - " + reason, "ADMIN");

        log.info("管理员取消订单: orderNo={}, reason={}", order.getOrderNo(), reason);
        return Result.success();
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
        return vo;
    }
}