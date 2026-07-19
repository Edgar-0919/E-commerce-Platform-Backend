package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.model.vo.SkuVO;

import java.util.List;
import java.util.Map;

public interface ProductService {

    PageResult<ProductVO> page(ProductQueryDTO query);

    /** MySQL LIKE 搜索（替代 ES 全文搜索） */
    PageResult<ProductVO> search(ProductQueryDTO query);

    ProductVO getById(Long id);

    void save(ProductSaveDTO dto);

    void update(Long id, ProductSaveDTO dto);

    void updateStatus(Long id, Integer status);

    List<CategoryVO> categoryTree();

    SkuVO getSkuById(Long id);

    /** 删除商品（逻辑删除） */
    void delete(Long id);

    /** 批量查询商品名称，返回 id → name 映射 */
    Map<Long, String> getProductNames(List<Long> ids);
}
