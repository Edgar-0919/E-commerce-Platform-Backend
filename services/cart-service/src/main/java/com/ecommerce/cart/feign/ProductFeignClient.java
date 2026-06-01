package com.ecommerce.cart.feign;

import com.ecommerce.cart.feign.fallback.ProductFeignFallbackFactory;
import com.ecommerce.core.model.Result;
import com.ecommerce.cart.model.vo.ProductVO;
import com.ecommerce.cart.model.vo.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端 — 获取 SKU 及商品信息
 * <p>
 * 通过 Nacos 服务发现调用 product-service，
 * 用于购物车添加商品时获取 SKU 名称、价格、库存等信息。
 * <p>
 * fallbackFactory：调用失败时触发降级，返回异常信息而非直接报错
 */
@FeignClient(name = "product-service", path = "/api/product",
        fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    /** 获取 SKU 详情（含名称、价格、规格、库存、图片） */
    @GetMapping("/sku/{id}")
    Result<SkuVO> getSkuById(@PathVariable("id") Long skuId);

    /** 获取商品详情（含名称、主图） */
    @GetMapping("/{id}")
    Result<ProductVO> getProductById(@PathVariable("id") Long productId);
}