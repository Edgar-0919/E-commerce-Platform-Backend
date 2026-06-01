package com.ecommerce.inventory.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.core.model.Result;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "库存管理", description = "库存查询、锁定、释放、扣减")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{skuId}")
    @Operation(summary = "查询库存")
    public Result<Integer> getStock(@PathVariable Long skuId) {
        return Result.success(inventoryService.getStock(skuId));
    }

    /**
     * 库存锁定 — 防超卖核心操作，需要限流和降级保护
     */
    @PostMapping("/lock")
    @Operation(summary = "锁定库存（内部调用）")
    @SentinelResource(value = "lockStock", blockHandler = "lockBlockHandler", fallback = "lockFallback")
    public Result<Void> lock(@Valid @RequestBody List<StockOperationDTO> items) {
        inventoryService.lockStock(items);
        return Result.success();
    }

    @PostMapping("/release")
    @Operation(summary = "释放库存（内部调用）")
    public Result<Void> release(@Valid @RequestBody List<StockOperationDTO> items) {
        inventoryService.releaseStock(items);
        return Result.success();
    }

    /**
     * 库存扣减 — 支付成功后执行，需要限流保护
     */
    @PostMapping("/deduct")
    @Operation(summary = "扣减库存（内部调用）")
    @SentinelResource(value = "deductStock", blockHandler = "deductBlockHandler", fallback = "deductFallback")
    public Result<Void> deduct(@Valid @RequestBody List<StockOperationDTO> items) {
        inventoryService.deductStock(items);
        return Result.success();
    }

    // ===== Sentinel fallback/blockHandler =====

    public Result<Void> lockBlockHandler(List<StockOperationDTO> items, BlockException ex) {
        log.warn("[库存] 库存锁定被Sentinel限流 — items={}", items.size());
        return Result.fail(429, "库存锁定请求繁忙，请稍后再试");
    }

    public Result<Void> lockFallback(List<StockOperationDTO> items, Throwable ex) {
        log.error("[库存] 库存锁定服务降级 —", ex);
        return Result.fail(503, "库存服务暂时不可用");
    }

    public Result<Void> deductBlockHandler(List<StockOperationDTO> items, BlockException ex) {
        log.warn("[库存] 库存扣减被Sentinel限流 — items={}", items.size());
        return Result.fail(429, "库存扣减请求繁忙，请稍后再试");
    }

    public Result<Void> deductFallback(List<StockOperationDTO> items, Throwable ex) {
        log.error("[库存] 库存扣减服务降级 —", ex);
        return Result.fail(503, "库存服务暂时不可用");
    }

    @GetMapping("/alert")
    @Operation(summary = "库存预警列表")
    public Result<List<Long>> alert() {
        return Result.success(inventoryService.getLowStockSkus());
    }

    @PutMapping("/sync/{skuId}")
    @Operation(summary = "同步库存到Redis")
    public Result<Void> sync(@PathVariable Long skuId) {
        inventoryService.syncStockToRedis(skuId);
        return Result.success();
    }
}
