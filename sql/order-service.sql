CREATE DATABASE IF NOT EXISTS db_order DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_order;

CREATE TABLE t_order (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    pay_amount DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态: 0待支付 1待发货 2已发货 3已收货 4已取消 5退款中 6已退款',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收件人',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    address VARCHAR(500) NOT NULL COMMENT '收货地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    deliver_time DATETIME DEFAULT NULL COMMENT '发货时间',
    receive_time DATETIME DEFAULT NULL COMMENT '收货时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE t_order_item (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    spec_desc VARCHAR(300) DEFAULT NULL COMMENT '规格描述',
    price DECIMAL(12,2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    amount DECIMAL(12,2) NOT NULL COMMENT '小计',
    image VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

CREATE TABLE t_order_log (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '日志ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    from_status TINYINT DEFAULT NULL COMMENT '原状态',
    to_status TINYINT NOT NULL COMMENT '新状态',
    operator VARCHAR(50) DEFAULT NULL COMMENT '操作人',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志表';
