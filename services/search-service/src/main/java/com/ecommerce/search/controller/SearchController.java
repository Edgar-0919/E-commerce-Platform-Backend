package com.ecommerce.search.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索服务", description = "商品搜索、建议、过滤")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/product")
    @Operation(summary = "商品搜索")
    public Result<List<ProductDocument>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "id") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(keyword, categoryId, brandId,
                minPrice, maxPrice, sortField, sortOrder, page, size));
    }

    @GetMapping("/suggest")
    @Operation(summary = "搜索建议")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }

    @GetMapping("/filter")
    @Operation(summary = "筛选条件")
    public Result<Map<String, Object>> filters(@RequestParam(required = false) String keyword) {
        return Result.success(searchService.getFilters(keyword));
    }
}
