package com.ecommerce.order.controller;


import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.order.model.dto.OrderCreateDTO;
import com.ecommerce.order.model.vo.OrderVO;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 * <p>
 * 支持前端传递字符串状态（如 PENDING, PAID）或数字状态码
 * 前端状态映射：
 * - PENDING → 待支付 (0)
 * - PAID → 待发货 (1)
 * - COMPLETED → 待收货 (2)
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单创建、查询、取消")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单 — 电商核心链路
     */
    @PostMapping
    @Operation(summary = "创建订单")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        OrderVO vo = orderService.createOrder(UserContext.currentUserId(), dto);
        return Result.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "订单分页")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "status", required = false) String status) {
        Integer statusCode = parseStatus(status);
        return Result.success(orderService.page(UserContext.currentUserId(), page, size, statusCode));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> getById(@PathVariable("id") Long id) {
        return Result.success(orderService.getById(id));
    }

    @GetMapping("/status/{orderNo}")
    @Operation(summary = "查询订单状态")
    public Result<OrderVO> getByOrderNo(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        orderService.cancel(UserContext.currentUserId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    @Operation(summary = "确认收货")
    public Result<Void> receive(@PathVariable("id") Long id) {
        orderService.confirmReceive(UserContext.currentUserId(), id);
        return Result.success();
    }

    /**
     * 解析前端传递的状态参数
     * 支持字符串（PENDING/PAID/COMPLETED）或数字状态码
     *
     * @param status 前端传递的状态参数
     * @return 后端状态码
     */
    private Integer parseStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            return switch (status.toUpperCase()) {
                case "PENDING" -> OrderStatusEnum.PENDING_PAY.getCode();      // 待支付
                case "PAID" -> OrderStatusEnum.PENDING_DELIVER.getCode();    // 待发货
                case "COMPLETED" -> OrderStatusEnum.DELIVERED.getCode();    // 待收货（已发货）
                case "RECEIVED" -> OrderStatusEnum.RECEIVED.getCode();      // 已收货
                case "CANCELLED" -> OrderStatusEnum.CANCELLED.getCode();    // 已取消
                case "REFUNDING" -> OrderStatusEnum.REFUNDING.getCode();    // 退款中
                case "REFUNDED" -> OrderStatusEnum.REFUNDED.getCode();      // 已退款
                case "AFTERSALE" -> -1;                                      // 售后（已收货+退款中+已退款）
                default -> null;
            };
        }
    }
}
