SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_inventory DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_inventory;

CREATE TABLE t_stock (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '库存ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '已锁定库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    safety_stock INT NOT NULL DEFAULT 0 COMMENT '安全库存阈值',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

CREATE TABLE t_stock_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    type VARCHAR(20) NOT NULL COMMENT '操作类型: lock/release/deduct/increase',
    quantity INT NOT NULL COMMENT '变更数量',
    before_qty INT NOT NULL COMMENT '变更前可用库存',
    after_qty INT NOT NULL COMMENT '变更后可用库存',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sku_id (sku_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变更日志表';

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
