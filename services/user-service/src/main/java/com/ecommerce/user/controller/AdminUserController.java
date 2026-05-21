package com.ecommerce.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.user.mapper.RoleMapper;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.model.entity.Role;
import com.ecommerce.user.model.entity.User;
import com.ecommerce.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理后台-用户管理")
public class AdminUserController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @GetMapping
    @Operation(summary = "用户分页列表")
    public Result<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(keyword), User::getUsername, keyword)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> records = p.getRecords().stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            List<String> roles = roleMapper.findRoleCodesByUserId(u.getId());
            vo.setRoles(roles != null ? roles : Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), records));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public Result<UserVO> getById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(10001, "用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        List<String> roles = roleMapper.findRoleCodesByUserId(id);
        vo.setRoles(roles != null ? roles : Collections.emptyList());
        return Result.success(vo);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(10001, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        List<String> roleCodes = body.get("roles");
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Result.success();
        }
        List<Role> allRoles = roleMapper.selectList(null);
        userMapper.deleteUserRoles(id);
        for (String code : roleCodes) {
            Role matched = allRoles.stream().filter(r -> r.getRoleCode().equals(code)).findFirst().orElse(null);
            if (matched != null) {
                userMapper.insertUserRole(id, matched.getId());
            }
        }
        return Result.success();
    }
}