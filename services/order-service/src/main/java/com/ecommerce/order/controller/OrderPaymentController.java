package com.ecommerce.order.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.order.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付、退款、回调")
public class OrderPaymentController {

    private final PaymentService paymentService;

    /** 发起支付 — 金融敏感接口，需要限流和降级保护 */
    @PostMapping("/pay")
    @Operation(summary = "发起支付")
    public Result<Map<String, String>> pay(@RequestParam("orderId") Long orderId,
                               @RequestParam("orderNo") String orderNo,
                               @RequestParam("amount") BigDecimal amount,
                               @RequestParam(name = "channel", defaultValue = "alipay") String channel) {
        Map<String, String> result = paymentService.createPayment(
                UserContext.currentUserId(), orderId, orderNo, amount, channel);
        return Result.success(result);
    }

    @PostMapping("/callback/{channel}")
    @Operation(summary = "支付回调")
    public Result<Void> callback(@PathVariable("channel") String channel,
                                  @RequestParam("paymentNo") String paymentNo,
                                  @RequestParam("transactionNo") String transactionNo) {
        paymentService.handleCallback(channel, paymentNo, transactionNo);
        return Result.success();
    }

    @PostMapping("/callback/simulate/{orderNo}")
    @Operation(summary = "模拟支付成功（前端测试用）")
    public Result<Void> simulateCallback(@PathVariable("orderNo") String orderNo) {
        paymentService.simulatePayment(orderNo);
        return Result.success();
    }

    @PostMapping("/refund")
    @Operation(summary = "申请退款")
    public Result<Void> refund(@RequestParam("orderId") Long orderId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(required = false, value = "reason") String reason) {
        paymentService.requestRefund(UserContext.currentUserId(), orderId, amount,
                reason != null ? reason : "用户申请退款");
        return Result.success();
    }

    @PostMapping("/refund/apply")
    @Operation(summary = "申请退款/退货（支持退货退款）")
    public Result<Map<String, Object>> applyRefund(@RequestBody Map<String, Object> body) {
        Long orderId = toLong(body.get("orderId"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Integer refundType = (Integer) body.getOrDefault("refundType", 1);
        String refundReason = (String) body.get("refundReason");
        String reason = (String) body.getOrDefault("reason", "");
        String refundImages = (String) body.get("refundImages");
        Long refundId = paymentService.requestRefund(
                UserContext.currentUserId(), orderId, amount, reason,
                refundType, refundReason, refundImages);
        return Result.success(Map.of("refundId", refundId));
    }

    @PutMapping("/refund/{refundId}/logistics")
    @Operation(summary = "填写退货物流单号")
    public Result<Void> submitReturnLogistics(@PathVariable("refundId") Long refundId,
                                               @RequestBody Map<String, Object> body) {
        String logisticsNo = (String) body.get("logisticsNo");
        paymentService.submitReturnLogistics(refundId, logisticsNo);
        return Result.success();
    }

    @GetMapping("/refund/{orderId}")
    @Operation(summary = "查询订单退款信息")
    public Result<Map<String, Object>> getRefundInfo(@PathVariable("orderId") Long orderId) {
        return Result.success(paymentService.getRefundInfo(orderId));
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "查询支付状态")
    public Result<Integer> status(@PathVariable("orderNo") String orderNo) {
        return Result.success(paymentService.getStatus(orderNo));
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return null; }
    }
}