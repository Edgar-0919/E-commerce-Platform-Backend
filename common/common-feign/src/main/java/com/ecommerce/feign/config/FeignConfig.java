package com.ecommerce.feign.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置扩展
 * <p>
 * 透传用户上下文到下游服务
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }
}
