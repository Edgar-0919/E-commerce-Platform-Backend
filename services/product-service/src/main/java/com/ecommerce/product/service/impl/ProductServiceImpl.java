package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.util.JsonUtils;
import com.ecommerce.product.mapper.*;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.dto.SkuSaveDTO;
import com.ecommerce.product.model.entity.*;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.model.vo.SkuVO;
import com.ecommerce.product.mq.ProductMqProducer;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 * 核心功能：
 * 1. 商品CRUD操作
 * 2. 商品分类树结构查询
 * 3. SKU库存管理
 * 4. 商品搜索查询
 * 数据结构：
 * - Product: 商品主表
 * - Sku: 库存单元
 * - Category: 商品分类
 * - Brand: 品牌信息
 * - SpecGroup/SpecParam: 规格参数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final SkuMapper skuMapper;
    private final SpecGroupMapper specGroupMapper;
    private final SpecParamMapper specParamMapper;
    private final ProductMqProducer productMqProducer;

    @Override
    public PageResult<ProductVO> page(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(query.getBrandId() != null, Product::getBrandId, query.getBrandId())
                .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Product::getName, query.getKeyword())
                .orderByDesc(Product::getCreateTime);

        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<ProductVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        return toVO(product);
    }

    @Override
    @Transactional
    public void save(ProductSaveDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setImages(dto.getImages() != null ? JsonUtils.toJson(dto.getImages()) : null);
        product.setStatus(1);
        productMapper.insert(product);

        if (dto.getSkus() != null) {
            for (SkuSaveDTO skuDTO : dto.getSkus()) {
                Sku sku = new Sku();
                BeanUtils.copyProperties(skuDTO, sku);
                sku.setProductId(product.getId());
                sku.setSpecValues(JsonUtils.toJson(skuDTO.getSpecValues()));
                sku.setStatus(1);
                sku.setStock(skuDTO.getStock() != null ? skuDTO.getStock() : 0);
                skuMapper.insert(sku);
            }
        }
        log.info("商品创建成功: id={}, name={}", product.getId(), product.getName());
        // 发送 MQ 异步同步 ES 索引
        sendProductChangeMq(product);
    }

    @Override
    @Transactional
    public void update(Long id, ProductSaveDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        BeanUtils.copyProperties(dto, product);
        product.setImages(dto.getImages() != null ? JsonUtils.toJson(dto.getImages()) : null);
        productMapper.updateById(product);
        log.info("商品更新成功: id={}", id);
        // 发送 MQ 异步同步 ES 索引
        sendProductChangeMq(product);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        product.setStatus(status);
        productMapper.updateById(product);
        // 发送 MQ 异步同步 ES 索引
        sendProductChangeMq(product);
    }

    @Override
    public List<CategoryVO> categoryTree() {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));
        Map<Long, List<CategoryVO>> childrenMap = new HashMap<>();
        List<CategoryVO> roots = new ArrayList<>();

        for (Category c : all) {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(c, vo);
            vo.setChildren(new ArrayList<>());
            childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(vo);
            if (c.getParentId() == 0) {
                roots.add(vo);
            }
        }

        buildTree(roots, childrenMap);
        return roots;
    }

    private void buildTree(List<CategoryVO> nodes, Map<Long, List<CategoryVO>> childrenMap) {
        for (CategoryVO node : nodes) {
            List<CategoryVO> children = childrenMap.get(node.getId());
            if (children != null) {
                node.setChildren(children);
                buildTree(children, childrenMap);
            }
        }
    }

    @Override
    public SkuVO getSkuById(Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BusinessException(ResultCodeEnum.SKU_NOT_EXIST);
        }
        return toSkuVO(sku);
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        if (product.getImages() != null) {
            vo.setImages(JsonUtils.fromJson(product.getImages(),
                    new TypeReference<List<String>>() {}));
        }

        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) vo.setCategoryName(category.getName());
        }

        if (product.getBrandId() != null) {
            Brand brand = brandMapper.selectById(product.getBrandId());
            if (brand != null) vo.setBrandName(brand.getName());
        }

        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, product.getId()));
        if (skus != null && !skus.isEmpty()) {
            vo.setSkus(skus.stream().map(this::toSkuVO).collect(Collectors.toList()));
        } else {
            vo.setSkus(new ArrayList<>());
        }

        return vo;
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO vo = new SkuVO();
        BeanUtils.copyProperties(sku, vo);
        if (sku.getSpecValues() != null) {
            vo.setSpecValues(JsonUtils.fromJson(sku.getSpecValues(),
                    new TypeReference<Map<String, String>>() {}));
        }
        // 关联查询 Product 表获取商品名称，供订单服务创建订单项时动态获取真实商品名
        Product product = productMapper.selectById(sku.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
        }
        return vo;
    }

    // ==================== ES 索引同步 ====================

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            productMapper.deleteById(id);
            log.info("商品删除: id={}, name={}", id, product.getName());
            // 发送 MQ 通知 ES 删除索引
            productMqProducer.sendProductDelete(id);
        }
    }

    @Override
    public List<ProductVO> getAllOnSale() {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getId));
        return products.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 构建并发送商品变更 MQ 消息（异步同步 ES） */
    private void sendProductChangeMq(Product product) {
        try {
            ProductVO vo = toVO(product);
            // 取最低价 SKU 作为搜索价格
            BigDecimal minPrice = null;
            BigDecimal minMarketPrice = null;
            int totalStock = 0;
            if (vo.getSkus() != null && !vo.getSkus().isEmpty()) {
                for (SkuVO sku : vo.getSkus()) {
                    if (sku.getPrice() != null) {
                        minPrice = minPrice == null ? sku.getPrice()
                                : minPrice.min(sku.getPrice());
                    }
                    if (sku.getMarketPrice() != null) {
                        minMarketPrice = minMarketPrice == null ? sku.getMarketPrice()
                                : minMarketPrice.min(sku.getMarketPrice());
                    }
                    if (sku.getStock() != null) {
                        totalStock += sku.getStock();
                    }
                }
            }
            productMqProducer.sendProductChange(
                    product.getId(), product.getName(),
                    product.getCategoryId(), vo.getCategoryName(),
                    product.getBrandId(), vo.getBrandName(),
                    product.getMainImage(), product.getDescription(),
                    product.getStatus(), minPrice, minMarketPrice, totalStock);
        } catch (Exception e) {
            log.error("发送商品变更MQ失败: id={}, name={}", product.getId(), product.getName(), e);
        }
    }
}
