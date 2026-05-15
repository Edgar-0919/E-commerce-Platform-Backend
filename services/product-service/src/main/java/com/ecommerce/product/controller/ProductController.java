package com.ecommerce.product.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品、分类、SKU")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    @Operation(summary = "商品分页列表")
    public Result<PageResult<ProductVO>> page(ProductQueryDTO query) {
        return Result.success(productService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增商品")
    public Result<Void> save(@Valid @RequestBody ProductSaveDTO dto) {
        productService.save(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "商品上下架")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
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
    public Result<SkuVO> getSku(@PathVariable Long id) {
        return Result.success(productService.getSkuById(id));
    }
}
