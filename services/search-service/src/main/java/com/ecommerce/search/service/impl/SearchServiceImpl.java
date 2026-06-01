package com.ecommerce.search.service.impl;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.model.dto.SearchResultDTO;
import com.ecommerce.search.repository.ProductRepository;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchResultDTO search(String keyword, Long categoryId, Long brandId,
                                   BigDecimal minPrice, BigDecimal maxPrice,
                                   String sortField, String sortOrder,
                                   int page, int size) {
        try {
            // 使用 NativeQuery 直接构造 ES DSL，确保 match 查询正确走 IK 分词
            var boolBuilder = new BoolQuery.Builder();

            // 过滤：只搜上架商品
            boolBuilder.filter(f -> f.term(t -> t.field("status").value(1)));

            // 关键词搜索：match 查询，正确走 IK 分词
            if (StringUtils.hasText(keyword)) {
                boolBuilder.must(m -> m.bool(b -> b
                        .should(s -> s.match(mt -> mt.field("name").query(keyword)))
                        .should(s -> s.match(mt -> mt.field("description").query(keyword)))
                        .minimumShouldMatch("1")));
            }

            // 分类/品牌过滤
            if (categoryId != null) {
                boolBuilder.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
            }
            if (brandId != null) {
                boolBuilder.filter(f -> f.term(t -> t.field("brandId").value(brandId)));
            }

            // 价格区间
            if (minPrice != null || maxPrice != null) {
                boolBuilder.filter(f -> f.range(r -> {
                    var range = r.field("price");
                    if (minPrice != null) range.gte(co.elastic.clients.json.JsonData.of(minPrice));
                    if (maxPrice != null) range.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                    return range;
                }));
            }

            // 排序
            Sort sort = Sort.unsorted();
            if (StringUtils.hasText(sortField)) {
                String field = sortField;
                String order = "desc";
                if ("price_asc".equalsIgnoreCase(sortField)) {
                    field = "price";
                    order = "asc";
                } else if ("price_desc".equalsIgnoreCase(sortField)) {
                    field = "price";
                    order = "desc";
                } else if ("sales".equalsIgnoreCase(sortField)) {
                    field = "id";
                    order = "desc";
                }
                sort = "asc".equalsIgnoreCase(order)
                        ? Sort.by(field).ascending()
                        : Sort.by(field).descending();
            }

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolBuilder.build()))
                    .withPageable(PageRequest.of(page - 1, size))
                    .withSort(sort)
                    .build();

            SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

            long total = hits.getTotalHits();
            List<ProductDocument> records = hits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList());

            return new SearchResultDTO(records, total);
        } catch (Exception e) {
            log.error("搜索异常，ES可能不可用: keyword={}", keyword, e);
            return new SearchResultDTO(Collections.emptyList(), 0);
        }
    }

    @Override
    public List<String> suggest(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
            return Collections.emptyList();
        }
        try {
            return productRepository.findByName(keyword)
                    .stream()
                    .limit(8)
                    .map(ProductDocument::getName)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索建议查询失败: keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取搜索过滤条件（分类、品牌聚合）。
     * <p>
     * 使用 ES terms aggregation 统计当前关键词下的分类和品牌分布，
     * 供前端搜索页侧边栏过滤使用。
     * 注意：当 ES 不可用时返回空列表，前端应隐藏过滤栏而非报错。
     */
    @Override
    public Map<String, Object> getFilters(String keyword) {
        Map<String, Object> filters = new HashMap<>();
        try {
            var boolBuilder = new BoolQuery.Builder()
                    .filter(f -> f.term(t -> t.field("status").value(1)));
            if (StringUtils.hasText(keyword)) {
                boolBuilder.must(m -> m.bool(b -> b
                        .should(s -> s.match(mt -> mt.field("name").query(keyword)))
                        .should(s -> s.match(mt -> mt.field("description").query(keyword)))
                        .minimumShouldMatch("1")));
            }

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolBuilder.build()))
                    .withAggregation("categories", Aggregation.of(a -> a
                            .terms(t -> t.field("categoryName").size(20))))
                    .withAggregation("brands", Aggregation.of(a -> a
                            .terms(t -> t.field("brandName").size(20))))
                    .withPageable(PageRequest.of(0, 0)) // 不需要文档，只要聚合结果
                    .build();

            SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

            // 从 ES 聚合结果中提取分类和品牌列表
            // Spring Data ES 5.x 聚合包装链：ElasticsearchAggregations → ElasticsearchAggregation → Aggregation → Aggregate
            var aggsContainer = hits.getAggregations();
            if (aggsContainer != null) {
                ElasticsearchAggregations aggs = (ElasticsearchAggregations) aggsContainer;

                List<Map<String, Object>> categories = new ArrayList<>();
                var catAgg = aggs.get("categories");
                if (catAgg != null) {
                    var aggregate = catAgg.aggregation().getAggregate();
                    if (aggregate.isSterms()) {
                        aggregate.sterms().buckets().array().forEach(bucket -> {
                            Map<String, Object> cat = new HashMap<>();
                            cat.put("name", bucket.key());
                            cat.put("count", bucket.docCount());
                            categories.add(cat);
                        });
                    }
                }
                filters.put("categories", categories);

                List<Map<String, Object>> brands = new ArrayList<>();
                var brandAgg = aggs.get("brands");
                if (brandAgg != null) {
                    var aggregate = brandAgg.aggregation().getAggregate();
                    if (aggregate.isSterms()) {
                        aggregate.sterms().buckets().array().forEach(bucket -> {
                            Map<String, Object> brand = new HashMap<>();
                            brand.put("name", bucket.key());
                            brand.put("count", bucket.docCount());
                            brands.add(brand);
                        });
                    }
                }
                filters.put("brands", brands);
            } else {
                filters.put("categories", Collections.emptyList());
                filters.put("brands", Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("获取搜索过滤器失败，ES可能不可用: keyword={}", keyword, e);
            filters.put("categories", Collections.emptyList());
            filters.put("brands", Collections.emptyList());
        }
        return filters;
    }

    @Override
    public void indexProduct(ProductDocument doc) {
        productRepository.save(doc);
        log.info("索引商品: id={}, name={}", doc.getId(), doc.getName());
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        log.info("删除索引: id={}", id);
    }
}