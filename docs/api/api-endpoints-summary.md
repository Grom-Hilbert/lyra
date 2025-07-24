# Lyra 云盘系统 API 端点总览

## API 设计原则

### RESTful 设计规范

- 使用标准HTTP方法：GET、POST、PUT、DELETE
- 统一的URL路径结构：`/api/{module}/{resource}`
- 管理员接口统一前缀：`/api/admin/{module}`
- 一致的响应格式和错误处理

### 认证与授权

- JWT Bearer Token认证
- 基于角色的权限控制（RBAC）
- 细粒度权限验证

### 响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": {},
  "timestamp": 1642780800000
}
```

## API 端点分类

### 1. 认证管理 (`/api/auth`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/register` | 用户注册 | 公开 |
| POST | `/api/auth/logout` | 用户登出 | 认证用户 |
| POST | `/api/auth/refresh` | 刷新令牌 | 认证用户 |
| GET | `/api/auth/me` | 获取当前用户信息 | 认证用户 |
| PUT | `/api/auth/profile` | 更新用户资料 | 认证用户 |
| PUT | `/api/auth/password` | 修改密码 | 认证用户 |
| POST | `/api/auth/password/reset-request` | 请求重置密码 | 公开 |
| POST | `/api/auth/password/reset-confirm` | 确认重置密码 | 公开 |
| POST | `/api/auth/email/verify` | 验证邮箱 | 认证用户 |
| POST | `/api/auth/email/resend` | 重发验证邮件 | 认证用户 |

### 2. 文件管理 (`/api/files`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| POST | `/api/files/upload` | 单文件上传 | 认证用户 |
| POST | `/api/files/upload/chunked/init` | 初始化分块上传 | 认证用户 |
| POST | `/api/files/upload/chunked/chunk` | 上传文件块 | 认证用户 |
| POST | `/api/files/upload/chunked/complete` | 完成分块上传 | 认证用户 |
| GET | `/api/files/{fileId}` | 获取文件信息 | 认证用户 |
| GET | `/api/files/{fileId}/download` | 下载文件 | 认证用户 |
| PUT | `/api/files/{fileId}` | 更新文件信息 | 文件所有者 |
| DELETE | `/api/files/{fileId}` | 删除文件 | 文件所有者 |
| POST | `/api/files/{fileId}/copy` | 复制文件 | 认证用户 |
| PUT | `/api/files/{fileId}/move` | 移动文件 | 文件所有者 |
| GET | `/api/files/space/{spaceId}` | 获取空间文件列表 | 空间成员 |
| GET | `/api/files/space/{spaceId}/statistics` | 获取文件统计 | 空间成员 |

### 3. 文件夹管理 (`/api/folders`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| POST | `/api/folders` | 创建文件夹 | 认证用户 |
| GET | `/api/folders` | 获取文件夹列表 | 认证用户 |
| GET | `/api/folders/tree` | 获取文件夹树 | 认证用户 |
| GET | `/api/folders/{folderId}` | 获取文件夹详情 | 认证用户 |
| PUT | `/api/folders/{folderId}` | 更新文件夹 | 文件夹所有者 |
| DELETE | `/api/folders/{folderId}` | 删除文件夹 | 文件夹所有者 |

### 4. 搜索功能 (`/api/search`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| POST | `/api/search` | 执行搜索 | 认证用户 |
| GET | `/api/search/suggestions` | 获取搜索建议 | 认证用户 |
| GET | `/api/search/history` | 获取搜索历史 | 认证用户 |
| DELETE | `/api/search/history` | 清空搜索历史 | 认证用户 |

### 5. 文件预览 (`/api/preview`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| GET | `/api/preview/file/{fileId}` | 获取文件预览 | 认证用户 |
| GET | `/api/preview/thumbnail/{fileId}` | 获取缩略图 | 认证用户 |
| GET | `/api/preview/supported-types` | 获取支持的预览类型 | 认证用户 |

### 6. 在线编辑 (`/api/editor`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| POST | `/api/editor/session/start` | 开始编辑会话 | 认证用户 |
| PUT | `/api/editor/session/{sessionId}/save` | 保存编辑内容 | 会话所有者 |
| DELETE | `/api/editor/session/{sessionId}` | 结束编辑会话 | 会话所有者 |
| GET | `/api/editor/supported-types` | 获取支持的编辑类型 | 认证用户 |

### 7. 系统管理 (`/api/admin/system`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| GET | `/api/admin/system/users` | 获取用户列表 | 管理员 |
| POST | `/api/admin/system/users` | 创建用户 | 管理员 |
| GET | `/api/admin/system/users/{userId}` | 获取用户详情 | 管理员 |
| PUT | `/api/admin/system/users/{userId}` | 更新用户信息 | 管理员 |
| DELETE | `/api/admin/system/users/{userId}` | 删除用户 | 管理员 |
| POST | `/api/admin/system/users/batch` | 批量用户操作 | 管理员 |
| GET | `/api/admin/system/roles` | 获取角色列表 | 管理员 |
| POST | `/api/admin/system/roles` | 创建角色 | 管理员 |
| PUT | `/api/admin/system/roles/{roleId}` | 更新角色 | 管理员 |
| DELETE | `/api/admin/system/roles/{roleId}` | 删除角色 | 管理员 |

### 8. 系统监控 (`/api/admin/statistics`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| GET | `/api/admin/statistics/users` | 获取用户统计 | 管理员 |
| GET | `/api/admin/statistics/files` | 获取文件统计 | 管理员 |
| GET | `/api/admin/statistics/storage` | 获取存储统计 | 管理员 |
| GET | `/api/admin/statistics/system` | 获取系统统计 | 管理员 |
| GET | `/api/admin/statistics/dashboard` | 获取仪表板数据 | 管理员 |

### 9. 配置管理 (`/api/admin/config`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| GET | `/api/admin/config` | 获取系统配置 | 管理员 |
| PUT | `/api/admin/config` | 更新系统配置 | 管理员 |
| POST | `/api/admin/config/reload` | 重载配置 | 管理员 |
| GET | `/api/admin/config/validate` | 验证配置 | 管理员 |

### 10. 缓存管理 (`/api/admin/cache`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| GET | `/api/admin/cache/statistics` | 获取缓存统计 | 管理员 |
| DELETE | `/api/admin/cache/{cacheName}` | 清理指定缓存 | 管理员 |
| DELETE | `/api/admin/cache/all` | 清理所有缓存 | 管理员 |
| POST | `/api/admin/cache/warm-up` | 缓存预热 | 管理员 |

### 11. WebDAV 协议 (`/webdav`)

| 方法 | 端点 | 描述 | 权限要求 |
|------|------|------|----------|
| PROPFIND | `/webdav/**` | 获取资源属性 | 认证用户 |
| GET | `/webdav/**` | 下载文件 | 认证用户 |
| PUT | `/webdav/**` | 上传文件 | 认证用户 |
| DELETE | `/webdav/**` | 删除资源 | 认证用户 |
| MKCOL | `/webdav/**` | 创建集合 | 认证用户 |
| COPY | `/webdav/**` | 复制资源 | 认证用户 |
| MOVE | `/webdav/**` | 移动资源 | 认证用户 |
| OPTIONS | `/webdav/**` | 获取支持的方法 | 认证用户 |

## 监控端点

### Spring Boot Actuator (`/actuator`)

| 端点 | 描述 | 权限要求 |
|------|------|----------|
| `/actuator/health` | 健康检查 | 公开 |
| `/actuator/info` | 应用信息 | 公开 |
| `/actuator/metrics` | 系统指标 | 管理员 |
| `/actuator/prometheus` | Prometheus指标 | 管理员 |

## API 版本控制

当前版本：`v1.0.0`

### 版本策略

- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

### 版本标识

- HTTP Header: `API-Version: 1.0.0`
- URL路径: `/api/v1/...` (未来版本)

## 错误处理

### 标准错误响应

```json
{
  "success": false,
  "message": "错误描述",
  "errors": ["详细错误信息"],
  "timestamp": 1642780800000
}
```

### HTTP状态码

- `200` - 成功
- `201` - 创建成功
- `400` - 请求参数错误
- `401` - 未授权
- `403` - 权限不足
- `404` - 资源不存在
- `409` - 资源冲突
- `429` - 请求频率过高
- `500` - 服务器内部错误

## 分页和排序

### 分页参数

- `page`: 页码（从0开始）
- `size`: 每页大小（默认20，最大100）

### 排序参数

- `sort`: 排序字段
- `direction`: 排序方向（asc/desc）

### 分页响应

```json
{
  "success": true,
  "data": [...],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20
}
```

## 安全考虑

### 认证

- JWT Token有效期：24小时
- Refresh Token有效期：7天
- 支持Token刷新机制

### 权限控制

- 基于角色的访问控制（RBAC）
- 资源级权限验证
- 操作级权限检查

### 安全头

- `Authorization: Bearer <token>`
- `Content-Type: application/json`
- `X-Requested-With: XMLHttpRequest`

## 限流策略

### API限流

- 认证用户：1000请求/小时
- 匿名用户：100请求/小时
- 管理员：无限制

### 文件上传限流

- 单文件最大：100MB
- 每用户每日上传：1GB
- 并发上传数：5个

## 开发工具

### API文档

- OpenAPI 3.0规范
- Swagger UI: `/swagger-ui.html`
- API文档: `/api-docs`

### 测试工具

- Postman集合
- 自动化测试套件
- 性能测试脚本
