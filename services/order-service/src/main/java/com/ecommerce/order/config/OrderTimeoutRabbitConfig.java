package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单超时延迟消息 —— RabbitMQ 原生 TTL + DLX 声明配置（不依赖任何插件）。
 * <p>
 * 链路：
 *   Producer 发送带 expiration(TTL) 的消息 → Exchange(order-timeout-delay)
 *     → Queue(order-timeout.delay)【无消费者，消息在此等待 TTL 到期】
 *       → TTL 到期后由 DLX 转发 → Exchange(order-timeout)【真正的业务交换机】
 *         → Queue(order-timeout.order-service-group)【Spring Cloud Stream 自动声明并由 OrderTimeoutConsumer 消费】
 */
@Configuration
public class OrderTimeoutRabbitConfig {

    // 延迟交换机（发 TTL 消息到此）
    public static final String DELAY_EXCHANGE = "order-timeout-delay";
    // 业务交换机（TTL到期后 DLX 转发到此，Consumer 监听的 Exchange）
    public static final String BIZ_EXCHANGE = "order-timeout";
    // 延迟队列（无消费者，TTL 在这排队）
    public static final String DELAY_QUEUE = "order-timeout.delay";
    // 业务队列名（与 Spring Cloud Stream destination.group 约定一致：{destination}.{group}）
    public static final String BIZ_QUEUE = "order-timeout.order-service-group";
    // 统一 routing key
    public static final String ROUTING_KEY = "order-service-group";

    /** 延迟交换机：类型 Topic（+ routing key=`#`）避免 Stream 默认 routing key=destination 与手动 binding key 不匹配 */
    @Bean
    public TopicExchange orderTimeoutDelayExchange() {
        return ExchangeBuilder.topicExchange(DELAY_EXCHANGE).durable(true).build();
    }

    /** 业务交换机：类型 Topic，TTL 到期后 DLX 转发到此（消费端监听的 Exchange） */
    @Bean
    public TopicExchange orderTimeoutExchange() {
        return ExchangeBuilder.topicExchange(BIZ_EXCHANGE).durable(true).build();
    }

    /** 延迟队列：无消费者消费；TTL 到期后通过 DLX 转发到业务 Exchange */
    @Bean
    public Queue orderTimeoutDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 死信交换机 = 业务 Exchange（类型 Topic，任何 routing key 都能转发）
        args.put("x-dead-letter-exchange", BIZ_EXCHANGE);
        // 死信转发 routing key：保持原消息 routing key（不设置就会沿用原 routing key）；
        // 若需要固定值才配置 x-dead-letter-routing-key。这里使用 Topic Exchange，无需强制固定值。
        return QueueBuilder.durable(DELAY_QUEUE).withArguments(args).build();
    }

    /** 业务队列：真正消费的队列（Spring Cloud Stream consumer 也会声明，这里显式声明保证幂等） */
    @Bean
    public Queue orderTimeoutBizQueue() {
        return QueueBuilder.durable(BIZ_QUEUE).build();
    }

    /** 将延迟队列绑定到延迟交换机：binding key=`#`（匹配任意 routing key），兼容 StreamBridge 默认 routing key=destination */
    @Bean
    public Binding delayQueueBinding(@Qualifier("orderTimeoutDelayQueue") Queue orderTimeoutDelayQueue,
                                     TopicExchange orderTimeoutDelayExchange) {
        return BindingBuilder.bind(orderTimeoutDelayQueue)
                .to(orderTimeoutDelayExchange)
                .with("#");
    }

    /** 将业务队列绑定到业务交换机：binding key=`#`，兼容 Stream 自动声明与 DLX 转发的任意 routing key */
    @Bean
    public Binding bizQueueBinding(@Qualifier("orderTimeoutBizQueue") Queue orderTimeoutBizQueue,
                                   TopicExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutBizQueue)
                .to(orderTimeoutExchange)
                .with("#");
    }
}
