import request from './request'
import type {
  IUser,
  IApiResponse,
  IPagedResponse,
  UserStatistics,
  SystemStatistics
} from '@/types/index'

// ==================== 管理员相关类型 ====================
export interface CreateUserRequest {
  username: string
  email: string
  password: string
  displayName: string
  roles: string[]
  status?: 'ACTIVE' | 'INACTIVE'
}

export interface UpdateUserRequest {
  username?: string
  email?: string
  displayName?: string
  roles?: string[]
  status?: 'ACTIVE' | 'INACTIVE' | 'LOCKED'
}

export interface BatchUserOperationRequest {
  userIds: number[]
  operation: 'activate' | 'deactivate' | 'lock' | 'unlock' | 'delete'
}

export interface SystemConfig {
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
  security: {
    passwordMinLength: number
    sessionTimeout: number
    maxLoginAttempts: number
  }
}

// ==================== 用户管理API ====================
export const adminUserApi = {
  // 获取用户列表
  async getUsers(params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    role?: string
    sort?: string
    direction?: 'asc' | 'desc'
  }): Promise<IPagedResponse<IUser>> {
    const response = await request.get('/api/admin/system/users', { params })
    return response.data
  },

  // 获取用户详情
  async getUserDetail(userId: number): Promise<IApiResponse<IUser>> {
    const response = await request.get(`/api/admin/system/users/${userId}`)
    return response.data
  },

  // 创建用户
  async createUser(data: CreateUserRequest): Promise<IApiResponse<IUser>> {
    const response = await request.post('/api/admin/system/users', data)
    return response.data
  },

  // 更新用户
  async updateUser(userId: number, data: UpdateUserRequest): Promise<IApiResponse<IUser>> {
    const response = await request.put(`/api/admin/system/users/${userId}`, data)
    return response.data
  },

  // 删除用户
  async deleteUser(userId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/admin/system/users/${userId}`)
    return response.data
  },

  // 批量操作用户
  async batchOperateUsers(data: BatchUserOperationRequest): Promise<IApiResponse<void>> {
    const response = await request.post('/api/admin/system/users/batch', data)
    return response.data
  },

  // 重置用户密码
  async resetUserPassword(userId: number, data: {
    newPassword: string
    forceChange?: boolean
  }): Promise<IApiResponse<void>> {
    const response = await request.post(`/api/admin/system/users/${userId}/reset-password`, data)
    return response.data
  },

  // 获取用户登录历史
  async getUserLoginHistory(userId: number, params?: {
    page?: number
    size?: number
  }): Promise<IPagedResponse<{
    id: number
    ip: string
    userAgent: string
    location?: string
    success: boolean
    createdAt: string
  }>> {
    const response = await request.get(`/api/admin/system/users/${userId}/login-history`, { params })
    return response.data
  },

  // 获取用户存储使用情况
  async getUserStorage(userId: number): Promise<IApiResponse<{
    used: number
    quota: number
    usagePercentage: number
    fileCount: number
    spaceCount: number
  }>> {
    const response = await request.get(`/api/admin/system/users/${userId}/storage`)
    return response.data
  },
}

// ==================== 系统统计API ====================
export const adminStatisticsApi = {
  // 获取用户统计
  async getUserStatistics(): Promise<IApiResponse<UserStatistics>> {
    const response = await request.get('/api/admin/statistics/users')
    return response.data
  },

  // 获取文件统计
  async getFileStatistics(): Promise<IApiResponse<{
    totalFiles: number
    totalSize: number
    filesByType: Record<string, number>
    uploadTrend: Array<{
      date: string
      count: number
      size: number
    }>
  }>> {
    const response = await request.get('/api/admin/statistics/files')
    return response.data
  },

  // 获取存储统计
  async getStorageStatistics(): Promise<IApiResponse<{
    totalUsed: number
    totalQuota: number
    usagePercentage: number
    userBreakdown: Array<{
      userId: number
      username: string
      used: number
      quota: number
    }>
  }>> {
    const response = await request.get('/api/admin/statistics/storage')
    return response.data
  },

  // 获取系统统计
  async getSystemStatistics(): Promise<IApiResponse<SystemStatistics>> {
    const response = await request.get('/api/admin/statistics/system')
    return response.data
  },

  // 获取仪表板数据
  async getDashboardData(): Promise<IApiResponse<{
    users: UserStatistics
    files: {
      totalFiles: number
      totalSize: number
      todayUploads: number
    }
    storage: {
      used: number
      total: number
      usagePercentage: number
    }
    activity: Array<{
      date: string
      logins: number
      uploads: number
      downloads: number
    }>
    alerts: Array<{
      id: number
      type: 'warning' | 'error' | 'info'
      message: string
      createdAt: string
    }>
  }>> {
    const response = await request.get('/api/admin/statistics/dashboard')
    return response.data
  },
}

// ==================== 系统配置API ====================
export const adminConfigApi = {
  // 获取系统配置
  async getSystemConfig(): Promise<IApiResponse<SystemConfig>> {
    const response = await request.get('/api/admin/config/system')
    return response.data
  },

  // 更新系统配置
  async updateSystemConfig(data: Partial<SystemConfig>): Promise<IApiResponse<SystemConfig>> {
    const response = await request.put('/api/admin/config/system', data)
    return response.data
  },

  // 获取系统健康状态
  async getSystemHealth(): Promise<IApiResponse<{
    status: 'healthy' | 'warning' | 'error'
    uptime: number
    version: string
    components: Array<{
      name: string
      status: 'up' | 'down' | 'degraded'
      responseTime?: number
      message?: string
    }>
    metrics: {
      cpu: number
      memory: number
      disk: number
      network: number
    }
  }>> {
    const response = await request.get('/api/admin/system/health')
    return response.data
  },

  // 获取系统日志
  async getSystemLogs(params?: {
    level?: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
    component?: string
    startTime?: string
    endTime?: string
    page?: number
    size?: number
  }): Promise<IPagedResponse<{
    id: number
    level: string
    component: string
    message: string
    timestamp: string
    metadata?: Record<string, any>
  }>> {
    const response = await request.get('/api/admin/system/logs', { params })
    return response.data
  },

  // 清理系统缓存
  async clearCache(cacheType?: string): Promise<IApiResponse<void>> {
    const response = await request.post('/api/admin/system/cache/clear', { cacheType })
    return response.data
  },

  // 获取缓存统计
  async getCacheStatistics(): Promise<IApiResponse<{
    caches: Array<{
      name: string
      size: number
      hitRate: number
      missRate: number
      evictions: number
    }>
    totalMemory: number
    usedMemory: number
  }>> {
    const response = await request.get('/api/admin/system/cache/statistics')
    return response.data
  },
}

// ==================== 管理员工具函数 ====================
export const adminUtils = {
  // 格式化用户状态
  formatUserStatus(status: string): { text: string; color: string } {
    const statusMap: Record<string, { text: string; color: string }> = {
      ACTIVE: { text: '活跃', color: 'green' },
      INACTIVE: { text: '未激活', color: 'gray' },
      LOCKED: { text: '已锁定', color: 'red' }
    }
    return statusMap[status] || { text: status, color: 'gray' }
  },

  // 格式化系统健康状态
  formatHealthStatus(status: string): { text: string; color: string; icon: string } {
    const statusMap: Record<string, { text: string; color: string; icon: string }> = {
      healthy: { text: '健康', color: 'green', icon: 'lucide:check-circle' },
      warning: { text: '警告', color: 'yellow', icon: 'lucide:alert-triangle' },
      error: { text: '错误', color: 'red', icon: 'lucide:x-circle' }
    }
    return statusMap[status] || { text: status, color: 'gray', icon: 'lucide:help-circle' }
  },

  // 计算增长率
  calculateGrowthRate(current: number, previous: number): {
    rate: number
    trend: 'up' | 'down' | 'stable'
    formatted: string
  } {
    if (previous === 0) {
      return { rate: 0, trend: 'stable', formatted: '0%' }
    }
    
    const rate = ((current - previous) / previous) * 100
    const trend = rate > 0 ? 'up' : rate < 0 ? 'down' : 'stable'
    const formatted = `${rate > 0 ? '+' : ''}${rate.toFixed(1)}%`
    
    return { rate, trend, formatted }
  },

  // 验证配置值
  validateConfig(key: string, value: any): { isValid: boolean; error?: string } {
    const validators: Record<string, (value: any) => boolean> = {
      maxFileSize: (v) => typeof v === 'number' && v > 0 && v <= 10737418240, // 10GB
      maxUsers: (v) => typeof v === 'number' && v > 0 && v <= 100000,
      quotaPerUser: (v) => typeof v === 'number' && v > 0,
      passwordMinLength: (v) => typeof v === 'number' && v >= 6 && v <= 128,
      sessionTimeout: (v) => typeof v === 'number' && v > 0 && v <= 86400, // 24小时
      maxLoginAttempts: (v) => typeof v === 'number' && v > 0 && v <= 100,
    }
    
    const validator = validators[key]
    if (!validator) {
      return { isValid: true }
    }
    
    if (!validator(value)) {
      return { isValid: false, error: `配置项 ${key} 的值无效` }
    }
    
    return { isValid: true }
  },
}
