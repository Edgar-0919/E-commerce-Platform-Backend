package com.ecommerce.order.controller;

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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "订单创建、查询、取消")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        OrderVO vo = orderService.createOrder(UserContext.currentUserId(), dto);
        return Result.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "订单分页")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(orderService.page(UserContext.currentUserId(), page, size, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @GetMapping("/status/{orderNo}")
    @Operation(summary = "查询订单状态")
    public Result<OrderVO> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.currentUserId(), id);
        return Result.success();
    }
}
