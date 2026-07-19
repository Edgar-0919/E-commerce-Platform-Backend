package com.ecommerce.marketing.mq;

import com.ecommerce.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 优惠券核销消息消费者 — 订单支付成功后消费优惠券核销事件
 * <p>
 * 监听 destination: coupon-use，消费者组: marketing-service
 * 作为 Feign 调用失败时的兜底方案，通过 MQ 异步重试确保最终一致性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsedConsumer {

    private final MarketingService marketingService;

    @Bean
    public Consumer<Map<String, Object>> couponUse() {
        return message -> {
            Long userId = toLong(message.get("userId"));
            Long couponId = toLong(message.get("couponId"));
            Long orderId = toLong(message.get("orderId"));
            String orderNo = (String) message.get("orderNo");
            log.info("收到优惠券确认事件(兜底): userId={}, couponId={}, orderId={}, orderNo={}",
                    userId, couponId, orderId, orderNo);

            if (couponId == null || orderId == null) {
                log.error("优惠券确认事件缺少必要参数: couponId={}, orderId={}", couponId, orderId);
                throw new RuntimeException("缺少必要参数");
            }

            try {
                marketingService.confirmCoupon(couponId, orderId);
                log.info("优惠券确认成功(兜底): couponId={}, orderId={}", couponId, orderId);
            } catch (Exception e) {
                log.error("优惠券确认失败(兜底): couponId={}, orderId={}, error={}",
                        couponId, orderId, e.getMessage(), e);
                throw new RuntimeException("优惠券确认失败: " + e.getMessage(), e);
            }
        };
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}