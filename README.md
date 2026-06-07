# 智能图书馆借阅与座位预约系统

## 项目简介

智能图书馆借阅与座位预约系统是一个面向图书馆业务场景的 Spring Boot 后端服务，覆盖用户认证、图书查询、借阅流转、座位预约、基础数据维护、操作日志、定时任务和文件上传等能力。

系统围绕两条核心业务链路构建：

- 图书借阅：申请、审核、出借、续借、归还、逾期处理。
- 座位预约：创建预约、冲突校验、签到、取消、结束使用、过期处理。

## 技术栈

- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Redis
- Redisson
- JWT
- AOP
- Bean Validation
- Testcontainers
- springdoc-openapi
- Aliyun OSS

## 核心业务流程

### 登录认证

1. 用户登录成功后生成 JWT，包含 `userId`、`jti`、`tokenVersion`。
2. Redis 保存服务端 Session，接口请求通过 `Authorization: Bearer {token}` 访问。
3. 登录拦截器调用 `SessionManager.authenticate` 校验 JWT、Redis Session、黑名单、用户状态和 `tokenVersion`。
4. 认证通过后写入 `UserContext`，请求结束后清理 ThreadLocal。
5. 退出登录时删除 Redis Session，并将当前 `jti` 写入黑名单。
6. Session 临近过期时执行滑动续期。

### 图书借阅

```text
申请借阅 -> 审核通过/驳回 -> 确认出借 -> 用户续借 -> 确认归还 -> 逾期扫描
```

关键规则：

- 申请阶段只创建 `APPLYING` 订单，不扣减库存。
- 出借阶段通过状态条件更新流转为 `LENT`，并扣减可借库存。
- 归还阶段从 `LENT` 或 `OVERDUE` 流转为 `RETURNED`，并恢复库存。
- 续借只允许订单本人对 `LENT` 状态且未超过续借次数的订单操作。
- 关键状态流转使用 Guard、Validator 和条件更新防止重复操作。

### 座位预约

```text
创建预约 -> 签到 -> 结束使用
       -> 取消
       -> 过期处理
```

关键规则：

- 创建预约前校验座位、自习室、时间范围和资源状态。
- 使用 Redisson 按 `seatId` 和 `userId` 维度加组合锁。
- 锁内校验同一座位时间冲突和同一用户时间冲突。
- 签到、取消、结束使用都通过状态校验和条件更新防止重复操作。

## 工程能力

- JWT + Redis Session：兼顾客户端令牌和服务端会话控制，支持退出失效、黑名单、滑动续期和 `tokenVersion` 踢下线。
- Redis 缓存：热门图书、图书详情、分类、自习室列表等高频读数据使用缓存，写操作后主动失效相关缓存。
- Redisson 分布式锁：借阅申请按 `userId + bookId` 防重复，座位预约按 `seatId + userId` 控制并发冲突。
- MyBatis-Plus 条件更新：关键状态流转通过状态条件约束，降低重复提交和并发写入风险。
- AOP 操作日志：通过注解统一记录关键操作，并对 `token`、`password`、`secret` 等敏感字段脱敏。
- 定时任务：扫描逾期借阅、过期未签到预约、超时使用中的预约。
- Testcontainers：使用临时 MySQL 和 Redis 验证核心链路。
- OSS 上传：提供头像、封面等图片资源上传能力。

## 项目结构

### 核心业务模块

- 认证：`AuthController`、`SessionManager`、`RedisSessionManager`、`LoginInterceptor`
- 图书查询：`BookController`、`BookService`
- 借阅流程：`BorrowController`、`BorrowManageController`、`BorrowOrderService`
- 座位预约：`ReservationController`、`ReservationService`

### 基础支撑模块

- 图书分类管理
- 图书基础数据管理
- 自习室基础数据管理
- 座位基础数据管理
- Dashboard overview
- OSS 图片上传

### 工程支撑模块

- Redis 缓存
- Redisson 分布式锁
- AOP 操作日志
- 全局异常处理
- DTO 参数校验
- Spring Schedule 定时任务
- Testcontainers 集成测试
- springdoc-openapi 接口文档

## 本地启动

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

内置账号密码均为 `123456`：

- 管理员：`admin`
- 馆员：`librarian`
- 普通用户：`user01`

### 3. 启动服务

```bash
mvn spring-boot:run
```

访问地址：

- API 根路径：`http://localhost:18080/api`
- Swagger UI：`http://localhost:18080/api/swagger-ui.html`
- OpenAPI JSON：`http://localhost:18080/api/v3/api-docs`

## 测试

```bash
mvn -DskipTests compile
mvn test
```

说明：

- `CoreBusinessFlowTest` 覆盖借阅状态流转、预约冲突与并发边界、认证失效边界。
- Testcontainers 集成测试依赖 Docker。
- 当 Docker 不可用时，带有 `disabledWithoutDocker = true` 的容器测试会自动跳过。
- Docker 可用时，测试会启动临时 MySQL 和 Redis 验证真实链路。

## 后续优化方向

- 接入 Spring Security 统一认证授权链路。
- 引入 Refresh Token 优化登录续期体验。
- 使用 XXL-JOB 或 Quartz 管理定时任务。
- 将文件上传能力独立为文件服务。
- 补充更完整的并发测试、监控指标和告警策略。
