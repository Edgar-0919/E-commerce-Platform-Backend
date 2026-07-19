package com.ecommerce.product.inventory.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.OrderStatusEnum;
import com.ecommerce.product.inventory.model.dto.StockOperationDTO;
import com.ecommerce.product.inventory.model.entity.StockPreLock;
import com.ecommerce.product.inventory.InventoryService;
import com.ecommerce.product.inventory.feign.OrderFeignClient;
import com.ecommerce.product.inventory.mapper.StockPreLockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预扣对账补偿任务
 * <p>
 * 三层兜底机制：
 * 1. 正常流程：延迟消息 → 自动取消 → 释放库存
 * 2. 对账补偿：定时扫描超时预扣记录 → 查询订单状态 → 执行补偿回滚
 * 3. 告警：对账重试超过3次仍失败 → 记录告警日志（可接入钉钉/邮件等）
 * <p>
 * 每5分钟执行一次，每次最多处理100条，防止批量操作打爆DB。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class StockReconciliationTask {

    private final StockPreLockMapper stockPreLockMapper;
    private final OrderFeignClient orderFeignClient;
    private final InventoryService inventoryService;

    /** 超时阈值：预扣超过30分钟仍未处理 */
    private static final int TIMEOUT_MINUTES = 30;
    /** 每次处理上限 */
    private static final int BATCH_SIZE = 100;
    /** 告警阈值：重试次数 */
    private static final int ALERT_RETRY_COUNT = 3;

    /**
     * 每5分钟执行一次对账
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void reconcile() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<StockPreLock> pendingList = stockPreLockMapper.selectList(
                new LambdaQueryWrapper<StockPreLock>()
                        .eq(StockPreLock::getStatus, 0) // 预扣中
                        .lt(StockPreLock::getCreateTime, threshold)
                        .last("LIMIT " + BATCH_SIZE));

        if (pendingList.isEmpty()) return;

        log.info("[对账任务] 发现{}条超时预扣记录，开始补偿处理", pendingList.size());
        int successCount = 0;
        int failCount = 0;

        for (StockPreLock preLock : pendingList) {
            try {
                processReconciliation(preLock);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("[对账任务] 处理失败: orderId={}, skuId={}, retry={}",
                        preLock.getOrderId(), preLock.getSkuId(), preLock.getRetryCount(), e);

                preLock.setRetryCount(preLock.getRetryCount() + 1);
                stockPreLockMapper.updateById(preLock);

                if (preLock.getRetryCount() >= ALERT_RETRY_COUNT) {
                    sendAlert(preLock, e);
                }
            }
        }

        log.info("[对账任务] 完成: 成功={}, 失败={}", successCount, failCount);
    }

    /**
     * 处理单条对账记录
     */
    private void processReconciliation(StockPreLock preLock) {
        // 通过 Feign 查询订单当前状态
        Integer orderStatus;
        try {
            var result = orderFeignClient.getById(preLock.getOrderId());
            if (result == null || result.getData() == null) {
                log.warn("[对账任务] 订单不存在: orderId={}", preLock.getOrderId());
                // 订单不存在，标记预扣为已回滚
                preLock.setStatus(2);
                stockPreLockMapper.updateById(preLock);
                return;
            }
            Object statusObj = result.getData().get("status");
            orderStatus = statusObj != null ? ((Number) statusObj).intValue() : null;
        } catch (Exception e) {
            log.error("[对账任务] Feign查询订单状态失败: orderId={}", preLock.getOrderId(), e);
            throw e; // 抛出异常，外层统一处理重试
        }

        if (orderStatus == null) {
            log.warn("[对账任务] 订单状态为空: orderId={}", preLock.getOrderId());
            return;
        }

        if (orderStatus == OrderStatusEnum.PENDING_PAY.getCode()) {
            // 订单仍待支付 → 执行回滚释放库存
            StockOperationDTO dto = new StockOperationDTO();
            dto.setOrderId(preLock.getOrderId());
            dto.setSkuId(preLock.getSkuId());
            dto.setQuantity(preLock.getQuantity());
            inventoryService.releaseStock(List.of(dto));
            log.info("[对账任务] 补偿回滚成功: orderId={}, skuId={}, qty={}",
                    preLock.getOrderId(), preLock.getSkuId(), preLock.getQuantity());
        } else if (orderStatus == OrderStatusEnum.CANCELLED.getCode()
                || orderStatus == OrderStatusEnum.REFUNDED.getCode()) {
            // 订单已取消/已退款但预扣状态未更新 → 补更新状态为已回滚
            preLock.setStatus(2);
            stockPreLockMapper.updateById(preLock);
            log.info("[对账任务] 补更新预扣状态为已回滚: orderId={}", preLock.getOrderId());
        } else {
            // 订单已支付/已发货/已收货 → 标记为已实扣
            preLock.setStatus(1);
            stockPreLockMapper.updateById(preLock);
            log.info("[对账任务] 补更新预扣状态为已实扣: orderId={}, orderStatus={}",
                    preLock.getOrderId(), orderStatus);
        }
    }

    /**
     * 告警通知 — 对账重试超过阈值仍未成功
     * <p>
     * 当前仅记录错误日志，生产环境可接入钉钉机器人/企业微信/邮件等。
     */
    private void sendAlert(StockPreLock preLock, Exception e) {
        log.error("[库存对账告警] 预扣记录对账失败超过{}次，" +
                        "orderId={}, skuId={}, qty={}, retryCount={}, error={}",
                ALERT_RETRY_COUNT,
                preLock.getOrderId(),
                preLock.getSkuId(),
                preLock.getQuantity(),
                preLock.getRetryCount(),
                e.getMessage());
    }
}