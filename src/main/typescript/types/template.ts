import { BaseEntity } from './common';
import { User } from './user';

/**
 * 模板类型枚举
 */
export enum TemplateType {
  FOLDER = 'FOLDER',
  PROJECT = 'PROJECT',
  DOCUMENT = 'DOCUMENT'
}

/**
 * 模板文件类型枚举
 */
export enum TemplateFileType {
  TEXT = 'TEXT',
  BINARY = 'BINARY',
  FOLDER = 'FOLDER'
}

/**
 * 模板接口
 */
export interface Template extends BaseEntity {
  name: string;
  description?: string;
  templateData: string;
  templateType: TemplateType;
  isPublic: boolean;
  createdBy: User;
  templateFiles: TemplateFile[];
}

/**
 * 模板文件接口
 */
export interface TemplateFile extends BaseEntity {
  templateId: number;
  name: string;
  relativePath: string;
  fileType: TemplateFileType;
  content?: string;
  sourcePath?: string;
  permissionsConfig?: string;
}

/**
 * 模板信息（简化版）
 */
export interface TemplateInfo {
  id: number;
  name: string;
  description?: string;
  templateType: TemplateType;
  isPublic: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
  filesCount: number;
  usageCount: number;
}

/**
 * 模板创建请求
 */
export interface CreateTemplateRequest {
  name: string;
  description?: string;
  templateType: TemplateType;
  isPublic?: boolean;
  templateFiles: CreateTemplateFileRequest[];
}

/**
 * 模板文件创建请求
 */
export interface CreateTemplateFileRequest {
  name: string;
  relativePath: string;
  fileType: TemplateFileType;
  content?: string;
  sourcePath?: string;
  permissionsConfig?: Record<string, any>;
}

/**
 * 模板更新请求
 */
export interface UpdateTemplateRequest {
  name?: string;
  description?: string;
  isPublic?: boolean;
  templateFiles?: UpdateTemplateFileRequest[];
}

/**
 * 模板文件更新请求
 */
export interface UpdateTemplateFileRequest {
  id?: number;
  name?: string;
  relativePath?: string;
  fileType?: TemplateFileType;
  content?: string;
  sourcePath?: string;
  permissionsConfig?: Record<string, any>;
  action?: 'CREATE' | 'UPDATE' | 'DELETE';
}

/**
 * 应用模板请求
 */
export interface ApplyTemplateRequest {
  templateId: number;
  targetPath: string;
  targetParentId?: number;
  folderName: string;
  variables?: Record<string, string>;
  applyPermissions?: boolean;
}

/**
 * 应用模板响应
 */
export interface ApplyTemplateResponse {
  folderId: number;
  folderPath: string;
  createdFiles: number;
  createdFolders: number;
  appliedPermissions: number;
  warnings: string[];
}

/**
 * 模板导出请求
 */
export interface ExportTemplateRequest {
  templateId: number;
  format: 'JSON' | 'ZIP';
  includeContent?: boolean;
}

/**
 * 模板导入请求
 */
export interface ImportTemplateRequest {
  file: File;
  name?: string;
  description?: string;
  isPublic?: boolean;
  overwriteExisting?: boolean;
}

/**
 * 模板搜索请求
 */
export interface TemplateSearchRequest {
  keyword?: string;
  templateType?: TemplateType;
  isPublic?: boolean;
  createdBy?: string;
  dateFrom?: string;
  dateTo?: string;
}

/**
 * 模板变量定义
 */
export interface TemplateVariable {
  name: string;
  description?: string;
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE';
  required: boolean;
  defaultValue?: string;
  options?: string[];
}

/**
 * 模板预览响应
 */
export interface TemplatePreviewResponse {
  structure: Array<{
    path: string;
    type: 'FILE' | 'FOLDER';
    size?: number;
    permissions?: string[];
  }>;
  variables: TemplateVariable[];
  estimatedSize: number;
}