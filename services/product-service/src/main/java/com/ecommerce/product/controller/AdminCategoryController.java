package com.ecommerce.product.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.model.entity.Category;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Tag(name = "管理端-分类管理", description = "分类CRUD、排序调整")
public class AdminCategoryController {

    private final ProductService productService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    @Operation(summary = "分类树")
    public Result<List<CategoryVO>> tree() {
        return Result.success(productService.categoryTree());
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public Result<Void> save(@RequestBody Category category) {
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        Category existing = categoryMapper.selectById(id);
        if (existing != null) {
            BeanUtils.copyProperties(category, existing, "id", "createTime");
            existing.setUpdateTime(LocalDateTime.now());
            categoryMapper.updateById(existing);
        }
        return Result.success();
    }

    @PutMapping("/{id}/sort")
    @Operation(summary = "调整排序")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sort) {
        Category category = categoryMapper.selectById(id);
        if (category != null) {
            category.setSort(sort);
            category.setUpdateTime(LocalDateTime.now());
            categoryMapper.updateById(category);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.success();
    }
}