package com.ecommerce.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.user.mapper.MerchantMapper;
import com.ecommerce.user.model.entity.Merchant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantMapper merchantMapper;

    public PageResult<Merchant> page(Integer pageNum, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .like(name != null, Merchant::getName, name)
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreateTime);
        Page<Merchant> p = merchantMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords());
    }

    public Merchant getById(Long id) {
        return merchantMapper.selectById(id);
    }

    public void create(Merchant merchant) {
        merchantMapper.insert(merchant);
    }

    public void update(Merchant merchant) {
        merchantMapper.updateById(merchant);
    }

    public void updateStatus(Long id, Integer status) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant != null) {
            merchant.setStatus(status);
            merchantMapper.updateById(merchant);
        }
    }
}