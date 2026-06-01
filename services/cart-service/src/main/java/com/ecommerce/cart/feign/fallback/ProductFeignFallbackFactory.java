package com.ecommerce.cart.feign.fallback;

import com.ecommerce.cart.feign.ProductFeignClient;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.model.Result;
import com.ecommerce.cart.model.vo.ProductVO;
import com.ecommerce.cart.model.vo.SkuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 降级工厂 — 当 product-service 不可用时返回降级结果
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("[购物车] 商品服务Feign调用失败 — {}", cause.getMessage());
        return new ProductFeignClient() {
            @Override
            public Result<SkuVO> getSkuById(Long skuId) {
                return Result.fail(ResultCodeEnum.CART_SKU_INFO_FAILED.getCode(),
                        "获取商品SKU信息失败，请稍后重试");
            }

            @Override
            public Result<ProductVO> getProductById(Long productId) {
                return Result.fail(ResultCodeEnum.CART_SKU_INFO_FAILED.getCode(),
                        "获取商品信息失败，请稍后重试");
            }
        };
    }
}