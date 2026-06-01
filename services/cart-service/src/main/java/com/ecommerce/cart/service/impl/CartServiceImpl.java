package com.ecommerce.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.cart.feign.ProductFeignClient;
import com.ecommerce.cart.mapper.CartItemMapper;
import com.ecommerce.cart.model.dto.CartItemDTO;
import com.ecommerce.cart.model.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.cart.model.vo.ProductVO;
import com.ecommerce.cart.model.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductFeignClient productFeignClient;

    @Override
    public List<CartItem> list(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime));
    }

    @Override
    public void add(Long userId, CartItemDTO dto) {
        // 相同 SKU 已存在时合并数量，避免重复条目
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, dto.getSkuId()));

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + dto.getQuantity());
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setSkuId(dto.getSkuId());
            item.setQuantity(dto.getQuantity());
            item.setSelected(1);
            // 通过 Feign 获取 SKU 和商品信息填充购物车展示字段
            enrichCartItem(item, dto.getSkuId());
            cartItemMapper.insert(item);
        }
    }

    /**
     * 通过 Feign 调用 product-service 获取 SKU 和商品信息，
     * 填充购物车项的 productId、productName、specDesc、price、mainImage。
     * 若 product-service 不可达导致关键字段为空，则拒绝加购并返回明确错误。
     */
    private void enrichCartItem(CartItem item, Long skuId) {
        try {
            SkuVO sku = productFeignClient.getSkuById(skuId).getData();
            if (sku != null) {
                item.setProductId(sku.getProductId());
                item.setPrice(sku.getPrice());
                item.setMainImage(sku.getImage());
                // 从规格值 Map 拼接规格描述文本，如 "曜金黑/256GB"
                if (sku.getSpecValues() != null && !sku.getSpecValues().isEmpty()) {
                    item.setSpecDesc(sku.getSpecValues().values().stream()
                            .collect(Collectors.joining("/")));
                }
                // 获取商品名称（非关键，失败不阻塞）
                if (sku.getProductId() != null) {
                    try {
                        ProductVO product = productFeignClient.getProductById(sku.getProductId()).getData();
                        if (product != null) {
                            item.setProductName(product.getName());
                            // SKU 没有独立图片时回退到商品主图
                            if (item.getMainImage() == null) {
                                item.setMainImage(product.getMainImage());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取商品名称失败 productId={}", sku.getProductId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取SKU信息失败 skuId={}", skuId, e);
            throw new BusinessException(ResultCodeEnum.CART_SKU_INFO_FAILED);
        }
        // 关键字段为空则拒绝插入，避免违反数据库 NOT NULL 约束
        if (item.getProductId() == null || item.getPrice() == null) {
            throw new BusinessException(ResultCodeEnum.CART_SKU_INFO_FAILED);
        }
    }

    @Override
    public void update(Long userId, Long itemId, Integer quantity, Integer selected) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.CART_ITEM_NOT_EXIST);
        }
        if (quantity != null) item.setQuantity(quantity);
        if (selected != null) item.setSelected(selected);
        cartItemMapper.updateById(item);
    }

    @Override
    public void delete(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.CART_ITEM_NOT_EXIST);
        }
        cartItemMapper.deleteById(itemId);
    }

    @Override
    public void clearSelected(Long userId) {
        List<CartItem> selected = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSelected, 1));
        for (CartItem item : selected) {
            cartItemMapper.deleteById(item.getId());
        }
    }
}
