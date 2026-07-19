package com.ecommerce.order.feign;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.model.vo.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端 — 获取 SKU 信息
 */
@FeignClient(name = "product-service", contextId = "order-product", path = "/api/product")
public interface ProductFeignClient {

    @GetMapping("/sku/{id}")
    Result<SkuVO> getSkuById(@PathVariable("id") Long skuId);
}