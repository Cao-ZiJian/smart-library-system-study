# 系统设计说明

## 系统概览

智能图书馆借阅与座位预约系统提供图书馆常见业务的后端能力，包括用户认证、图书检索、借阅流程、座位预约、后台基础数据维护、操作审计和定时状态处理。

系统以借阅状态流转和座位预约并发控制为核心，配合 JWT + Redis Session、Redis 缓存、Redisson 分布式锁、MyBatis-Plus 条件更新、AOP 操作日志和 Testcontainers 集成测试，形成可维护的后端服务结构。

## 登录认证链路

1. `AuthController` 接收登录请求。
2. `UserServiceImpl` 校验用户名、密码和用户状态。
3. 登录成功后调用 `SessionManager.createSession(user)`。
4. `RedisSessionManager` 创建 JWT、生成 `jti`、写入 Redis Session。
5. `LoginInterceptor` 提取 Bearer token，调用 `SessionManager.authenticate`。
6. 认证通过后写入 `UserContext`。
7. 退出登录时调用 `SessionManager.removeSession(token)`，删除 Session 并写入黑名单。

## 借阅状态流转

```text
APPLYING -> APPROVED -> LENT -> RETURNED
APPLYING -> REJECTED
LENT -> OVERDUE -> RETURNED
```

关键规则：

- 申请阶段只建单，不扣减库存。
- 出借阶段扣减可借库存。
- 归还后恢复库存。
- 非 `LENT` 状态不可续借。
- 非 `LENT` 或 `OVERDUE` 状态不可归还。
- 重复审核、重复出借、重复归还均通过状态条件更新拦截。

## 座位预约并发控制

```text
PENDING_CHECK_IN -> IN_USE -> FINISHED
PENDING_CHECK_IN -> CANCELED
PENDING_CHECK_IN -> EXPIRED
```

关键规则：

- 同一座位同一时间段不可重复预约。
- 同一用户同一时间段不可预约多个座位。
- 已取消预约不可签到。
- 已结束预约不可再次结束。
- 创建预约时使用 Redisson 组合锁，锁内完成冲突校验和记录创建。

## Redis 缓存设计

- 缓存热门图书、图书详情、分类、自习室列表等高频读数据。
- 管理端写操作后主动失效相关缓存。
- 缓存策略保持简单明确，避免引入不必要的多级缓存和复杂一致性策略。

## Redisson 锁设计

- 借阅申请：按 `userId + bookId` 防止同一用户重复申请同一本书。
- 座位预约：按 `seatId + userId` 控制同座位和同用户两个冲突维度。
- 锁执行逻辑封装在执行器中，业务代码只表达被锁保护的业务动作。

## AOP 操作日志

- 使用 `@OperationLog` 标记关键操作。
- AOP 统一记录用户、接口、请求参数、执行耗时、结果和异常。
- 对 `password`、`token`、`secret` 等敏感字段脱敏。
- 日志保存失败只记录告警，不影响主业务返回。

## 定时任务

- 借阅逾期扫描：将超过应还时间的 `LENT` 订单标记为 `OVERDUE`。
- 预约过期处理：将未签到且已过期的预约标记为 `EXPIRED`。
- 使用中自动结束：将超过结束时间的 `IN_USE` 预约标记为 `FINISHED`。

## Testcontainers 测试

- `CoreBusinessFlowTest` 覆盖核心业务集成场景。
- 覆盖借阅状态流转、库存一致性、预约冲突、并发边界和认证失效边界。
- Docker 可用时启动临时 MySQL 和 Redis 验证真实链路。
- Docker 不可用时通过 `@Testcontainers(disabledWithoutDocker = true)` 跳过容器测试。
