package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.order.model.dto.StockOperationDTO;
import com.ecommerce.order.feign.InventoryFeignClient;
import com.ecommerce.order.feign.UserAdminFeignClient;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderLogMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import com.ecommerce.order.model.entity.OrderLog;
import com.ecommerce.order.model.vo.OrderItemVO;
import com.ecommerce.order.model.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final UserAdminFeignClient userAdminFeignClient;
    private final Map<Long, String> usernameCache = new java.util.concurrent.ConcurrentHashMap<>();

    @GetMapping
    @Operation(summary = "管理端订单分页")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {

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
        List<Order> orders = p.getRecords();

        // 批量查用户名（去重后一次 Feign 调用）
        Map<Long, String> usernameMap = fetchUsernames(orders);

        List<OrderVO> records = orders.stream()
                .map(o -> toVO(o, usernameMap, null))
                .collect(Collectors.toList());
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), records));
    }

    @GetMapping("/{id}")
    @Operation(summary = "管理端订单详情")
    public Result<OrderVO> getById(@PathVariable("id") Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        // 单条详情：单独查一次该用户的名称
        String username = null;
        if (order.getUserId() != null) {
            try {
                Map<Long, String> names = userAdminFeignClient.getUsernames(List.of(order.getUserId())).getData();
                if (names != null) username = names.get(order.getUserId());
            } catch (Exception e) {
                log.warn("查询订单详情用户名称失败: {}", e.getMessage());
            }
        }
        return Result.success(toVO(order, null, username));
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "管理端发货")
    @Transactional
    public Result<Void> ship(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
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
    public Result<Void> cancel(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
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

        // 1. 提前组装库存释放请求（事务内读取订单项，afterCommit 使用）
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        final List<StockOperationDTO> releases = items.stream().map(item -> {
            StockOperationDTO dto = new StockOperationDTO();
            dto.setOrderId(order.getId());
            dto.setSkuId(item.getSkuId());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        // 2. 事务内先更新订单状态 + 日志（失败则整体回滚，避免库存已释放但订单仍未取消）
        int fromStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED.getCode());
        orderMapper.updateById(order);
        saveOrderLog(order.getId(), fromStatus, order.getStatus(), "管理员取消 - " + reason, "ADMIN");

        // 3. 事务提交成功后再触发库存释放（不一致场景由对账任务补偿）
        final Long orderId = order.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    inventoryFeignClient.releaseStock(releases);
                    log.info("管理端取消-库存释放成功: orderId={}", orderId);
                } catch (Exception e) {
                    log.error("管理端取消订单-库存释放失败: orderId={}", orderId, e);
                }
            }
        });

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

    /**
     * 批量获取用户名（带本地轻量缓存，避免同个用户被重复查询）
     */
    private Map<Long, String> fetchUsernames(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<Long> needFetch = orders.stream()
                .map(Order::getUserId)
                .filter(id -> id != null && !usernameCache.containsKey(id))
                .distinct()
                .collect(Collectors.toList());
        if (!needFetch.isEmpty()) {
            try {
                Result<Map<Long, String>> result = userAdminFeignClient.getUsernames(needFetch);
                if (result != null && result.getData() != null) {
                    usernameCache.putAll(result.getData());
                }
            } catch (Exception e) {
                log.warn("批量查询用户名称失败，将降级显示为空: {}", e.getMessage());
            }
        }
        return usernameCache;
    }

    private OrderVO toVO(Order order, Map<Long, String> usernameMap, String explicitUsername) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");

        // 填充订单项列表
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO ivo = new OrderItemVO();
            BeanUtils.copyProperties(item, ivo);
            return ivo;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        // 填充用户名：优先使用显式传入值，其次查 Map
        if (explicitUsername != null) {
            vo.setUsername(explicitUsername);
        } else if (usernameMap != null && order.getUserId() != null) {
            String name = usernameMap.get(order.getUserId());
            vo.setUsername(name != null ? name : ("用户" + order.getUserId()));
        } else if (order.getUserId() != null) {
            vo.setUsername("用户" + order.getUserId());
        } else {
            vo.setUsername("-");
        }

        return vo;
    }

    /** 兼容旧调用 —— 内部委托到带 Map 版本 */
    @SuppressWarnings("unused")
    private OrderVO toVO(Order order) {
        return toVO(order, null, null);
    }
}