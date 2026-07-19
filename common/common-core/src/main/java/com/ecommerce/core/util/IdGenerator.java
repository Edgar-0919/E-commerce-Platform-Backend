package com.ecommerce.core.util;

/**
 * ID生成器工具类
 * 基于雪花算法(Snowflake)生成分布式唯一ID
 * 结构：64位二进制 = 1位符号位 + 41位时间戳 + 10位机器标识 + 12位序列号
 * 特性：
 * - 单机部署：workerId=1, datacenterId=1
 * - 多节点部署时需动态分配workerId和datacenterId
 * - 支持生成订单号、支付号、退款号等业务ID
 */
public class IdGenerator {

    // 起始时间戳 (2025-01-01 00:00:00)
    private static final long EPOCH = 1700000000000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    private static final long workerId = 1;
    private static final long dataCenterId = 1;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards, refusing to generate id");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    public static String nextIdStr() {
        return String.valueOf(nextId());
    }

    public static String orderNo() {
        return nextIdStr();
    }

    public static String paymentNo() {
        return nextIdStr();
    }

    public static String refundNo() {
        return "RF" + nextIdStr();
    }
}