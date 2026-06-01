package com.ecommerce.product.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品数据同步接口 — 供 search-service 全量拉取商品数据写入 ES
 * <p>
 * 此接口为内部服务调用，不对外暴露到 Gateway 路由。
 */
@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
@Tag(name = "内部-商品同步", description = "供 search-service 全量同步 ES 索引")
public class ProductSyncController {

    private final ProductService productService;

    @GetMapping("/all-for-search")
    @Operation(summary = "获取全部上架商品（供 ES 全量索引）")
    public Result<List<ProductVO>> getAllForSearch() {
        return Result.success(productService.getAllOnSale());
    }
}