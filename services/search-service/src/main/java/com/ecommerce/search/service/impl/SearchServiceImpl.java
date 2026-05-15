package com.ecommerce.search.service.impl;

import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.repository.ProductRepository;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
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
    public List<ProductDocument> search(String keyword, Long categoryId, Long brandId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String sortField, String sortOrder,
                                         int page, int size) {
        Criteria criteria = new Criteria("status").is(1);

        if (StringUtils.hasText(keyword)) {
            criteria = criteria.and(new Criteria("name").contains(keyword).or(new Criteria("description").contains(keyword)));
        }
        if (categoryId != null) {
            criteria = criteria.and("categoryId").is(categoryId);
        }
        if (brandId != null) {
            criteria = criteria.and("brandId").is(brandId);
        }
        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = new Criteria("price");
            if (minPrice != null) {
                priceCriteria = priceCriteria.greaterThanEqual(minPrice.doubleValue());
            }
            if (maxPrice != null) {
                priceCriteria = priceCriteria.lessThanEqual(maxPrice.doubleValue());
            }
            criteria = criteria.and(priceCriteria);
        }

        Sort sort = Sort.unsorted();
        if (StringUtils.hasText(sortField)) {
            sort = "asc".equalsIgnoreCase(sortOrder)
                    ? Sort.by(sortField).ascending()
                    : Sort.by(sortField).descending();
        }

        Query query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(page - 1, size))
                .addSort(sort);

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> suggest(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
            return Collections.emptyList();
        }
        return productRepository.findByNameContaining(keyword)
                .stream()
                .limit(10)
                .map(ProductDocument::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getFilters(String keyword) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("categories", Collections.emptyList());
        filters.put("brands", Collections.emptyList());
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