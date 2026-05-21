package com.ecommerce.cart.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.product.model.vo.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端 — 获取 SKU 信息
 * <p>
 * 通过 Nacos 服务发现调用 product-service，
 * 用于购物车添加商品时获取 SKU 名称、价格、库存等信息。
 */
@FeignClient(name = "product-service", path = "/api/product")
public interface ProductFeignClient {

    /** 获取 SKU 详情（含名称、价格、规格、库存、图片） */
    @GetMapping("/sku/{id}")
    Result<SkuVO> getSkuById(@PathVariable("id") Long skuId);
}