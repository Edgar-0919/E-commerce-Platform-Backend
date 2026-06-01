package com.ecommerce.payment.mq;

import com.ecommerce.mq.producer.AbstractMqProducer;
import com.ecommerce.payment.model.event.PaymentEvent;
import com.ecommerce.payment.model.event.RefundEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付消息生产者
 * <p>
 * - payment-success: 支付成功后通知 inventory-service 扣减库存
 * - refund-success: 退款成功后通知 order-service 更新状态
 */
@Slf4j
@Component
public class PaymentProducer extends AbstractMqProducer {

    /** 支付成功 output binding */
    static final String BINDING_PAYMENT_SUCCESS = "paymentSuccess-out-0";
    /** 退款成功 output binding */
    static final String BINDING_REFUND_SUCCESS = "refundSuccess-out-0";

    /** 发送支付成功事件 */
    public void sendPaymentSuccess(PaymentEvent event) {
        send(BINDING_PAYMENT_SUCCESS, event);
    }

    /** 发送退款成功事件 */
    public void sendRefundSuccess(RefundEvent event) {
        send(BINDING_REFUND_SUCCESS, event);
    }
}