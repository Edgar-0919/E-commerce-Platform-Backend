package com.ecommerce.order.controller;

import com.ecommerce.order.cart.model.dto.CartItemDTO;
import com.ecommerce.order.cart.model.entity.CartItem;
import com.ecommerce.order.cart.CartService;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.AuthException;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "购物车管理")
public class OrderCartController {

    private final CartService cartService;

    private Long getCurrentUserId() {
        Long userId = UserContext.currentUserId();
        if (userId == null) {
            throw new AuthException(ResultCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    @GetMapping
    @Operation(summary = "购物车列表")
    public Result<List<CartItem>> list() {
        return Result.success(cartService.list(getCurrentUserId()));
    }

    /** 添加购物车 — 高频操作，需要限流保护 */
    @PostMapping
    @Operation(summary = "添加购物车")
    public Result<Void> add(@Valid @RequestBody CartItemDTO dto) {
        cartService.add(getCurrentUserId(), dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改购物车项")
    public Result<Void> update(@PathVariable("id") Long id,
                                @RequestParam(value = "quantity", required = false) Integer quantity,
                                @RequestParam(value = "selected", required = false) Integer selected) {
        cartService.update(getCurrentUserId(), id, quantity, selected);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车项")
    public Result<Void> delete(@PathVariable("id") Long id) {
        cartService.delete(getCurrentUserId(), id);
        return Result.success();
    }
}