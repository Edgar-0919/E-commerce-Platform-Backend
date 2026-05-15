package com.ecommerce.cart.controller;

import com.ecommerce.cart.model.dto.CartItemDTO;
import com.ecommerce.cart.model.entity.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "购物车管理")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "购物车列表")
    public Result<List<CartItem>> list() {
        return Result.success(cartService.list(UserContext.currentUserId()));
    }

    @PostMapping
    @Operation(summary = "添加购物车")
    public Result<Void> add(@Valid @RequestBody CartItemDTO dto) {
        cartService.add(UserContext.currentUserId(), dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改购物车项")
    public Result<Void> update(@PathVariable Long id,
                                @RequestParam(required = false) Integer quantity,
                                @RequestParam(required = false) Integer selected) {
        cartService.update(UserContext.currentUserId(), id, quantity, selected);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车项")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(UserContext.currentUserId(), id);
        return Result.success();
    }
}
