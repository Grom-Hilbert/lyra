# OpenAPI 文档结构说明

## 文档拆分方案

为了提高可维护性和可读性，我们将原来的单一OpenAPI文档拆分为多个模块化文件。

## 文件结构

```plaintext
src/main/api/
├── openapi.yaml                 # 主文件（基本信息、安全配置、主要引用）
├── openapi-backup.yaml          # 原始完整文档备份
├── openapi-new.yaml             # 新的简化主文件示例
├── components/                  # 组件定义
│   ├── schemas/                 # 数据模型定义
│   │   ├── auth.yaml            # 认证相关schema
│   │   ├── file.yaml            # 文件相关schema
│   │   ├── folder.yaml          # 文件夹相关schema
│   │   ├── user.yaml            # 用户相关schema
│   │   ├── system.yaml          # 系统管理schema
│   │   └── common.yaml          # 通用schema
│   ├── responses/               # 响应定义
│   │   └── common.yaml          # 通用响应
│   ├── parameters/              # 参数定义
│   │   └── common.yaml          # 通用参数
│   └── security/                # 安全方案
│       └── schemes.yaml         # 安全认证方案
├── paths/                       # API路径定义
│   ├── auth.yaml                # 认证API
│   ├── files.yaml               # 文件管理API
│   ├── folders.yaml             # 文件夹管理API
│   ├── search.yaml              # 搜索API
│   ├── preview.yaml             # 预览API
│   ├── editor.yaml              # 编辑API
│   ├── admin/                   # 管理员API
│   │   ├── system.yaml          # 系统管理API
│   │   ├── statistics.yaml      # 统计监控API
│   │   ├── config.yaml          # 配置管理API
│   │   └── cache.yaml           # 缓存管理API
│   └── webdav.yaml              # WebDAV API
└── tags/                        # 标签定义
    └── definitions.yaml         # API分组标签
```

## 拆分优势

### 1. 可维护性提升

- **模块化管理**: 每个功能模块独立文件，便于维护
- **职责分离**: 不同类型的定义分别管理
- **版本控制**: 变更更容易追踪和回滚

### 2. 协作效率提升

- **并行开发**: 不同开发者可以同时编辑不同模块
- **冲突减少**: 避免多人同时修改同一个大文件
- **代码审查**: 更容易进行针对性的代码审查

### 3. 可读性提升

- **文件大小**: 每个文件更小，更容易阅读
- **逻辑清晰**: 相关功能聚合在一起
- **查找便捷**: 快速定位到特定功能的定义

## 文件内容说明

### 主文件 (openapi.yaml)

包含：

- API基本信息（title, description, version等）
- 服务器配置
- 安全配置
- 主要的schema和path引用
- 标签定义

### 组件文件 (components/)

#### schemas/

- **auth.yaml**: 登录请求、响应、用户信息等认证相关模型
- **file.yaml**: 文件信息、上传请求、下载响应等文件相关模型
- **folder.yaml**: 文件夹信息、创建请求、树结构等文件夹相关模型
- **user.yaml**: 用户详情、用户列表、用户统计等用户相关模型
- **system.yaml**: 系统配置、批量操作、统计信息等系统相关模型
- **common.yaml**: 通用响应、分页信息、错误响应等通用模型

#### responses/

- **common.yaml**: 标准HTTP响应（400, 401, 403, 404, 500等）

#### parameters/

- **common.yaml**: 通用查询参数（page, size, sort, direction等）

#### security/

- **schemes.yaml**: JWT认证方案定义

### 路径文件 (paths/)

#### 核心功能API

- **auth.yaml**: 登录、注册、令牌管理等认证API
- **files.yaml**: 文件上传、下载、管理等文件API
- **folders.yaml**: 文件夹创建、查询、管理等文件夹API
- **search.yaml**: 搜索、筛选、建议等搜索API
- **preview.yaml**: 文件预览、缩略图等预览API
- **editor.yaml**: 在线编辑、协同编辑等编辑API

#### 管理员API (admin/)

- **system.yaml**: 用户管理、角色管理等系统管理API
- **statistics.yaml**: 用户统计、系统监控等统计API
- **config.yaml**: 配置查询、更新等配置管理API
- **cache.yaml**: 缓存统计、清理等缓存管理API

#### 协议API

- **webdav.yaml**: WebDAV协议相关API

### 标签文件 (tags/)

- **definitions.yaml**: API分组标签和描述

## 引用机制

### 文件间引用

使用相对路径引用其他文件：

```yaml
$ref: './components/schemas/auth.yaml#/LoginRequest'
```

### 文件内引用

使用锚点引用同文件内容：

```yaml
$ref: '#/components/schemas/ApiResponse'
```

### 路径引用

OpenAPI路径需要特殊编码：

```yaml
/api/auth/login:
  $ref: './paths/auth.yaml#/~1api~1auth~1login'
```

## 使用建议

### 1. 开发流程

1. 在对应模块文件中定义新的API或schema
2. 在主文件中添加引用
3. 验证OpenAPI文档的完整性
4. 更新相关的使用文档

### 2. 文件命名规范

- 使用小写字母和连字符
- 文件名应该清楚表达内容
- 保持一致的命名风格

### 3. 内容组织原则

- 相关功能聚合在同一文件
- 避免循环引用
- 保持引用关系的清晰性

## 迁移计划

### 阶段1: 备份和准备

- ✅ 备份原始文档为 `openapi-backup.yaml`
- ✅ 创建新的目录结构
- ✅ 创建简化的主文件示例

### 阶段2: 组件拆分（建议）

- 创建通用组件文件
- 拆分schemas到对应模块
- 拆分responses和parameters

### 阶段3: 路径拆分（建议）

- 按功能模块拆分API路径
- 创建管理员API子目录
- 验证所有引用关系

### 阶段4: 验证和优化（建议）

- 使用OpenAPI工具验证文档完整性
- 测试Swagger UI是否正常工作
- 优化文档结构和内容

## 工具支持

### 验证工具

```bash
# 使用swagger-codegen验证
swagger-codegen validate -i src/main/api/openapi.yaml

# 使用openapi-generator验证
openapi-generator validate -i src/main/api/openapi.yaml
```

### 合并工具

如果需要生成单一文件：

```bash
# 使用swagger-codegen合并
swagger-codegen generate -i src/main/api/openapi.yaml -l openapi-yaml -o dist/
```

## 注意事项

1. **引用路径**: 确保所有$ref路径正确
2. **循环引用**: 避免schema之间的循环引用
3. **文件编码**: 统一使用UTF-8编码
4. **YAML格式**: 保持一致的缩进和格式
5. **版本控制**: 合理使用Git来管理文件变更

## 当前状态

- ✅ 原始文档已备份
- ✅ 目录结构已创建
- ✅ 简化主文件已创建
- ⏳ 组件拆分（待完成）
- ⏳ 路径拆分（待完成）
- ⏳ 引用验证（待完成）

建议根据项目需要和团队情况，逐步完成剩余的拆分工作。
