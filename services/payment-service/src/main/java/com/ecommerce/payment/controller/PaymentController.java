package com.ecommerce.payment.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付、退款、回调")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    @Operation(summary = "发起支付")
    public Result<String> pay(@RequestParam Long orderId,
                               @RequestParam String orderNo,
                               @RequestParam BigDecimal amount,
                               @RequestParam(defaultValue = "alipay") String channel) {
        String paymentNo = paymentService.createPayment(
                UserContext.currentUserId(), orderId, orderNo, amount, channel);
        return Result.success(paymentNo);
    }

    @PostMapping("/callback/{channel}")
    @Operation(summary = "支付回调")
    public Result<Void> callback(@PathVariable String channel,
                                  @RequestParam String paymentNo,
                                  @RequestParam String transactionNo) {
        paymentService.handleCallback(channel, paymentNo, transactionNo);
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
