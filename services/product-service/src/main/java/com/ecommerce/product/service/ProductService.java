package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.model.vo.SkuVO;

import java.util.List;

public interface ProductService {

    PageResult<ProductVO> page(ProductQueryDTO query);

    ProductVO getById(Long id);

    void save(ProductSaveDTO dto);

    void update(Long id, ProductSaveDTO dto);

    void updateStatus(Long id, Integer status);

    List<CategoryVO> categoryTree();

    SkuVO getSkuById(Long id);

    /** 删除商品（逻辑删除） */
    void delete(Long id);

    /** 获取全部上架商品（供 ES 全量索引） */
    List<ProductVO> getAllOnSale();
}
