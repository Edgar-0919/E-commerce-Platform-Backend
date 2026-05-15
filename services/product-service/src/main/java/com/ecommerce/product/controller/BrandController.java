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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product/brand")
@RequiredArgsConstructor
@Tag(name = "品牌管理")
public class BrandController {

    private final BrandMapper brandMapper;

    @GetMapping("/page")
    @Operation(summary = "品牌分页")
    public Result<PageResult<Brand>> page(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Brand> wrapper = new LambdaQueryWrapper<Brand>()
                .like(StringUtils.hasText(keyword), Brand::getName, keyword)
                .orderByAsc(Brand::getSort);
        Page<Brand> p = brandMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords()));
    }

    @GetMapping("/all")
    @Operation(summary = "全部品牌")
    public Result<java.util.List<Brand>> all() {
        return Result.success(brandMapper.selectList(
                new LambdaQueryWrapper<Brand>()
                        .eq(Brand::getStatus, 1)
                        .orderByAsc(Brand::getSort)));
    }
}
