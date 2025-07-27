// ==================== 用户相关类型 ====================
export interface IUser {
  id: number
  username: string
  email: string
  displayName: string
  avatar?: string
  roles: string[]
  permissions?: string[]
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED'
  emailVerified: boolean
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface ILoginForm {
  usernameOrEmail: string
  password: string
  rememberMe?: boolean
}

export interface IRegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  displayName: string
  inviteCode?: string
}

// ==================== 认证相关类型 ====================
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: IUser
}

export interface RegisterResponse {
  user: IUser
  requiresEmailVerification: boolean
  message: string
}

export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

// ==================== 文件相关类型 ====================
export interface IFileInfo {
  id: number
  filename: string
  originalName: string
  path: string
  sizeBytes: number
  mimeType: string
  fileHash: string
  version: number
  isPublic: boolean
  downloadCount: number
  spaceId: number
  folderId?: number
  uploaderId: number
  status: 'ACTIVE' | 'DELETED' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}

// ==================== 文件夹相关类型 ====================
export interface IFolderInfo {
  id: number
  name: string
  path: string
  parentId?: number
  spaceId: number
  level: number
  isRoot: boolean
  fileCount: number
  sizeBytes: number
  createdAt: string
  updatedAt: string
}

export interface CreateFolderRequest {
  name: string
  spaceId: number
  parentFolderId?: number
}

export interface FolderTreeNode {
  folder: IFolderInfo
  children: FolderTreeNode[]
}

// ==================== 空间相关类型 ====================
export interface ISpace {
  id: number
  name: string
  description?: string
  type: 'personal' | 'shared' | 'enterprise'
  ownerId: number
  members: ISpaceMember[]
  settings: ISpaceSettings
  createdAt: string
  updatedAt: string
}

export interface ISpaceMember {
  userId: number
  username: string
  displayName: string
  role: string
  permissions: string[]
  joinedAt: string
}

export interface ISpaceSettings {
  isPublic: boolean
  allowGuest: boolean
  maxFileSize: number
  quota: number
  versionControl: 'none' | 'basic' | 'git'
}

// ==================== 文件上传相关类型 ====================
export interface FileUploadRequest {
  file: File
  spaceId: number
  folderId?: number
  description?: string
}

export interface FileUploadResponse {
  file: IFileInfo
}

export interface FileSearchRequest {
  spaceId: number
  keyword?: string
  mimeType?: string
  includeDeleted?: boolean
  page?: number
  size?: number
}

// ==================== 权限相关类型 ====================
export interface IPermission {
  resource: string
  action: string
  allowed: boolean
}

export interface IRole {
  id: number
  name: string
  description?: string
  permissions: string[]
  isSystem: boolean
}

// ==================== API响应类型 ====================
export interface IApiResponse<T = any> {
  success: boolean
  data?: T
  message?: string
  errors?: string[]
  timestamp: number
}

export interface IPagedResponse<T = any> extends IApiResponse<T[]> {
  pagination: {
    page: number
    size: number
    total: number
    totalPages: number
  }
}

// ==================== 路由相关类型 ====================
export interface IRouteMenuItem {
  name: string
  path: string
  title: string
  icon?: string
  children?: IRouteMenuItem[]
  meta?: {
    requiresAuth?: boolean
    roles?: string[]
    hidden?: boolean
  }
}

// ==================== 系统配置类型 ====================
export interface ISystemConfig {
  siteName: string
  version: string
  features: {
    registration: boolean
    guestAccess: boolean
    webdav: boolean
    versionControl: boolean
  }
  limits: {
    maxFileSize: number
    maxUsers: number
    quotaPerUser: number
  }
}

// ==================== 上传相关类型 ====================
export interface IUploadProgress {
  fileId: number
  fileName: string
  loaded: number
  total: number
  percentage: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  error?: string
}

export interface ChunkedUploadInitRequest {
  filename: string
  fileSize: number
  chunkSize: number
  spaceId: number
  folderId?: number
  description?: string
}

export interface ChunkedUploadInitResponse {
  uploadId: string
  chunkSize: number
  totalChunks: number
}

export interface ChunkedUploadChunkRequest {
  uploadId: string
  chunkIndex: number
  chunk: Blob
}

export interface ChunkedUploadCompleteRequest {
  uploadId: string
}

// ==================== 管理员相关类型 ====================
export interface UserStatistics {
  totalUsers: number
  activeUsers: number
  newUsersToday: number
  newUsersThisWeek: number
  newUsersThisMonth: number
}

export interface SystemStatistics {
  users: UserStatistics
  storage: {
    usedSpace: number
    totalSpace: number
    usagePercentage: number
  }
}