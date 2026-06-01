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
    start_time DATETIME NOT NULL COMMENT '可用开始时间',
    end_time DATETIME NOT NULL COMMENT '可用结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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

CREATE TABLE t_promotion (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '促销ID',
    name VARCHAR(100) NOT NULL COMMENT '促销名称',
    type VARCHAR(30) NOT NULL COMMENT '促销类型: flash_sale/group_buy/full_reduction',
    product_id BIGINT DEFAULT NULL COMMENT '关联商品ID',
    sku_id BIGINT DEFAULT NULL COMMENT '关联SKU ID',
    rules JSON NOT NULL COMMENT '促销规则(JSON)',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未开始 1进行中 2已结束',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_type (type),
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='促销活动表';

CREATE TABLE t_user_points (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '积分账户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_points INT NOT NULL DEFAULT 0 COMMENT '总积分',
    frozen_points INT NOT NULL DEFAULT 0 COMMENT '冻结积分',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分表';

CREATE TABLE t_points_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    change_type VARCHAR(30) NOT NULL COMMENT '变动类型: earn/consume/freeze/unfreeze',
    points INT NOT NULL COMMENT '变动积分数(正为增加,负为减少)',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

CREATE TABLE IF NOT EXISTS `undo_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `branch_id` BIGINT(20) NOT NULL,
    `xid` VARCHAR(100) NOT NULL,
    `context` VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB NOT NULL,
    `log_status` INT(11) NOT NULL,
    `log_created` DATETIME NOT NULL,
    `log_modified` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
