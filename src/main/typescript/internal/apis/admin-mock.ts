import type {
  IUserDetail,
  IUserListResponse,
  ICreateUserRequest,
  IUpdateUserRequest,
  IBatchUserOperationRequest,
  IBatchOperationResult,
  IUserSearchRequest,
  IUserStatistics,
  IFileStatistics,
  IStorageStatistics,
  ISystemStatistics,
  ISystemHealth,
  IDashboardData,
  ISystemConfiguration,
  IUpdateSystemConfigRequest,
  IVersionControlSettings,
  IVersionHistory,
  ISearchStatistics,
  IPaginationParams,
  IRoleInfo
} from '@/types/admin'

// 模拟数据
const mockUsers: IUserDetail[] = [
  {
    id: 1,
    username: 'admin',
    email: 'admin@lyra.com',
    displayName: '系统管理员',
    status: 'ACTIVE',
    emailVerified: true,
    roles: [{ id: 1, name: '管理员', code: 'ADMIN', isSystem: true, permissions: [], userCount: 1, createdAt: '2024-01-01T00:00:00Z' }],
    permissions: ['system.admin'],
    storageQuota: 10737418240,
    storageUsed: 1073741824,
    loginCount: 150,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  }
]

/**
 * 简化的管理后台API接口（模拟版本）
 */
export const adminApi = {
  // ==================== 用户管理 ====================
  
  async getUserList(params: IPaginationParams & IUserSearchRequest): Promise<IUserListResponse> {
    // 模拟延迟
    await new Promise(resolve => setTimeout(resolve, 500))
    
    return {
      users: mockUsers,
      pagination: {
        page: params.page || 0,
        size: params.size || 20,
        totalElements: mockUsers.length,
        totalPages: 1
      }
    }
  },

  async getUserDetail(userId: number): Promise<IUserDetail> {
    await new Promise(resolve => setTimeout(resolve, 300))
    return mockUsers.find(u => u.id === userId) || mockUsers[0]
  },

  async createUser(data: ICreateUserRequest): Promise<IUserDetail> {
    await new Promise(resolve => setTimeout(resolve, 800))
    const newUser: IUserDetail = {
      id: Date.now(),
      username: data.username,
      email: data.email,
      displayName: data.displayName,
      status: data.status,
      emailVerified: false,
      roles: [],
      permissions: [],
      storageQuota: data.storageQuota,
      storageUsed: 0,
      loginCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    mockUsers.push(newUser)
    return newUser
  },

  async updateUser(userId: number, data: IUpdateUserRequest): Promise<IUserDetail> {
    await new Promise(resolve => setTimeout(resolve, 500))
    const user = mockUsers.find(u => u.id === userId)
    if (user) {
      Object.assign(user, data)
    }
    return user || mockUsers[0]
  },

  async deleteUser(userId: number): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 500))
    const index = mockUsers.findIndex(u => u.id === userId)
    if (index > -1) {
      mockUsers.splice(index, 1)
    }
  },

  async batchUserOperation(data: IBatchUserOperationRequest): Promise<IBatchOperationResult> {
    await new Promise(resolve => setTimeout(resolve, 1000))
    return {
      total: data.userIds.length,
      successful: data.userIds.length - 1,
      failed: 1,
      failures: [{ id: data.userIds[0], reason: '示例失败' }]
    }
  },

  // ==================== 统计监控 ====================

  async getUserStatistics(): Promise<IUserStatistics> {
    await new Promise(resolve => setTimeout(resolve, 300))
    return {
      totalUsers: 1250,
      activeUsers: 890,
      newUsersThisMonth: 45,
      newUsersLastMonth: 38,
      userGrowthRate: 18.4,
      usersByStatus: {
        active: 890,
        inactive: 200,
        locked: 160,
        pending: 0
      },
      registrationTrend: [
        { date: '2024-01-01', count: 15 },
        { date: '2024-01-02', count: 12 },
        { date: '2024-01-03', count: 18 }
      ],
      loginTrend: [
        { date: '2024-01-01', count: 450 },
        { date: '2024-01-02', count: 520 },
        { date: '2024-01-03', count: 480 }
      ]
    }
  },

  async getFileStatistics(): Promise<IFileStatistics> {
    await new Promise(resolve => setTimeout(resolve, 300))
    return {
      totalFiles: 15000,
      totalSize: 1073741824,
      totalSizeReadable: '1.0 GB',
      averageFileSize: 71582,
      filesUploadedToday: 150,
      filesUploadedThisMonth: 3500,
      uploadTrend: [
        { date: '2024-01-01', count: 150, size: 1048576 },
        { date: '2024-01-02', count: 200, size: 1572864 }
      ],
      filesByType: {
        pdf: { count: 5000, size: 524288000 },
        docx: { count: 3000, size: 314572800 },
        xlsx: { count: 2000, size: 209715200 }
      }
    }
  },

  async getStorageStatistics(): Promise<IStorageStatistics> {
    await new Promise(resolve => setTimeout(resolve, 300))
    return {
      totalSpace: 10737418240,
      usedSpace: 1073741824,
      freeSpace: 9663676416,
      usagePercentage: 10.0,
      totalSpaceReadable: '10.0 GB',
      usedSpaceReadable: '1.0 GB',
      freeSpaceReadable: '9.0 GB',
      storageByUser: [
        {
          userId: 1,
          username: 'admin',
          usedSpace: 536870912,
          quota: 10737418240,
          usagePercentage: 5.0
        }
      ],
      storageGrowthTrend: [
        { date: '2024-01-01', totalSize: 1000000000, growth: 50000000 }
      ]
    }
  },

  async getSystemStatistics(): Promise<ISystemStatistics> {
    await new Promise(resolve => setTimeout(resolve, 400))
    const users = await this.getUserStatistics()
    const files = await this.getFileStatistics()
    const storage = await this.getStorageStatistics()
    
    return {
      users,
      files,
      storage,
      system: {
        uptime: 86400,
        uptimeReadable: '1 day',
        cpuUsage: 25.5,
        memoryUsage: 60.2,
        diskUsage: 45.8,
        networkIn: 1048576,
        networkOut: 2097152,
        activeConnections: 150,
        requestsPerSecond: 25.5,
        averageResponseTime: 120.5,
        errorRate: 0.5
      },
      timestamp: new Date().toISOString()
    }
  },

  async getDashboardData(): Promise<IDashboardData> {
    await new Promise(resolve => setTimeout(resolve, 500))
    return {
      overview: {
        totalUsers: 1250,
        totalFiles: 15000,
        totalStorage: 1073741824,
        systemHealth: 'UP'
      },
      trends: {
        userGrowth: [
          { date: '2024-01-01', value: 1200, change: 50, changePercent: 4.3 }
        ],
        fileUploads: [
          { date: '2024-01-01', value: 150 }
        ],
        storageUsage: [
          { date: '2024-01-01', value: 1000000000 }
        ]
      },
      alerts: [
        {
          type: 'WARNING',
          message: '存储使用率超过80%',
          timestamp: new Date().toISOString()
        }
      ],
      recentActivities: [
        {
          type: 'USER_LOGIN',
          description: '用户登录',
          user: 'admin',
          timestamp: new Date().toISOString()
        }
      ]
    }
  },

  // ==================== 其他API模拟 ====================

  async getSystemHealth(): Promise<ISystemHealth> {
    return {
      status: 'UP',
      components: {
        database: { status: 'UP', details: {} },
        redis: { status: 'UP', details: {} },
        storage: { status: 'UP', details: {} },
        email: { status: 'UP', details: {} }
      },
      checks: []
    }
  },

  async getSystemConfiguration(): Promise<ISystemConfiguration> {
    return {
      general: {
        siteName: 'Lyra 云盘系统',
        siteDescription: '企业级文档管理系统',
        adminEmail: 'admin@lyra.com',
        timezone: 'Asia/Shanghai',
        language: 'zh-CN'
      },
      storage: {
        maxFileSize: '100MB',
        allowedFileTypes: ['pdf', 'docx', 'xlsx', 'jpg', 'png'],
        defaultQuota: '10GB',
        enableVersioning: true
      },
      security: {
        jwtExpiration: '24h',
        passwordMinLength: 8,
        enableTwoFactor: false,
        sessionTimeout: '30m'
      },
      email: {
        smtpHost: 'smtp.example.com',
        smtpPort: 587,
        enableTLS: true,
        fromAddress: 'noreply@lyra.com'
      }
    }
  },

  async updateSystemConfiguration(data: IUpdateSystemConfigRequest): Promise<ISystemConfiguration> {
    await new Promise(resolve => setTimeout(resolve, 800))
    return this.getSystemConfiguration()
  },

  async getVersionControlSettings(): Promise<IVersionControlSettings> {
    return {
      enabled: true,
      maxVersions: 10,
      retentionDays: 365,
      compressionEnabled: true,
      autoCleanup: true
    }
  },

  async updateVersionControlSettings(data: Partial<IVersionControlSettings>): Promise<IVersionControlSettings> {
    await new Promise(resolve => setTimeout(resolve, 500))
    return this.getVersionControlSettings()
  },

  async getFileVersionHistory(fileId: number): Promise<IVersionHistory> {
    return {
      file: {
        id: fileId,
        name: 'example.pdf',
        path: '/documents/example.pdf',
        currentVersion: 3
      },
      versions: [
        {
          id: 1,
          fileId,
          version: 1,
          size: 1048576,
          sizeReadable: '1 MB',
          checksum: 'abc123',
          createdAt: '2024-01-01T00:00:00Z',
          createdBy: 'admin'
        }
      ],
      totalVersions: 1
    }
  },

  async cleanupFileVersions(fileId: number, keepVersions: number): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 1000))
  },

  async getSearchStatistics(): Promise<ISearchStatistics> {
    return {
      totalSearches: 5000,
      popularKeywords: [
        { keyword: '报告', count: 150 },
        { keyword: '文档', count: 120 }
      ],
      searchTrend: [
        { date: '2024-01-01', count: 100 }
      ],
      noResultQueries: [
        { keyword: '测试', count: 10 }
      ]
    }
  },

  async getAllRoles(): Promise<IRoleInfo[]> {
    return [
      {
        id: 1,
        name: '管理员',
        code: 'ADMIN',
        isSystem: true,
        permissions: ['system.admin'],
        userCount: 1,
        createdAt: '2024-01-01T00:00:00Z'
      }
    ]
  },

  async createRole(data: any): Promise<IRoleInfo> {
    return {
      id: Date.now(),
      name: data.name,
      code: data.code,
      isSystem: false,
      permissions: data.permissions,
      userCount: 0,
      createdAt: new Date().toISOString()
    }
  },

  async updateRole(roleId: number, data: any): Promise<IRoleInfo> {
    return this.getAllRoles().then(roles => roles[0])
  },

  async deleteRole(roleId: number): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 500))
  },

  async getAllPermissions(): Promise<string[]> {
    return ['system.admin', 'file.read', 'file.write']
  },

  async getSystemLogs(): Promise<any> {
    return {
      logs: [],
      pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0 }
    }
  },

  async getCacheStatistics(): Promise<any> {
    return {
      totalKeys: 1000,
      memoryUsage: 50000000,
      hitRate: 0.85,
      missRate: 0.15,
      cachesByName: {
        users: { keys: 100, hits: 800, misses: 200, size: 10000000 }
      }
    }
  },

  async clearCache(cacheName: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 500))
  },

  async clearAllCache(): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
}

export default adminApi 