package com.ecommerce.search.repository;

import com.ecommerce.search.model.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {

    List<ProductDocument> findByNameContaining(String keyword);

    void deleteByCategoryId(Long categoryId);
}
