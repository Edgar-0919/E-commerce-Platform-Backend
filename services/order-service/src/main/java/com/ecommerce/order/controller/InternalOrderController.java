package com.ecommerce.order.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单服务内部接口 — 供其他微服务通过 Feign 调用
 * <p>
 * 这些接口不对外暴露，仅供 payment-service 等服务在支付回调、退款回调时更新订单状态。
 * Gateway 白名单不包含此路径，调用依赖 Feign 内部通信链路。
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/order")
@RequiredArgsConstructor
@Tag(name = "内部-订单", description = "供微服务间调用的订单内部接口")
public class InternalOrderController {

    private final OrderService orderService;

    /** 更新订单状态（支付回调、退款回调等事件驱动） */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = (Integer) body.get("status");
        String operator = (String) body.getOrDefault("operator", "SYSTEM");
        orderService.updateStatus(id, status, operator);
        log.info("内部调用-订单状态更新: orderId={}, status={}, operator={}", id, status, operator);
        return Result.success();
    }
}