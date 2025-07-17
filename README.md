# Lyra

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.3-blue.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Lyra 是一个基于 Spring Boot 的现代化 Web 应用程序，集成了 Spring Security 和 OAuth2 认证，为构建安全、可扩展的企业级应用提供了坚实的基础。

## ✨ 特性

- 🚀 **现代化技术栈**: 基于 Spring Boot 3.5.3 和 Java 21
- 🔐 **安全认证**: 集成 Spring Security 和 OAuth2 Client
- 🏗️ **前后端分离**: 支持 Vue.js 前端框架
- 📦 **容器化支持**: 支持 GraalVM Native Image 构建
- 🛠️ **开发友好**: 集成 Lombok，简化开发流程
- 📋 **完整测试**: 包含完整的单元测试和集成测试

## 🛠️ 技术栈

### 后端
- **Java 21** - 最新的 LTS 版本
- **Spring Boot 3.5.3** - 企业级应用框架
- **Spring Security** - 安全框架
- **Spring OAuth2 Client** - OAuth2 认证客户端
- **Lombok** - 减少样板代码

### 前端
- **Vue.js** - 渐进式 JavaScript 框架（规划中）

### 构建工具
- **Gradle 8.14.3** - 现代化构建工具
- **GraalVM Native Image** - 原生镜像支持

## 🚀 快速开始

### 环境要求

- Java 21 或更高版本
- Gradle 8.14.3 或更高版本（可选，项目包含 Gradle Wrapper）

### 安装和运行

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd lyra
   ```

2. **构建项目**
   ```bash
   ./gradlew build
   ```

3. **运行应用**
   ```bash
   ./gradlew bootRun
   ```

4. **访问应用**

   应用启动后，访问 [http://localhost:8080](http://localhost:8080)

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行测试并生成报告
./gradlew test jacocoTestReport
```

## 📁 项目结构

```
lyra/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tslc/beihaiyun/lyra/    # Java 源代码
│   │   ├── resources/                  # 资源文件
│   │   │   ├── application.properties  # 应用配置
│   │   │   ├── static/                # 静态资源
│   │   │   └── templates/             # 模板文件
│   │   ├── api/                       # API 定义（规划中）
│   │   └── vue/                       # Vue.js 前端（规划中）
│   └── test/                          # 测试代码
├── docs/                              # 项目文档
├── deployment/                        # 部署相关文件
├── scripts/                          # 脚本文件
├── build.gradle                      # Gradle 构建配置
├── settings.gradle                   # Gradle 设置
└── README.md                         # 项目说明
```

## 🔧 开发指南

### 配置文件

主要配置文件位于 `src/main/resources/application.properties`：

```properties
spring.application.name=lyra
# 在此添加其他配置项
```

### 添加新功能

1. 在 `src/main/java/tslc/beihaiyun/lyra/` 下创建相应的包和类
2. 遵循 Spring Boot 的约定和最佳实践
3. 为新功能编写相应的测试用例

### 代码规范

- 使用 Lombok 注解减少样板代码
- 遵循 Java 命名约定
- 编写清晰的注释和文档
- 保持代码的可读性和可维护性

## 🚢 部署

### 传统部署

1. **构建 JAR 包**
   ```bash
   ./gradlew bootJar
   ```

2. **运行 JAR 包**
   ```bash
   java -jar build/libs/lyra-0.0.1-SNAPSHOT.jar
   ```

### Native Image 部署

1. **构建 Native Image**
   ```bash
   ./gradlew nativeCompile
   ```

2. **运行 Native 可执行文件**
   ```bash
   ./build/native/nativeCompile/lyra
   ```

### Docker 部署

```dockerfile
# Dockerfile 示例（需要创建）
FROM openjdk:21-jre-slim
COPY build/libs/lyra-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 贡献指南

我们欢迎所有形式的贡献！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 开发规范

- 遵循现有的代码风格
- 为新功能添加测试
- 更新相关文档
- 确保所有测试通过

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详细信息请查看 [LICENSE](LICENSE) 文件。

## 📞 联系方式

- 项目维护者: tslc.beihaiyun
- 问题反馈: 请使用 GitHub Issues

## 🗺️ 路线图

- [ ] 完善 OAuth2 认证流程
- [ ] 集成 Vue.js 前端
- [ ] 添加 API 文档
- [ ] 实现用户管理功能
- [ ] 添加监控和日志
- [ ] 容器化部署支持

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！