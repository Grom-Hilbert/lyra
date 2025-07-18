import { BaseEntity, SpaceType, VersionControlType } from './common';
import { User } from './user';
import { FilePermission } from './permission';

/**
 * 文件接口
 */
export interface FileEntity extends BaseEntity {
  name: string;
  path: string;
  mimeType?: string;
  size: number;
  checksum?: string;
  spaceType: SpaceType;
  versionControlType: VersionControlType;
  folderId?: number;
  owner: User;
  accessedAt?: string;
  versions?: FileVersion[];
  permissions?: FilePermission[];
}

/**
 * 文件版本接口
 */
export interface FileVersion extends BaseEntity {
  fileId: number;
  versionNumber: number;
  versionDescription?: string;
  filePath: string;
  size: number;
  checksum?: string;
  gitCommitHash?: string;
  createdBy: User;
  isCurrent: boolean;
}

/**
 * 文件信息（简化版）
 */
export interface FileInfo {
  id: number;
  name: string;
  path: string;
  type: 'FILE' | 'FOLDER';
  mimeType?: string;
  size: number;
  spaceType: SpaceType;
  versionControlType: VersionControlType;
  owner: string;
  createdAt: string;
  updatedAt?: string;
  accessedAt?: string;
  permissions: string[];
  isShared: boolean;
  hasVersions: boolean;
}

/**
 * 文件上传请求
 */
export interface FileUploadRequest {
  file: File;
  path: string;
  spaceType: SpaceType;
  versionControlType?: VersionControlType;
  description?: string;
  isVersionUpdate?: boolean;
}

/**
 * 文件上传响应
 */
export interface FileUploadResponse {
  fileId: number;
  name: string;
  path: string;
  size: number;
  checksum: string;
  versionNumber?: number;
  uploadTime: string;
}

/**
 * 文件下载请求
 */
export interface FileDownloadRequest {
  fileId: number;
  versionNumber?: number;
  inline?: boolean;
}

/**
 * 文件移动请求
 */
export interface MoveFileRequest {
  targetPath: string;
  targetFolderId?: number;
  newName?: string;
}

/**
 * 文件复制请求
 */
export interface CopyFileRequest {
  targetPath: string;
  targetFolderId?: number;
  newName?: string;
  copyVersions?: boolean;
}

/**
 * 文件重命名请求
 */
export interface RenameFileRequest {
  newName: string;
}

/**
 * 文件分享请求
 */
export interface ShareFileRequest {
  fileId: number;
  shareType: 'PUBLIC' | 'PRIVATE';
  expiresAt?: string;
  password?: string;
  allowDownload?: boolean;
  allowPreview?: boolean;
}

/**
 * 文件分享响应
 */
export interface ShareFileResponse {
  shareId: string;
  shareUrl: string;
  expiresAt?: string;
  createdAt: string;
}

/**
 * 文件搜索请求
 */
export interface FileSearchRequest {
  keyword: string;
  path?: string;
  spaceType?: SpaceType;
  mimeType?: string;
  owner?: string;
  dateFrom?: string;
  dateTo?: string;
  sizeMin?: number;
  sizeMax?: number;
}

/**
 * 文件预览请求
 */
export interface FilePreviewRequest {
  fileId: number;
  versionNumber?: number;
  page?: number;
  quality?: 'LOW' | 'MEDIUM' | 'HIGH';
}

/**
 * 文件预览响应
 */
export interface FilePreviewResponse {
  previewType: 'TEXT' | 'IMAGE' | 'PDF' | 'OFFICE' | 'UNSUPPORTED';
  content?: string;
  imageUrl?: string;
  pageCount?: number;
  currentPage?: number;
}

/**
 * 文件编辑请求
 */
export interface FileEditRequest {
  fileId: number;
  content: string;
  versionDescription?: string;
}

/**
 * 批量文件操作请求
 */
export interface BatchFileOperationRequest {
  fileIds: number[];
  operation: 'DELETE' | 'MOVE' | 'COPY' | 'SHARE';
  targetPath?: string;
  targetFolderId?: number;
}

/**
 * 批量操作响应
 */
export interface BatchOperationResponse {
  successCount: number;
  failureCount: number;
  results: Array<{
    fileId: number;
    success: boolean;
    message?: string;
  }>;
}