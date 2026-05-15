package com.ecommerce.inventory.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/lock")
    @Operation(summary = "锁定库存（内部调用）")
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

    @PostMapping("/deduct")
    @Operation(summary = "扣减库存（内部调用）")
    public Result<Void> deduct(@Valid @RequestBody List<StockOperationDTO> items) {
        inventoryService.deductStock(items);
        return Result.success();
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
