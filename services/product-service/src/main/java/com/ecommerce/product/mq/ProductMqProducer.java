package com.ecommerce.product.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品消息生产者 — 商品变更时发 MQ 通知 search-service 同步 ES 索引
 * <p>
 * 发送 binding: productChange-out-0 → destination: product-change
 */
@Slf4j
@Component
public class ProductMqProducer extends AbstractMqProducer {

    static final String BINDING = "productChange-out-0";

    /** 商品新增或更新 */
    public void sendProductChange(Long productId, String name, Long categoryId, String categoryName,
                                  Long brandId, String brandName, String mainImage,
                                  String description, Integer status,
                                  java.math.BigDecimal minPrice, java.math.BigDecimal marketPrice,
                                  Integer stock) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "UPSERT");
        event.put("id", productId);
        event.put("name", name);
        event.put("categoryId", categoryId);
        event.put("categoryName", categoryName);
        event.put("brandId", brandId);
        event.put("brandName", brandName);
        event.put("mainImage", mainImage);
        event.put("description", description);
        event.put("status", status);
        event.put("price", minPrice);
        event.put("marketPrice", marketPrice);
        event.put("stock", stock);
        send(BINDING, event);
    }

    /** 商品删除（逻辑删除） */
    public void sendProductDelete(Long productId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "DELETE");
        event.put("id", productId);
        send(BINDING, event);
    }
}