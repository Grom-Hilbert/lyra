import { BaseEntity, SpaceType } from './common';
import { User } from './user';
import { FileEntity } from './file';
import { FolderPermission } from './permission';

/**
 * 文件夹接口
 */
export interface FolderEntity extends BaseEntity {
  name: string;
  path: string;
  description?: string;
  spaceType: SpaceType;
  parentId?: number;
  parent?: FolderEntity;
  children?: FolderEntity[];
  files?: FileEntity[];
  owner: User;
  permissions?: FolderPermission[];
}

/**
 * 文件夹信息（简化版）
 */
export interface FolderInfo {
  id: number;
  name: string;
  path: string;
  description?: string;
  spaceType: SpaceType;
  parentId?: number;
  owner: string;
  createdAt: string;
  updatedAt?: string;
  permissions: string[];
  childrenCount: number;
  filesCount: number;
  totalSize: number;
  isShared: boolean;
}

/**
 * 文件夹创建请求
 */
export interface CreateFolderRequest {
  name: string;
  parentId?: number;
  parentPath?: string;
  description?: string;
  spaceType: SpaceType;
  templateId?: number;
  versionControlType?: 'NONE' | 'BASIC' | 'ADVANCED';
}

/**
 * 文件夹更新请求
 */
export interface UpdateFolderRequest {
  name?: string;
  description?: string;
}

/**
 * 文件夹移动请求
 */
export interface MoveFolderRequest {
  targetParentId?: number;
  targetPath?: string;
  newName?: string;
}

/**
 * 文件夹复制请求
 */
export interface CopyFolderRequest {
  targetParentId?: number;
  targetPath?: string;
  newName?: string;
  copyContents?: boolean;
  copyPermissions?: boolean;
}

/**
 * 文件夹内容列表请求
 */
export interface ListFolderContentsRequest {
  folderId?: number;
  path?: string;
  includeFiles?: boolean;
  includeFolders?: boolean;
  sortBy?: 'NAME' | 'SIZE' | 'CREATED_AT' | 'UPDATED_AT';
  sortDirection?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
}

/**
 * 文件夹内容响应
 */
export interface FolderContentsResponse {
  folder: FolderInfo;
  folders: FolderInfo[];
  files: FileEntity[];
  totalFolders: number;
  totalFiles: number;
  totalSize: number;
  breadcrumbs: Array<{
    id: number;
    name: string;
    path: string;
  }>;
}

/**
 * 文件夹树节点
 */
export interface FolderTreeNode {
  id: number;
  name: string;
  path: string;
  spaceType: SpaceType;
  parentId?: number;
  hasChildren: boolean;
  children?: FolderTreeNode[];
  expanded?: boolean;
  selected?: boolean;
  permissions: string[];
}

/**
 * 文件夹统计信息
 */
export interface FolderStatistics {
  folderId: number;
  totalFolders: number;
  totalFiles: number;
  totalSize: number;
  lastModified: string;
  fileTypeDistribution: Array<{
    mimeType: string;
    count: number;
    size: number;
  }>;
  sizeDistribution: Array<{
    range: string;
    count: number;
  }>;
}

/**
 * 文件夹分享请求
 */
export interface ShareFolderRequest {
  folderId: number;
  shareType: 'PUBLIC' | 'PRIVATE';
  expiresAt?: string;
  password?: string;
  allowUpload?: boolean;
  allowDownload?: boolean;
  allowPreview?: boolean;
}

/**
 * 文件夹搜索请求
 */
export interface FolderSearchRequest {
  keyword: string;
  parentPath?: string;
  spaceType?: SpaceType;
  owner?: string;
  dateFrom?: string;
  dateTo?: string;
}