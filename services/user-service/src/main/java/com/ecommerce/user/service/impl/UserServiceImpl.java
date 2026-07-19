package com.ecommerce.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.BusinessException;
import com.ecommerce.core.model.TokenPayload;
import com.ecommerce.security.util.JwtUtils;
import com.ecommerce.user.mapper.MerchantApplicationMapper;
import com.ecommerce.user.mapper.RoleMapper;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.model.dto.LoginDTO;
import com.ecommerce.user.model.dto.RegisterDTO;
import com.ecommerce.user.model.entity.MerchantApplication;
import com.ecommerce.user.model.entity.User;
import com.ecommerce.user.model.vo.LoginVO;
import com.ecommerce.user.model.vo.UserVO;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 用户服务实现类
 * 提供用户注册、登录、信息查询和更新等功能
 * 安全策略：
 * - 使用BCrypt加密存储密码
 * - JWT Token包含用户角色信息
 * - 密码验证使用matches方法避免时序攻击
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MerchantApplicationMapper applicationMapper;
    // BCrypt 哈希密码，static final 避免重复创建 encoder 实例
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "账号已被禁用");
        }
        if (!ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // JWT 中包含用户角色，Gateway 解析后透传给下游服务做权限判断
        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        if (roles == null) roles = Collections.emptyList();

        TokenPayload payload = new TokenPayload();
        payload.setUserId(user.getId());
        payload.setUsername(user.getUsername());
        payload.setNickname(user.getNickname());
        payload.setRoles(roles);
        payload.setMerchantId(user.getMerchantId());

        String token = JwtUtils.generateToken(payload);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXIST);
        }

        // 检查手机号是否已存在
        if (dto.getPhone() != null) {
            count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, dto.getPhone()));
            if (count > 0) {
                throw new BusinessException(ResultCodeEnum.PHONE_EXIST);
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        // 默认分配普通用户角色（ROLE_USER）
        userMapper.insertUserRole(IdWorker.getId(), user.getId(), 1L);

        log.info("用户注册成功: username={}, id={}", dto.getUsername(), user.getId());
    }

    @Override
    public UserVO getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }
        List<String> roles = roleMapper.findRoleCodesByUserId(userId);
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roles != null ? roles : Collections.emptyList());

        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantApplication::getUserId, userId);
        wrapper.orderByDesc(MerchantApplication::getCreateTime);
        wrapper.last("LIMIT 1");
        MerchantApplication application = applicationMapper.selectOne(wrapper);
        if (application != null) {
            vo.setMerchantApplicationStatus(application.getStatus());
            vo.setMerchantApplicationStatusText(getApplicationStatusText(application.getStatus()));
        }

        return vo;
    }

    private String getApplicationStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }

    @Override
    public void updateUserInfo(Long userId, UserVO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
    }
}
