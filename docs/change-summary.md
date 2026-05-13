# 重构收尾总结

## 当前项目最终定位

项目名称：智能图书馆借阅与座位预约系统

一句话定位：面向 Java 后端实习展示的中小型 Spring Boot 项目，聚焦图书借阅状态流转和座位预约并发控制。

最终主线：

- 登录认证：JWT + Redis Session、`jti`、黑名单、滑动续期、`tokenVersion`
- 图书查询：作为借阅入口，保留分页、分类和热门图书缓存
- 借阅流程：申请、审核、出借、续借、归还、逾期扫描
- 座位预约：创建、冲突校验、签到、取消、结束、过期处理、并发控制
- 工程能力：Redis、Redisson、AOP、全局异常、参数校验、定时任务、Testcontainers、Knife4j

## 第一阶段：展示面重构

改动：

- 将 README 改为面试展示型结构。
- 调整项目名称、描述和 Knife4j 展示口径。
- 增加模块定位文档，区分核心业务、基础支撑和工程能力。

原因：

- 原项目容易被看成“功能堆叠式后台 CRUD”。
- 面试需要 2 到 5 分钟内讲清主线和技术亮点。

## 第二/三阶段：认证边界收口

改动：

- 删除冗余旧会话服务接口与实现。
- 保留 `SessionManager` / `RedisSessionManager` 作为唯一认证会话入口。
- `AuthController`、`UserServiceImpl`、`LoginInterceptor` 统一调用 `SessionManager`。
- 补充 Redis Session 相关单元测试。

原因：

- 原有 Session 相关职责分散，容易讲不清。
- 收口后认证链路更适合面试表达。

## 第四阶段：删除导出链路

改动：

- 删除借阅导出 Controller、Service、Mapper、DTO、XML。
- 移除导出相关 Maven 依赖。
- 删除相关测试和文档描述。

原因：

- 导出功能对当前项目主线帮助有限。
- 删除后项目更聚焦借阅状态流转、预约并发控制和认证会话管理。

## 第五阶段：删除复杂 Report

改动：

- 删除复杂统计 Controller、Service、Mapper、XML 和专用 VO。
- 删除统计行 DTO。
- 保留 Dashboard overview。
- 删除 `/admin/report/*` 相关测试。

原因：

- 复杂报表容易喧宾夺主。
- Dashboard overview 足够作为后台首页概览，Report 不再作为项目亮点。

## 第六阶段：核心测试补强

改动：

- 新增 `CoreBusinessFlowTest`。
- 覆盖借阅完整流转、库存一致性、非法状态、库存不足。
- 覆盖预约创建、签到、结束、取消后不可签到、结束后不可重复结束。
- 覆盖同座位冲突、同用户冲突、并发创建最终只能成功一个。
- 覆盖登录、`/auth/me`、登出黑名单、`tokenVersion`、禁用用户失效。
- Testcontainers 在 Docker 不可用时使用 `disabledWithoutDocker = true` 跳过。

原因：

- 面试项目不仅要能跑，还要能证明核心业务风险被测试覆盖。
- 测试用例名称直接表达业务语义，方便面试讲解。

## 当前诚实验证状态

- `mvn -DskipTests compile` 通过。
- `mvn test` 通过。
- 当前本机 Docker 探测异常时，Testcontainers 集成测试会跳过。
- Docker 正常环境下应执行 `CoreBusinessFlowTest`，验证真实 MySQL + Redis 链路。

## GitHub 提交建议

- 不要提交 `target/` 构建产物。
- 不要提交 `.idea/`、`*.iml`、本机压缩包和历史笔记。
- README、docs、pom 应保持 UTF-8 且无异常字符。
- commit message 建议：`refactor: focus smart library project for backend internship showcase`
