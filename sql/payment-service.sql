SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_payment DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_payment;

CREATE TABLE t_payment (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '支付ID',
    payment_no VARCHAR(32) NOT NULL COMMENT '支付单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道: alipay/wechat',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态: 0待支付 1已支付 2退款中 3已退款 4已关闭',
    paid_time DATETIME DEFAULT NULL COMMENT '支付时间',
    transaction_no VARCHAR(100) DEFAULT NULL COMMENT '第三方交易流水号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

CREATE TABLE t_refund (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '退款ID',
    refund_no VARCHAR(32) NOT NULL COMMENT '退款单号',
    payment_id BIGINT NOT NULL COMMENT '支付ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    reason VARCHAR(500) DEFAULT NULL COMMENT '退款原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态: 0处理中 1已退款 2已拒绝',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_payment_id (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

CREATE TABLE t_payment_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    payment_id BIGINT NOT NULL COMMENT '支付ID',
    type VARCHAR(30) NOT NULL COMMENT '请求类型: CREATE/CALLBACK/REFUND/QUERY',
    request TEXT DEFAULT NULL COMMENT '请求内容',
    response TEXT DEFAULT NULL COMMENT '响应内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payment_id (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付日志表';
