# Lyra 文档管理系统

Lyra（天琴座）是一个企业级云原生文档管理系统，专为企业内部网络设计。它提供了全面的文档存储、版本控制和协作解决方案，具有先进的Git集成功能。

## 核心特性

### 🔐 身份认证与授权

- **多提供商认证**: 支持本地、OAuth2、OIDC、LDAP认证
- **RBAC系统**: 4层角色结构（超级管理员、管理员、用户、访客）
- **JWT令牌管理**: 基于令牌的认证和刷新机制
- **管理员审批**: 邮箱注册需要超级管理员审批
- **可视化管理**: 基于Web的用户、角色和权限管理界面

### 📁 文件和文件夹管理

- **完整文件操作**: 上传、下载、删除、移动、复制、重命名、分享
- **文件预览和编辑**: 文本/图片预览、压缩包提取、内联文本编辑
- **空间管理**: 企业空间（共享）和个人空间分离
- **WebDAV集成**: 将企业/个人空间挂载为本地驱动器
- **元数据和搜索**: 文件元数据跟踪和基于文件名的搜索
- **权限继承**: 分层权限系统和临时访问授权

### 💾 多种存储后端支持

- **本地文件系统**: 默认的本地存储
- **NFS网络文件系统**: 企业级共享存储支持
- **S3兼容对象存储**: 支持AWS S3和兼容的对象存储
- **存储抽象层**: 统一的存储接口，支持动态切换

### 🔄 版本控制系统

- **三层版本控制**: 无版本控制、基础版本控制、高级版本控制
- **Git集成**: 完整的Git功能和远程仓库同步
- **智能文件处理**: 二进制文件版本控制 + 高级模式下的文本内容版本控制
- **WebDAV兼容性**: Git模式激活时文本文件在WebDAV中为只读
- **批量操作**: 批量上传时的版本更新提示和文件匹配
- **格式转换**: Markitdown集成，支持非文本到文本的转换

### 📋 模板和插件系统

- **文件夹模板**: 预定义的文件夹结构，包含默认内容和权限
- **模板仓库**: 集中化模板管理和分享功能
- **插件框架**: 前端和后端插件扩展性
- **插件管理**: 安装、卸载、启用、禁用、升级操作

## 技术栈

### 后端技术

- **Java 21+**: 现代Java特性支持
- **Spring Boot 3.5**: 企业级应用框架
- **Spring Security**: 安全认证和授权
- **Spring Data JPA**: 数据访问层
- **JGit 6.x+**: Git版本控制集成
- **Milton WebDAV 3.x+**: WebDAV协议实现
- **Apache Tika 2.x+**: 文件内容提取和格式转换

### 前端技术

- **Vue 3.5+**: 现代前端框架
- **TypeScript 5.x+**: 类型安全的JavaScript
- **Vite**: 快速构建工具
- **Pinia**: 状态管理
- **Vue Router**: 路由管理

### 数据库和存储

- **SQLite**: 默认嵌入式数据库
- **MySQL 8.x+ / PostgreSQL 13+**: 可选的外部数据库
- **Redis 7.x+**: 可选缓存层
- **多种存储后端**: 本地文件系统、NFS、S3兼容对象存储

### 部署和监控

- **Docker**: 容器化部署
- **Kubernetes**: 容器编排
- **OpenTelemetry**: 统一可观测性数据导出
- **Grafana Alloy**: 数据收集和转发

## 快速开始

### 使用Docker运行

#### 本地存储模式

```bash
docker run -d \
  --name lyra \
  -p 8080:8080 \
  -v lyra-data:/data \
  -v lyra-storage:/storage \
  lyra:latest
```

#### NFS存储模式

```bash
docker run -d \
  --name lyra-nfs \
  --privileged \
  -p 8080:8080 \
  -e STORAGE_TYPE=nfs \
  -e NFS_ENABLED=true \
  -e NFS_SERVER=192.168.1.100 \
  -e NFS_EXPORT_PATH=/exports/lyra-storage \
  -v lyra-data:/data \
  lyra:latest
```

### 使用Docker Compose

```bash
# 本地存储
docker-compose up -d

# NFS存储
docker-compose -f deployment/docker/docker-compose-nfs.yml up -d
```

### 本地开发

```bash
# 克隆仓库
git clone https://github.com/your-org/lyra.git
cd lyra

# 构建项目
./gradlew build

# 运行应用
./gradlew bootRun
```

访问 <http://localhost:8080> 开始使用Lyra。

## 存储配置

### 本地文件系统存储

```yaml
lyra:
  storage:
    primary: local
    local:
      root: ./storage
      max-file-size: 104857600  # 100MB
```

### NFS网络文件系统存储

```yaml
lyra:
  storage:
    primary: nfs
    nfs:
      enabled: true
      server: 192.168.1.100
      export-path: /exports/lyra-storage
      mount-point: /mnt/nfs-storage
      mount-options: rw,sync,hard,intr
      max-file-size: 104857600
      connection-timeout: 30000
      read-timeout: 60000
      retry-count: 3
```

### S3兼容对象存储

```yaml
lyra:
  storage:
    primary: s3
    s3:
      enabled: true
      endpoint: https://s3.amazonaws.com
      region: us-east-1
      access-key: YOUR_ACCESS_KEY
      secret-key: YOUR_SECRET_KEY
      bucket: lyra-storage
      max-file-size: 104857600
```

## 部署选项

### Docker部署

- 单容器部署
- Docker Compose多服务部署
- 支持多种存储后端

### Kubernetes部署

- Helm Chart支持
- 水平扩展
- 配置管理和密钥管理
- 健康检查和自动恢复

### 原生部署

- JAR包直接运行
- 系统服务集成
- 传统部署环境支持

## 配置管理

Lyra支持多层配置管理，按优先级从高到低：

1. **动态配置**: Nacos配置中心（最高优先级）
2. **命令行参数**: 开发调试用（高优先级）
3. **环境变量**: 容器化部署常用（中等优先级）
4. **静态配置**: YAML配置文件（最低优先级）

## 监控和可观测性

- **健康检查**: `/actuator/health`
- **指标监控**: Prometheus格式指标
- **分布式追踪**: OpenTelemetry集成
- **日志聚合**: 结构化日志输出
- **存储监控**: 各种存储后端的健康状态和统计信息

## 安全特性

- **JWT令牌认证**: 安全的无状态认证
- **RBAC权限控制**: 细粒度的权限管理
- **输入验证**: 防止注入攻击
- **文件类型检查**: 安全的文件上传
- **审计日志**: 完整的操作审计跟踪

## 文档

- [NFS存储配置指南](docs/storage/NFS-Storage-Guide.md)
- [API文档](http://localhost:8080/swagger-ui.html)
- [开发者指南](docs/development/)
- [部署指南](docs/deployment/)

## 贡献

我们欢迎社区贡献！请阅读我们的贡献指南：

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 许可证

本项目采用MIT许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 支持

- 📧 邮箱: <support@lyra-docs.com>
- 💬 讨论: [GitHub Discussions](https://github.com/your-org/lyra/discussions)
- 🐛 问题报告: [GitHub Issues](https://github.com/your-org/lyra/issues)
- 📖 文档: [官方文档](https://docs.lyra-docs.com)

---

**Lyra** - 让文档管理变得简单而强大 🚀
