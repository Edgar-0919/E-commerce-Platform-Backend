package com.ecommerce.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
public class GatewayApplication {

    public static void main(String[] args) {
        // 注册 Sentinel 网关流控自定义异常处理
        GatewayCallbackManager.setBlockHandler(new GatewaySentinelBlockHandler());
        SpringApplication.run(GatewayApplication.class, args);
    }
}
