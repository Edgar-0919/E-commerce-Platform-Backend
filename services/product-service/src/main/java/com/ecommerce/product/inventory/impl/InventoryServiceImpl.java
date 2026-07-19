package com.ecommerce.product.inventory.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.RedisKeyConstants;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.InventoryException;
import com.ecommerce.core.util.IdGenerator;
import com.ecommerce.product.inventory.mapper.StockLogMapper;
import com.ecommerce.product.inventory.mapper.StockMapper;
import com.ecommerce.product.inventory.mapper.StockPreLockMapper;
import com.ecommerce.product.inventory.model.dto.StockOperationDTO;
import com.ecommerce.product.inventory.model.entity.Stock;
import com.ecommerce.product.inventory.model.entity.StockLog;
import com.ecommerce.product.inventory.model.entity.StockPreLock;
import com.ecommerce.product.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存服务实现类
 * 库存管理策略：
 * 1. Redis做热点缓存，减少DB访问
 * 2. Lua脚本保证原子性操作，防止超卖
 * 3. 库存锁定机制：下单锁定→支付成功扣减→取消释放
 * 核心方法：
 * - lockStock: 锁定库存（Redis原子操作）
 * - releaseStock: 释放库存（回退锁定量）
 * - deductStock: 确认扣减（持久化到DB）
 */
@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final StockMapper stockMapper;
    private final StockLogMapper stockLogMapper;
    private final StockPreLockMapper stockPreLockMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> lockScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final DefaultRedisScript<Long> deductScript;

    public InventoryServiceImpl(
            StockMapper stockMapper,
            StockLogMapper stockLogMapper,
            StockPreLockMapper stockPreLockMapper,
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("lockScript") DefaultRedisScript<Long> lockScript,
            @Qualifier("releaseScript") DefaultRedisScript<Long> releaseScript,
            @Qualifier("deductScript") DefaultRedisScript<Long> deductScript) {
        this.stockMapper = stockMapper;
        this.stockLogMapper = stockLogMapper;
        this.stockPreLockMapper = stockPreLockMapper;
        this.redisTemplate = redisTemplate;
        this.lockScript = lockScript;
        this.releaseScript = releaseScript;
        this.deductScript = deductScript;
    }

    @Override
    public Integer getStock(Long skuId) {
        String key = RedisKeyConstants.STOCK_PREFIX + skuId;
        Object val = redisTemplate.opsForValue().get(key);
        // Redis 缓存未命中时从 DB 同步（冷数据回种）
        if (val == null) {
            syncStockToRedis(skuId);
            val = redisTemplate.opsForValue().get(key);
        }
        return val != null ? ((Number) val).intValue() : 0;
    }

    // 锁定：Redis Lua 原子扣减（防超卖）+ 记录锁定明细 + DB 本地消息表双写 + 变更日志
    @Override
    @Transactional
    public void lockStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            String stockKey = RedisKeyConstants.STOCK_PREFIX + item.getSkuId();
            Long result = redisTemplate.execute(lockScript,
                    Collections.singletonList(stockKey),
                    item.getQuantity());

            if (result == null || result == -1) {
                syncStockToRedis(item.getSkuId());
                result = redisTemplate.execute(lockScript,
                        Collections.singletonList(stockKey),
                        item.getQuantity());

                if (result == null || result == -1) {
                    throw new InventoryException("SKU库存未初始化: " + item.getSkuId());
                }
            }
            if (result == 0) {
                throw new InventoryException(ResultCodeEnum.STOCK_INSUFFICIENT);
            }

            // Record lock in Redis (for later deduct/release)
            String lockKey = RedisKeyConstants.STOCK_LOCK_PREFIX + item.getOrderId() + ":" + item.getSkuId();
            redisTemplate.opsForValue().set(lockKey, item.getQuantity());

            // DB 本地消息表双写：记录预扣流水，用于超时对账补偿
            StockPreLock preLock = new StockPreLock();
            preLock.setId(IdGenerator.nextId());
            preLock.setOrderId(item.getOrderId());
            preLock.setSkuId(item.getSkuId());
            preLock.setQuantity(item.getQuantity());
            preLock.setStatus(0); // 预扣中
            stockPreLockMapper.insert(preLock);

            // 变更日志（before_qty/after_qty 表字段 NOT NULL，必须赋值）
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getSkuId, item.getSkuId()));
            int before = stock != null ? stock.getAvailableStock() : 0;
            StockLog logBo = new StockLog();
            logBo.setSkuId(item.getSkuId());
            logBo.setOrderId(item.getOrderId());
            logBo.setType("lock");
            logBo.setQuantity(item.getQuantity());
            logBo.setBeforeQty(before);
            logBo.setAfterQty(Math.max(0, before - item.getQuantity()));
            logBo.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(logBo);

            log.info("库存预扣成功: orderId={}, skuId={}, qty={}", item.getOrderId(), item.getSkuId(), item.getQuantity());
        }
    }

    // 释放：订单取消/超时，Redis Lua 退回锁定库存到可用库存 + 更新 DB 预扣状态 + 变更日志
    // 释放失败不抛异常，由对账任务兜底补偿
    @Override
    @Transactional
    public void releaseStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            String stockKey = RedisKeyConstants.STOCK_PREFIX + item.getSkuId();
            String lockKey = RedisKeyConstants.STOCK_LOCK_PREFIX + item.getOrderId() + ":" + item.getSkuId();
            Long result = redisTemplate.execute(releaseScript,
                    List.of(stockKey, lockKey),
                    item.getQuantity());

            if (result == null || result == -1) {
                log.error("库存释放失败(Redis Lua返回-1): orderId={}, skuId={}, qty={}",
                        item.getOrderId(), item.getSkuId(), item.getQuantity());
                // 不抛异常，由对账任务兜底
            }

            // 变更日志（before_qty/after_qty 表字段 NOT NULL，必须赋值）
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getSkuId, item.getSkuId()));
            int before = stock != null ? stock.getAvailableStock() : 0;
            StockLog logBo = new StockLog();
            logBo.setSkuId(item.getSkuId());
            logBo.setOrderId(item.getOrderId());
            logBo.setType("release");
            logBo.setQuantity(item.getQuantity());
            logBo.setBeforeQty(before);
            logBo.setAfterQty(before + item.getQuantity());
            logBo.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(logBo);

            // 更新 DB 预扣流水状态 → 已回滚
            StockPreLock preLock = stockPreLockMapper.selectOne(
                    new LambdaQueryWrapper<StockPreLock>()
                            .eq(StockPreLock::getOrderId, item.getOrderId())
                            .eq(StockPreLock::getSkuId, item.getSkuId()));
            if (preLock != null) {
                preLock.setStatus(2); // 已回滚
                stockPreLockMapper.updateById(preLock);
            }

            log.info("库存释放成功: orderId={}, skuId={}, qty={}, luaResult={}",
                    item.getOrderId(), item.getSkuId(), item.getQuantity(), result);
        }
    }

    // 确认扣减：支付成功后，清除 Redis 锁定明细 + 持久化到 DB + 更新预扣状态 + 变更日志
    // 注意：stock key 在锁定时已扣减，此处仅清理 lock key + DB 落库
    // 使用乐观锁（version 字段）防止并发扣减时的数据覆盖
    @Override
    @Transactional
    public void deductStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            String lockKey = RedisKeyConstants.STOCK_LOCK_PREFIX + item.getOrderId() + ":" + item.getSkuId();
            redisTemplate.execute(deductScript,
                    Collections.singletonList(lockKey),
                    item.getQuantity());

            // 使用乐观锁更新 DB：MyBatis-Plus 的 updateById 会检查 version 字段
            // 若 version 不匹配（被其他事务修改），更新行数为 0，此处循环重试最多 3 次
            int beforeQty = 0;
            int afterQty = 0;
            boolean updatedOk = false;
            int retry = 0;
            while (retry < 3) {
                Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getSkuId, item.getSkuId()));
                if (stock != null) {
                    // 变更前可用库存（变更前快照）
                    beforeQty = stock.getAvailableStock();
                    stock.setTotalStock(stock.getTotalStock() - item.getQuantity());
                    stock.setLockedStock(Math.max(0, stock.getLockedStock() - item.getQuantity()));
                    stock.setAvailableStock(stock.getTotalStock() - stock.getLockedStock());
                    // 变更后可用库存（已重新计算 availableStock）
                    afterQty = stock.getAvailableStock();
                    int updated = stockMapper.updateById(stock);
                    if (updated > 0) {
                        updatedOk = true;
                        break; // 乐观锁更新成功
                    }
                } else {
                    break; // stock 不存在，跳过
                }
                retry++;
                log.warn("库存扣减乐观锁冲突，重试 {}/3: skuId={}, orderId={}", retry, item.getSkuId(), item.getOrderId());
            }
            if (!updatedOk) {
                throw new RuntimeException("库存扣减失败, orderId=" + item.getOrderId());
            }

            // 变更日志（before_qty/after_qty 表字段 NOT NULL，必须赋值）
            StockLog stockLog = new StockLog();
            stockLog.setSkuId(item.getSkuId());
            stockLog.setOrderId(item.getOrderId());
            stockLog.setType("deduct");
            stockLog.setQuantity(item.getQuantity());
            stockLog.setBeforeQty(beforeQty);
            stockLog.setAfterQty(afterQty);
            stockLog.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(stockLog);

            // 更新 DB 预扣流水状态 → 已实扣
            StockPreLock preLock = stockPreLockMapper.selectOne(
                    new LambdaQueryWrapper<StockPreLock>()
                            .eq(StockPreLock::getOrderId, item.getOrderId())
                            .eq(StockPreLock::getSkuId, item.getSkuId()));
            if (preLock != null) {
                preLock.setStatus(1); // 已实扣
                stockPreLockMapper.updateById(preLock);
            }

            log.info("库存实扣成功: orderId={}, skuId={}, qty={}, before={}, after={}",
                    item.getOrderId(), item.getSkuId(), item.getQuantity(), beforeQty, afterQty);
        }
    }

    /**
     * 退款成功后恢复库存：DB 乐观锁增加 total_stock + available_stock + 同步 Redis + 变更日志
     */
    @Override
    @Transactional
    public void increaseStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            int beforeQty = 0;
            int afterQty = 0;
            boolean updatedOk = false;
            int retry = 0;
            while (retry < 3) {
                Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getSkuId, item.getSkuId()));
                if (stock != null) {
                    beforeQty = stock.getAvailableStock();
                    stock.setTotalStock(stock.getTotalStock() + item.getQuantity());
                    stock.setAvailableStock(stock.getAvailableStock() + item.getQuantity());
                    afterQty = stock.getAvailableStock();
                    int updated = stockMapper.updateById(stock);
                    if (updated > 0) {
                        updatedOk = true;
                        break;
                    }
                }
                retry++;
                log.warn("库存恢复乐观锁冲突，重试 {}/3: skuId={}, orderId={}", retry, item.getSkuId(), item.getOrderId());
            }
            if (!updatedOk) {
                throw new RuntimeException("库存恢复失败, orderId=" + item.getOrderId());
            }

            // 变更日志
            StockLog stockLog = new StockLog();
            stockLog.setSkuId(item.getSkuId());
            stockLog.setOrderId(item.getOrderId());
            stockLog.setType("increase");
            stockLog.setQuantity(item.getQuantity());
            stockLog.setBeforeQty(beforeQty);
            stockLog.setAfterQty(afterQty);
            stockLog.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(stockLog);

            // 同步 Redis 缓存
            syncStockToRedis(item.getSkuId());

            log.info("库存恢复成功: orderId={}, skuId={}, qty={}, before={}, after={}",
                    item.getOrderId(), item.getSkuId(), item.getQuantity(), beforeQty, afterQty);
        }
    }

    @Override
    public void syncStockToRedis(Long skuId) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getSkuId, skuId));
        String key = RedisKeyConstants.STOCK_PREFIX + skuId;
        if (stock != null) {
            redisTemplate.opsForValue().set(key, stock.getAvailableStock());
        } else {
            redisTemplate.opsForValue().set(key, 0);
        }
    }

    @Override
    public List<Long> getLowStockSkus() {
        return stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                        .apply("available_stock < safety_stock"))
                .stream()
                .map(Stock::getSkuId)
                .collect(Collectors.toList());
    }
}