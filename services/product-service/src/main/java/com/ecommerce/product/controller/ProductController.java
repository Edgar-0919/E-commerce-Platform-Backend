package com.ecommerce.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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

import java.util.List;

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
    @SentinelResource(value = "getProductDetail", blockHandler = "getByIdBlockHandler", fallback = "getByIdFallback")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO vo = productService.getById(id);
        if (vo == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        if (vo.getStatus() != null && vo.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        return Result.success(vo);
    }

    public Result<ProductVO> getByIdBlockHandler(Long id, BlockException ex) {
        log.warn("[商品] 商品详情被Sentinel限流 — id={}", id);
        return Result.fail(429, "商品信息查询繁忙，请稍后再试");
    }

    public Result<ProductVO> getByIdFallback(Long id, Throwable ex) {
        log.error("[商品] 商品详情服务降级 — id={}", id, ex);
        return Result.fail(503, "商品信息暂时不可用");
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
