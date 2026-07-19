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

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

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
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
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
    public Result<UserVO> getById(@PathVariable("id") Long id) {
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

    @GetMapping("/batch/usernames")
    @Operation(summary = "批量查询用户名（供订单管理等下游服务填充用户名）")
    public Result<Map<Long, String>> getUsernames(
            @RequestParam("ids") List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(Map.of());
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> map = users.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return Result.success(map);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
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
    public Result<Void> assignRoles(@PathVariable("id") Long id, @RequestBody Map<String, List<String>> body) {
        List<String> roleCodes = body.get("roles");
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Result.success();
        }
        List<Role> allRoles = roleMapper.selectList(null);
        userMapper.deleteUserRoles(id);
        for (String code : roleCodes) {
            Role matched = allRoles.stream().filter(r -> r.getRoleCode().equals(code)).findFirst().orElse(null);
            if (matched != null) {
                userMapper.insertUserRole(IdWorker.getId(), id, matched.getId());
            }
        }
        return Result.success();
    }
}