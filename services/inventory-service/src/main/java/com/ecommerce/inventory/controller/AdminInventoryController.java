package com.ecommerce.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.Result;
import com.ecommerce.inventory.mapper.StockLogMapper;
import com.ecommerce.inventory.mapper.StockMapper;
import com.ecommerce.inventory.model.entity.Stock;
import com.ecommerce.inventory.model.entity.StockLog;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "管理端-库存管理", description = "库存预警、库存查询、Redis同步、库存调整")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;

    @GetMapping("/list")
    @Operation(summary = "库存分页列表")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean lowStock) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<Stock>();
        wrapper.eq(productId != null, Stock::getProductId, productId);
        if (lowStock != null && lowStock) {
            wrapper.apply("available_stock < safety_stock");
        }
        wrapper.orderByDesc(Stock::getAvailableStock);
        Page<Stock> result = stockMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/alert")
    @Operation(summary = "低库存预警列表")
    public Result<List<Stock>> alert() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .apply("available_stock <= safety_stock"));
        return Result.success(stocks);
    }

    @GetMapping("/{skuId}")
    @Operation(summary = "查询SKU库存")
    public Result<Integer> getStock(@PathVariable Long skuId) {
        return Result.success(inventoryService.getStock(skuId));
    }

    @PutMapping("/{skuId}/adjust")
    @Operation(summary = "调整库存")
    public Result<Void> adjust(@PathVariable Long skuId,
                                @RequestParam Integer delta,
                                @RequestParam(required = false, defaultValue = "管理员手动调整") String remark) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getSkuId, skuId));
        if (stock == null) {
            return Result.success();
        }
        int beforeQty = stock.getAvailableStock();
        stock.setTotalStock(stock.getTotalStock() + delta);
        stock.setAvailableStock(stock.getAvailableStock() + delta);
        stockMapper.updateById(stock);

        StockLog log = new StockLog();
        log.setSkuId(skuId);
        log.setType(delta >= 0 ? "increase" : "decrease");
        log.setQuantity(Math.abs(delta));
        log.setBeforeQty(beforeQty);
        log.setAfterQty(stock.getAvailableStock());
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        stockLogMapper.insert(log);

        inventoryService.syncStockToRedis(skuId);
        return Result.success();
    }

    @GetMapping("/log/{skuId}")
    @Operation(summary = "库存变更日志")
    public Result<List<StockLog>> log(@PathVariable Long skuId,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        Page<StockLog> result = stockLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<StockLog>()
                        .eq(StockLog::getSkuId, skuId)
                        .orderByDesc(StockLog::getCreateTime));
        return Result.success(result.getRecords());
    }

    @PutMapping("/sync/{skuId}")
    @Operation(summary = "同步库存到Redis")
    public Result<Void> sync(@PathVariable Long skuId) {
        inventoryService.syncStockToRedis(skuId);
        return Result.success();
    }
}