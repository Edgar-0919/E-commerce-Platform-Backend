package com.ecommerce.user.service;

import com.ecommerce.user.model.entity.UserAddress;

import java.util.List;

public interface UserAddressService {

    List<UserAddress> listByUserId(Long userId);

    UserAddress getById(Long id);

    void save(UserAddress address);

    void update(UserAddress address);

    void delete(Long id, Long userId);
}
