package com.ecommerce.search.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.core.model.Result;
import com.ecommerce.search.model.dto.SearchResultDTO;
import com.ecommerce.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索服务", description = "商品搜索、建议、过滤")
public class SearchController {

    private final SearchService searchService;

    /**
     * 商品搜索 — ES 压力大时需要降级保护，返回空结果而非报错
     */
    @GetMapping("/product")
    @Operation(summary = "商品搜索")
    @SentinelResource(value = "productSearch", blockHandler = "searchBlockHandler", fallback = "searchFallback")
    public Result<SearchResultDTO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(keyword, categoryId, brandId,
                minPrice, maxPrice, sortField, sortOrder, page, size));
    }

    public Result<SearchResultDTO> searchBlockHandler(String keyword, Long categoryId,
            Long brandId, BigDecimal minPrice, BigDecimal maxPrice, String sortField,
            String sortOrder, int page, int size, BlockException ex) {
        log.warn("[搜索] 商品搜索被Sentinel限流 — keyword={}", keyword);
        SearchResultDTO empty = new SearchResultDTO();
        empty.setRecords(Collections.emptyList());
        empty.setTotal(0L);
        return Result.success(empty);
    }

    public Result<SearchResultDTO> searchFallback(String keyword, Long categoryId,
            Long brandId, BigDecimal minPrice, BigDecimal maxPrice, String sortField,
            String sortOrder, int page, int size, Throwable ex) {
        log.error("[搜索] 商品搜索服务降级 — keyword={}", keyword, ex);
        SearchResultDTO empty = new SearchResultDTO();
        empty.setRecords(Collections.emptyList());
        empty.setTotal(0L);
        return Result.success(empty);
    }

    @GetMapping("/suggest")
    @Operation(summary = "搜索建议（自动补全）")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }

    @GetMapping("/filter")
    @Operation(summary = "筛选条件聚合")
    public Result<Map<String, Object>> filters(@RequestParam(required = false) String keyword) {
        return Result.success(searchService.getFilters(keyword));
    }
}
