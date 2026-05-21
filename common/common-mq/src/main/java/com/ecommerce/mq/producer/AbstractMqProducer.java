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
        String json = JsonUtils.toJson(payload);
        Message<String> message = MessageBuilder.withPayload(json).build();
        if (streamBridge.send(bindingName, message)) {
            log.info("[MQ发送] binding={}, payload={}", bindingName, json);
        } else {
            log.error("[MQ发送失败] binding={}, payload={}", bindingName, json);
        }
    }

    /**
     * 异步发送消息（RabbitMQ binder 默认异步，此方法与 send 等效）
     */
    protected void sendAsync(String bindingName, Object payload) {
        send(bindingName, payload);
    }

    /**
     * 发送延迟消息
     * <p>
     * RabbitMQ 需启用 delayed-message-exchange 插件。
     * 在 binding 的 producer 配置中设置 spring.cloud.stream.rabbit.bindings.{name}.producer.delayed-exchange=true
     * 然后通过消息头 x-delay 设置延迟时间（毫秒）。
     *
     * @param bindingName binding 名称
     * @param payload     消息体
     * @param delayMs     延迟毫秒数
     */
    protected void sendDelay(String bindingName, Object payload, long delayMs) {
        String json = JsonUtils.toJson(payload);
        Message<String> message = MessageBuilder.withPayload(json)
                .setHeader("x-delay", delayMs)
                .build();
        if (streamBridge.send(bindingName, message)) {
            log.info("[MQ延迟发送] binding={}, delayMs={}, payload={}", bindingName, delayMs, json);
        } else {
            log.error("[MQ延迟发送失败] binding={}, delayMs={}, payload={}", bindingName, delayMs, json);
        }
    }
}