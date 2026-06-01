package com.ecommerce.search.mq;

import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 商品变更消息消费者 — 接收 product-service 发出的 MQ 事件，增量同步 ES 索引
 * <p>
 * 监听 destination: product-change, consumer group: search-service
 * 事件类型：
 * - UPSERT: 新增或更新商品索引
 * - DELETE: 从 ES 中删除商品索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductChangeConsumer {

    private final SearchService searchService;

    @Bean
    public Consumer<Map<String, Object>> productChange() {
        return message -> {
            String type = (String) message.get("type");
            Long id = toLong(message.get("id"));
            if (id == null) {
                log.error("商品变更事件缺少id: {}", message);
                return;
            }

            try {
                if ("DELETE".equalsIgnoreCase(type)) {
                    searchService.deleteProduct(id);
                    log.info("ES索引已删除: productId={}", id);
                } else {
                    ProductDocument doc = buildDocument(message);
                    searchService.indexProduct(doc);
                    log.info("ES索引已同步: productId={}, name={}, status={}",
                            id, doc.getName(), doc.getStatus());
                }
            } catch (Exception e) {
                log.error("ES索引同步失败: type={}, id={}, error={}", type, id, e.getMessage());
                // 抛异常触发 MQ 重试
                throw new RuntimeException("ES索引同步失败, productId=" + id, e);
            }
        };
    }

    /** 从 MQ 消息 Map 构建 ProductDocument */
    private ProductDocument buildDocument(Map<String, Object> msg) {
        ProductDocument doc = new ProductDocument();
        doc.setId(toLong(msg.get("id")));
        doc.setName((String) msg.get("name"));
        doc.setCategoryId(toLong(msg.get("categoryId")));
        doc.setCategoryName((String) msg.get("categoryName"));
        doc.setBrandId(toLong(msg.get("brandId")));
        doc.setBrandName((String) msg.get("brandName"));
        doc.setMainImage((String) msg.get("mainImage"));
        doc.setDescription((String) msg.get("description"));
        doc.setStatus(toInt(msg.get("status"), 1));
        doc.setPrice(toBigDecimal(msg.get("price")));
        doc.setMarketPrice(toBigDecimal(msg.get("marketPrice")));
        doc.setStock(toInt(msg.get("stock"), 0));
        return doc;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}