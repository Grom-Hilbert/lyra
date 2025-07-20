// 用户相关类型
export interface IUser {
  id: string
  username: string
  email: string
  displayName: string
  avatar?: string
  roles: string[]
  createdAt: string
  updatedAt: string
}

export interface ILoginForm {
  username: string
  password: string
  rememberMe?: boolean
}

export interface IRegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  displayName: string
}

// 文件相关类型
export interface IFileInfo {
  id: string
  name: string
  path: string
  size: number
  type: 'file' | 'folder'
  mimeType?: string
  isVersioned: boolean
  permissions: IPermission[]
  createdAt: string
  updatedAt: string
  createdBy: string
  modifiedBy: string
}

export interface IFolder {
  id: string
  name: string
  path: string
  parentId?: string
  children: IFileInfo[]
  permissions: IPermission[]
  createdAt: string
  updatedAt: string
}

export interface ISpace {
  id: string
  name: string
  description?: string
  type: 'personal' | 'shared' | 'enterprise'
  ownerId: string
  members: ISpaceMember[]
  settings: ISpaceSettings
  createdAt: string
  updatedAt: string
}

export interface ISpaceMember {
  userId: string
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

// 权限相关类型
export interface IPermission {
  resource: string
  action: string
  allowed: boolean
}

export interface IRole {
  id: string
  name: string
  description?: string
  permissions: string[]
  isSystem: boolean
}

// API响应类型
export interface IApiResponse<T = any> {
  success: boolean
  data?: T
  message?: string
  errors?: string[]
  timestamp: string
}

export interface IPagedResponse<T = any> extends IApiResponse<T[]> {
  pagination: {
    page: number
    size: number
    total: number
    totalPages: number
  }
}

// 路由相关类型
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

// 系统配置类型
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

// 上传相关类型
export interface IUploadProgress {
  fileId: string
  fileName: string
  loaded: number
  total: number
  percentage: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  error?: string
}

export interface IUploadRequest {
  file: File
  path: string
  spaceId: string
  overwrite?: boolean
} 