package com.ecommerce.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.cart.mapper.CartItemMapper;
import com.ecommerce.cart.model.dto.CartItemDTO;
import com.ecommerce.cart.model.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;

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
            cartItemMapper.insert(item);
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
