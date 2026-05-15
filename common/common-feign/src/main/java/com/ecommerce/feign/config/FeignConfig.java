package com.ecommerce.feign.config;

import com.ecommerce.feign.config.FeignHeaderInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }
}
