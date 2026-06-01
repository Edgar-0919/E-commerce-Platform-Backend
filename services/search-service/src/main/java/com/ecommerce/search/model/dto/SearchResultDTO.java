package com.ecommerce.search.model.dto;

import com.ecommerce.search.model.ProductDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果封装，包含商品列表和总数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {

    /** 商品记录列表 */
    private List<ProductDocument> records;

    /** 符合条件的总记录数 */
    private long total;
}