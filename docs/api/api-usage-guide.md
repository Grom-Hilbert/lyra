# Lyra 云盘系统 API 使用指南

## 快速开始

### 1. 获取访问令牌

```bash
# 用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "admin123"
  }'
```

响应示例：

```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@lyra.com",
      "displayName": "系统管理员",
      "roles": ["ADMIN"]
    }
  }
}
```

### 2. 使用访问令牌

在后续请求中添加Authorization头：

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## 核心功能使用示例

### 文件上传

#### 单文件上传

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/file.pdf" \
  -F "spaceId=1" \
  -F "folderId=1" \
  -F "description=重要文档"
```

#### 分块上传（大文件）

```bash
# 1. 初始化分块上传
curl -X POST http://localhost:8080/api/files/upload/chunked/init \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "large-file.zip",
    "fileSize": 104857600,
    "chunkSize": 1048576,
    "spaceId": 1,
    "folderId": 1
  }'

# 2. 上传文件块
curl -X POST http://localhost:8080/api/files/upload/chunked/chunk \
  -H "Authorization: Bearer <token>" \
  -F "uploadId=<upload-id>" \
  -F "chunkNumber=1" \
  -F "chunk=@chunk-1.bin"

# 3. 完成上传
curl -X POST http://localhost:8080/api/files/upload/chunked/complete \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "uploadId": "<upload-id>",
    "totalChunks": 100
  }'
```

### 文件管理

#### 获取文件列表

```bash
curl -X GET "http://localhost:8080/api/files/space/1?page=0&size=20&sort=updatedAt&direction=desc" \
  -H "Authorization: Bearer <token>"
```

#### 下载文件

```bash
curl -X GET http://localhost:8080/api/files/123/download \
  -H "Authorization: Bearer <token>" \
  -o downloaded-file.pdf
```

#### 文件操作

```bash
# 复制文件
curl -X POST http://localhost:8080/api/files/123/copy \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "targetFolderId": 2,
    "newName": "copy-of-file.pdf"
  }'

# 移动文件
curl -X PUT http://localhost:8080/api/files/123/move \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "targetFolderId": 3
  }'
```

### 文件夹管理

#### 创建文件夹

```bash
curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "新文件夹",
    "spaceId": 1,
    "parentFolderId": 1
  }'
```

#### 获取文件夹树

```bash
curl -X GET "http://localhost:8080/api/folders/tree?spaceId=1&maxDepth=3" \
  -H "Authorization: Bearer <token>"
```

### 搜索功能

#### 执行搜索

```bash
curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "重要文档",
    "type": "ALL",
    "spaceId": 1,
    "filters": {
      "fileType": "pdf",
      "dateRange": {
        "start": "2024-01-01",
        "end": "2024-12-31"
      }
    }
  }'
```

### 文件预览

#### 获取文件预览

```bash
curl -X GET http://localhost:8080/api/preview/file/123 \
  -H "Authorization: Bearer <token>"
```

#### 获取缩略图

```bash
curl -X GET http://localhost:8080/api/preview/thumbnail/123 \
  -H "Authorization: Bearer <token>" \
  -o thumbnail.jpg
```

## 管理员功能

### 用户管理

#### 获取用户列表

```bash
curl -X GET "http://localhost:8080/api/admin/system/users?page=0&size=20&keyword=admin" \
  -H "Authorization: Bearer <admin-token>"
```

#### 创建用户

```bash
curl -X POST http://localhost:8080/api/admin/system/users \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "password123",
    "displayName": "新用户",
    "roles": ["USER"]
  }'
```

#### 批量用户操作

```bash
curl -X POST http://localhost:8080/api/admin/system/users/batch \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [1, 2, 3],
    "operation": "ACTIVATE"
  }'
```

### 系统监控

#### 获取用户统计

```bash
curl -X GET "http://localhost:8080/api/admin/statistics/users?period=month" \
  -H "Authorization: Bearer <admin-token>"
```

#### 获取系统统计

```bash
curl -X GET http://localhost:8080/api/admin/statistics/system \
  -H "Authorization: Bearer <admin-token>"
```

### 配置管理

#### 获取系统配置

```bash
curl -X GET http://localhost:8080/api/admin/config \
  -H "Authorization: Bearer <admin-token>"
```

#### 更新配置

```bash
curl -X PUT http://localhost:8080/api/admin/config \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "storage.maxFileSize": "200MB",
    "security.jwt.expiration": "48h"
  }'
```

## WebDAV 使用

### 配置WebDAV客户端

#### Windows 文件资源管理器

1. 打开文件资源管理器
2. 右键"此电脑" -> "映射网络驱动器"
3. 输入地址：`http://localhost:8080/webdav`
4. 输入用户名和密码

#### macOS Finder

1. 打开Finder
2. 按Cmd+K
3. 输入地址：`http://localhost:8080/webdav`
4. 输入用户名和密码

#### Linux (davfs2)

```bash
# 安装davfs2
sudo apt-get install davfs2

# 挂载WebDAV
sudo mount -t davfs http://localhost:8080/webdav /mnt/lyra
```

### WebDAV命令行操作

#### 使用curl

```bash
# 列出文件
curl -X PROPFIND http://localhost:8080/webdav/ \
  -u "username:password" \
  -H "Depth: 1"

# 上传文件
curl -X PUT http://localhost:8080/webdav/file.txt \
  -u "username:password" \
  --data-binary @local-file.txt

# 下载文件
curl -X GET http://localhost:8080/webdav/file.txt \
  -u "username:password" \
  -o downloaded-file.txt

# 创建文件夹
curl -X MKCOL http://localhost:8080/webdav/newfolder/ \
  -u "username:password"
```

## 错误处理

### 常见错误码

#### 401 未授权

```json
{
  "success": false,
  "message": "访问令牌无效或已过期",
  "timestamp": 1642780800000
}
```

解决方案：

1. 检查令牌是否正确
2. 检查令牌是否过期
3. 使用refresh token刷新令牌

#### 403 权限不足

```json
{
  "success": false,
  "message": "权限不足，无法访问此资源",
  "timestamp": 1642780800000
}
```

解决方案：

1. 检查用户角色权限
2. 联系管理员分配权限

#### 400 请求参数错误

```json
{
  "success": false,
  "message": "请求参数验证失败",
  "errors": ["文件名不能为空", "文件大小超出限制"],
  "timestamp": 1642780800000
}
```

解决方案：

1. 检查请求参数格式
2. 验证必填字段
3. 检查参数值范围

### 令牌刷新

当访问令牌过期时，使用refresh token获取新令牌：

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

## 最佳实践

### 1. 安全性

- 始终使用HTTPS（生产环境）
- 定期刷新访问令牌
- 不要在URL中传递敏感信息
- 使用强密码策略

### 2. 性能优化

- 使用分页查询大量数据
- 大文件使用分块上传
- 合理使用缓存
- 避免频繁的API调用

### 3. 错误处理

- 实现重试机制
- 记录错误日志
- 提供用户友好的错误信息
- 监控API调用状态

### 4. 开发建议

- 使用API版本控制
- 实现请求幂等性
- 添加请求超时设置
- 使用连接池管理

## SDK 和工具

### JavaScript SDK

```javascript
import { LyraClient } from '@lyra/sdk';

const client = new LyraClient({
  baseURL: 'http://localhost:8080',
  token: 'your-access-token'
});

// 上传文件
const result = await client.files.upload(file, {
  spaceId: 1,
  folderId: 1
});
```

### Python SDK

```python
from lyra_sdk import LyraClient

client = LyraClient(
    base_url='http://localhost:8080',
    token='your-access-token'
)

# 获取文件列表
files = client.files.list(space_id=1, page=0, size=20)
```

### 开发工具

- Postman集合：`/docs/api/postman-collection.json`
- OpenAPI规范：`/api-docs`
- Swagger UI：`/swagger-ui.html`
