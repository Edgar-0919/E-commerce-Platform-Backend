package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.model.Result;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.model.entity.Order;
import com.ecommerce.order.model.entity.OrderItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "管理端-仪表盘", description = "管理端统计数据")
public class AdminDashboardController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @GetMapping("/stats")
    @Operation(summary = "仪表盘统计数据")
    public Result<Map<String, Object>> stats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart));

        List<Order> todayPaidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)
                .in(Order::getStatus,
                        OrderStatusEnum.PENDING_DELIVER.getCode(),
                        OrderStatusEnum.DELIVERED.getCode(),
                        OrderStatusEnum.RECEIVED.getCode()));
        BigDecimal todayRevenue = todayPaidOrders.stream()
                .map(Order::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatusEnum.PENDING_PAY.getCode()));

        long totalOrderCount = orderMapper.selectCount(null);

        Map<String, Object> data = new HashMap<>();
        data.put("todayOrders", todayOrderCount);
        data.put("todayRevenue", todayRevenue);
        data.put("pendingOrders", pendingOrderCount);
        data.put("totalOrders", totalOrderCount);

        return Result.success(data);
    }

    @GetMapping("/trend")
    @Operation(summary = "近30天销售趋势")
    public Result<List<Map<String, Object>>> trend() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(day, LocalTime.MAX);

            List<Order> dayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, start)
                    .le(Order::getCreateTime, end)
                    .in(Order::getStatus,
                            OrderStatusEnum.PENDING_DELIVER.getCode(),
                            OrderStatusEnum.DELIVERED.getCode(),
                            OrderStatusEnum.RECEIVED.getCode()));

            BigDecimal dayRevenue = dayOrders.stream()
                    .map(Order::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> point = new HashMap<>();
            point.put("date", day.toString());
            point.put("orderCount", dayOrders.size());
            point.put("revenue", dayRevenue);
            result.add(point);
        }

        return Result.success(result);
    }

    @GetMapping("/top-products")
    @Operation(summary = "商品销量排行Top10")
    public Result<List<Map<String, Object>>> topProducts() {
        QueryWrapper<OrderItem> wrapper = new QueryWrapper<>();
        wrapper.select("product_name", "SUM(quantity) AS totalQty")
                .groupBy("product_name")
                .orderByDesc("totalQty")
                .last("LIMIT 10");

        List<Map<String, Object>> raw = orderItemMapper.selectMaps(wrapper);
        return Result.success(raw);
    }
}