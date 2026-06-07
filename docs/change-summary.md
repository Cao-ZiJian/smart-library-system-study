# 变更记录

## 项目定位

项目名称：智能图书馆借阅与座位预约系统

系统定位：面向图书馆借阅和空间预约业务的 Spring Boot 后端服务，聚焦认证会话、借阅状态流转、座位预约并发控制、基础数据维护和运维支撑能力。

核心链路：

- 登录认证：JWT + Redis Session、`jti`、黑名单、滑动续期、`tokenVersion`。
- 图书查询：分页查询、分类筛选、详情查询、热门图书缓存。
- 借阅流程：申请、审核、出借、续借、归还、逾期扫描。
- 座位预约：创建、冲突校验、签到、取消、结束、过期处理、并发控制。
- 工程支撑：Redis、Redisson、AOP、全局异常、参数校验、定时任务、Testcontainers、springdoc-openapi。

## 认证链路收敛

改动：

- 删除冗余旧会话服务接口与实现。
- 保留 `SessionManager` 和 `RedisSessionManager` 作为统一认证会话入口。
- `AuthController`、`UserServiceImpl`、`LoginInterceptor` 统一调用 `SessionManager`。
- 补充 Redis Session 相关单元测试。

效果：

- 会话创建、认证、续期、退出和黑名单写入集中在统一边界内。
- 拦截器只负责提取凭证、调用认证入口和维护用户上下文。

## 非核心链路精简

改动：

- 删除借阅导出 Controller、Service、Mapper、DTO、XML。
- 移除导出相关 Maven 依赖。
- 删除导出相关测试和文档描述。

效果：

- 系统主线聚焦借阅状态流转、预约并发控制和认证会话管理。
- 降低非核心模块对维护和测试范围的影响。

## 报表链路精简

改动：

- 删除复杂统计 Controller、Service、Mapper、XML 和专用 VO。
- 删除统计表 DTO。
- 保留 Dashboard overview。
- 删除 `/admin/report/*` 相关测试。

效果：

- Dashboard overview 作为后台首页聚合视图保留。
- 复杂报表不再作为当前服务边界的一部分。

## 核心测试补充

改动：

- 新增 `CoreBusinessFlowTest`。
- 覆盖借阅完整流转、库存一致性、非法状态、库存不足。
- 覆盖预约创建、签到、结束、取消后不可签到、结束后不可重复结束。
- 覆盖同座位冲突、同用户冲突、并发创建最终只能成功一个。
- 覆盖登录、`/auth/me`、退出黑名单、`tokenVersion`、禁用用户失效。
- Testcontainers 在 Docker 不可用时使用 `disabledWithoutDocker = true` 跳过。

效果：

- 核心业务风险由集成测试覆盖。
- Docker 可用环境下可通过临时 MySQL 和 Redis 验证真实链路。

## 接口文档迁移

改动：

- 删除 Springfox 和 Knife4j 依赖。
- 删除 `Knife4jConfig` 和 `SwaggerCompatibilityConfig`。
- 引入 `springdoc-openapi-starter-webmvc-ui`。
- Controller 注解迁移到 OpenAPI 3 注解。
- WebMvc 拦截器放行 `/swagger-ui.html`、`/swagger-ui/**`、`/v3/api-docs/**`。

效果：

- 接口文档适配 Spring Boot 3。
- Swagger UI 由 springdoc-openapi 自动生成。

## 验证状态

- `mvn -DskipTests compile`：用于验证编译。
- `mvn test`：用于验证单元测试和集成测试。
- Docker 可用时应执行 `CoreBusinessFlowTest`，通过临时 MySQL 和 Redis 验证真实链路。

## 提交规范建议

- 不提交 `target/` 构建产物。
- 不提交 `.idea/`、`*.iml`、本地压缩包和临时文件。
- README、docs、pom 保持 UTF-8 编码。
- commit message 建议使用工程化语义，例如：`docs: normalize smart library project documentation`。
