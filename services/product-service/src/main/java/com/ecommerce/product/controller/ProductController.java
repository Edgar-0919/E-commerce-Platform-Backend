package com.ecommerce.product.controller;


import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.model.vo.SkuVO;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品、分类、SKU")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    @Operation(summary = "商品分页列表")
    public Result<PageResult<ProductVO>> page(ProductQueryDTO query) {
        if (query.getStatus() == null) {
            query.setStatus(1);
        }
        return Result.success(productService.page(query));
    }

    /**
     * 商品详情 — 电商流量最大接口，需限流和热点参数保护
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<ProductVO> getById(@PathVariable("id") Long id) {
        ProductVO vo = productService.getById(id);
        if (vo == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        if (vo.getStatus() != null && vo.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        return Result.success(vo);
    }

    @PostMapping
    @Operation(summary = "新增商品")
    public Result<Void> save(@Valid @RequestBody ProductSaveDTO dto) {
        productService.save(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ProductSaveDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "商品上下架")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        productService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/category/tree")
    @Operation(summary = "分类树")
    public Result<List<CategoryVO>> categoryTree() {
        return Result.success(productService.categoryTree());
    }

    @GetMapping("/sku/{id}")
    @Operation(summary = "SKU详情")
    public Result<SkuVO> getSku(@PathVariable("id") Long id) {
        return Result.success(productService.getSkuById(id));
    }

    @GetMapping("/names")
    @Operation(summary = "批量查询商品名称（id → name）")
    public Result<Map<Long, String>> getNames(@RequestParam("ids") List<Long> ids) {
        return Result.success(productService.getProductNames(ids));
    }

    /**
     * 商品搜索 — 基于 MySQL LIKE 替代 ES 全文搜索
     */
    @GetMapping("/search")
    @Operation(summary = "商品搜索")
    public Result<PageResult<ProductVO>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setSortBy(sortBy);
        query.setPage(page);
        query.setSize(size);
        query.setStatus(1);
        return Result.success(productService.search(query));
    }
}
