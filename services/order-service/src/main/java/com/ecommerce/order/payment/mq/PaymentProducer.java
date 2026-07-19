package com.ecommerce.order.payment.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import com.ecommerce.order.payment.model.event.PaymentEvent;
import com.ecommerce.order.payment.model.event.RefundEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付消息生产者
 * <p>
 * - payment-success: 支付成功后通知 product-service 扣减库存（跨服务 MQ）
 * - refund-success: 退款成功后通知 product-service 恢复库存（跨服务 MQ）
 */
@Slf4j
@Component
public class PaymentProducer extends AbstractMqProducer {

    /** 支付成功 output binding */
    static final String BINDING_PAYMENT_SUCCESS = "paymentSuccess-out-0";
    /** 退款成功 output binding */
    static final String BINDING_REFUND_SUCCESS = "refundSuccess-out-0";
    /** 优惠券核销 output binding */
    static final String BINDING_COUPON_USE = "couponUse-out-0";

    /** 发送支付成功事件（跨服务：通知 product-service 扣减库存） */
    public void sendPaymentSuccess(PaymentEvent event) {
        send(BINDING_PAYMENT_SUCCESS, event);
    }

    /** 发送退款成功事件（跨服务：通知 product-service 恢复库存） */
    public void sendRefundSuccess(RefundEvent event) {
        send(BINDING_REFUND_SUCCESS, event);
    }

    /** 发送优惠券核销事件（跨服务：通知 marketing-service 核销优惠券） */
    public void sendCouponUse(Map<String, Object> data) {
        send(BINDING_COUPON_USE, data);
    }
}