// 文件相关类型定义

export type FileStatus = 'UPLOADING' | 'PROCESSING' | 'ACTIVE' | 'DELETED' | 'QUARANTINED'

export interface FileInfo {
  id: number
  name: string
  originalName: string
  path: string
  size: number
  sizeReadable: string
  mimeType: string
  extension: string
  md5?: string
  sha256?: string
  description?: string
  tags: string[]
  folderId?: number
  folderPath: string
  spaceId: number
  spaceName: string
  ownerId: number
  ownerName: string
  isPublic: boolean
  downloadCount: number
  viewCount: number
  version: number
  isLatestVersion: boolean
  status: FileStatus
  thumbnailUrl?: string
  previewUrl?: string
  downloadUrl: string
  createdAt: string
  updatedAt: string
  uploadedAt: string
}

export interface FileUploadRequest {
  file: File
  spaceId: number
  folderId?: number
  description?: string
  tags?: string[]
  isPublic?: boolean
  overwrite?: boolean
}

export interface FileUploadResponse {
  file: FileInfo
  uploadId?: string
  message: string
}

export interface ChunkedUploadInitRequest {
  filename: string
  fileSize: number
  chunkSize: number
  mimeType?: string
  md5?: string
  spaceId: number
  folderId?: number
  description?: string
}

export interface ChunkedUploadInitResponse {
  uploadId: string
  totalChunks: number
  chunkSize: number
  uploadUrls?: Array<{
    chunkNumber: number
    uploadUrl: string
  }>
}

export interface FileUpdateRequest {
  name?: string
  description?: string
  tags?: string[]
  folderId?: number
  isPublic?: boolean
}

export interface FileCopyRequest {
  targetFolderId: number
  targetSpaceId?: number
  newName?: string
  copyVersions?: boolean
}

export interface FileMoveRequest {
  targetFolderId: number
  targetSpaceId?: number
}

export interface FileSearchRequest {
  query?: string
  spaceId?: number
  folderId?: number
  mimeType?: string
  extension?: string
  tags?: string[]
  sizeRange?: {
    min?: number
    max?: number
  }
  dateRange?: {
    start?: string
    end?: string
  }
  ownerId?: number
}

export interface FileStatistics {
  totalFiles: number
  totalSize: number
  totalSizeReadable: string
  averageFileSize: number
  filesByType: Record<string, number>
  filesBySize: {
    small: number
    medium: number
    large: number
  }
  uploadTrend: Array<{
    date: string
    count: number
    size: number
  }>
  topFileTypes: Array<{
    mimeType: string
    extension: string
    count: number
    totalSize: number
  }>
}

// 文件夹相关类型定义

export interface FolderInfo {
  id: number
  name: string
  description?: string
  path: string
  fullPath: string
  parentId?: number
  parentPath?: string
  spaceId: number
  spaceName: string
  ownerId: number
  ownerName: string
  level: number
  isRoot: boolean
  isPublic: boolean
  fileCount: number
  folderCount: number
  totalFileCount: number
  totalSize: number
  totalSizeReadable: string
  tags: string[]
  permissions: {
    read: boolean
    write: boolean
    delete: boolean
    share: boolean
  }
  createdAt: string
  updatedAt: string
}

export interface CreateFolderRequest {
  name: string
  description?: string
  parentId?: number
  spaceId: number
  tags?: string[]
  isPublic?: boolean
}

export interface UpdateFolderRequest {
  name?: string
  description?: string
  tags?: string[]
  isPublic?: boolean
}

export interface MoveFolderRequest {
  targetParentId?: number
  targetSpaceId?: number
}

export interface CopyFolderRequest {
  targetParentId: number
  targetSpaceId?: number
  newName?: string
  copyFiles?: boolean
  copySubfolders?: boolean
}

export interface FolderTreeNode {
  id: number
  name: string
  path: string
  parentId?: number
  level: number
  isRoot: boolean
  hasChildren: boolean
  fileCount: number
  folderCount: number
  isExpanded?: boolean
  children?: FolderTreeNode[]
  permissions: {
    read: boolean
    write: boolean
    delete: boolean
  }
}

export interface FolderContent {
  folder: FolderInfo
  subfolders: FolderInfo[]
  files: FileInfo[]
  breadcrumb: Array<{
    id: number
    name: string
    path: string
  }>
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
}

export interface FolderSearchRequest {
  query?: string
  spaceId?: number
  parentId?: number
  tags?: string[]
  isPublic?: boolean
  dateRange?: {
    start?: string
    end?: string
  }
  ownerId?: number
}

export interface BatchFolderOperationRequest {
  folderIds: number[]
  operation: 'DELETE' | 'MOVE' | 'COPY' | 'SET_PUBLIC' | 'SET_PRIVATE'
  targetParentId?: number
  targetSpaceId?: number
}

// 批量文件操作
export interface BatchFileOperationRequest {
  fileIds: number[]
  operation: 'DELETE' | 'MOVE' | 'COPY' | 'SET_PUBLIC' | 'SET_PRIVATE'
  targetFolderId?: number
  targetSpaceId?: number
}

// 上传进度
export interface UploadProgress {
  id: string
  filename: string
  name?: string
  progress: number
  status: 'pending' | 'uploading' | 'completed' | 'error'
  error?: string
  file?: File
  size?: number
}

// 文件预览
export interface FilePreview {
  id: number
  type: 'image' | 'video' | 'audio' | 'document' | 'text' | 'code' | 'archive' | 'other'
  url: string
  thumbnailUrl?: string
  canPreview: boolean
  canEdit: boolean
}

// 空间信息（简化版）
export interface SpaceInfo {
  id: number
  name: string
  type: 'PERSONAL' | 'TEAM' | 'PUBLIC'
  description?: string
  isDefault: boolean
  quota: {
    used: number
    total: number
    usedReadable: string
    totalReadable: string
    usagePercentage: number
  }
}

// 排序选项
export interface SortOption {
  field: 'name' | 'size' | 'createdAt' | 'updatedAt' | 'extension' | 'type'
  direction: 'asc' | 'desc'
}

// 过滤选项
export interface FilterOption {
  type?: string[]
  size?: {
    min?: number
    max?: number
  }
  dateRange?: {
    start?: string
    end?: string
  }
  tags?: string[]
  isPublic?: boolean
}

// 分页参数
export interface PaginationParams {
  page: number
  size: number
  sort?: SortOption
}

// API响应通用格式
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  timestamp: string
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
} 