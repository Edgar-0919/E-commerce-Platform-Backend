package com.ecommerce.user.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.user.model.dto.AddressDTO;
import com.ecommerce.user.model.entity.UserAddress;
import com.ecommerce.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
@Tag(name = "用户地址管理")
public class AddressController {

    private final UserAddressService addressService;

    @GetMapping
    @Operation(summary = "地址列表")
    public Result<List<UserAddress>> list() {
        Long userId = UserContext.currentUserId();
        return Result.success(addressService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "地址详情")
    public Result<UserAddress> getById(@PathVariable("id") Long id) {
        return Result.success(addressService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public Result<UserAddress> save(@Valid @RequestBody AddressDTO dto) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(dto, address);
        if (dto.getIsDefault() != null) {
            address.setIsDefault(dto.getIsDefault() ? 1 : 0);
        } else {
            address.setIsDefault(0);
        }
        address.setUserId(UserContext.currentUserId());
        addressService.save(address);
        return Result.success(address);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改地址")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody AddressDTO dto) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(dto, address);
        if (dto.getIsDefault() != null) {
            address.setIsDefault(dto.getIsDefault() ? 1 : 0);
        }
        address.setId(id);
        addressService.update(address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> delete(@PathVariable("id") Long id) {
        addressService.delete(id, UserContext.currentUserId());
        return Result.success();
    }
}
