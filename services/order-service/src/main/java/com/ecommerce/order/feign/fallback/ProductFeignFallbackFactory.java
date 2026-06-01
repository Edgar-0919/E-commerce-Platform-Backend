package com.ecommerce.order.feign.fallback;

import com.ecommerce.core.model.Result;
import com.ecommerce.order.feign.ProductFeignClient;
import com.ecommerce.order.model.vo.SkuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 降级工厂 — 当 product-service 不可用时返回空结果
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("[订单] 商品服务Feign调用失败 — {}", cause.getMessage());
        return skuId -> {
            log.warn("[订单] 获取SKU降级 — skuId={}", skuId);
            SkuVO fallback = new SkuVO();
            fallback.setId(skuId);
            fallback.setProductName("商品信息加载中");
            return Result.success(fallback);
        };
    }
}