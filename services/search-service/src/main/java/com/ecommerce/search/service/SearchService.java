package com.ecommerce.search.service;

import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.model.dto.SearchResultDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SearchService {

    SearchResultDTO search(String keyword, Long categoryId, Long brandId,
                            BigDecimal minPrice, BigDecimal maxPrice,
                            String sortField, String sortOrder,
                            int page, int size);

    List<String> suggest(String keyword);

    Map<String, Object> getFilters(String keyword);

    void indexProduct(ProductDocument doc);

    void deleteProduct(Long id);
}
