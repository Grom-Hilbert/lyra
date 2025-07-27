# API不匹配问题记录

## 概述

本文档记录了前端API调用与后端API实现之间的不匹配问题，这些问题在任务13.5（完善文件管理主界面）开发过程中被发现。

## 发现时间

- **发现日期**: 2025-01-27
- **相关任务**: 任务13.5 - 完善文件管理主界面(FileManagerView.vue)
- **影响范围**: 前端文件管理功能

## 问题详情

### 1. 文件分享功能API缺失

#### 问题描述

前端实现了完整的文件分享功能API调用，但后端控制器中没有对应的实现。

#### 前端API调用

```typescript
// 文件分享相关API (fileApi.ts)
async createFileShare(fileId: number, data: CreateShareRequest): Promise<IApiResponse<ShareResponse>>
async getFileShares(fileId: number): Promise<IApiResponse<ShareResponse[]>>
async deleteFileShare(fileId: number, shareId: number): Promise<IApiResponse<void>>
async updateFileShare(fileId: number, shareId: number, data: UpdateShareRequest): Promise<IApiResponse<void>>
```

#### 缺失的后端端点

- `POST /api/files/{fileId}/share` - 创建文件分享链接
- `GET /api/files/{fileId}/shares` - 获取文件分享链接列表
- `DELETE /api/files/{fileId}/shares/{shareId}` - 删除文件分享链接
- `PUT /api/files/{fileId}/shares/{shareId}` - 更新文件分享链接

#### 影响功能

- FileOperationDialog.vue中的分享功能
- 右键菜单中的分享选项
- 文件详情页面的分享管理

### 2. 文件夹分享功能API缺失

#### 问题描述

前端实现了文件夹分享功能API调用，但后端控制器中没有对应的实现。

#### 前端API调用

```typescript
// 文件夹分享相关API (fileApi.ts)
async createFolderShare(folderId: number, data: CreateShareRequest): Promise<IApiResponse<ShareResponse>>
async getFolderShares(folderId: number): Promise<IApiResponse<ShareResponse[]>>
async deleteFolderShare(folderId: number, shareId: number): Promise<IApiResponse<void>>
async updateFolderShare(folderId: number, shareId: number, data: UpdateShareRequest): Promise<IApiResponse<void>>
```

#### 缺失的后端端点

- `POST /api/folders/{folderId}/share` - 创建文件夹分享链接
- `GET /api/folders/{folderId}/shares` - 获取文件夹分享链接列表
- `DELETE /api/folders/{folderId}/shares/{shareId}` - 删除文件夹分享链接
- `PUT /api/folders/{folderId}/shares/{shareId}` - 更新文件夹分享链接

#### 影响功能

- FileOperationDialog.vue中的文件夹分享功能
- 右键菜单中的文件夹分享选项

### 3. 存储配额查询API缺失

#### 问题描述

前端实现了存储配额查询功能，但后端没有暴露对应的API端点。

#### 前端API调用

```typescript
// 存储配额API (spaceApi.ts 和 fileApi.ts)
async getSpaceQuota(spaceId: number): Promise<IApiResponse<StorageQuotaResponse>>
```

#### 缺失的后端端点

- `GET /api/spaces/{spaceId}/quota` - 获取空间存储配额信息

#### 后端现状

- Space实体中已有配额相关字段（quotaLimit, quotaUsed）
- StorageQuota值对象已实现
- 相关业务逻辑已存在
- 仅缺少控制器端点暴露

#### 影响功能

- FileManagerView.vue中的存储配额显示
- 空间切换时的配额信息更新

## 已确认正确的API

### 1. 文件操作API

- ✅ 文件移动: `POST /api/files/{fileId}/move`
- ✅ 文件复制: `POST /api/files/{fileId}/copy`
- ✅ 文件重命名: `POST /api/files/{fileId}/rename`
- ✅ 文件删除: `DELETE /api/files/{fileId}`

### 2. 文件夹操作API

- ✅ 文件夹移动: `POST /api/folders/move`
- ✅ 文件夹创建: `POST /api/folders`
- ✅ 文件夹删除: `DELETE /api/folders/{folderId}`
- ✅ 文件夹更新: `PUT /api/folders/{folderId}`

### 3. 批量操作API

- ✅ 批量删除文件: `POST /api/files/batch/delete`
- ✅ 批量移动文件: `POST /api/files/batch/move`
- ✅ 批量复制文件: `POST /api/files/batch/copy`

## 解决方案建议

### 短期解决方案（推荐）

1. **禁用相关功能**: 在前端暂时禁用分享功能和存储配额显示
2. **添加TODO注释**: 在代码中标记待实现的功能
3. **用户提示**: 在界面上显示"功能开发中"的提示

### 长期解决方案

1. **实现分享功能后端API**:
   - 创建ShareController
   - 实现ShareService
   - 添加Share实体和相关数据库表

2. **实现存储配额API**:
   - 在SpaceController中添加getSpaceQuota端点
   - 利用现有的配额业务逻辑

3. **完善API文档**:
   - 更新OpenAPI规范
   - 添加新端点的文档说明

## 相关文件

### 前端文件

- `src/main/typescript/internal/apis/fileApi.ts` - 文件和文件夹API
- `src/main/typescript/internal/apis/spaceApi.ts` - 空间API
- `src/main/typescript/internal/components/FileOperationDialog.vue` - 文件操作对话框
- `src/main/typescript/internal/views/FileManagerView.vue` - 文件管理主界面

### 后端文件

- `src/main/java/tslc/beihaiyun/lyra/controller/FileController.java` - 文件控制器
- `src/main/java/tslc/beihaiyun/lyra/controller/FolderController.java` - 文件夹控制器
- `src/main/java/tslc/beihaiyun/lyra/controller/SpaceController.java` - 空间控制器（需要添加配额端点）
- `src/main/java/tslc/beihaiyun/lyra/entity/Space.java` - 空间实体（已有配额字段）

### 类型定义

- `src/main/typescript/internal/types/index.ts` - TypeScript类型定义

## 优先级

1. **高优先级**: 存储配额API - 影响用户体验，实现相对简单
2. **中优先级**: 文件分享API - 重要功能，但实现复杂度较高
3. **中优先级**: 文件夹分享API - 与文件分享功能类似

## 备注

- 这些问题不影响现有功能的正常使用
- 构建和测试都能正常通过
- 用户界面会显示相关功能，但调用API时会失败
- 建议在实现后端API之前，先在前端添加适当的错误处理和用户提示
