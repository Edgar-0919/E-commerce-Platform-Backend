package com.ecommerce.mq.producer;

import com.ecommerce.core.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * 消息生产者抽象基类 — 基于 Spring Cloud Stream + StreamBridge
 * <p>
 * 各服务继承此类，按业务 binding 封装发送方法。
 * 使用 StreamBridge 实现动态目标绑定，避免硬编码 output channel。
 * <p>
 * 使用方式：
 * <pre>
 * &#064;Component
 * public class PaymentProducer extends AbstractMqProducer {
 *     public void sendPaymentSuccess(PaymentEvent event) {
 *         send("payment-success", event);
 *     }
 * }
 * </pre>
 * <p>
 * 消费者端通过 spring.cloud.stream.bindings 配置文件定义 input binding。
 */
@Slf4j
public abstract class AbstractMqProducer {

    @Autowired
    protected StreamBridge streamBridge;

    /**
     * 同步发送消息到指定 binding
     *
     * @param bindingName binding 名称（对应 spring.cloud.stream.bindings 配置中的 output）
     * @param payload     消息体（自动 JSON 序列化）
     */
    protected void send(String bindingName, Object payload) {
        Message<Object> message = MessageBuilder.withPayload(payload).build();
        if (streamBridge.send(bindingName, message)) {
            log.info("[MQ发送] binding={}, payload={}", bindingName, payload);
        } else {
            log.error("[MQ发送失败] binding={}, payload={}", bindingName, payload);
        }
    }

    /**
     * 异步发送消息（RabbitMQ binder 默认异步，此方法与 send 等效）
     */
    protected void sendAsync(String bindingName, Object payload) {
        send(bindingName, payload);
    }

    /**
     * 发送延迟消息 — 基于 RabbitMQ 原生 TTL + DLX（死信交换机），不依赖任何插件。
     * <p>
     * 链路：Producer 发送消息（设置 AMQP expiration = TTL 毫秒数）
     *        → 投递到 binding 对应的 delay Exchange
     *        → 进入 Delay Queue（无消费者消费）
     *        → TTL 到期自动转发到 DLX（死信交换机 = 真正的业务 Exchange）
     *        → 由业务 Queue 接收，Consumer 消费（相当于延迟生效）。
     * <p>
     * 每个业务 binding 需要配套定义：
     *   1) delay Exchange（类型 direct，比如 order-timeout-delay）
     *   2) Delay Queue（绑定到 delay Exchange，参数含 x-dead-letter-exchange/x-dead-letter-routing-key）
     *   3) 业务 Exchange（真正消费的 Exchange，比如 order-timeout）+ 业务 Queue
     *
     * @param bindingName binding 名称（应指向 delay exchange 对应的 output binding，不是业务 binding）
     * @param payload     消息体
     * @param delayMs     延迟毫秒数（消息级 TTL）
     */
    protected void sendDelay(String bindingName, Object payload, long delayMs) {
        // AMQP 0-9-1 协议的 expiration 字段（毫秒数字符串）表示消息在队列中的最大存活时间
        // Spring Cloud Stream Rabbit Binder 会自动识别该 header 并透传到 MessageProperties.expiration
        Message<Object> message = MessageBuilder.withPayload(payload)
                .setHeader("expiration", String.valueOf(delayMs))
                .build();
        if (streamBridge.send(bindingName, message)) {
            log.info("[MQ延迟发送] binding={}, ttlMs={}, payload={}", bindingName, delayMs, payload);
        } else {
            log.error("[MQ延迟发送失败] binding={}, ttlMs={}, payload={}", bindingName, delayMs, payload);
        }
    }
}