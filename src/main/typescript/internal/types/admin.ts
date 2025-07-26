// 管理后台相关类型定义

// 用户管理相关类型
export interface IUserDetail {
  id: number
  username: string
  email: string
  displayName: string
  avatar?: string
  bio?: string
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'PENDING'
  emailVerified: boolean
  roles: IRoleInfo[]
  permissions: string[]
  storageQuota: number
  storageUsed: number
  lastLoginAt?: string
  lastLoginIp?: string
  loginCount: number
  createdAt: string
  updatedAt: string
  createdBy?: string
}

export interface IRoleInfo {
  id: number
  name: string
  code: string
  description?: string
  isSystem: boolean
  permissions: string[]
  userCount: number
  createdAt: string
}

export interface ICreateUserRequest {
  username: string
  email: string
  password: string
  displayName: string
  roles: string[]
  storageQuota: number
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING'
  sendWelcomeEmail: boolean
}

export interface IUpdateUserRequest {
  email?: string
  displayName?: string
  bio?: string
  status?: 'ACTIVE' | 'INACTIVE' | 'LOCKED'
  roles?: string[]
  storageQuota?: number
}

export interface IBatchUserOperationRequest {
  userIds: number[]
  operation: 'ACTIVATE' | 'DEACTIVATE' | 'LOCK' | 'UNLOCK' | 'DELETE'
  reason?: string
}

export interface IBatchOperationResult {
  total: number
  successful: number
  failed: number
  failures: Array<{
    id: number
    reason: string
  }>
}

export interface IUserListResponse {
  users: IUserDetail[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
  filters?: IUserSearchRequest
}

export interface IUserSearchRequest {
  keyword?: string
  status?: 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'PENDING'
  role?: string
  emailVerified?: boolean
  createdDateRange?: {
    start: string
    end: string
  }
  lastLoginDateRange?: {
    start: string
    end: string
  }
}

// 统计相关类型
export interface IUserStatistics {
  totalUsers: number
  activeUsers: number
  newUsersThisMonth: number
  newUsersLastMonth: number
  userGrowthRate: number
  usersByStatus: {
    active: number
    inactive: number
    locked: number
    pending: number
  }
  registrationTrend: Array<{
    date: string
    count: number
  }>
  loginTrend: Array<{
    date: string
    count: number
  }>
}

export interface IFileStatistics {
  totalFiles: number
  totalSize: number
  totalSizeReadable: string
  averageFileSize: number
  filesUploadedToday: number
  filesUploadedThisMonth: number
  uploadTrend: Array<{
    date: string
    count: number
    size: number
  }>
  filesByType: Record<string, {
    count: number
    size: number
  }>
}

export interface IStorageStatistics {
  totalSpace: number
  usedSpace: number
  freeSpace: number
  usagePercentage: number
  totalSpaceReadable: string
  usedSpaceReadable: string
  freeSpaceReadable: string
  storageByUser: Array<{
    userId: number
    username: string
    usedSpace: number
    quota: number
    usagePercentage: number
  }>
  storageGrowthTrend: Array<{
    date: string
    totalSize: number
    growth: number
  }>
}

export interface ISystemMetrics {
  uptime: number
  uptimeReadable: string
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  networkIn: number
  networkOut: number
  activeConnections: number
  requestsPerSecond: number
  averageResponseTime: number
  errorRate: number
}

export interface ISystemStatistics {
  users: IUserStatistics
  files: IFileStatistics
  storage: IStorageStatistics
  system: ISystemMetrics
  timestamp: string
}

export interface IHealthComponent {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN'
  details: Record<string, any>
}

export interface IHealthCheck {
  name: string
  status: 'PASS' | 'FAIL' | 'WARN'
  message: string
  duration: number
  timestamp: string
}

export interface ISystemHealth {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN'
  components: {
    database: IHealthComponent
    redis: IHealthComponent
    storage: IHealthComponent
    email: IHealthComponent
  }
  checks: IHealthCheck[]
}

// 仪表板相关类型
export interface IDashboardData {
  overview: {
    totalUsers: number
    totalFiles: number
    totalStorage: number
    systemHealth: 'UP' | 'DOWN' | 'DEGRADED'
  }
  trends: {
    userGrowth: ITrendData[]
    fileUploads: ITrendData[]
    storageUsage: ITrendData[]
  }
  alerts: IAlert[]
  recentActivities: IActivity[]
}

export interface ITrendData {
  date: string
  value: number
  change?: number
  changePercent?: number
}

export interface IAlert {
  type: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
  message: string
  timestamp: string
}

export interface IActivity {
  type: string
  description: string
  user: string
  timestamp: string
}

// 系统配置相关类型
export interface ISystemConfiguration {
  general: {
    siteName: string
    siteDescription: string
    adminEmail: string
    timezone: string
    language: string
  }
  storage: {
    maxFileSize: string
    allowedFileTypes: string[]
    defaultQuota: string
    enableVersioning: boolean
  }
  security: {
    jwtExpiration: string
    passwordMinLength: number
    enableTwoFactor: boolean
    sessionTimeout: string
  }
  email: {
    smtpHost: string
    smtpPort: number
    enableTLS: boolean
    fromAddress: string
  }
}

export interface IUpdateSystemConfigRequest {
  general?: Record<string, any>
  storage?: Record<string, any>
  security?: Record<string, any>
  email?: Record<string, any>
}

// 版本控制相关类型
export interface IVersionControlSettings {
  enabled: boolean
  maxVersions: number
  retentionDays: number
  compressionEnabled: boolean
  autoCleanup: boolean
}

export interface IFileVersion {
  id: number
  fileId: number
  version: number
  size: number
  sizeReadable: string
  checksum: string
  comment?: string
  createdAt: string
  createdBy: string
}

export interface IVersionHistory {
  file: {
    id: number
    name: string
    path: string
    currentVersion: number
  }
  versions: IFileVersion[]
  totalVersions: number
}

// 搜索相关类型
export interface ISearchStatistics {
  totalSearches: number
  popularKeywords: Array<{
    keyword: string
    count: number
  }>
  searchTrend: Array<{
    date: string
    count: number
  }>
  noResultQueries: Array<{
    keyword: string
    count: number
  }>
}

// API响应包装类型
export interface IApiResponse<T = any> {
  success: boolean
  message: string
  data?: T
  timestamp: number
}

export interface IErrorResponse {
  success: false
  message: string
  errors?: string[]
  timestamp: number
}

// 分页相关类型
export interface IPaginationParams {
  page?: number
  size?: number
  sort?: string
  direction?: 'ASC' | 'DESC'
}

export interface IPaginationInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 简化的系统日志类型，避免API文件中的类型错误
export interface ISystemLog {
  id: number
  level: string
  logger: string
  message: string
  timestamp: string
  userId?: number
  ipAddress?: string
} 