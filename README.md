# 智能图书馆借阅与座位预约系统

## 项目简介

智能图书馆借阅与座位预约系统是一个面向 Java 后端实习展示的中小型 Spring Boot 项目，聚焦图书借阅状态流转和座位预约并发控制，展示认证会话、缓存、分布式锁、操作日志、定时任务和集成测试等后端工程能力。

## 技术栈

- Spring Boot 2.7
- MyBatis-Plus
- MySQL 8
- Redis
- Redisson
- JWT
- AOP
- Bean Validation
- Testcontainers
- Knife4j / Swagger
- Aliyun OSS 基础上传

## 核心业务流程

### 登录认证流程

1. 用户登录成功后生成 JWT，JWT 携带 `userId`、`jti`、`tokenVersion`。
2. Redis 保存服务端 Session，接口请求通过 `Authorization: Bearer {token}` 访问。
3. 拦截器调用 `SessionManager.authenticate` 校验 JWT、Redis Session、黑名单、用户状态和 `tokenVersion`。
4. 认证通过后写入 `UserContext`，请求结束后清理 ThreadLocal。
5. 登出时删除 Redis Session 并将 `jti` 加入黑名单；Session 临近过期时执行滑动续期。

### 借阅流程

```text
申请借阅 -> 审核通过/驳回 -> 确认出借 -> 用户续借 -> 确认归还 -> 逾期扫描
```

设计重点：

- 申请阶段只创建 `APPLYING` 订单，不扣库存。
- 出借阶段通过状态条件更新流转为 `LENT`，并扣减可借库存。
- 归还阶段从 `LENT` / `OVERDUE` 流转为 `RETURNED`，并恢复库存。
- 续借只允许本人、`LENT` 状态、且未超过续借次数的订单。
- 关键状态流转使用 Guard / Validator 和条件更新防止重复操作。

### 预约流程

```text
创建预约 -> 签到 -> 结束使用
       -> 取消
       -> 过期处理
```

设计重点：

- 创建预约前校验座位、自习室、时间范围。
- 使用 Redisson 对 `seatId` 和 `userId` 维度加组合锁。
- 锁内校验同一座位时间冲突、同一用户时间冲突。
- 签到、取消、结束使用都通过状态校验和条件更新防止重复操作。

## 核心技术亮点

- JWT + Redis Session：兼顾 JWT 的轻量和服务端会话的可控性，支持登出失效、黑名单、滑动续期、`tokenVersion` 踢下线。
- Redis 缓存：热门图书、图书详情、分类、自习室列表等高频查询使用缓存，写操作后主动失效相关缓存。
- Redisson 分布式锁：借阅申请按 `userId + bookId` 防重复，座位预约按 `seatId + userId` 控制并发冲突。
- 状态流转：借阅和预约都用明确状态机表达业务闭环，面试时可以围绕“为什么申请不扣库存、为什么出借才扣库存”展开。
- AOP 操作日志：通过注解统一记录关键操作，并对 token、password、secret 等敏感字段脱敏。
- 定时任务：扫描逾期借阅、过期未签到预约、超时使用中的预约。
- Testcontainers：补充核心流程集成测试，Docker 正常环境下可启动 MySQL + Redis 验证真实链路。

## 项目结构说明

### 核心业务模块

- 认证：`AuthController`、`SessionManager`、`RedisSessionManager`、`LoginInterceptor`
- 图书查询：`BookController`、`BookService`
- 借阅流程：`BorrowController`、`BorrowManageController`、`BorrowOrderService`
- 座位预约：`ReservationController`、`ReservationService`

### 基础支撑模块

- 图书分类 CRUD
- 图书管理 CRUD
- 自习室 CRUD
- 座位 CRUD
- Dashboard overview
- OSS 基础文件上传

### 工程能力模块

- Redis 缓存
- Redisson 分布式锁
- AOP 操作日志
- 全局异常处理
- DTO 参数校验
- Spring Schedule 定时任务
- Testcontainers 集成测试
- Knife4j / Swagger 接口展示

## 本地启动方式

### 1. 启动 MySQL 和 Redis

```bash
docker compose up -d
```

默认开发配置：

- MySQL：`localhost:3306`
- 数据库：`smart_library`
- 用户名：`root`
- 密码：`1234`
- Redis：`localhost:6379`

### 2. 初始化数据库

```bash
mysql -uroot -p1234 smart_library < src/main/resources/schema.sql
mysql -uroot -p1234 smart_library < src/main/resources/data-test-init.sql
```

测试账号密码均为 `123456`：

- 管理员：`admin`
- 馆员：`librarian`
- 普通用户：`user01`

### 3. 启动项目

```bash
mvn spring-boot:run
```

访问地址：

- API 根路径：`http://localhost:18080/api`
- Knife4j 文档：`http://localhost:18080/api/doc.html`

## 测试方式

```bash
mvn -DskipTests compile
mvn test
```

说明：

- 已补充 `CoreBusinessFlowTest`，覆盖借阅状态流转、预约冲突与并发边界、认证失效边界。
- Testcontainers 集成测试依赖 Docker。当前本机 Docker 环境异常时会被 `disabledWithoutDocker = true` 跳过。
- Docker 正常环境下，应执行 `CoreBusinessFlowTest`，通过临时 MySQL + Redis 验证真实链路。
- 不应写成“所有容器集成测试已在本机完整跑通”，以实际环境结果为准。

## 面试讲解稿

### 2 分钟版本

这个项目是一个智能图书馆后端系统，我把主线收敛到两个真实业务闭环：图书借阅状态流转和座位预约并发控制。借阅流程包括申请、审核、出借、续借、归还和逾期扫描，重点是申请不扣库存、出借才扣库存，并通过状态校验和条件更新防止重复出借、重复归还。预约流程包括创建、签到、取消、结束和过期处理，创建时用 Redisson 按座位和用户维度加组合锁，在锁内做时间冲突校验，避免并发重复预约。

工程上，认证使用 JWT + Redis Session，JWT 携带 `userId`、`jti`、`tokenVersion`，Redis 控制在线会话，支持登出黑名单、滑动续期和踢下线。项目还使用 Redis 缓存热门图书和基础数据，AOP 记录操作日志，全局异常处理和 DTO 校验统一接口质量，并用 Testcontainers 补充核心流程集成测试。

### 5 分钟版本

这个项目不是大而全的后台管理系统，而是一个适合实习面试展示的中小型 Spring Boot 后端项目。核心业务有两条：图书借阅和座位预约。

借阅流程中，用户提交申请时系统只创建 `APPLYING` 订单，不立即扣库存，因为还需要馆员审核。审核通过后订单变为 `APPROVED`，馆员确认出借时才把订单流转为 `LENT`，同时扣减可借库存。用户续借时要校验本人、订单状态和续借次数。归还时从 `LENT` 或 `OVERDUE` 流转为 `RETURNED`，并恢复库存。这里的重点不是 CRUD，而是状态机、库存一致性和重复操作防护。

座位预约的主要风险是并发冲突。两个请求可能同时查询到某个座位没有冲突，然后同时插入预约记录。为了解决这个问题，创建预约时使用 Redisson 组合锁，同时锁住座位维度和用户维度，锁内再校验同一座位时间段冲突和同一用户时间段冲突。这样既能防同一座位被重复预约，也能防同一用户同一时间预约多个座位。

认证方面，项目没有使用纯 JWT，而是 JWT + Redis Session。JWT 只作为客户端凭证，服务端 Redis 保存登录态。拦截器统一调用 `SessionManager.authenticate` 校验 JWT、Redis Session、黑名单、用户状态和 `tokenVersion`。登出时删除 Session 并把 `jti` 加入黑名单，用户禁用或 `tokenVersion` 变化后旧 token 立即失效，Session TTL 低于阈值时自动续期。

测试方面，除了单元测试，还补充了 Testcontainers 集成测试，用临时 MySQL + Redis 验证核心链路。当前机器 Docker 异常时会跳过容器测试；Docker 正常环境下，核心流程测试会实际执行。

## 后续可优化方向

- 接入 Spring Security 统一认证授权链路。
- 引入 Refresh Token 优化登录续期体验。
- 使用 XXL-JOB 或 Quartz 管理定时任务。
- 将文件上传独立为文件服务。
- 为并发测试增加更完整的错误消息和监控断言。
