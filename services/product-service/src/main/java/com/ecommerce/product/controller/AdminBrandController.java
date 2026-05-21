package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.product.mapper.BrandMapper;
import com.ecommerce.product.model.entity.Brand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
@Tag(name = "管理端-品牌管理", description = "品牌CRUD、启用/禁用")
public class AdminBrandController {

    private final BrandMapper brandMapper;

    @GetMapping
    @Operation(summary = "品牌列表")
    public Result<PageResult<Brand>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<Brand>()
                .like(StringUtils.hasText(keyword), Brand::getName, keyword)
                .orderByAsc(Brand::getSort);
        Page<Brand> p = brandMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords()));
    }

    @PostMapping
    @Operation(summary = "新增品牌")
    public Result<Void> save(@RequestBody Brand brand) {
        brand.setCreateTime(LocalDateTime.now());
        brandMapper.insert(brand);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改品牌")
    public Result<Void> update(@PathVariable Long id, @RequestBody Brand brand) {
        Brand existing = brandMapper.selectById(id);
        if (existing != null) {
            BeanUtils.copyProperties(brand, existing, "id", "createTime");
            existing.setUpdateTime(LocalDateTime.now());
            brandMapper.updateById(existing);
        }
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用品牌")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Brand brand = brandMapper.selectById(id);
        if (brand != null) {
            brand.setStatus(status);
            brand.setUpdateTime(LocalDateTime.now());
            brandMapper.updateById(brand);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除品牌")
    public Result<Void> delete(@PathVariable Long id) {
        brandMapper.deleteById(id);
        return Result.success();
    }
}