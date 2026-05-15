package com.ecommerce.mq.producer;

import com.ecommerce.core.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
// 各服务继承此类，按业务 topic 封装发送方法，避免直接操作 RocketMQTemplate
public abstract class AbstractMqProducer {

    @Autowired
    protected RocketMQTemplate rocketMQTemplate;

    protected void send(String topic, Object payload) {
        String json = JsonUtils.toJson(payload);
        Message<String> message = MessageBuilder.withPayload(json).build();
        rocketMQTemplate.send(topic, message);
        log.info("[MQ发送] topic={}, payload={}", topic, json);
    }

    // 异步发送
    protected void sendAsync(String topic, Object payload) {
        String json = JsonUtils.toJson(payload);
        Message<String> message = MessageBuilder.withPayload(json).build();
        rocketMQTemplate.asyncSend(topic, message, null);
        log.info("[MQ异步发送] topic={}, payload={}", topic, json);
    }

    protected void sendDelay(String topic, Object payload, int delayLevel) {
        String json = JsonUtils.toJson(payload);
        Message<String> message = MessageBuilder.withPayload(json).build();
        rocketMQTemplate.syncSend(topic, message, 3000, delayLevel);
        log.info("[MQ延迟发送] topic={}, delayLevel={}, payload={}", topic, delayLevel, json);
    }
}
