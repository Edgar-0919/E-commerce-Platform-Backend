# 小羊电商 — 后端服务

基于 Spring Cloud Alibaba 微服务架构的电商平台后端。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| JDK | OpenJDK | 17 |
| 框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 3.2.4 / 2023.0.1 / 2023.0.1.0 |
| 注册配置 | Nacos | 2.3.2 |
| 网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus (mybatis-plus-spring-boot3-starter) | 3.5.9 |
| 缓存 | Redis + Redisson | 3.30.0 |
| 消息队列 | RocketMQ (rocketmq-spring-boot-starter) | 2.3.3 |
| 搜索引擎 | Elasticsearch | 8.13.x |
| 认证 | Spring Security + JWT (jjwt) | 0.12.6 |
| 服务调用 | OpenFeign + Sentinel | （由 SCA BOM 管理） |
| 分布式事务 | Seata | 2.0.0 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |

## 项目结构

```
backend/
├── pom.xml                         # 父 POM，统一版本管理
├── common/                         # 公共模块
│   ├── common-core/                # Result、枚举、异常、工具类
│   ├── common-security/            # JWT、Security 配置
│   ├── common-web/                 # 全局异常处理、统一响应、Knife4j
│   ├── common-mybatis/             # BaseEntity、MP 配置
│   ├── common-cache/               # Redis/Redisson
│   ├── common-mq/                  # RocketMQ 生产者抽象
│   └── common-feign/               # Feign 拦截器（传递用户上下文）
├── gateway/                        # API 网关（JWT 解析、路由转发、CORS）
├── services/                       # 微服务（8 个）
│   ├── user-service/               # 用户中心
│   ├── product-service/            # 商品服务
│   ├── order-service/              # 订单服务
│   ├── payment-service/            # 支付服务
│   ├── inventory-service/          # 库存服务
│   ├── cart-service/               # 购物车服务
│   ├── search-service/             # 搜索服务
│   └── marketing-service/          # 营销服务
├── sql/                            # 各服务建表 SQL
└── docker/                         # Docker Compose 基础设施
```

## 微服务架构

```
                                   ┌─────────────────┐
                                   │  Nacos 注册中心   │
                                   └────────┬────────┘
                                            │
   前端 / 外部请求 ──────▶  ┌──────┴──────┐       ┌──────────────────┐
                          │   Gateway   │──────▶│ user-service     │
                          │   :8080     │──┐    │ product-service  │
                          └─────────────┘  │    │ order-service    │
                                           ├───▶│ payment-service  │
                                           │    │ inventory-service│
                                           │    │ cart-service     │
                                           │    │ search-service   │
                                           │    │ marketing-service│
                                           │    └──────────────────┘
                                           │
                                           ├── Redis（库存扣减、缓存）
                                           ├── RocketMQ（异步消息）
                                           ├── Elasticsearch（商品搜索）
                                           └── MySQL × 8（每服务独立数据库）
```

## 微服务内部包结构

每个服务遵循 DDD-lite 分层：

```
{service}/src/main/java/com/ecommerce/{service}/
├── controller/      # REST 接口
├── service/         # 业务接口
│   └── impl/        # 业务实现
├── mapper/          # MyBatis-Plus Mapper
├── model/
│   ├── entity/      # 数据库实体
│   ├── dto/         # 数据传输对象
│   ├── vo/          # 视图对象
│   └── converter/   # MapStruct 转换器
├── config/          # 服务配置
└── feign/           # Feign 客户端
```

## 功能模块

| 模块 | 功能 |
|------|------|
| Gateway | JWT 解析与认证、路由转发、CORS、限流 |
| 用户中心 | 注册登录、个人信息管理、收货地址 CRUD |
| 商品服务 | 商品列表/详情、分类树、品牌管理、SKU/规格管理、商品上下架 |
| 订单服务 | 下单（含库存锁定）、订单列表/详情、取消订单、订单日志 |
| 支付服务 | 发起支付、支付回调、退款、支付日志（幂等） |
| 库存服务 | Redis Lua 原子锁库存/释放/扣减、库存预警、乐观锁持久化 |
| 购物车 | 添加/修改/删除、同 SKU 合并数量 |
| 搜索服务 | 商品全文搜索（ES）、搜索建议、筛选条件 |
| 营销服务 | 优惠券（领取/使用）、积分（增减/流水）、促销活动 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. 启动基础设施

```bash
cd docker
docker compose up -d
```

启动 MySQL（3306）、Redis（6379）、Nacos（8848）、Elasticsearch（9200）、RocketMQ（9876）。

SQL 建表脚本在容器启动时自动执行（`sql/` 挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d`）。

### 2. 编译 & 启动

```bash
# 编译全部模块
mvn clean install -DskipTests

# 启动 Gateway
cd gateway
mvn spring-boot:run

# 按需启动微服务
cd ../services/user-service
mvn spring-boot:run
```

### 3. API 文档

各服务启动后访问 `http://localhost:{port}/doc.html`（Knife4j）。

### 服务端口

| 服务 | 端口 |
|------|------|
| Gateway | 8080 |
| Nacos | 8848 |
| user-service | 8101 |
| product-service | 8102 |
| order-service | 8103 |
| payment-service | 8104 |
| inventory-service | 8105 |
| cart-service | 8106 |
| search-service | 8107 |
| marketing-service | 8108 |

## 架构约定

### 认证流程

Gateway 解析 JWT → 将 userId/username/roles 写入请求头 `X-User-Id`、`X-Username`、`X-User-Roles` → 下游服务从请求头读取（不再重复解析 JWT）→ 微服务内通过 `UserContext.get()` 获取当前用户

### 统一响应格式

所有 API 返回 `Result<T>`：`{ code, message, data, timestamp }`
- 成功：code=200
- 业务异常：code 按模块分段（用户 1xxxx、商品 2xxxx、订单 3xxxx、支付 4xxxx、库存 5xxxx）

### 服务间通信

- **同步调用**：OpenFeign（请求头通过 FeignHeaderInterceptor 自动传递）
- **异步消息**：RocketMQ
  - `payment-success` → inventory-service, order-service
  - `order-created` → cart-service
  - `product-status-change` → search-service
  - `refund-success` → order-service

### 分布式事务

- **强一致场景**（订单创建+库存锁定+优惠券核销）：Seata AT 模式
- **最终一致场景**（支付回调、商品同步 ES）：消息队列重试 + 幂等

### 数据库

- 每服务独立数据库（database-per-service）
- 雪花算法生成分布式 ID（MyBatis-Plus `IdType.ASSIGN_ID`）
- 核心表逻辑删除（deleted 字段）
- 库存表使用乐观锁（version 字段）

### 库存扣减

1. Redis Lua 脚本原子锁库存（防超卖）
2. 支付成功后异步持久化到 DB
3. 订单超时未支付 → 释放 Redis 锁定库存

## 易错提醒

1. MyBatis-Plus 依赖必须用 `mybatis-plus-spring-boot3-starter`（不是 `mybatis-plus-boot-starter`），否则启动报 `factoryBeanObjectType` 错误
2. RocketMQ 版本 ≥ 2.3.2 才支持 JDK 17，2.2.x 会报 `NoClassDefFoundError: MessageModel`
3. Sentinel 和 Seata 版本由 SCA BOM 统一管理，模块 POM 中不要单独声明版本号
4. JDK 版本统一使用 17
5. Maven 编译插件需要配置 `lombok-mapstruct-binding` 使 Lombok 和 MapStruct 共存
