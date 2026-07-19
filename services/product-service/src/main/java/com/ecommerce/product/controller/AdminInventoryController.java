package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.Result;
import com.ecommerce.product.inventory.mapper.StockLogMapper;
import com.ecommerce.product.inventory.mapper.StockMapper;
import com.ecommerce.product.inventory.model.entity.Stock;
import com.ecommerce.product.inventory.model.entity.StockLog;
import com.ecommerce.product.inventory.model.vo.StockVO;
import com.ecommerce.product.inventory.InventoryService;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "管理端-库存管理", description = "库存预警、库存查询、Redis同步、库存调整")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final ProductService productService;

    @GetMapping("/list")
    @Operation(summary = "库存分页列表（含商品名称）")
    public Result<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "lowStock", required = false) Boolean lowStock) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<Stock>();
        wrapper.eq(productId != null, Stock::getProductId, productId);
        if (lowStock != null && lowStock) {
            wrapper.apply("available_stock < safety_stock");
        }
        wrapper.orderByDesc(Stock::getAvailableStock);
        Page<Stock> result = stockMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查询商品名称（改 Feign 调用为本地调用）
        List<Stock> stocks = result.getRecords();
        Map<Long, String> productNameMap = Collections.emptyMap();
        if (!stocks.isEmpty()) {
            List<Long> productIds = stocks.stream()
                    .map(Stock::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            try {
                productNameMap = productService.getProductNames(productIds);
                if (productNameMap == null) productNameMap = Collections.emptyMap();
            } catch (Exception e) {
                log.warn("批量查询商品名称失败，将显示为空: {}", e.getMessage());
            }
        }

        // 组装 StockVO
        Map<Long, String> finalProductNameMap = productNameMap;
        List<StockVO> voList = stocks.stream().map(stock -> {
            StockVO vo = new StockVO();
            BeanUtils.copyProperties(stock, vo);
            vo.setProductName(finalProductNameMap.getOrDefault(stock.getProductId(), "—"));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", voList);
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
    public Result<Integer> getStock(@PathVariable("skuId") Long skuId) {
        return Result.success(inventoryService.getStock(skuId));
    }

    @PostMapping("/init")
    @Operation(summary = "批量初始化库存（新增商品/ SKU 后调用）")
    public Result<Void> initStock(@RequestBody List<Map<String, Object>> skuList) {
        if (skuList == null || skuList.isEmpty()) {
            return Result.success();
        }
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (Map<String, Object> item : skuList) {
            Long skuId = item.get("skuId") != null ? Long.valueOf(item.get("skuId").toString()) : null;
            Long productId = item.get("productId") != null ? Long.valueOf(item.get("productId").toString()) : null;
            int qty = item.get("stock") != null ? Integer.parseInt(item.get("stock").toString()) : 0;
            if (skuId == null) continue;

            // 防重复：若该 SKU 已存在库存记录，跳过初始化
            Long existing = stockMapper.selectCount(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getSkuId, skuId));
            if (existing != null && existing > 0) continue;

            Stock stock = new Stock();
            stock.setSkuId(skuId);
            stock.setProductId(productId);
            stock.setTotalStock(qty);
            stock.setLockedStock(0);
            stock.setAvailableStock(qty);
            stock.setSafetyStock(10);
            stock.setVersion(1);
            stock.setUpdateTime(now);
            stockMapper.insert(stock);
            created++;
        }
        log.info("批量初始化库存完成，新增 {} 条记录", created);
        return Result.success();
    }

    @PutMapping("/{skuId}/adjust")
    @Operation(summary = "调整库存")
    public Result<Void> adjust(@PathVariable("skuId") Long skuId,
                                @RequestParam("delta") Integer delta,
                                @RequestParam(value = "remark", required = false, defaultValue = "管理员手动调整") String remark) {
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
    public Result<List<StockLog>> log(@PathVariable("skuId") Long skuId,
                                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                                       @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<StockLog> result = stockLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<StockLog>()
                        .eq(StockLog::getSkuId, skuId)
                        .orderByDesc(StockLog::getCreateTime));
        return Result.success(result.getRecords());
    }

    @PutMapping("/sync/{skuId}")
    @Operation(summary = "同步库存到Redis")
    public Result<Void> sync(@PathVariable("skuId") Long skuId) {
        inventoryService.syncStockToRedis(skuId);
        return Result.success();
    }
}