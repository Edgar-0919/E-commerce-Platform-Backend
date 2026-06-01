package com.ecommerce.product.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品事件生产者 — 基于 Spring Cloud Stream StreamBridge 发送商品变更事件
 * <p>
 * 发送 product-change 事件给 search-service，触发 ES 索引增量同步。
 * 在商品上架、下架、信息变更时由 ProductServiceImpl 调用。
 */
@Slf4j
@Component
public class ProductEventProducer extends AbstractMqProducer {

    /** 商品上架或更新时，同步到 ES 索引 */
    public void sendUpsert(Long productId, String name, Long categoryId,
                           String categoryName, Long brandId, String brandName,
                           String mainImage, String description, Integer status,
                           BigDecimal price, BigDecimal marketPrice, Integer stock) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "UPSERT");
        payload.put("id", productId);
        payload.put("name", name);
        payload.put("categoryId", categoryId);
        payload.put("categoryName", categoryName);
        payload.put("brandId", brandId);
        payload.put("brandName", brandName);
        payload.put("mainImage", mainImage);
        payload.put("description", description);
        payload.put("status", status);
        payload.put("price", price);
        payload.put("marketPrice", marketPrice);
        payload.put("stock", stock);
        send("productChange-out-0", payload);
    }

    /** 商品下架或删除时，从 ES 索引中移除 */
    public void sendDelete(Long productId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DELETE");
        payload.put("id", productId);
        send("productChange-out-0", payload);
    }
}