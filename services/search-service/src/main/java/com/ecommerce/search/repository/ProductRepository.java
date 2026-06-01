package com.ecommerce.search.repository;

import com.ecommerce.search.model.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * 根据名称匹配（ES match 查询，走 IK 分词），用于搜索建议
     */
    List<ProductDocument> findByName(String keyword);

    void deleteByCategoryId(Long categoryId);
}
