/**
 * 数据模型接口定义
 * 定义前端数据操作的标准接口
 */

import {
  User, FileEntity, FolderEntity, Template, Role, Permission,
  FilePermission, FolderPermission, UserSettings, AuditLog,
  ValidationResult, PageResponse, ApiResponse
} from '../types';

/**
 * 基础数据服务接口
 */
export interface BaseDataService<T, CreateRequest, UpdateRequest> {
  /**
   * 获取单个实体
   */
  getById(id: number): Promise<ApiResponse<T>>;

  /**
   * 获取所有实体（分页）
   */
  getAll(page?: number, size?: number): Promise<ApiResponse<PageResponse<T>>>;

  /**
   * 创建实体
   */
  create(request: CreateRequest): Promise<ApiResponse<T>>;

  /**
   * 更新实体
   */
  update(id: number, request: UpdateRequest): Promise<ApiResponse<T>>;

  /**
   * 删除实体
   */
  delete(id: number): Promise<ApiResponse<void>>;

  /**
   * 批量删除实体
   */
  batchDelete(ids: number[]): Promise<ApiResponse<void>>;
}

/**
 * 用户数据服务接口
 */
export interface UserDataService extends BaseDataService<User, any, any> {
  /**
   * 用户登录
   */
  login(username: string, password: string): Promise<ApiResponse<any>>;

  /**
   * 用户登出
   */
  logout(): Promise<ApiResponse<void>>;

  /**
   * 刷新令牌
   */
  refreshToken(refreshToken: string): Promise<ApiResponse<any>>;

  /**
   * 获取当前用户信息
   */
  getCurrentUser(): Promise<ApiResponse<User>>;

  /**
   * 修改密码
   */
  changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<void>>;

  /**
   * 搜索用户
   */
  search(criteria: any): Promise<ApiResponse<PageResponse<User>>>;

  /**
   * 检查用户名是否可用
   */
  checkUsernameAvailable(username: string): Promise<ApiResponse<boolean>>;

  /**
   * 检查邮箱是否可用
   */
  checkEmailAvailable(email: string): Promise<ApiResponse<boolean>>;

  /**
   * 获取用户权限
   */
  getUserPermissions(userId: number): Promise<ApiResponse<string[]>>;
}

/**
 * 文件数据服务接口
 */
export interface FileDataService extends BaseDataService<FileEntity, any, any> {
  /**
   * 上传文件
   */
  upload(file: File, path: string, options?: any): Promise<ApiResponse<any>>;

  /**
   * 下载文件
   */
  download(fileId: number, versionNumber?: number): Promise<Blob>;

  /**
   * 预览文件
   */
  preview(fileId: number, versionNumber?: number): Promise<ApiResponse<any>>;

  /**
   * 编辑文件内容
   */
  editContent(fileId: number, content: string, description?: string): Promise<ApiResponse<void>>;

  /**
   * 移动文件
   */
  move(fileId: number, targetPath: string, newName?: string): Promise<ApiResponse<void>>;

  /**
   * 复制文件
   */
  copy(fileId: number, targetPath: string, newName?: string): Promise<ApiResponse<void>>;

  /**
   * 重命名文件
   */
  rename(fileId: number, newName: string): Promise<ApiResponse<void>>;

  /**
   * 分享文件
   */
  share(fileId: number, options: any): Promise<ApiResponse<any>>;

  /**
   * 搜索文件
   */
  search(criteria: any): Promise<ApiResponse<PageResponse<FileEntity>>>;

  /**
   * 获取文件版本历史
   */
  getVersionHistory(fileId: number): Promise<ApiResponse<any[]>>;

  /**
   * 回滚到指定版本
   */
  revertToVersion(fileId: number, versionNumber: number): Promise<ApiResponse<void>>;

  /**
   * 批量操作文件
   */
  batchOperation(fileIds: number[], operation: string, options?: any): Promise<ApiResponse<any>>;
}

/**
 * 文件夹数据服务接口
 */
export interface FolderDataService extends BaseDataService<FolderEntity, any, any> {
  /**
   * 获取文件夹内容
   */
  getContents(folderId: number, options?: any): Promise<ApiResponse<any>>;

  /**
   * 创建文件夹
   */
  createFolder(parentId: number, name: string, options?: any): Promise<ApiResponse<FolderEntity>>;

  /**
   * 移动文件夹
   */
  move(folderId: number, targetParentId: number, newName?: string): Promise<ApiResponse<void>>;

  /**
   * 复制文件夹
   */
  copy(folderId: number, targetParentId: number, newName?: string, options?: any): Promise<ApiResponse<void>>;

  /**
   * 获取文件夹树
   */
  getTree(rootId?: number, maxDepth?: number): Promise<ApiResponse<any>>;

  /**
   * 获取面包屑导航
   */
  getBreadcrumbs(folderId: number): Promise<ApiResponse<any[]>>;

  /**
   * 搜索文件夹
   */
  search(criteria: any): Promise<ApiResponse<PageResponse<FolderEntity>>>;

  /**
   * 获取文件夹统计信息
   */
  getStatistics(folderId: number): Promise<ApiResponse<any>>;

  /**
   * 分享文件夹
   */
  share(folderId: number, options: any): Promise<ApiResponse<any>>;
}

/**
 * 权限数据服务接口
 */
export interface PermissionDataService {
  /**
   * 授予权限
   */
  grant(request: any): Promise<ApiResponse<void>>;

  /**
   * 撤销权限
   */
  revoke(request: any): Promise<ApiResponse<void>>;

  /**
   * 检查权限
   */
  check(resourceId: number, resourceType: string, action: string): Promise<ApiResponse<boolean>>;

  /**
   * 获取资源权限列表
   */
  getResourcePermissions(resourceId: number, resourceType: string): Promise<ApiResponse<any[]>>;

  /**
   * 获取用户权限列表
   */
  getUserPermissions(userId: number): Promise<ApiResponse<string[]>>;

  /**
   * 批量授予权限
   */
  batchGrant(requests: any[]): Promise<ApiResponse<void>>;

  /**
   * 批量撤销权限
   */
  batchRevoke(requests: any[]): Promise<ApiResponse<void>>;
}

/**
 * 角色数据服务接口
 */
export interface RoleDataService extends BaseDataService<Role, any, any> {
  /**
   * 分配权限给角色
   */
  assignPermissions(roleId: number, permissionIds: number[]): Promise<ApiResponse<void>>;

  /**
   * 获取角色权限
   */
  getRolePermissions(roleId: number): Promise<ApiResponse<Permission[]>>;

  /**
   * 分配角色给用户
   */
  assignToUser(userId: number, roleIds: number[]): Promise<ApiResponse<void>>;

  /**
   * 获取用户角色
   */
  getUserRoles(userId: number): Promise<ApiResponse<Role[]>>;

  /**
   * 搜索角色
   */
  search(criteria: any): Promise<ApiResponse<PageResponse<Role>>>;
}

/**
 * 模板数据服务接口
 */
export interface TemplateDataService extends BaseDataService<Template, any, any> {
  /**
   * 应用模板
   */
  apply(templateId: number, targetPath: string, options?: any): Promise<ApiResponse<any>>;

  /**
   * 预览模板
   */
  preview(templateId: number): Promise<ApiResponse<any>>;

  /**
   * 导出模板
   */
  export(templateId: number, format: string): Promise<Blob>;

  /**
   * 导入模板
   */
  import(file: File, options?: any): Promise<ApiResponse<Template>>;

  /**
   * 搜索模板
   */
  search(criteria: any): Promise<ApiResponse<PageResponse<Template>>>;

  /**
   * 获取模板使用统计
   */
  getUsageStatistics(templateId: number): Promise<ApiResponse<any>>;

  /**
   * 复制模板
   */
  duplicate(templateId: number, newName: string): Promise<ApiResponse<Template>>;
}

/**
 * 版本控制数据服务接口
 */
export interface VersionControlDataService {
  /**
   * 初始化版本控制
   */
  initialize(folderId: number, options: any): Promise<ApiResponse<void>>;

  /**
   * 提交更改
   */
  commit(repositoryPath: string, message: string, files?: string[]): Promise<ApiResponse<any>>;

  /**
   * 获取版本历史
   */
  getHistory(repositoryPath: string, options?: any): Promise<ApiResponse<any>>;

  /**
   * 比较版本
   */
  compare(repositoryPath: string, fromCommit: string, toCommit: string): Promise<ApiResponse<any>>;

  /**
   * 回滚版本
   */
  revert(repositoryPath: string, commitHash: string, options?: any): Promise<ApiResponse<void>>;

  /**
   * 获取Git状态
   */
  getStatus(repositoryPath: string): Promise<ApiResponse<any>>;

  /**
   * 同步远程仓库
   */
  sync(repositoryPath: string, options?: any): Promise<ApiResponse<any>>;

  /**
   * 创建分支
   */
  createBranch(repositoryPath: string, branchName: string): Promise<ApiResponse<void>>;

  /**
   * 切换分支
   */
  switchBranch(repositoryPath: string, branchName: string): Promise<ApiResponse<void>>;

  /**
   * 合并分支
   */
  mergeBranch(repositoryPath: string, sourceBranch: string, targetBranch: string): Promise<ApiResponse<void>>;
}

/**
 * 用户设置数据服务接口
 */
export interface UserSettingsDataService {
  /**
   * 获取用户设置
   */
  getUserSettings(): Promise<ApiResponse<UserSettings>>;

  /**
   * 更新用户设置
   */
  updateUserSettings(settings: any): Promise<ApiResponse<UserSettings>>;

  /**
   * 重置用户设置
   */
  resetUserSettings(): Promise<ApiResponse<UserSettings>>;

  /**
   * 获取系统配置
   */
  getSystemConfig(): Promise<ApiResponse<any>>;

  /**
   * 更新系统配置（管理员）
   */
  updateSystemConfig(config: any): Promise<ApiResponse<void>>;
}

/**
 * 审计日志数据服务接口
 */
export interface AuditLogDataService {
  /**
   * 获取审计日志
   */
  getLogs(criteria?: any): Promise<ApiResponse<PageResponse<AuditLog>>>;

  /**
   * 获取用户活动摘要
   */
  getUserActivitySummary(userId: number, dateRange?: any): Promise<ApiResponse<any>>;

  /**
   * 获取系统活动摘要
   */
  getSystemActivitySummary(dateRange?: any): Promise<ApiResponse<any>>;

  /**
   * 获取安全事件
   */
  getSecurityEvents(criteria?: any): Promise<ApiResponse<any[]>>;

  /**
   * 生成合规报告
   */
  generateComplianceReport(request: any): Promise<ApiResponse<any>>;

  /**
   * 导出审计日志
   */
  exportLogs(criteria: any, format: string): Promise<Blob>;
}

/**
 * 数据验证服务接口
 */
export interface DataValidationService {
  /**
   * 验证用户数据
   */
  validateUser(user: any): ValidationResult;

  /**
   * 验证文件数据
   */
  validateFile(file: any): ValidationResult;

  /**
   * 验证文件夹数据
   */
  validateFolder(folder: any): ValidationResult;

  /**
   * 验证模板数据
   */
  validateTemplate(template: any): ValidationResult;

  /**
   * 验证权限数据
   */
  validatePermission(permission: any): ValidationResult;

  /**
   * 验证文件路径
   */
  validateFilePath(path: string): ValidationResult;

  /**
   * 验证密码强度
   */
  validatePasswordStrength(password: string): ValidationResult & { strength: string };
}