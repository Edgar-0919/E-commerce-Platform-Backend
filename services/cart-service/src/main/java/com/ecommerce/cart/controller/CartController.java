package com.ecommerce.cart.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.cart.model.dto.CartItemDTO;
import com.ecommerce.cart.model.entity.CartItem;
import com.ecommerce.cart.service.CartService;
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
public class CartController {

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

    /**
     * 添加购物车 — 高频操作，需要限流保护
     */
    @PostMapping
    @Operation(summary = "添加购物车")
    @SentinelResource(value = "addCartItem", blockHandler = "addBlockHandler", fallback = "addFallback")
    public Result<Void> add(@Valid @RequestBody CartItemDTO dto) {
        cartService.add(getCurrentUserId(), dto);
        return Result.success();
    }

    public Result<Void> addBlockHandler(CartItemDTO dto, BlockException ex) {
        log.warn("[购物车] 添加购物车被Sentinel限流");
        return Result.fail(429, "购物车服务繁忙，请稍后再试");
    }

    public Result<Void> addFallback(CartItemDTO dto, Throwable ex) {
        log.error("[购物车] 添加购物车服务降级 —", ex);
        return Result.fail(503, "购物车服务暂时不可用");
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改购物车项")
    public Result<Void> update(@PathVariable Long id,
                                @RequestParam(required = false) Integer quantity,
                                @RequestParam(required = false) Integer selected) {
        cartService.update(getCurrentUserId(), id, quantity, selected);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车项")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(getCurrentUserId(), id);
        return Result.success();
    }
}
