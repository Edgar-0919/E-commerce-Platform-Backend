package com.ecommerce.payment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付、退款、回调")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 发起支付 — 金融敏感接口，需要限流和降级保护
     */
    @PostMapping("/pay")
    @Operation(summary = "发起支付")
    @SentinelResource(value = "createPayment", blockHandler = "payBlockHandler", fallback = "payFallback")
    public Result<Map<String, String>> pay(@RequestParam Long orderId,
                               @RequestParam String orderNo,
                               @RequestParam BigDecimal amount,
                               @RequestParam(defaultValue = "alipay") String channel) {
        Map<String, String> result = paymentService.createPayment(
                UserContext.currentUserId(), orderId, orderNo, amount, channel);
        return Result.success(result);
    }

    public Result<Map<String, String>> payBlockHandler(Long orderId, String orderNo,
            BigDecimal amount, String channel, BlockException ex) {
        log.warn("[支付] 支付请求被Sentinel限流 — orderNo={}", orderNo);
        return Result.fail(429, "支付请求繁忙，请稍后再试");
    }

    public Result<Map<String, String>> payFallback(Long orderId, String orderNo,
            BigDecimal amount, String channel, Throwable ex) {
        log.error("[支付] 支付服务降级 — orderNo={}", orderNo, ex);
        return Result.fail(503, "支付服务暂时不可用");
    }

    @PostMapping("/callback/{channel}")
    @Operation(summary = "支付回调")
    public Result<Void> callback(@PathVariable String channel,
                                  @RequestParam String paymentNo,
                                  @RequestParam String transactionNo) {
        paymentService.handleCallback(channel, paymentNo, transactionNo);
        return Result.success();
    }

    @PostMapping("/callback/simulate/{orderNo}")
    @Operation(summary = "模拟支付成功（前端测试用）")
    public Result<Void> simulateCallback(@PathVariable String orderNo) {
        paymentService.simulatePayment(orderNo);
        return Result.success();
    }

    @PostMapping("/refund")
    @Operation(summary = "申请退款")
    public Result<Void> refund(@RequestParam Long orderId,
                                @RequestParam BigDecimal amount,
                                @RequestParam(required = false) String reason) {
        paymentService.refund(UserContext.currentUserId(), orderId, amount,
                reason != null ? reason : "用户申请退款");
        return Result.success();
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询支付状态")
    public Result<Integer> status(@PathVariable String orderNo) {
        return Result.success(paymentService.getStatus(orderNo));
    }
}
