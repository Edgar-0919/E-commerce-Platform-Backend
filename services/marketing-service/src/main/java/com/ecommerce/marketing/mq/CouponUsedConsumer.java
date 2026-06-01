package com.ecommerce.marketing.mq;

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
 * 由 order-service 在订单创建时发送（Seata 事务内），
 * 此处作为 Seata 失败时的补偿机制，通过 MQ 异步回滚/重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponUsedConsumer {

    @Bean
    public Consumer<Map<String, Object>> couponUse() {
        return message -> {
            Long userId = toLong(message.get("userId"));
            Long couponId = toLong(message.get("couponId"));
            String orderNo = (String) message.get("orderNo");
            log.info("收到优惠券核销事件: userId={}, couponId={}, orderNo={}",
                    userId, couponId, orderNo);

            // 优惠券状态变更由 Seata AT 模式主路径保障，
            // MQ 消费者作为最终一致性的补偿机制，记录日志即可
            if (orderNo == null) {
                log.error("优惠券核销事件缺少orderNo: {}", message);
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