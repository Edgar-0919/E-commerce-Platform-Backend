package com.ecommerce.product.inventory.mq;

import com.ecommerce.product.inventory.InventoryService;
import com.ecommerce.product.inventory.model.dto.StockOperationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 退款成功消息消费者 — 接收退款成功事件，恢复库存
 * <p>
 * 退款成功后，订单商品库存需要回退。通过扫描订单的 order_item 获取 SKU 和数量，
 * 调用 increaseStock 完成库存恢复。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundSuccessConsumer {

    private final InventoryService inventoryService;

    @Bean
    public Consumer<Map<String, Object>> refundSuccess() {
        return message -> {
            Long orderId = toLong(message.get("orderId"));
            String refundNo = (String) message.get("refundNo");
            long paymentNo = toLong(message.get("paymentNo"));
            log.info("收到退款成功事件: orderId={}, refundNo={}", orderId, refundNo);

            if (orderId == null) {
                log.error("退款成功事件缺少orderId: {}", message);
                return;
            }

            // 获取订单项列表（从消息中携带）
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orderItems = (List<Map<String, Object>>) message.get("orderItems");
            if (orderItems == null || orderItems.isEmpty()) {
                log.warn("退款成功事件缺少订单项，无法恢复库存: orderId={}", orderId);
                return;
            }

            List<StockOperationDTO> items = new ArrayList<>();
            for (Map<String, Object> item : orderItems) {
                StockOperationDTO dto = new StockOperationDTO();
                dto.setOrderId(orderId);
                dto.setSkuId(toLong(item.get("skuId")));
                dto.setQuantity(toInt(item.get("quantity")));
                items.add(dto);
            }

            try {
                inventoryService.increaseStock(items);
                log.info("退款库存恢复完成: orderId={}, refundNo={}, itemCount={}", orderId, refundNo, items.size());
            } catch (Exception e) {
                log.error("退款库存恢复失败: orderId={}, error={}", orderId, e.getMessage());
                throw new RuntimeException("退款库存恢复失败, orderId=" + orderId, e);
            }
        };
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return null; }
    }
}