SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_product DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_product;

CREATE TABLE t_category (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID, 0为顶级',
    level TINYINT NOT NULL DEFAULT 1 COMMENT '层级',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) DEFAULT NULL COMMENT '图标',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE t_product (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID(默认1=平台自营)',
    main_image VARCHAR(255) DEFAULT NULL COMMENT '主图',
    images TEXT DEFAULT NULL COMMENT '商品图片集(JSON数组)',
    description TEXT DEFAULT NULL COMMENT '商品描述(富文本)',
    unit VARCHAR(20) DEFAULT NULL COMMENT '计量单位',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1上架 0下架',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '累计销量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_category_id (category_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_sales_count (sales_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE t_spec_group (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '规格组ID',
    name VARCHAR(50) NOT NULL COMMENT '规格组名称(如:颜色、尺寸)',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格组表';

CREATE TABLE t_spec_param (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '规格参数ID',
    group_id BIGINT NOT NULL COMMENT '规格组ID',
    name VARCHAR(50) NOT NULL COMMENT '参数名',
    `values` TEXT NOT NULL COMMENT '可选值(JSON数组)',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    KEY idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格参数表';

CREATE TABLE t_sku (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_code VARCHAR(100) NOT NULL COMMENT 'SKU编码',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    spec_values JSON NOT NULL COMMENT '规格值如:{"颜色":"红色","尺寸":"XL"}',
    price DECIMAL(12,2) NOT NULL COMMENT '售价',
    market_price DECIMAL(12,2) DEFAULT NULL COMMENT '市场价',
    weight DECIMAL(10,2) DEFAULT NULL COMMENT '重量(g)',
    image VARCHAR(255) DEFAULT NULL COMMENT 'SKU图片',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU表';

-- ============================================================
-- 以下表来自原 inventory-service（库存管理），合并到 db_product
-- ============================================================

CREATE TABLE t_stock (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '库存ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '已锁定库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    safety_stock INT NOT NULL DEFAULT 0 COMMENT '安全库存阈值',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sku_id (sku_id),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

CREATE TABLE t_stock_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    type VARCHAR(20) NOT NULL COMMENT '操作类型: lock/release/deduct/increase',
    quantity INT NOT NULL COMMENT '变更数量',
    before_qty INT NOT NULL COMMENT '变更前可用库存',
    after_qty INT NOT NULL COMMENT '变更后可用库存',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sku_id (sku_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变更日志表';

-- 库存预扣流水表（本地消息表）：记录预扣→实扣→回滚全生命周期
-- 预扣时写入（status=0），实扣时更新为1，回滚时更新为2
-- 定时对账任务扫描超时仍为status=0的记录，执行补偿回滚
CREATE TABLE t_stock_pre_lock (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    quantity INT NOT NULL COMMENT '预扣数量',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=预扣中, 1=已实扣, 2=已回滚',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '对账重试次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id),
    KEY idx_status_create (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预扣流水表';
