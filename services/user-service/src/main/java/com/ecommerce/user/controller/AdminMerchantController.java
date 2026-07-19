package com.ecommerce.user.controller;

import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.core.model.UserContext;
import com.ecommerce.user.model.dto.ReviewApplicationDTO;
import com.ecommerce.user.model.entity.Merchant;
import com.ecommerce.user.model.vo.MerchantApplicationVO;
import com.ecommerce.user.service.MerchantApplicationService;
import com.ecommerce.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理端-商户管理", description = "商户CRUD（仅ROLE_ADMIN）")
public class AdminMerchantController {

    private final MerchantService merchantService;
    private final MerchantApplicationService applicationService;

    @GetMapping("/merchants")
    @Operation(summary = "商户分页列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<Merchant>> page(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status) {
        return Result.success(merchantService.page(pageNum, pageSize, name, status));
    }

    @GetMapping("/merchants/{id}")
    @Operation(summary = "商户详情")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Merchant> detail(@PathVariable("id") Long id) {
        return Result.success(merchantService.getById(id));
    }

    @PostMapping("/merchants")
    @Operation(summary = "创建商户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Merchant merchant = new Merchant();
        merchant.setName((String) body.get("name"));
        merchant.setContactName((String) body.get("contactName"));
        merchant.setContactPhone((String) body.get("contactPhone"));
        merchant.setEmail((String) body.get("email"));
        merchant.setAddress((String) body.get("address"));
        merchant.setStatus(1);
        merchant.setCreateTime(LocalDateTime.now());
        merchant.setUpdateTime(LocalDateTime.now());
        merchantService.create(merchant);
        return Result.success();
    }

    @PutMapping("/merchants/{id}")
    @Operation(summary = "更新商户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Merchant merchant = merchantService.getById(id);
        if (merchant == null) {
            return Result.fail(404, "商户不存在");
        }
        if (body.containsKey("name")) merchant.setName((String) body.get("name"));
        if (body.containsKey("contactName")) merchant.setContactName((String) body.get("contactName"));
        if (body.containsKey("contactPhone")) merchant.setContactPhone((String) body.get("contactPhone"));
        if (body.containsKey("email")) merchant.setEmail((String) body.get("email"));
        if (body.containsKey("address")) merchant.setAddress((String) body.get("address"));
        merchant.setUpdateTime(LocalDateTime.now());
        merchantService.update(merchant);
        return Result.success();
    }

    @PutMapping("/merchants/{id}/status")
    @Operation(summary = "启用/禁用商户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        merchantService.updateStatus(id, (Integer) body.get("status"));
        return Result.success();
    }

    @GetMapping("/merchant-applications")
    @Operation(summary = "商户申请列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<MerchantApplicationVO>> listApplications(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status) {
        return Result.success(applicationService.page(pageNum, pageSize, status));
    }

    @GetMapping("/merchant-applications/{id}")
    @Operation(summary = "商户申请详情")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MerchantApplicationVO> getApplication(@PathVariable("id") Long id) {
        return Result.success(applicationService.getApplicationById(id));
    }

    @PutMapping("/merchant-applications/{id}/review")
    @Operation(summary = "审核商户申请")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reviewApplication(@PathVariable("id") Long id, @Valid @RequestBody ReviewApplicationDTO dto) {
        Long reviewerId = UserContext.currentUserId();
        applicationService.review(id, dto, reviewerId);
        return Result.success();
    }
}