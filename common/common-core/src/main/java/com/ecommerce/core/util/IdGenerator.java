package com.ecommerce.core.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

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

    // 单节点部署：workerId=1, datacenterId=1。多节点部署时需改为动态分配
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }

    public static String orderNo() {
        return SNOWFLAKE.nextIdStr();
    }

    public static String paymentNo() {
        return SNOWFLAKE.nextIdStr();
    }

    public static String refundNo() {
        return "RF" + SNOWFLAKE.nextIdStr();
    }
}
