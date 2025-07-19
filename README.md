# Lyra - 企业级云原生文档管理系统

![Lyra Logo](docs/assets/logo.png)

## 项目概述

Lyra 是一个企业级云原生文档管理系统，旨在为中小型企业（10-100人）提供带有版本控制的文档管理服务。系统支持WebDAV协议，内置Git版本控制，可以作为插件扩展更多丰富功能。

### 核心特性

- 🔐 **多种认证方式** - 本地认证、JWT、OAuth2、LDAP、SAML
- 📁 **完整文件管理** - 上传、下载、预览、编辑、搜索、版本控制
- 🌐 **WebDAV协议** - 支持主流客户端，无缝集成现有工作流
- 📝 **版本控制** - 普通版本控制 + Git集成（高级功能）
- 🔧 **插件架构** - 可扩展的插件系统
- 📊 **系统监控** - 健康检查、指标收集、可观测性
- ☁️ **云原生** - 支持Docker、Kubernetes、Helm部署

### 技术架构

- **后端**: Spring Boot 3.5.x + Spring Security + JPA + Milton WebDAV
- **前端**: Vue 3.5+ + TypeScript + Vite
- **数据库**: SQLite（默认）/ MySQL / PostgreSQL + Redis（可选）
- **构建工具**: Gradle 8.x+
- **部署**: Docker + Kubernetes + Helm

## 快速开始

### 系统要求

- Java 21+
- Node.js 18+
- pnpm 8+
- Git 2.40+

### 开发环境安装

1. **克隆项目**

```bash
git clone https://github.com/your-org/lyra.git
cd lyra
```

2. **后端启动**

```bash
# 构建项目
./gradlew build

# 启动开发服务器
./gradlew bootRun
```

3. **前端启动**

```bash
# 进入前端目录
cd src/main/typescript

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

4. **访问应用**

- Web界面: <http://localhost:3000>
- API文档: <http://localhost:8080/swagger-ui.html>
- WebDAV: <http://localhost:8080/webdav>

### Docker 部署

```bash
# 构建镜像
docker build -t lyra:latest .

# 启动服务
docker-compose up -d
```

### Kubernetes 部署

```bash
# 使用 Helm 部署
helm install lyra ./deployments/helm/lyra
```

## 项目结构

```
lyra/
├── src/
│   ├── main/
│   │   ├── java/tslc/beihaiyun/lyra/    # Java后端代码
│   │   ├── typescript/                  # Vue前端代码
│   │   └── resources/                   # 配置文件和静态资源
│   └── test/                           # 测试代码
├── docs/                               # 项目文档
├── deployments/                        # 部署配置
├── scripts/                            # 脚本文件
└── .cursor/                           # Cursor AI 规则
```

## 开发指南

### 编码规范

- **Java**: 遵循Google Java Style Guide，使用camelCase
- **TypeScript/Vue**: 遵循Vue官方风格指南，使用严格模式
- **数据库**: 使用snake_case命名
- **注释**: 使用中文编写，JavaDoc注释必需
- **提交**: 遵循Conventional Commits规范

### 测试策略

- **单元测试**: JUnit 5 + Mockito（目标覆盖率 >= 80%）
- **前端测试**: Vitest + Vue Test Utils
- **集成测试**: Spring Boot Test + Testcontainers
- **E2E测试**: Playwright

### API 文档

访问 `/swagger-ui.html` 查看完整的 API 文档和交互式测试界面。

## 功能特性

### 第一阶段 - 核心MVP功能

- ✅ 基础身份认证（本地认证 + JWT）
- ✅ 简单权限管理（两级角色体系）
- 🚧 核心文件管理（完整CRUD操作）
- 🚧 WebDAV协议支持（完整协议实现）
- 🚧 基础版本控制（普通版本控制模式）
- 🚧 系统监控（健康检查 + 基础指标）

### 第二阶段 - 增强功能

- ⏳ 高级身份认证（OAuth2、LDAP等）
- ⏳ 细粒度权限控制
- ⏳ 完整系统管理
- ⏳ 基础插件框架
- ⏳ 高级版本控制（Git集成）

### 第三阶段 - 生态插件

- ⏳ Git扩展插件
- ⏳ CSV数据库同步插件
- ⏳ 搜索增强插件（MeiliSearch）
- ⏳ 协同办公插件（OnlyOffice）
- ⏳ AI助手插件

## 部署和运维

### 配置管理

系统采用分层配置策略：

1. 默认配置（application.yml）
2. 环境变量覆盖
3. 外部配置文件
4. 命令行参数

### 监控指标

- 系统健康检查：`/actuator/health`
- Prometheus指标：`/actuator/prometheus`
- 应用信息：`/actuator/info`

### 性能优化

- 多级缓存（内存 + Redis）
- 数据库连接池优化
- 异步任务处理
- 文件存储优化

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交变更 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发流程

1. 查看 [任务列表](docs/development/tasks.md)
2. 阅读 [开发指南](docs/development/guide.md)
3. 参考 [架构设计](docs/development/design.md)
4. 遵循 [编码规范](.cursor/rules/)

## 社区和支持

- 📖 [项目文档](docs/)
- 🐛 [问题反馈](https://github.com/your-org/lyra/issues)
- 💬 [讨论区](https://github.com/your-org/lyra/discussions)
- 📧 [邮件联系](mailto:lyra-support@example.com)

## 许可证

本项目基于 Apache 2.0 许可证开源。查看 [LICENSE](LICENSE) 文件了解更多信息。

## 致谢

感谢所有为 Lyra 项目做出贡献的开发者和社区成员。

---

**Lyra** - 让文档管理更简单、更智能、更安全。
