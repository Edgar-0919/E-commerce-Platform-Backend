# 小羊电商 — 后端服务

基于 Spring Cloud Alibaba 微服务架构的电商平台后端。
前端仓库：[frontend](https://github.com/Edgar-0919/E-Commerce-Platform-Frontend)

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| JDK | OpenJDK | 17 |
| 框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 3.2.4 / 2023.0.1 / 2023.0.1.0 |
| 注册配置 | Nacos | 2.3.2 |
| 网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus (mybatis-plus-spring-boot3-starter) | 3.5.9 |
| 缓存 | Redis + Redisson | 3.30.0 |
| 消息队列 | Spring Cloud Stream + RabbitMQ | 4.1.0 |
| 搜索引擎 | Elasticsearch | 8.13.x |
| 认证 | Spring Security + JWT (jjwt) | 0.12.6 |
| 服务调用 | OpenFeign + Sentinel | （由 SCA BOM 管理） |
| 服务容错 | Sentinel — 流控、熔断降级、热点参数限流、系统自适应 | （由 SCA BOM 管理） |
| 消息驱动 | Spring Cloud Stream（RabbitMQ Binder）— 函数式编程模型 | 4.1.0 |
| 分布式链路追踪 | Micrometer Tracing + Brave + Zipkin | （由 Spring Boot 3.2.4 管理） |
| 数据一致性 | 消息队列 + 幂等 + 对账兜底（最终一致） | - |
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
├── services/                       # 微服务（4 个）
│   ├── user-service/               # 用户中心（含商户申请、商户管理）
│   ├── product-service/            # 商品服务（含库存、搜索）
│   ├── order-service/              # 订单服务（含支付、购物车）
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
                                           ├───▶│ marketing-service│
                                           │    └──────────────────┘
                                           │
                                           ├── Redis（库存扣减、缓存）
                                           ├── RabbitMQ（异步消息 / Spring Cloud Stream）
                                           └── MySQL × 4（每服务独立数据库）
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
| Gateway | JWT 解析与认证、路由转发、CORS、商户数据隔离 |
| 用户中心 | 注册登录（默认ROLE_USER）、个人信息管理、收货地址 CRUD、商户入驻申请、商户管理 |
| 商品服务 | 商品列表/详情、分类树、品牌管理、SKU/规格管理、商品上下架、库存扣减 |
| 订单服务 | 下单（含库存锁定 + 优惠券核销）、订单列表/详情、取消订单、支付、购物车管理 |
| 营销服务 | 优惠券（领取/使用）、轮播图管理 |

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

启动 MySQL（3306）、Redis（6379）、Nacos（8848）、RabbitMQ（5672/15672）。

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
| marketing-service | 8108 |

## 架构约定

### 认证流程

Gateway 解析 JWT → 将 userId/username/roles/merchantId 写入请求头 `X-User-Id`、`X-Username`、`X-User-Roles`、`X-Merchant-Id` → 下游服务从请求头读取（不再重复解析 JWT）→ 微服务内通过 `UserContext.get()` 获取当前用户

### 商户数据隔离

- 使用 MyBatis-Plus `TenantLineInnerInterceptor` 实现商户数据隔离，自动在查询中注入 `merchant_id` 条件
- 策略：**指定隔离表**模式，仅对 `t_product`、`t_sku`、`t_stock`、`t_stock_log`、`t_order`、`t_order_item` 表应用商户隔离
- `ROLE_ADMIN` 用户传入 `X-Merchant-Id: ALL` 跳过租户过滤，可查看所有商户数据
- C端用户（`merchantId == null`）时，所有表都不隔离，允许用户浏览全部商品
- `t_merchant_application` 表跳过租户过滤，管理员可查看所有入驻申请

### 商户入驻流程

1. C端用户在个人中心点击"成为商家"，填写商户信息并提交申请
2. 后端校验申请信息，检测是否已有待审核/已通过申请，防止重复提交
3. B端管理员在商户管理页面查看入驻申请列表
4. 管理员审核通过：自动创建商户记录、分配ROLE_MERCHANT角色、关联用户与商户
5. 管理员审核拒绝：记录拒绝原因，用户可重新申请
6. 用户信息API返回商户申请状态，用户中心显示状态徽章

### 统一响应格式

所有 API 返回 `Result<T>`：`{ code, message, data, timestamp }`
- 成功：code=200
- 业务异常：code 按模块分段（用户 1xxxx、商品 2xxxx、订单 3xxxx、支付 4xxxx、库存 5xxxx）

### 服务间通信

- **同步调用**：OpenFeign（请求头通过 FeignHeaderInterceptor 自动传递）
- 异步消息：Spring Cloud Stream + RabbitMQ
  - `payment-success` → product-service（库存扣减）
  - `order-created` → order-service 内部（购物车清除）
  - `refund-success` → order-service（订单状态更新）
  - `coupon-use` → marketing-service（优惠券核销）

### 数据一致性

全部采用最终一致方案：
- 订单创建：本地事务 + Feign 同步调用，失败触发本地回滚
- 跨服务状态对齐：消息队列重试 + 幂等 + 定时对账兜底

### 数据库

- 每服务独立数据库（database-per-service）
- 雪花算法生成分布式 ID（MyBatis-Plus `IdType.ASSIGN_ID`）
- 核心表逻辑删除（deleted 字段）
- 库存表使用乐观锁（version 字段）

### 库存扣减

1. Redis Lua 脚本原子锁库存（防超卖）
2. 支付成功后异步持久化到 DB
3. 订单超时未支付 → 释放 Redis 锁定库存

### Sentinel 服务容错

**双层级拦截**：Gateway 限流（WebFlux）+ 微服务 Controller @SentinelResource（Spring MVC）+ Feign fallbackFactory

核心接口（创建订单、商品详情、搜索、库存锁定、支付、登录、领券）均已配置限流、熔断降级和热点参数保护。

规则通过 Nacos 持久化，Sentinel Dashboard 访问 `http://localhost:8730`（用户名/密码: sentinel/sentinel）。

### Spring Cloud Stream 消息驱动

函数式编程模型（`java.util.function.Consumer`），5 条消息链路全量覆盖电商异步场景。

死信队列全局配置，消费失败自动重试。

### 分布式链路追踪

Micrometer Tracing + Brave + Zipkin（Spring Boot 3.x 已移除 Sleuth）。

Gateway + 所有微服务自动生成 traceId/spanId，日志格式：`[service-name,traceId,spanId]`。

Zipkin UI 访问 `http://localhost:9411` 查看调用链路拓扑和耗时分析。
