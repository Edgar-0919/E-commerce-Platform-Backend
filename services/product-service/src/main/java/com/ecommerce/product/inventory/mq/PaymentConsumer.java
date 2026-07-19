package com.ecommerce.product.inventory.mq;

import com.ecommerce.core.constant.RedisKeyConstants;
import com.ecommerce.product.inventory.model.dto.StockOperationDTO;
import com.ecommerce.product.inventory.InventoryService;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
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
 * 库存消息消费者 — 接收支付成功事件，触发库存扣减和销量递增
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final InventoryService inventoryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductMapper productMapper;

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

                // 更新商品销量
                updateProductSales(orderId, message);

            } catch (Exception e) {
                log.error("库存扣减失败: orderId={}, error={}", orderId, e.getMessage());
                throw new RuntimeException("库存扣减失败, orderId=" + orderId, e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void updateProductSales(Long orderId, Map<String, Object> message) {
        try {
            List<Map<String, Object>> orderItems = (List<Map<String, Object>>) message.get("orderItems");
            if (orderItems == null || orderItems.isEmpty()) {
                log.warn("订单明细为空, 跳过销量更新: orderId={}", orderId);
                return;
            }

            for (Map<String, Object> item : orderItems) {
                Long productId = toLong(item.get("productId"));
                Integer quantity = item.get("quantity") instanceof Number
                        ? ((Number) item.get("quantity")).intValue() : null;

                if (productId != null && quantity != null && quantity > 0) {
                    Product product = productMapper.selectById(productId);
                    if (product != null) {
                        int newSales = (product.getSalesCount() != null ? product.getSalesCount() : 0) + quantity;
                        product.setSalesCount(newSales);
                        productMapper.updateById(product);
                        log.info("销量更新成功: productId={}, salesCount={}", productId, newSales);
                    }
                }
            }
        } catch (Exception e) {
            log.error("销量更新失败(不影响主流程): orderId={}, error={}", orderId, e.getMessage());
        }
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