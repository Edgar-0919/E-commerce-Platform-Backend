package com.ecommerce.inventory.mq;

import com.ecommerce.core.constant.MqTopicConstants;
import com.ecommerce.core.constant.RedisKeyConstants;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 库存消息消费者 — 接收支付成功事件，触发库存扣减
 * <p>
 * 支付成功后，通过 Redis 中锁定时记录的 key（stock:lock:{orderId}:{skuId}）
 * 找到该订单的所有锁定 SKU，调用 deductStock 完成持久化扣减。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final InventoryService inventoryService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public Consumer<Map<String, Object>> paymentSuccess() {
        return message -> {
            Long orderId = toLong(message.get("orderId"));
            String paymentNo = (String) message.get("paymentNo");
            log.info("收到支付成功事件: orderId={}, paymentNo={}", orderId, paymentNo);

            if (orderId == null) {
                log.error("支付成功事件缺少orderId: {}", message);
                return;
            }

            // 扫描 Redis 中该订单的所有锁定量 key: stock:lock:{orderId}:*
            String lockKeyPattern = RedisKeyConstants.STOCK_LOCK_PREFIX + orderId + ":*";
            Set<String> lockKeys = redisTemplate.keys(lockKeyPattern);
            if (lockKeys == null || lockKeys.isEmpty()) {
                log.warn("未找到订单锁定量记录, 可能已被处理: orderId={}", orderId);
                return;
            }

            // 构建 StockOperationDTO 列表，调用 deductStock 完成持久化
            List<StockOperationDTO> items = new ArrayList<>();
            for (String key : lockKeys) {
                // key 格式: stock:lock:{orderId}:{skuId}
                String skuIdStr = key.substring(key.lastIndexOf(":") + 1);
                Long skuId = Long.parseLong(skuIdStr);
                Object qtyObj = redisTemplate.opsForValue().get(key);
                if (qtyObj != null) {
                    StockOperationDTO dto = new StockOperationDTO();
                    dto.setOrderId(orderId);
                    dto.setSkuId(skuId);
                    dto.setQuantity(((Number) qtyObj).intValue());
                    items.add(dto);
                }
            }

            if (items.isEmpty()) {
                log.warn("锁定量为空, 无法扣减: orderId={}", orderId);
                return;
            }

            try {
                inventoryService.deductStock(items);
                log.info("库存扣减完成: orderId={}, skuCount={}", orderId, items.size());
            } catch (Exception e) {
                log.error("库存扣减失败: orderId={}, error={}", orderId, e.getMessage());
                // 抛出异常触发 MQ 重试机制
                throw new RuntimeException("库存扣减失败, orderId=" + orderId, e);
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