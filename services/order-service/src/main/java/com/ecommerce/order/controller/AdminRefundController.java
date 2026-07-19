package com.ecommerce.order.controller;

import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.order.payment.model.entity.Refund;
import com.ecommerce.order.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-退款管理
 * <p>
 * 退款流程：
 * 1. C端用户提交退款申请 → 退款单状态=处理中(0)，订单/支付状态=退款中
 * 2. B端管理员审核 → 通过则调用支付渠道退款 → 退款单=已退款(1)，订单/支付=已退款
 * 3. B端管理员审核 → 拒绝则回退 → 退款单=已拒绝(2)，订单/支付回退到退款前状态
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "管理端-退款管理", description = "退款审核、退款列表")
public class AdminRefundController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "退款分页列表")
    public Result<PageResult<Refund>> page(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "status", required = false) Integer status) {
        return Result.success(paymentService.refundPage(page, size, status));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过退款")
    public Result<Void> approve(@PathVariable("id") Long id) {
        paymentService.processRefund(id);
        log.info("管理员审核通过退款: refundId={}", id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝退款")
    public Result<Void> reject(@PathVariable("id") Long id) {
        paymentService.declineRefund(id);
        log.info("管理员拒绝退款: refundId={}", id);
        return Result.success();
    }

    @PutMapping("/{id}/confirm-return")
    @Operation(summary = "确认收到退货")
    public Result<Void> confirmReturn(@PathVariable("id") Long id) {
        paymentService.confirmReturnReceived(id);
        log.info("管理员确认收到退货: refundId={}", id);
        return Result.success();
    }
}