package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.feign.fallback.ProductFeignFallbackFactory;
import com.ecommerce.order.model.vo.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端 — 获取 SKU 信息
 * <p>
 * fallbackFactory：调用失败时触发降级，避免订单创建因无法获取商品信息而失败
 */
@FeignClient(name = "product-service", path = "/api/product",
        fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    @GetMapping("/sku/{id}")
    Result<SkuVO> getSkuById(@PathVariable("id") Long skuId);
}