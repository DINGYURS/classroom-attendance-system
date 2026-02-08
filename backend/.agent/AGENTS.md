# 基于人脸检测与识别的课堂快速点名系统 AI 开发指令

## 项目概述：

这是一个基于人脸检测与识别的课堂快速点名系统开发，技术栈如下

- **前端**：Vue 3 (Script Setup) + Element Plus + Axios + ECharts + Tailwindcss + **compressorjs** (图片压缩)。
- **后端**：Spring Boot 3 + MyBatis + Hutool (工具类/加密)。
- **算法微服务**：Python 3.11 + FastAPI + PyTorch + **YOLOv11** + **InsightFace**。
- **数据存储**：
  - 结构化数据：MySQL 8.0 (使用 **JSON** 类型存特征，开启 AES 加密)。
  - 非结构化数据：MinIO (存储原始图片)。
- **部署运维**：Docker + Docker Compose + Nginx (反向代理)

## 前端

### 开发规范

- 使用 TypeScript，确保类型安全
- 组件使用函数式组件 + Hooks
- 使用 Tailwind CSS 编写样式
- 使用 Vite5 作为构建工具
- 使用 Vue Router 作为路由组件
- 采用企业级开发规范
- 使用pnpm作为包管理工具

### 代码风格
- 使用 ESLint 和 Prettier
- 组件名使用 PascalCase
- 函数名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE

### 注意事项
- 保持代码简洁，避免过度设计
- 优先实现核心功能
- 确保移动端适配

## 后端

### 1. 项目架构概览

- **单模块 Maven 架构**：使用单一 `pom.xml` 管理依赖与构建。
- **技术栈**：Spring Boot + MyBatis（XML Mapper）。
- **配置方式**：`application.yml` + `application-dev.yml` 分环境配置。
- **资源组织**：Mapper XML 与模板文件集中在 `resources` 下。

### 2. 目录与包规范

#### 2.1 根目录

- `pom.xml`: 依赖与插件管理。

#### 2.2 `src/main/java`

- `anno`: 通用注解。
- `constant`: 常量定义（JWT、角色、状态、消息等）。
- `context`: 上下文持有（如当前用户/请求上下文/threadLocal）。
- `exception`: 统一异常体系与业务异常。
- `json`: JSON 配置与工具类。
- `properties`: 配置属性绑定。
- `result`: 统一响应模型。
- `utils`: 通用工具类。

- `dto`: DTO，数据传输对象。
- `entity`: Entity，领域实体/持久化映射模型。
- `vo`: VO，视图对象。

- `SkyApplication`: Spring Boot 入口。
- `aop`: AOP 切面（日志、鉴权等）。
- `config`: Spring 配置（Web MVC、拦截器、WebSocket 等）。
- `controller`: Web 控制器层。
- `handler`: 异常处理/WebSocket 处理等。
- `interceptor`: 请求/响应拦截器。
- `mapper`: MyBatis Mapper 接口。
- `service`: 业务服务层。
- `webSocket`: WebSocket 相关逻辑。

#### 2.3 资源目录

- `resources/mapper`: MyBatis XML 映射文件。
- `resources/template`: 模板文件（例如导出 Excel 模板）。
- `resources/application*.yml`: 环境配置。

### 3. 代码组织与分层规范

- **Controller** 仅处理请求与响应组装，不写复杂业务逻辑。
- **Service** 负责业务编排与事务边界。
- **Mapper** 负责数据访问，配套 XML 实现 SQL。
- **DTO/VO/Entity** 分工明确：
  - DTO：入参/出参传输。
  - Entity：领域/持久化模型。
  - VO：前端展示模型。
- **异常** 服务端统一处理并返回标准响应。
- **常量** 集中管理，避免魔法值。

### 4. 代码风格与约定

- **命名**：包名小写，类名与接口名使用 UpperCamelCase。
- **配置**：优先使用 `application.yml` 与 `@ConfigurationProperties`。
- **日志**：使用lambok的`@Slf4j`注解。
- **事务**：使用 `@Transactional` 管理事务边界。
- **接口文档**：使用 Swagger 注解生成 API 文档。
- 
### 5. 生成新项目时的建议约束

- 保持包结构一致，便于复用与扩展。
- 数据访问使用 MyBatis XML + Mapper 接口方式。
- 统一返回结构与异常处理策略。