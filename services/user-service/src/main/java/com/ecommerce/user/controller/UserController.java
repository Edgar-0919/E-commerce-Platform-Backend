package com.ecommerce.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.user.model.dto.LoginDTO;
import com.ecommerce.user.model.dto.RegisterDTO;
import com.ecommerce.user.model.dto.UpdateUserDTO;
import com.ecommerce.user.model.vo.LoginVO;
import com.ecommerce.user.model.vo.UserVO;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户注册、登录、个人信息")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 用户登录 — 防暴力破解，需要限流和授权规则保护（IP黑名单）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @SentinelResource(value = "userLogin", blockHandler = "loginBlockHandler", fallback = "loginFallback")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.success(vo);
    }

    public Result<LoginVO> loginBlockHandler(LoginDTO dto, BlockException ex) {
        log.warn("[用户] 登录请求被Sentinel限流 — username={}", dto.getUsername());
        return Result.fail(429, "登录请求过于频繁，请稍后再试");
    }

    public Result<LoginVO> loginFallback(LoginDTO dto, Throwable ex) {
        log.error("[用户] 登录服务降级 —", ex);
        return Result.fail(503, "登录服务暂时不可用");
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getInfo() {
        Long userId = UserContext.currentUserId();
        UserVO vo = userService.getCurrentUserInfo(userId);
        return Result.success(vo);
    }

    @PutMapping("/info")
    @Operation(summary = "修改个人信息")
    public Result<Void> updateInfo(@Valid @RequestBody UpdateUserDTO dto) {
        Long userId = UserContext.currentUserId();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(dto, userVO);
        userService.updateUserInfo(userId, userVO);
        return Result.success();
    }
}
