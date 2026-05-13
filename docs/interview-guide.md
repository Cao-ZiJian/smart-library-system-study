# 面试讲解指南

## 2 分钟项目介绍

这个项目是一个智能图书馆后端系统，我把重点放在两个真实业务闭环：图书借阅状态流转和座位预约并发控制。

借阅流程覆盖申请、审核、出借、续借、归还和逾期扫描。申请阶段不扣库存，出借阶段才扣库存，归还后恢复库存；关键操作通过状态校验和条件更新防止重复审核、重复出借、重复归还。

预约流程覆盖创建、签到、取消、结束和过期处理。创建预约时使用 Redisson 对座位和用户两个维度加组合锁，在锁内校验时间冲突，避免同一座位或同一用户在并发下重复预约。

工程上，认证使用 JWT + Redis Session，支持 `jti`、黑名单、滑动续期和 `tokenVersion` 失效；同时使用 Redis 缓存、AOP 操作日志、全局异常处理、参数校验、定时任务和 Testcontainers 集成测试。

## 5 分钟项目介绍

这个项目不是追求功能数量的后台管理系统，而是一个适合 Java 后端实习展示的中小型 Spring Boot 项目。它围绕图书馆里的两个真实场景展开：借阅和座位预约。

借阅流程里，用户先提交借阅申请，系统创建 `APPLYING` 订单。这里不立即扣库存，因为馆员还可能驳回申请。馆员审核通过后，订单变成 `APPROVED`。真正出借时，系统把订单更新为 `LENT`，同时扣减可借库存并增加借阅次数。归还时从 `LENT` 或 `OVERDUE` 更新为 `RETURNED`，再恢复库存。续借只允许本人、`LENT` 状态且未超过续借次数的订单。这个流程可以体现状态机、库存一致性和重复操作防护。

预约流程里，最大问题是并发冲突。两个请求可能同时查询到某个座位没有冲突，然后同时插入预约记录。项目用 Redisson 组合锁同时锁住 `seatId` 和 `userId`，然后在锁内校验座位时间冲突和用户时间冲突。这样既防止同一座位被重复预约，也防止同一用户同一时间预约多个座位。

认证方面，项目使用 JWT + Redis Session。JWT 只存 `userId`、`jti`、`tokenVersion`，Redis 保存服务端登录态。登出时删除 Session 并把 `jti` 加入黑名单；用户被禁用或 `tokenVersion` 变化后，旧 token 会失效；Session 快过期时会滑动续期。这个设计比纯 JWT 更可控，也比完整 Spring Security 更轻量，适合当前项目规模。

测试方面，项目补充了 `CoreBusinessFlowTest`，覆盖借阅完整状态流转、预约状态流转、预约并发冲突和认证边界。测试使用 Testcontainers，Docker 正常时会启动临时 MySQL + Redis 验证真实链路；当前本机 Docker 环境异常时会跳过容器测试，不能夸大成所有容器测试已在本机完整跑通。

## 登录认证链路

1. `AuthController` 接收登录请求。
2. `UserServiceImpl` 只负责用户名密码和用户状态校验。
3. 登录成功后调用 `SessionManager.createSession(user)`。
4. `RedisSessionManager` 创建 JWT、生成 `jti`、写入 Redis Session。
5. `LoginInterceptor` 提取 Bearer token，调用 `SessionManager.authenticate`。
6. 认证通过后写入 `UserContext`。
7. 登出时调用 `SessionManager.removeSession(token)`，删除 Session 并写入黑名单。

## 借阅状态流转链路

```text
APPLYING -> APPROVED -> LENT -> RETURNED
APPLYING -> REJECTED
LENT -> OVERDUE -> RETURNED
```

关键规则：

- 申请阶段只建单，不扣库存。
- 出借阶段才扣库存。
- 归还后恢复库存。
- 非 `LENT` 状态不能续借。
- 非 `LENT` / `OVERDUE` 状态不能归还。
- 重复审核、重复出借、重复归还都应失败。

## 座位预约并发控制链路

```text
PENDING_CHECK_IN -> IN_USE -> FINISHED
PENDING_CHECK_IN -> CANCELED
PENDING_CHECK_IN -> EXPIRED
```

关键规则：

- 同一座位同一时间段不能重复预约。
- 同一用户同一时间段不能预约多个座位。
- 已取消不能签到。
- 已结束不能再次结束。
- 创建预约时使用 Redisson 组合锁，锁内完成冲突校验和插入。

## Redis 缓存设计

- 缓存热门图书、图书详情、分类、自习室列表等高频读数据。
- 管理端写操作后主动失效相关缓存。
- 缓存策略保持克制，没有引入复杂预热、多级缓存或延迟双删。

## Redisson 锁设计

- 借阅申请：按 `userId + bookId` 防止同一用户重复申请同一本书。
- 座位预约：按 `seatId + userId` 组合锁控制同座位和同用户两个冲突维度。
- 锁执行逻辑封装在执行器中，业务代码只表达“锁保护的业务动作”。

## AOP 操作日志设计

- 使用 `@OperationLog` 标记关键操作。
- AOP 统一记录用户、接口、请求参数、执行耗时、结果和异常。
- 对 `password`、`token`、`secret` 等敏感字段脱敏。
- 日志保存失败不影响主业务返回。

## 定时任务设计

- 借阅逾期扫描：将超过应还时间的 `LENT` 订单标记为 `OVERDUE`。
- 预约过期处理：将未签到且已过期的预约标记为 `EXPIRED`。
- 使用中自动结束：将超过结束时间的 `IN_USE` 预约标记为 `FINISHED`。

## Testcontainers 测试说明

- 已补充核心集成测试 `CoreBusinessFlowTest`。
- 覆盖借阅状态流转、库存一致性、预约冲突、并发边界、认证失效边界。
- 当前本机 Docker 环境异常时，`@Testcontainers(disabledWithoutDocker = true)` 会跳过容器测试。
- Docker 正常环境下应执行 `CoreBusinessFlowTest`，通过临时 MySQL + Redis 验证真实链路。
- 不要说“所有容器集成测试已在本机完整跑通”，要按实际运行环境说明。

## 高频面试问题与回答

**为什么不用纯 JWT？**

纯 JWT 一旦签发，在过期前很难服务端主动失效。本项目用 JWT + Redis Session，JWT 负责客户端凭证，Redis 控制服务端登录态，因此可以支持登出失效、黑名单、滑动续期和 `tokenVersion` 踢下线。

**为什么申请借阅不扣库存？**

申请后还需要馆员审核，申请不代表一定出借。如果申请阶段扣库存，会导致大量待审核订单占用库存。项目在出借阶段才扣库存，更符合业务语义。

**库存怎么保证一致？**

出借时先用订单状态做条件更新，确保订单只能从 `APPROVED` 流转到 `LENT` 一次，再扣减库存。归还时也按状态条件更新，并恢复库存。

**预约为什么要锁座位和用户两个维度？**

只锁座位能防止同一座位重复预约，但不能防止同一用户同一时间预约多个座位。只锁用户也不能防同一座位被不同用户同时抢到。所以创建预约时同时锁两个维度。

**如果不用 Redisson 锁会怎样？**

并发下可能两个请求都通过“查询无冲突”，然后同时插入预约记录，造成重复预约。锁的作用是保护“冲突校验 + 插入”这段临界区。

**AOP 日志为什么不能影响主业务？**

操作日志是审计能力，不应该因为日志库异常导致借阅、预约失败。因此日志保存失败只记录 warn，不打断主流程。

**Testcontainers 的价值是什么？**

它能用真实 MySQL 和 Redis 验证 SQL、事务、Redis Session、缓存和分布式锁链路，比单纯 Mock 更接近生产环境。
