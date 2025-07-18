/**
 * 通用类型定义
 */

/**
 * 基础实体接口
 */
export interface BaseEntity {
  id: number;
  createdAt: string;
  updatedAt?: string;
}

/**
 * 分页请求参数
 */
export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

/**
 * 分页响应数据
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/**
 * API响应包装器
 */
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  timestamp: string;
}

/**
 * 错误响应
 */
export interface ErrorResponse {
  success: false;
  errorCode: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
}

/**
 * 空间类型枚举
 */
export enum SpaceType {
  ENTERPRISE = 'ENTERPRISE',
  PERSONAL = 'PERSONAL'
}

/**
 * 版本控制类型枚举
 */
export enum VersionControlType {
  NONE = 'NONE',
  BASIC = 'BASIC',
  ADVANCED = 'ADVANCED'
}

/**
 * 权限类型枚举
 */
export enum PermissionType {
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  SHARE = 'SHARE',
  ADMIN = 'ADMIN'
}

/**
 * 文件上传进度
 */
export interface UploadProgress {
  loaded: number;
  total: number;
  percentage: number;
}

/**
 * 文件上传选项
 */
export interface UploadOptions {
  onProgress?: (progress: UploadProgress) => void;
  onSuccess?: (response: any) => void;
  onError?: (error: Error) => void;
  chunkSize?: number;
  concurrent?: number;
}