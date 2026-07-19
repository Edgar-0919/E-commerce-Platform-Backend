package com.ecommerce.marketing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.core.model.PageResult;
import com.ecommerce.core.model.Result;
import com.ecommerce.marketing.mapper.BannerMapper;
import com.ecommerce.marketing.mapper.CouponTemplateMapper;
import com.ecommerce.marketing.model.entity.Banner;
import com.ecommerce.marketing.model.entity.CouponTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理端-营销管理", description = "优惠券模板、轮播图管理")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMarketingController {

    private final CouponTemplateMapper couponTemplateMapper;
    private final BannerMapper bannerMapper;

    // ======================== 优惠券模板 ========================

    @GetMapping("/coupons/templates")
    @Operation(summary = "优惠券模板列表")
    public Result<PageResult<CouponTemplate>> listTemplates(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<CouponTemplate>()
                .like(StringUtils.hasText(name), CouponTemplate::getName, name)
                .eq(status != null, CouponTemplate::getStatus, status)
                .orderByDesc(CouponTemplate::getCreateTime);
        Page<CouponTemplate> p = couponTemplateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords()));
    }

    @PostMapping("/coupons/templates")
    @Operation(summary = "创建优惠券模板")
    public Result<Void> createTemplate(@RequestBody Map<String, Object> body) {
        CouponTemplate template = new CouponTemplate();
        template.setName((String) body.get("name"));
        template.setType((Integer) body.get("type"));
        template.setThreshold(new BigDecimal(body.get("threshold").toString()));
        template.setAmount(new BigDecimal(body.get("amount").toString()));
        template.setTotalCount((Integer) body.get("totalCount"));
        template.setPerUserLimit((Integer) body.get("perUserLimit"));
        template.setMerchantId(1L);
        template.setStartTime(LocalDateTime.parse((String) body.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        template.setEndTime(LocalDateTime.parse((String) body.get("endTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        template.setStatus(1);
        template.setIssuedCount(0);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.insert(template);
        return Result.success();
    }

    @PutMapping("/coupons/templates/{id}")
    @Operation(summary = "更新优惠券模板")
    public Result<Void> updateTemplate(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            return Result.fail(404, "优惠券模板不存在");
        }
        if (body.containsKey("name")) template.setName((String) body.get("name"));
        if (body.containsKey("type")) template.setType((Integer) body.get("type"));
        if (body.containsKey("threshold")) template.setThreshold(new BigDecimal(body.get("threshold").toString()));
        if (body.containsKey("amount")) template.setAmount(new BigDecimal(body.get("amount").toString()));
        if (body.containsKey("totalCount")) template.setTotalCount((Integer) body.get("totalCount"));
        if (body.containsKey("perUserLimit")) template.setPerUserLimit((Integer) body.get("perUserLimit"));
        if (body.containsKey("startTime")) template.setStartTime(LocalDateTime.parse((String) body.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (body.containsKey("endTime")) template.setEndTime(LocalDateTime.parse((String) body.get("endTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        return Result.success();
    }

    @PutMapping("/coupons/templates/{id}/status")
    @Operation(summary = "启用/禁用优惠券模板")
    public Result<Void> toggleTemplateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            return Result.fail(404, "优惠券模板不存在");
        }
        template.setStatus((Integer) body.get("status"));
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/coupons/templates/{id}")
    @Operation(summary = "删除优惠券模板")
    public Result<Void> deleteTemplate(@PathVariable("id") Long id) {
        couponTemplateMapper.deleteById(id);
        return Result.success();
    }

    // ======================== 轮播图管理 ========================

    @GetMapping("/banners")
    @Operation(summary = "轮播图列表")
    public Result<PageResult<Banner>> listBanners(
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<Banner>()
                .eq(StringUtils.hasText(position), Banner::getPosition, position)
                .eq(status != null, Banner::getStatus, status)
                .orderByAsc(Banner::getSort);
        Page<Banner> p = bannerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords()));
    }

    @PostMapping("/banners")
    @Operation(summary = "创建轮播图")
    public Result<Void> createBanner(@RequestBody Map<String, Object> body) {
        Banner banner = new Banner();
        banner.setImage((String) body.get("image"));
        banner.setTitle((String) body.get("title"));
        banner.setDescription((String) body.get("description"));
        banner.setPosition((String) body.getOrDefault("position", "home"));
        banner.setLinkUrl((String) body.get("linkUrl"));
        banner.setSort((Integer) body.getOrDefault("sort", 0));
        banner.setStatus((Integer) body.getOrDefault("status", 1));
        if (body.containsKey("startTime")) banner.setStartTime(LocalDateTime.parse((String) body.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (body.containsKey("endTime")) banner.setEndTime(LocalDateTime.parse((String) body.get("endTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.insert(banner);
        return Result.success();
    }

    @PutMapping("/banners/{id}")
    @Operation(summary = "更新轮播图")
    public Result<Void> updateBanner(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail(404, "轮播图不存在");
        }
        if (body.containsKey("image")) banner.setImage((String) body.get("image"));
        if (body.containsKey("title")) banner.setTitle((String) body.get("title"));
        if (body.containsKey("description")) banner.setDescription((String) body.get("description"));
        if (body.containsKey("position")) banner.setPosition((String) body.get("position"));
        if (body.containsKey("linkUrl")) banner.setLinkUrl((String) body.get("linkUrl"));
        if (body.containsKey("sort")) banner.setSort((Integer) body.get("sort"));
        if (body.containsKey("startTime")) banner.setStartTime(LocalDateTime.parse((String) body.get("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (body.containsKey("endTime")) banner.setEndTime(LocalDateTime.parse((String) body.get("endTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(banner);
        return Result.success();
    }

    @PutMapping("/banners/{id}/status")
    @Operation(summary = "启用/禁用轮播图")
    public Result<Void> toggleBannerStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail(404, "轮播图不存在");
        }
        banner.setStatus((Integer) body.get("status"));
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(banner);
        return Result.success();
    }

    @DeleteMapping("/banners/{id}")
    @Operation(summary = "删除轮播图")
    public Result<Void> deleteBanner(@PathVariable("id") Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail(404, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
        return Result.success();
    }
}