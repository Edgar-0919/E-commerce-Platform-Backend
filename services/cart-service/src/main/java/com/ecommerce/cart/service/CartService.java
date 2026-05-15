package com.ecommerce.cart.service;

import com.ecommerce.cart.model.dto.CartItemDTO;
import com.ecommerce.cart.model.entity.CartItem;

import java.util.List;

public interface CartService {

    List<CartItem> list(Long userId);

    void add(Long userId, CartItemDTO dto);

    void update(Long userId, Long itemId, Integer quantity, Integer selected);

    void delete(Long userId, Long itemId);

    void clearSelected(Long userId);
}
