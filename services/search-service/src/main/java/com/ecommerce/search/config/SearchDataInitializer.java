package com.ecommerce.search.config;

import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.service.SearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动时从 product-service 全量拉取商品数据写入 ES 索引
 * <p>
 * 仅在 ES 索引为空时执行全量同步，避免每次重启覆盖已有数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchDataInitializer {

    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    // product-service 地址（开发环境直连，生产环境通过 Nacos 服务名调用）
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8102";

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            log.info("开始全量同步商品数据到 ES...");
            RestTemplate restTemplate = new RestTemplate();
            String url = PRODUCT_SERVICE_URL + "/api/internal/products/all-for-search";

            // 调用 product-service 内部接口获取全部上架商品
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                log.error("全量同步失败: product-service 返回空响应, url={}", url);
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt();
            if (code != 200) {
                log.error("全量同步失败: product-service 返回 code={}, msg={}",
                        code, root.path("message").asText());
                return;
            }

            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() == 0) {
                log.warn("全量同步: product-service 无上架商品数据");
                return;
            }

            List<Map<String, Object>> products = objectMapper.convertValue(
                    data, new TypeReference<List<Map<String, Object>>>() {});

            List<ProductDocument> docs = new ArrayList<>();
            for (Map<String, Object> p : products) {
                ProductDocument doc = new ProductDocument();
                doc.setId(toLong(p.get("id")));
                doc.setName((String) p.get("name"));
                doc.setCategoryId(toLong(p.get("categoryId")));
                doc.setCategoryName((String) p.get("categoryName"));
                doc.setBrandId(toLong(p.get("brandId")));
                doc.setBrandName((String) p.get("brandName"));
                doc.setMainImage((String) p.get("mainImage"));
                doc.setDescription((String) p.get("description"));
                doc.setStatus((Integer) p.get("status"));

                // 从 skus 中取最低价和总库存
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> skus = (List<Map<String, Object>>) p.get("skus");
                BigDecimal minPrice = null;
                BigDecimal minMarketPrice = null;
                int totalStock = 0;
                if (skus != null) {
                    for (Map<String, Object> sku : skus) {
                        BigDecimal price = toBigDecimal(sku.get("price"));
                        if (price != null) {
                            minPrice = minPrice == null ? price : minPrice.min(price);
                        }
                        BigDecimal marketPrice = toBigDecimal(sku.get("marketPrice"));
                        if (marketPrice != null) {
                            minMarketPrice = minMarketPrice == null
                                    ? marketPrice : minMarketPrice.min(marketPrice);
                        }
                        Object stock = sku.get("stock");
                        if (stock instanceof Number) {
                            totalStock += ((Number) stock).intValue();
                        }
                    }
                }
                doc.setPrice(minPrice);
                doc.setMarketPrice(minMarketPrice);
                doc.setStock(totalStock);
                docs.add(doc);
            }

            // 批量写入 ES（逐条 save 实际是 upsert 覆盖）
            for (ProductDocument doc : docs) {
                searchService.indexProduct(doc);
            }
            log.info("全量同步完成: 共索引 {} 条商品", docs.size());

        } catch (Exception e) {
            log.error("全量同步异常 (product-service 可能未启动): {}", e.getMessage());
        }
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