package com.ecommerce.product.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.mapper.SpecGroupMapper;
import com.ecommerce.product.mapper.SpecParamMapper;
import com.ecommerce.product.model.entity.Category;
import com.ecommerce.product.model.entity.SpecGroup;
import com.ecommerce.product.model.entity.SpecParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/spec")
@RequiredArgsConstructor
@Tag(name = "管理端-规格管理", description = "规格组和规格参数的CRUD")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecController {

    private final SpecGroupMapper specGroupMapper;
    private final SpecParamMapper specParamMapper;
    private final CategoryMapper categoryMapper;

    @Data
    public static class SpecGroupWithCategory {
        private Long id;
        private String name;
        private Long categoryId;
        private String categoryName;
        private Integer sort;
    }

    @GetMapping("/groups")
    @Operation(summary = "获取规格组列表")
    public Result<List<SpecGroupWithCategory>> listGroups(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        List<SpecGroup> groups;
        if (categoryId != null) {
            groups = specGroupMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SpecGroup>()
                            .eq(SpecGroup::getCategoryId, categoryId)
                            .orderByAsc(SpecGroup::getSort));
        } else {
            groups = specGroupMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SpecGroup>()
                            .orderByAsc(SpecGroup::getSort));
        }

        List<Long> categoryIds = groups.stream()
                .map(SpecGroup::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<SpecGroupWithCategory> result = groups.stream().map(g -> {
            SpecGroupWithCategory dto = new SpecGroupWithCategory();
            BeanUtils.copyProperties(g, dto);
            Category cat = categoryMap.get(g.getCategoryId());
            dto.setCategoryName(cat != null ? cat.getName() : "-");
            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @PostMapping("/groups")
    @Operation(summary = "新增规格组")
    public Result<Void> saveGroup(@RequestBody SpecGroup specGroup) {
        if (specGroup.getSort() == null) {
            specGroup.setSort(0);
        }
        specGroupMapper.insert(specGroup);
        return Result.success();
    }

    @PutMapping("/groups/{id}")
    @Operation(summary = "修改规格组")
    public Result<Void> updateGroup(@PathVariable("id") Long id, @RequestBody SpecGroup specGroup) {
        SpecGroup existing = specGroupMapper.selectById(id);
        if (existing != null) {
            BeanUtils.copyProperties(specGroup, existing, "id");
            specGroupMapper.updateById(existing);
        }
        return Result.success();
    }

    @DeleteMapping("/groups/{id}")
    @Operation(summary = "删除规格组")
    public Result<Void> deleteGroup(@PathVariable("id") Long id) {
        specGroupMapper.deleteById(id);
        specParamMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SpecParam>()
                .eq(SpecParam::getGroupId, id));
        return Result.success();
    }

    @GetMapping("/params")
    @Operation(summary = "获取规格参数列表")
    public Result<List<SpecParam>> listParams(@RequestParam(value = "groupId", required = false) Long groupId) {
        if (groupId != null) {
            return Result.success(specParamMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SpecParam>()
                            .eq(SpecParam::getGroupId, groupId)
                            .orderByAsc(SpecParam::getSort)));
        }
        return Result.success(specParamMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SpecParam>()
                        .orderByAsc(SpecParam::getSort)));
    }

    @PostMapping("/params")
    @Operation(summary = "新增规格参数")
    public Result<Void> saveParam(@RequestBody SpecParam specParam) {
        if (specParam.getSort() == null) {
            specParam.setSort(0);
        }
        specParamMapper.insert(specParam);
        return Result.success();
    }

    @PutMapping("/params/{id}")
    @Operation(summary = "修改规格参数")
    public Result<Void> updateParam(@PathVariable("id") Long id, @RequestBody SpecParam specParam) {
        SpecParam existing = specParamMapper.selectById(id);
        if (existing != null) {
            BeanUtils.copyProperties(specParam, existing, "id");
            specParamMapper.updateById(existing);
        }
        return Result.success();
    }

    @DeleteMapping("/params/{id}")
    @Operation(summary = "删除规格参数")
    public Result<Void> deleteParam(@PathVariable("id") Long id) {
        specParamMapper.deleteById(id);
        return Result.success();
    }
}