SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_marketing DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_marketing;

CREATE TABLE t_coupon_template (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '模板ID',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type TINYINT NOT NULL COMMENT '类型: 1满减券 2折扣券 3直减券',
    threshold DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
    amount DECIMAL(12,2) NOT NULL COMMENT '优惠金额/折扣率',
    total_count INT NOT NULL COMMENT '发放总量',
    issued_count INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    start_time DATETIME NOT NULL COMMENT '可用开始时间',
    end_time DATETIME NOT NULL COMMENT '可用结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

CREATE TABLE t_user_coupon (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '用户优惠券ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未使用 1已使用 2已过期',
    order_id BIGINT DEFAULT NULL COMMENT '使用的订单ID',
    used_time DATETIME DEFAULT NULL COMMENT '使用时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

CREATE TABLE t_banner (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '轮播图ID',
    image VARCHAR(500) NOT NULL COMMENT '图片URL',
    title VARCHAR(100) DEFAULT NULL COMMENT '标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    position VARCHAR(50) NOT NULL DEFAULT 'home' COMMENT '位置标识: home-首页轮播',
    link_url VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序(升序)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间(为空则立即生效)',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间(为空则永久有效)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_position_status (position, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';


