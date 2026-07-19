package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 路由配置 — 所有业务路由在此声明，保证启动即可工作，不依赖 Nacos 配置
 * <p>
 * 路由规则：
 * - B 端管理：/api/admin/** → 各业务服务
 * - C 端业务：/api/{product,user,order,payment,inventory,marketing,cart,search}/** → 各业务服务
 * <p>
 * 合并后架构（4 服务）：product(商品+库存+搜索) / order(订单+支付+购物车) / user / marketing
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator adminRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // ===== B 端管理路由 =====
                // 商品相关管理（商品/品牌/分类/库存 → product-service）
                .route("admin-products", r -> r
                        .path("/api/admin/products/**")
                        .uri("lb://product-service"))
                .route("admin-brands", r -> r
                        .path("/api/admin/brands/**")
                        .uri("lb://product-service"))
                .route("admin-categories", r -> r
                        .path("/api/admin/categories/**")
                        .uri("lb://product-service"))
                .route("admin-inventory", r -> r
                        .path("/api/admin/inventory/**")
                        .uri("lb://product-service"))
                // 订单相关管理（订单/仪表盘/退款 → order-service）
                .route("admin-orders", r -> r
                        .path("/api/admin/orders/**")
                        .uri("lb://order-service"))
                .route("admin-dashboard", r -> r
                        .path("/api/admin/dashboard/**")
                        .uri("lb://order-service"))
                .route("admin-refunds", r -> r
                        .path("/api/admin/refunds/**")
                        .uri("lb://order-service"))
                // 用户管理
                .route("admin-users", r -> r
                        .path("/api/admin/users/**")
                        .uri("lb://user-service"))
                // 商户管理（需在营销catch-all之前）
                .route("admin-merchants", r -> r
                        .path("/api/admin/merchants/**")
                        .uri("lb://user-service"))
                // 商户申请（需在营销catch-all之前）
                .route("admin-merchant-applications", r -> r
                        .path("/api/admin/merchant-applications/**")
                        .uri("lb://user-service"))
                // 营销管理（优惠券 + 促销）
                .route("admin-marketing", r -> r
                        .path("/api/admin/**")
                        .uri("lb://marketing-service"))
                // ===== C 端业务路由 =====
                .route("product-service", r -> r
                        .path("/api/product/**")
                        .uri("lb://product-service"))
                .route("product-inventory", r -> r
                        .path("/api/inventory/**")
                        .uri("lb://product-service"))
                .route("product-search", r -> r
                        .path("/api/search/**")
                        .uri("lb://product-service"))
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .uri("lb://user-service"))
                .route("order-service", r -> r
                        .path("/api/order/**")
                        .uri("lb://order-service"))
                .route("order-payment", r -> r
                        .path("/api/payment/**")
                        .uri("lb://order-service"))
                .route("order-cart", r -> r
                        .path("/api/cart/**")
                        .uri("lb://order-service"))
                .route("marketing-service", r -> r
                        .path("/api/marketing/**")
                        .uri("lb://marketing-service"))
                .build();
    }
}
