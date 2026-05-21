package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.Result;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.mapper.RefundMapper;
import com.ecommerce.payment.model.entity.Payment;
import com.ecommerce.payment.model.entity.Refund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "管理端-退款管理", description = "退款审核、退款列表")
public class AdminRefundController {

    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;

    @GetMapping
    @Operation(summary = "退款分页列表")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>()
                .eq(status != null, Refund::getStatus, status)
                .orderByDesc(Refund::getCreateTime);
        Page<Refund> result = refundMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "退款详情")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            return Result.success(null);
        }
        Payment payment = paymentMapper.selectById(refund.getPaymentId());
        Map<String, Object> data = new HashMap<>();
        data.put("refund", refund);
        data.put("payment", payment);
        return Result.success(data);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过退款")
    public Result<Void> approve(@PathVariable Long id) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null || refund.getStatus() != 0) {
            return Result.success();
        }
        refund.setStatus(1);
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        Payment payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment != null) {
            payment.setStatus(3);
            payment.setUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
        }
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝退款")
    public Result<Void> reject(@PathVariable Long id) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null || refund.getStatus() != 0) {
            return Result.success();
        }
        refund.setStatus(2);
        refund.setUpdateTime(LocalDateTime.now());
        refundMapper.updateById(refund);
        return Result.success();
    }
}