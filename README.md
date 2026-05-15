# 小羊电商 — E-Commerce Platform

前后端分离的电商平台，基于 Spring Cloud Alibaba 微服务架构。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| JDK | OpenJDK | 17 |
| 框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 3.2.4 / 2023.0.1 / 2023.0.1.0 |
| 注册配置 | Nacos | 2.3.2 |
| 网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus | 3.5.9 |
| 缓存 | Redis + Redisson | 7.2 / 3.30.0 |
| 消息队列 | RocketMQ | 5.2.0 |
| 搜索引擎 | Elasticsearch | 8.13.x |
| 认证 | Spring Security + JWT (jjwt) | 0.12.6 |
| 服务调用 | OpenFeign + Sentinel | — |
| 分布式事务 | Seata | 2.0.0 |
| API 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia | — |

## 项目结构

```
ecommerce-platform/
├── backend/
│   ├── common/                    # 公共模块
│   │   ├── common-core/           # Result、枚举、异常、工具类
│   │   ├── common-security/       # JWT、Security 配置
│   │   ├── common-web/            # 全局异常处理、统一响应、Knife4j
│   │   ├── common-mybatis/        # BaseEntity、MP 配置
│   │   ├── common-cache/          # Redis/Redisson
│   │   ├── common-mq/             # RocketMQ 生产者抽象
│   │   └── common-feign/          # Feign 拦截器（传递用户上下文）
│   ├── gateway/                   # API 网关（JWT 解析、路由转发、CORS）
│   ├── services/                  # 微服务（8 个）
│   │   ├── user-service/          # 用户中心
│   │   ├── product-service/       # 商品服务
│   │   ├── order-service/         # 订单服务
│   │   ├── payment-service/       # 支付服务
│   │   ├── inventory-service/     # 库存服务
│   │   ├── cart-service/          # 购物车服务
│   │   ├── search-service/        # 搜索服务
│   │   └── marketing-service/     # 营销服务
│   ├── sql/                       # 各服务建表 SQL
│   └── docker/                    # Docker Compose 基础设施
└── frontend/                      # Vue 3 前端
    └── src/
        ├── api/                   # Axios 接口封装
        ├── router/                # 路由配置
        ├── stores/                # Pinia 状态管理
        ├── views/                 # 页面组件
        ├── components/            # 通用组件
        └── styles/                # 样式系统（浅色/深色主题）
```

## 微服务架构

```
                    ┌─────────────────┐
                    │   Nacos 注册中心   │
                    └────────┬────────┘
                             │
  ┌──────────┐       ┌──────┴──────┐       ┌──────────┐
  │ 前端 SPA  │──────▶│   Gateway   │──────▶│ user-service    │
  │ :5173    │       │   :8080     │──┐    │ product-service │
  └──────────┘       └─────────────┘  │    │ order-service   │
                                      ├───▶│ payment-service │
                                      │    │ inventory-svc   │
                                      │    │ cart-service    │
                                      │    │ search-service  │
                                      │    │ marketing-svc   │
                                      │    └─────────────────┘
                                      │
                                      ├── Redis（库存扣减、缓存）
                                      ├── RocketMQ（异步消息）
                                      ├── Elasticsearch（商品搜索）
                                      └── MySQL × 7（每服务独立数据库）
```

## 功能模块

| 模块 | 功能 |
|------|------|
| 用户中心 | 注册登录（JWT）、个人信息管理、收货地址 CRUD |
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
- Node.js 18+
- Docker & Docker Compose

### 1. 启动基础设施

```bash
cd backend/docker
docker compose up -d
```

启动 MySQL（3306）、Redis（6379）、Nacos（8848）、Elasticsearch（9200）、RocketMQ（9876）。

SQL 建表脚本在容器启动时自动执行（`backend/sql/` 挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d`）。

### 2. 启动后端

```bash
cd backend
mvn clean install -DskipTests

# 启动 Gateway
cd gateway
mvn spring-boot:run

# 按需启动微服务（各服务端口详见下表）
cd ../services/user-service
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173，API 请求自动代理到 Gateway（8080）。

### 4. API 文档

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

- **认证流程**：Gateway 解析 JWT → 将 userId/username/roles 写入请求头 → 下游服务通过 `UserContext.get()` 读取
- **统一响应**：所有 API 返回 `Result<T>`（code/message/data/timestamp）
- **错误码分段**：用户 1xxxx、商品 2xxxx、订单 3xxxx、支付 4xxxx、库存 5xxxx
- **分布式 ID**：雪花算法（MyBatis-Plus `IdType.ASSIGN_ID`）
- **库存扣减**：Redis Lua 脚本原子操作 → 支付成功后异步持久化 DB → 超时未支付释放锁定
- **分布式事务**：强一致用 Seata AT 模式，最终一致用 RocketMQ + 重试 + 幂等
- **服务间通信**：同步用 OpenFeign（自动传递用户上下文），异步用 RocketMQ
- **数据库隔离**：每服务独立数据库（database-per-service）
