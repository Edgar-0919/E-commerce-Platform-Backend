package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.user.mapper.UserAddressMapper;
import com.ecommerce.user.model.entity.UserAddress;
import com.ecommerce.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper addressMapper;

    @Override
    public List<UserAddress> listByUserId(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
    }

    @Override
    public UserAddress getById(Long id) {
        UserAddress addr = addressMapper.selectById(id);
        if (addr == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "地址不存在");
        }
        return addr;
    }

    @Override
    @Transactional
    public void save(UserAddress address) {
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.clearDefault(address.getUserId());
        }
        addressMapper.insert(address);
    }

    @Override
    @Transactional
    public void update(UserAddress address) {
        UserAddress existing = getById(address.getId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.clearDefault(existing.getUserId());
        }
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long id, Long userId) {
        UserAddress existing = getById(id);
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "无权删除此地址");
        }
        addressMapper.deleteById(id);
    }
}
