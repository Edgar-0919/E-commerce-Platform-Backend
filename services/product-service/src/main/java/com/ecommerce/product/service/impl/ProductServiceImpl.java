package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.core.util.JsonUtils;
import com.ecommerce.product.mapper.*;
import com.ecommerce.product.model.dto.ProductQueryDTO;
import com.ecommerce.product.model.dto.ProductSaveDTO;
import com.ecommerce.product.model.dto.SkuSaveDTO;
import com.ecommerce.product.model.entity.*;
import com.ecommerce.product.model.vo.CategoryVO;
import com.ecommerce.product.model.vo.ProductVO;
import com.ecommerce.product.model.vo.SkuVO;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 * 核心功能：商品CRUD、分类树、SKU管理、MySQL LIKE 搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final SkuMapper skuMapper;
    private final SpecGroupMapper specGroupMapper;
    private final SpecParamMapper specParamMapper;
    private final com.ecommerce.product.inventory.mapper.StockMapper stockMapper;

    @Override
    public PageResult<ProductVO> page(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Product::getName, query.getKeyword())
                .orderByDesc(Product::getCreateTime);

        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<Product> products = page.getRecords();

        // 一次 SQL 拉取当前页所有商品的 SKU，避免 N+1
        Map<Long, List<Sku>> skuMap;
        if (products.isEmpty()) {
            skuMap = Map.of();
        } else {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<Sku> allSkus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                    .in(Sku::getProductId, productIds));
            skuMap = allSkus.stream().collect(Collectors.groupingBy(Sku::getProductId));
        }

        List<ProductVO> records = products.stream()
                .map(p -> toVO(p, skuMap.get(p.getId())))
                .collect(Collectors.toList());

        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public PageResult<ProductVO> search(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Product::getName, query.getKeyword());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }

        if ("sales".equals(query.getSortBy())) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, List<Sku>> skuMap;
        if (products.isEmpty()) {
            skuMap = Map.of();
        } else {
            List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<Sku> allSkus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                    .in(Sku::getProductId, productIds));
            skuMap = allSkus.stream().collect(Collectors.groupingBy(Sku::getProductId));
        }

        List<ProductVO> voList = products.stream()
                .map(p -> toVO(p, skuMap.get(p.getId())))
                .collect(Collectors.toList());

        if (query.getMinPrice() != null) {
            voList = voList.stream()
                    .filter(v -> v.getPrice() != null && v.getPrice().compareTo(query.getMinPrice()) >= 0)
                    .collect(Collectors.toList());
        }
        if (query.getMaxPrice() != null) {
            voList = voList.stream()
                    .filter(v -> v.getPrice() != null && v.getPrice().compareTo(query.getMaxPrice()) <= 0)
                    .collect(Collectors.toList());
        }

        if ("price_asc".equals(query.getSortBy())) {
            voList.sort((a, b) -> {
                BigDecimal pa = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal pb = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                return pa.compareTo(pb);
            });
        } else if ("price_desc".equals(query.getSortBy())) {
            voList.sort((a, b) -> {
                BigDecimal pa = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal pb = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                return pb.compareTo(pa);
            });
        }

        int total = voList.size();
        int page = query.getPage() != null ? query.getPage() : 1;
        int size = query.getSize() != null ? query.getSize() : 20;
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<ProductVO> pageList = fromIndex < total ? voList.subList(fromIndex, toIndex) : new ArrayList<>();

        return PageResult.of(page, size, total, pageList);
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, id));
        return toVO(product, skus);
    }

    @Override
    @Transactional
    public void save(ProductSaveDTO dto) {
        Long merchantId = UserContext.currentMerchantId();
        if (merchantId == null) {
            merchantId = 1L;
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setMerchantId(merchantId);
        product.setImages(dto.getImages() != null ? JsonUtils.toJson(dto.getImages()) : null);
        product.setStatus(1);
        productMapper.insert(product);

        List<Map<String, Object>> stockInitList = new ArrayList<>();

        if (dto.getSkus() != null) {
            for (SkuSaveDTO skuDTO : dto.getSkus()) {
                Sku sku = new Sku();
                BeanUtils.copyProperties(skuDTO, sku);
                sku.setProductId(product.getId());
                sku.setMerchantId(merchantId);
                sku.setSpecValues(JsonUtils.toJson(skuDTO.getSpecValues()));
                sku.setStatus(1);
                sku.setStock(skuDTO.getStock() != null ? skuDTO.getStock() : 0);
                skuMapper.insert(sku);

                Map<String, Object> item = new HashMap<>();
                item.put("skuId", sku.getId());
                item.put("productId", product.getId());
                item.put("merchantId", merchantId);
                item.put("stock", sku.getStock());
                stockInitList.add(item);
            }
        }

        // 本地初始化库存（inventory 已合并到 product-service）
        if (!stockInitList.isEmpty()) {
            initStockLocal(stockInitList);
        }

        log.info("商品创建成功: id={}, name={}, skuCount={}", product.getId(), product.getName(), stockInitList.size());
    }

    /** 本地初始化库存记录（替代原 Feign 调用 inventory-service） */
    private void initStockLocal(List<Map<String, Object>> skuList) {
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        for (Map<String, Object> item : skuList) {
            Long skuId = item.get("skuId") != null ? Long.valueOf(item.get("skuId").toString()) : null;
            Long productId = item.get("productId") != null ? Long.valueOf(item.get("productId").toString()) : null;
            Long merchantId = item.get("merchantId") != null ? Long.valueOf(item.get("merchantId").toString()) : 1L;
            int qty = item.get("stock") != null ? Integer.parseInt(item.get("stock").toString()) : 0;
            if (skuId == null) continue;

            Long existing = stockMapper.selectCount(new LambdaQueryWrapper<com.ecommerce.product.inventory.model.entity.Stock>()
                    .eq(com.ecommerce.product.inventory.model.entity.Stock::getSkuId, skuId));
            if (existing != null && existing > 0) continue;

            com.ecommerce.product.inventory.model.entity.Stock stock = new com.ecommerce.product.inventory.model.entity.Stock();
            stock.setSkuId(skuId);
            stock.setProductId(productId);
            stock.setMerchantId(merchantId);
            stock.setTotalStock(qty);
            stock.setLockedStock(0);
            stock.setAvailableStock(qty);
            stock.setSafetyStock(10);
            stock.setVersion(1);
            stock.setUpdateTime(now);
            stockMapper.insert(stock);
            created++;
        }
        log.info("批量初始化库存完成，新增 {} 条记录", created);
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
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCodeEnum.PRODUCT_NOT_EXIST);
        }
        product.setStatus(status);
        productMapper.updateById(product);
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

    /**
     * 将 Product 转为 ProductVO — 外部已预查好 SKU 列表时使用（page() 场景）。
     * <p>price 取所有 SKU 中最低价格，stock 取所有 SKU 库存之和。</p>
     */
    private ProductVO toVO(Product product, List<Sku> skus) {
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

        List<SkuVO> skuVOs;
        if (skus == null || skus.isEmpty()) {
            skuVOs = new ArrayList<>();
            vo.setPrice(BigDecimal.ZERO);
            vo.setStock(0);
        } else {
            BigDecimal minPrice = null;
            int totalStock = 0;
            skuVOs = new ArrayList<>(skus.size());
            for (Sku sku : skus) {
                if (sku.getPrice() != null) {
                    minPrice = minPrice == null ? sku.getPrice() : minPrice.min(sku.getPrice());
                }
                if (sku.getStock() != null) {
                    totalStock += sku.getStock();
                }
                skuVOs.add(toSkuVO(sku));
            }
            vo.setPrice(minPrice == null ? BigDecimal.ZERO : minPrice);
            vo.setStock(totalStock);
        }
        vo.setSkus(skuVOs);

        return vo;
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO vo = new SkuVO();
        BeanUtils.copyProperties(sku, vo);
        if (sku.getSpecValues() != null) {
            vo.setSpecValues(JsonUtils.fromJson(sku.getSpecValues(),
                    new TypeReference<Map<String, String>>() {}));
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
        }
        return vo;
    }

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            productMapper.deleteById(id);
            log.info("商品删除: id={}, name={}", id, product.getName());
        }
    }

    @Override
    public Map<Long, String> getProductNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productMapper.selectBatchIds(ids);
        return products.stream().collect(Collectors.toMap(Product::getId, Product::getName));
    }
}