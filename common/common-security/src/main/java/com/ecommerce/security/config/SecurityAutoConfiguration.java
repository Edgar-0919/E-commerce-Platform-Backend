package com.ecommerce.security.config;

import com.ecommerce.security.util.JwtUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 安全自动配置类
 * 使用@AutoConfiguration实现自动装配：
 * - 引入common-security模块即可自动注册JwtUtils Bean
 * - 通过spring.factories配置文件实现自动发现
 */
@AutoConfiguration
public class SecurityAutoConfiguration {

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils();
    }

    /**
     * Servlet 应用的 Security 配置（普通微服务）
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletSecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                // 禁用CSRF保护
                .csrf(csrf -> csrf.disable())
                // 配置会话管理：无状态
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置授权：所有请求都允许（认证在Gateway层处理）
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll())
                // 禁用HTTP Basic认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 禁用formLogin
                .formLogin(formLogin -> formLogin.disable())
                // 禁用logout
                .logout(logout -> logout.disable());

            return http.build();
        }
    }

    /**
     * WebFlux 应用的 Security 配置（网关）
     */
    @Configuration
    @EnableWebFluxSecurity
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class ReactiveSecurityConfig {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            http
                // 禁用CSRF保护
                .csrf(csrf -> csrf.disable())
                // 配置授权：所有请求都允许（认证在Gateway层处理）
                .authorizeExchange(exchange -> exchange
                    .anyExchange().permitAll())
                // 禁用HTTP Basic认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 禁用formLogin
                .formLogin(formLogin -> formLogin.disable())
                // 禁用logout
                .logout(logout -> logout.disable());

            return http.build();
        }
    }
}
