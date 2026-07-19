package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.user.mapper.MerchantApplicationMapper;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.model.dto.MerchantApplicationDTO;
import com.ecommerce.user.model.dto.ReviewApplicationDTO;
import com.ecommerce.user.model.entity.Merchant;
import com.ecommerce.user.model.entity.MerchantApplication;
import com.ecommerce.user.model.entity.User;
import com.ecommerce.user.model.vo.MerchantApplicationVO;
import com.ecommerce.user.service.MerchantApplicationService;
import com.ecommerce.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantApplicationServiceImpl implements MerchantApplicationService {

    private final MerchantApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final MerchantService merchantService;

    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(0, "待审核");
        STATUS_MAP.put(1, "已通过");
        STATUS_MAP.put(2, "已拒绝");
    }

    @Override
    @Transactional
    public void submitApplication(MerchantApplicationDTO dto, Long userId) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantApplication::getUserId, userId);
        wrapper.in(MerchantApplication::getStatus, 0, 1);
        Long count = applicationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "您已有待审核或已通过的商户申请");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }

        if (user.getMerchantId() != null) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "您已是商户");
        }

        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setMerchantName(dto.getMerchantName());
        application.setContactName(dto.getContactName());
        application.setContactPhone(dto.getContactPhone());
        application.setEmail(dto.getEmail());
        application.setAddress(dto.getAddress());
        application.setBusinessLicense(dto.getBusinessLicense());
        application.setStatus(0);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.insert(application);

        log.info("商户申请提交成功: userId={}, merchantName={}", userId, dto.getMerchantName());
    }

    @Override
    public MerchantApplicationVO getApplicationByUserId(Long userId) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantApplication::getUserId, userId);
        wrapper.orderByDesc(MerchantApplication::getCreateTime);
        wrapper.last("LIMIT 1");
        MerchantApplication application = applicationMapper.selectOne(wrapper);
        if (application == null) {
            return null;
        }
        return convertToVO(application);
    }

    @Override
    public MerchantApplicationVO getApplicationById(Long id) {
        MerchantApplication application = applicationMapper.selectById(id);
        if (application == null) {
            return null;
        }
        return convertToVO(application);
    }

    @Override
    public PageResult<MerchantApplicationVO> page(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, MerchantApplication::getStatus, status);
        wrapper.orderByDesc(MerchantApplication::getCreateTime);
        Page<MerchantApplication> p = applicationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(this::convertToVO).toList());
    }

    @Override
    @Transactional
    public void review(Long id, ReviewApplicationDTO dto, Long reviewerId) {
        MerchantApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND);
        }
        if (application.getStatus() != 0) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "该申请已处理");
        }

        if (dto.getStatus() == 1) {
            approveApplication(application, dto.getReviewRemark(), reviewerId);
        } else if (dto.getStatus() == 2) {
            rejectApplication(application, dto.getReviewRemark(), reviewerId);
        } else {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    private void approveApplication(MerchantApplication application, String remark, Long reviewerId) {
        User user = userMapper.selectById(application.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }

        Merchant merchant = new Merchant();
        merchant.setName(application.getMerchantName());
        merchant.setContactName(application.getContactName());
        merchant.setContactPhone(application.getContactPhone());
        merchant.setEmail(application.getEmail());
        merchant.setAddress(application.getAddress());
        merchant.setStatus(1);
        merchant.setCreateTime(LocalDateTime.now());
        merchant.setUpdateTime(LocalDateTime.now());
        merchantService.create(merchant);

        user.setMerchantId(merchant.getId());
        userMapper.updateById(user);

        userMapper.insertUserRole(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), user.getId(), 2L);

        application.setStatus(1);
        application.setReviewRemark(remark);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        log.info("商户申请审核通过: applicationId={}, userId={}, merchantId={}",
                application.getId(), user.getId(), merchant.getId());
    }

    private void rejectApplication(MerchantApplication application, String remark, Long reviewerId) {
        application.setStatus(2);
        application.setReviewRemark(remark);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        log.info("商户申请审核拒绝: applicationId={}, userId={}", application.getId(), application.getUserId());
    }

    private MerchantApplicationVO convertToVO(MerchantApplication application) {
        MerchantApplicationVO vo = new MerchantApplicationVO();
        BeanUtils.copyProperties(application, vo);
        vo.setStatusText(STATUS_MAP.getOrDefault(application.getStatus(), "未知"));

        User user = userMapper.selectById(application.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        if (application.getReviewerId() != null) {
            User reviewer = userMapper.selectById(application.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getUsername());
            }
        }

        return vo;
    }
}