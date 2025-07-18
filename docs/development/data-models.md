# 数据模型文档

## 概述

本文档描述了Lyra文档管理系统的核心数据模型，包括Java实体类和TypeScript接口定义。

## 核心实体

### 1. 用户管理

#### User (用户实体)

- **字段**：
  - `id`: 用户ID
  - `username`: 用户名（唯一）
  - `email`: 邮箱（唯一）
  - `displayName`: 显示名称
  - `status`: 用户状态（PENDING, ACTIVE, INACTIVE, SUSPENDED）
  - `authProvider`: 认证提供者（LOCAL, OAUTH2, OIDC, LDAP）
  - `passwordHash`: 密码哈希（本地认证）
  - `externalId`: 外部认证ID
  - `roles`: 用户角色列表

#### Role (角色实体)

- **字段**：
  - `id`: 角色ID
  - `name`: 角色名称（唯一）
  - `description`: 角色描述
  - `type`: 角色类型（SUPER_ADMIN, ADMIN, USER, GUEST）
  - `permissions`: 权限列表

#### Permission (权限实体)

- **字段**：
  - `id`: 权限ID
  - `name`: 权限名称（唯一）
  - `description`: 权限描述
  - `resource`: 资源类型
  - `action`: 操作类型

### 2. 文件管理

#### FileEntity (文件实体)

- **字段**：
  - `id`: 文件ID
  - `name`: 文件名
  - `path`: 文件路径（唯一）
  - `mimeType`: MIME类型
  - `size`: 文件大小
  - `checksum`: 文件校验和
  - `spaceType`: 空间类型（ENTERPRISE, PERSONAL）
  - `versionControlType`: 版本控制类型（NONE, BASIC, ADVANCED）
  - `owner`: 文件所有者
  - `folder`: 所属文件夹
  - `versions`: 版本历史
  - `permissions`: 权限列表

#### FolderEntity (文件夹实体)

- **字段**：
  - `id`: 文件夹ID
  - `name`: 文件夹名称
  - `path`: 文件夹路径（唯一）
  - `description`: 描述
  - `spaceType`: 空间类型
  - `parent`: 父文件夹
  - `children`: 子文件夹列表
  - `files`: 文件列表
  - `owner`: 所有者
  - `permissions`: 权限列表

#### FileVersion (文件版本实体)

- **字段**：
  - `id`: 版本ID
  - `file`: 关联文件
  - `versionNumber`: 版本号
  - `versionDescription`: 版本描述
  - `filePath`: 文件存储路径
  - `size`: 文件大小
  - `checksum`: 校验和
  - `gitCommitHash`: Git提交哈希
  - `createdBy`: 创建者
  - `isCurrent`: 是否为当前版本

### 3. 权限管理

#### FilePermission (文件权限实体)

- **字段**：
  - `id`: 权限ID
  - `file`: 关联文件
  - `user`: 用户（可选）
  - `role`: 角色（可选）
  - `permissionType`: 权限类型（READ, WRITE, DELETE, SHARE, ADMIN）
  - `grantedAt`: 授予时间
  - `expiresAt`: 过期时间
  - `grantedBy`: 授予者

#### FolderPermission (文件夹权限实体)

- **字段**：
  - `id`: 权限ID
  - `folder`: 关联文件夹
  - `user`: 用户（可选）
  - `role`: 角色（可选）
  - `permissionType`: 权限类型
  - `isInherited`: 是否继承
  - `grantedAt`: 授予时间
  - `expiresAt`: 过期时间
  - `grantedBy`: 授予者

### 4. 模板管理

#### Template (模板实体)

- **字段**：
  - `id`: 模板ID
  - `name`: 模板名称
  - `description`: 模板描述
  - `templateData`: 模板数据（JSON）
  - `templateType`: 模板类型（FOLDER, PROJECT, DOCUMENT）
  - `isPublic`: 是否公开
  - `createdBy`: 创建者
  - `templateFiles`: 模板文件列表

#### TemplateFile (模板文件实体)

- **字段**：
  - `id`: 模板文件ID
  - `template`: 关联模板
  - `name`: 文件名
  - `relativePath`: 相对路径
  - `fileType`: 文件类型（TEXT, BINARY, FOLDER）
  - `content`: 文件内容
  - `sourcePath`: 源文件路径
  - `permissionsConfig`: 权限配置

### 5. 版本控制

#### VersionCommit (版本提交实体)

- **字段**：
  - `id`: 提交ID
  - `commitHash`: 提交哈希（40字符）
  - `repositoryPath`: 仓库路径
  - `message`: 提交消息
  - `author`: 作者
  - `authorEmail`: 作者邮箱
  - `commitTime`: 提交时间
  - `parentCommitHash`: 父提交哈希
  - `treeHash`: 树哈希
  - `filesChanged`: 变更文件数
  - `insertions`: 插入行数
  - `deletions`: 删除行数

### 6. 用户设置

#### UserSettings (用户设置实体)

- **字段**：
  - `id`: 设置ID
  - `user`: 关联用户
  - `theme`: 主题（LIGHT, DARK, AUTO）
  - `language`: 语言（ZH_CN, EN_US, JA_JP）
  - `timezone`: 时区
  - `dateFormat`: 日期格式
  - `timeFormat`: 时间格式
  - `fileListView`: 文件列表视图
  - `showHiddenFiles`: 显示隐藏文件
  - `autoSave`: 自动保存
  - `notificationEnabled`: 通知启用
  - `customSettings`: 自定义设置（JSON）

### 7. 审计日志

#### AuditLog (审计日志实体)

- **字段**：
  - `id`: 日志ID
  - `user`: 操作用户
  - `action`: 操作类型
  - `resourcePath`: 资源路径
  - `resourceType`: 资源类型
  - `ipAddress`: IP地址
  - `userAgent`: 用户代理
  - `result`: 操作结果（SUCCESS, FAILURE, PARTIAL）
  - `errorMessage`: 错误消息
  - `requestData`: 请求数据
  - `responseData`: 响应数据
  - `executionTime`: 执行时间

## 数据验证

### Java验证

- 使用Jakarta Validation注解进行字段验证
- 自定义验证器：`DataIntegrityValidator`
- 验证组：`ValidationGroups`
- 文件路径验证：`@ValidFilePath`

### TypeScript验证

- 字段验证器：`FieldValidator`
- 对象验证器：`ObjectValidator`
- 预定义验证器：`validators`
- 验证工具函数：`validateFilePath`, `validateFileSize`, `validatePasswordStrength`

## 数据转换

### Java映射

- 实体映射器：`EntityMapper`
- DTO类：`UserDTO`, `FileDTO`, `RoleDTO`等
- 基础DTO：`BaseDTO`

### TypeScript映射

- 数据映射器：`UserMapper`, `FileMapper`, `FolderMapper`等
- 数据转换工具：`DataTransformer`
- 实体工具：`EntityFactory`, `EntityComparator`

## 常量定义

### Java常量

- 系统常量：`LyraConstants`
- 文件常量：`FILE_CONSTANTS`
- 用户常量：`USER_CONSTANTS`
- API常量：`API_CONSTANTS`

### TypeScript常量

- 文件常量：`FILE_CONSTANTS`
- 用户常量：`USER_CONSTANTS`
- 权限常量：`PERMISSION_CONSTANTS`
- 错误消息：`ERROR_MESSAGES`

## 使用示例

### 创建用户

```java
// Java
User user = new User();
user.setUsername("testuser");
user.setEmail("test@example.com");
user.setDisplayName("Test User");
user.setAuthProvider(User.AuthProvider.LOCAL);

ValidationResult result = validator.validateUser(user, true);
if (result.isValid()) {
    userRepository.save(user);
}
```

```typescript
// TypeScript
const userValidator = validators.user();
const userData = {
    username: 'testuser',
    email: 'test@example.com',
    displayName: 'Test User'
};

const result = userValidator.validate(userData);
if (result.valid) {
    await userService.create(userData);
}
```

### 文件上传验证

```java
// Java
FileEntity file = new FileEntity();
file.setName("document.pdf");
file.setPath("/documents/document.pdf");
file.setSize(1024000L);

ValidationResult result = validator.validateFile(file, true);
```

```typescript
// TypeScript
const fileValidator = validators.file();
const fileData = {
    name: 'document.pdf',
    path: '/documents/document.pdf',
    size: 1024000
};

const result = fileValidator.validate(fileData);
```

## 最佳实践

1. **数据验证**：始终在保存前验证数据完整性
2. **权限检查**：在操作前检查用户权限
3. **审计日志**：记录所有重要操作
4. **版本控制**：为重要文件启用版本控制
5. **错误处理**：提供清晰的错误消息
6. **性能优化**：使用适当的查询和缓存策略

## 扩展性

数据模型设计考虑了以下扩展性：

- 插件系统支持
- 多种认证方式
- 灵活的权限模型
- 可配置的模板系统
- 多种版本控制策略
- 国际化支持
