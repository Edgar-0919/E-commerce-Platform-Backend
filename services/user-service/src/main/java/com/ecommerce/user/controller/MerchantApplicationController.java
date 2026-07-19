package com.ecommerce.user.controller;

import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.user.model.dto.MerchantApplicationDTO;
import com.ecommerce.user.model.vo.MerchantApplicationVO;
import com.ecommerce.user.service.MerchantApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/merchant-application")
@RequiredArgsConstructor
@Tag(name = "用户端-商户申请", description = "商户入驻申请")
public class MerchantApplicationController {

    private final MerchantApplicationService applicationService;

    @PostMapping
    @Operation(summary = "提交商户申请")
    public Result<Void> submit(@Valid @RequestBody MerchantApplicationDTO dto) {
        Long userId = UserContext.currentUserId();
        applicationService.submitApplication(dto, userId);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "获取当前用户的商户申请状态")
    public Result<MerchantApplicationVO> getStatus() {
        Long userId = UserContext.currentUserId();
        MerchantApplicationVO vo = applicationService.getApplicationByUserId(userId);
        return Result.success(vo);
    }
}