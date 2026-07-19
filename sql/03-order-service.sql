SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_order DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_order;

CREATE TABLE t_order (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID(默认1=平台自营)',
    coupon_id BIGINT DEFAULT NULL COMMENT '优惠券ID',
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
    KEY idx_status (status),
    KEY idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE t_order_item (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
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

-- ============================================================
-- 以下表来自原 payment-service（支付管理），合并到 db_order
-- ============================================================

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
    refund_type TINYINT NOT NULL DEFAULT 1 COMMENT '退款类型: 1=仅退款, 2=退货退款',
    refund_reason VARCHAR(100) DEFAULT NULL COMMENT '退款原因分类: not_received/wrong_item/quality/dont_like/other',
    refund_images TEXT DEFAULT NULL COMMENT '退款凭证图片(JSON数组)',
    return_status TINYINT DEFAULT NULL COMMENT '退货物流状态: NULL=仅退款, 1=待退货, 2=已退货, 3=已收货',
    return_logistics_no VARCHAR(100) DEFAULT NULL COMMENT '退货物流单号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态: 0处理中 1已退款 2已拒绝',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_payment_id (payment_id),
    KEY idx_order_id (order_id)
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

-- ============================================================
-- 以下表来自原 cart-service（购物车），合并到 db_order
-- ============================================================

CREATE TABLE t_cart_item (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '购物车项ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    merchant_id BIGINT NOT NULL DEFAULT 1 COMMENT '商户ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    spec_desc VARCHAR(300) DEFAULT NULL COMMENT '规格描述',
    price DECIMAL(12,2) NOT NULL COMMENT '加入时单价',
    main_image VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中: 1选中 0未选中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    UNIQUE KEY uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';
