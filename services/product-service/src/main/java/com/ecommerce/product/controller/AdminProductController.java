package com.ecommerce.product.controller;

import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "管理端-商品管理", description = "商品CRUD、上下架、删除")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "商品列表")
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
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

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品（逻辑删除）")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}