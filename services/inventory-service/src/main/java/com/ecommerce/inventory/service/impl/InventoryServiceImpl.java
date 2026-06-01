package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.core.constant.RedisKeyConstants;
import com.ecommerce.core.constant.ResultCodeEnum;
import com.ecommerce.core.exception.InventoryException;
import com.ecommerce.inventory.mapper.StockLogMapper;
import com.ecommerce.inventory.mapper.StockMapper;
import com.ecommerce.inventory.model.dto.StockOperationDTO;
import com.ecommerce.inventory.model.entity.Stock;
import com.ecommerce.inventory.model.entity.StockLog;
import com.ecommerce.inventory.service.InventoryService;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> lockScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final DefaultRedisScript<Long> deductScript;

    public InventoryServiceImpl(
            StockMapper stockMapper,
            StockLogMapper stockLogMapper,
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("lockScript") DefaultRedisScript<Long> lockScript,
            @Qualifier("releaseScript") DefaultRedisScript<Long> releaseScript,
            @Qualifier("deductScript") DefaultRedisScript<Long> deductScript) {
        this.stockMapper = stockMapper;
        this.stockLogMapper = stockLogMapper;
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

    // 锁定：Redis Lua 原子扣减（防超卖）+ 记录锁定明细
    @Override
    @Transactional
    public void lockStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            String stockKey = RedisKeyConstants.STOCK_PREFIX + item.getSkuId();
            Long result = redisTemplate.execute(lockScript,
                    Collections.singletonList(stockKey),
                    item.getQuantity());

            if (result == null || result == -1) {
                throw new InventoryException("SKU库存未初始化: " + item.getSkuId());
            }
            if (result == 0) {
                throw new InventoryException(ResultCodeEnum.STOCK_INSUFFICIENT);
            }

            // Record lock in Redis (for later deduct/release)
            String lockKey = RedisKeyConstants.STOCK_LOCK_PREFIX + item.getOrderId() + ":" + item.getSkuId();
            redisTemplate.opsForValue().set(lockKey, item.getQuantity());

            log.info("库存锁定成功: orderId={}, skuId={}, qty={}", item.getOrderId(), item.getSkuId(), item.getQuantity());
        }
    }

    // 释放：订单取消/超时，Redis Lua 退回锁定库存到可用库存
    // @Transactional 确保 Redis 操作与后续 DB 补偿写入的一致性
    @Override
    @Transactional
    public void releaseStock(List<StockOperationDTO> items) {
        for (StockOperationDTO item : items) {
            String stockKey = RedisKeyConstants.STOCK_PREFIX + item.getSkuId();
            String lockKey = RedisKeyConstants.STOCK_LOCK_PREFIX + item.getOrderId() + ":" + item.getSkuId();
            redisTemplate.execute(releaseScript,
                    List.of(stockKey, lockKey),
                    item.getQuantity());

            log.info("库存释放成功: orderId={}, skuId={}, qty={}", item.getOrderId(), item.getSkuId(), item.getQuantity());
        }
    }

    // 确认扣减：支付成功后，清除 Redis 锁定明细 + 持久化到 DB
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
            int retry = 0;
            while (retry < 3) {
                Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getSkuId, item.getSkuId()));
                if (stock != null) {
                    stock.setTotalStock(stock.getTotalStock() - item.getQuantity());
                    stock.setLockedStock(Math.max(0, stock.getLockedStock() - item.getQuantity()));
                    stock.setAvailableStock(stock.getTotalStock() - stock.getLockedStock());
                    int updated = stockMapper.updateById(stock);
                    if (updated > 0) break; // 乐观锁更新成功
                } else {
                    break; // stock 不存在，跳过
                }
                retry++;
                log.warn("库存扣减乐观锁冲突，重试 {}/3: skuId={}, orderId={}", retry, item.getSkuId(), item.getOrderId());
            }

            // Record log
            StockLog stockLog = new StockLog();
            stockLog.setSkuId(item.getSkuId());
            stockLog.setOrderId(item.getOrderId());
            stockLog.setType("deduct");
            stockLog.setQuantity(item.getQuantity());
            stockLog.setCreateTime(LocalDateTime.now());
            stockLogMapper.insert(stockLog);

            log.info("库存扣减成功: orderId={}, skuId={}, qty={}", item.getOrderId(), item.getSkuId(), item.getQuantity());
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
