# Lyra API 文档

## 概述

这里包含了Lyra云盘系统的完整API文档，采用OpenAPI 3.0规范编写。

## 文档文件

### 主要文档

- `openapi-backup.yaml` - 原始完整文档（1800+行，包含所有API定义）
- `openapi-new.yaml` - 新的简化主文件示例（300行，展示拆分后的结构）

### 文档结构

```plaintext
├── openapi-backup.yaml          # 原始完整文档
├── openapi-new.yaml             # 简化主文件示例
├── components/                  # 组件定义（计划中）
├── paths/                       # API路径定义（计划中）
└── tags/                        # 标签定义（计划中）
```

## 使用方式

### 1. 查看完整API文档

使用原始完整文档：

```bash
# 启动Swagger UI
swagger-ui-serve openapi-backup.yaml

# 或使用在线工具
# 访问 https://editor.swagger.io/ 并导入 openapi-backup.yaml
```

### 2. 开发环境集成

在Spring Boot应用中，API文档会自动生成：

```plaintext
# 本地开发环境
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs
```

### 3. 文档验证

```bash
# 验证OpenAPI文档格式
openapi-generator validate -i openapi-backup.yaml

# 生成客户端代码
openapi-generator generate -i openapi-backup.yaml -g java -o ./client
```

## API 概览

### 核心模块

1. **认证管理** (`/api/auth/**`)
   - 用户登录、注册
   - JWT令牌管理
   - 密码重置

2. **文件管理** (`/api/files/**`)
   - 文件上传、下载
   - 文件信息管理
   - 版本控制

3. **文件夹管理** (`/api/folders/**`)
   - 文件夹CRUD操作
   - 文件夹树结构
   - 权限管理

4. **搜索功能** (`/api/search/**`)
   - 全文搜索
   - 高级筛选
   - 搜索建议

5. **文件预览** (`/api/preview/**`)
   - 多格式预览
   - 缩略图生成
   - 在线查看

6. **在线编辑** (`/api/editor/**`)
   - 文本编辑
   - 协同编辑
   - 版本控制

### 管理员模块

7. **系统管理** (`/api/admin/system/**`)
   - 用户管理
   - 角色权限管理
   - 批量操作

8. **系统监控** (`/api/admin/statistics/**`)
   - 用户统计
   - 系统性能监控
   - 业务指标分析

9. **配置管理** (`/api/admin/config/**`)
   - 系统配置查询
   - 动态配置更新
   - 配置验证

10. **缓存管理** (`/api/admin/cache/**`)
    - 缓存统计
    - 缓存清理
    - 性能监控

### 协议支持

11. **WebDAV** (`/webdav/**`)
    - 标准WebDAV协议
    - 文件同步
    - 客户端集成

## 认证方式

### JWT Bearer Token

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 获取令牌

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "admin123"
  }'
```

## 响应格式

### 成功响应

```json
{
  "success": true,
  "message": "操作成功",
  "data": {},
  "timestamp": 1642780800000
}
```

### 错误响应

```json
{
  "success": false,
  "message": "错误描述",
  "errors": ["详细错误信息"],
  "timestamp": 1642780800000
}
```

## 分页格式

### 请求参数

- `page`: 页码（从0开始）
- `size`: 每页大小（默认20，最大100）
- `sort`: 排序字段
- `direction`: 排序方向（asc/desc）

### 响应格式

```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

## 错误码说明

| HTTP状态码 | 说明 | 示例 |
|-----------|------|------|
| 200 | 成功 | 操作完成 |
| 201 | 创建成功 | 资源已创建 |
| 400 | 请求参数错误 | 参数验证失败 |
| 401 | 未授权 | 令牌无效或过期 |
| 403 | 权限不足 | 无访问权限 |
| 404 | 资源不存在 | 文件不存在 |
| 409 | 资源冲突 | 文件名重复 |
| 429 | 请求频率过高 | 超出限流阈值 |
| 500 | 服务器内部错误 | 系统异常 |

## 开发工具

### Postman集合

```bash
# 导入Postman集合（如果有）
# 文件位置: docs/api/postman-collection.json
```

### 代码生成

```bash
# 生成Java客户端
openapi-generator generate -i openapi-backup.yaml -g java -o ./java-client

# 生成JavaScript客户端
openapi-generator generate -i openapi-backup.yaml -g javascript -o ./js-client

# 生成Python客户端
openapi-generator generate -i openapi-backup.yaml -g python -o ./python-client
```

### 文档生成

```bash
# 生成HTML文档
openapi-generator generate -i openapi-backup.yaml -g html2 -o ./docs-html

# 生成Markdown文档
openapi-generator generate -i openapi-backup.yaml -g markdown -o ./docs-md
```

## 版本信息

- **当前版本**: 1.0.0
- **OpenAPI版本**: 3.0.3
- **最后更新**: 2025-07-24

## 更新日志

### v1.0.0 (2025-07-24)

- ✅ 完整的API文档定义
- ✅ 11个功能模块，100+个API端点
- ✅ 完整的认证和授权体系
- ✅ 系统管理和监控功能
- ✅ WebDAV协议支持
- ✅ 详细的错误处理和响应格式

## 联系方式

- **项目地址**: <https://github.com/your-org/lyra>
- **文档问题**: 请在GitHub上提交Issue
- **API支持**: <support@lyra.example.com>

## 许可证

MIT License - 详见项目根目录的LICENSE文件
