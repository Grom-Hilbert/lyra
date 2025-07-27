import request from './request'
import type {
  ISpace,
  ISpaceMember,
  IApiResponse,
  IPagedResponse
} from '@/types/index'

// ==================== 空间管理API ====================
export const spaceApi = {
  // 获取用户空间列表
  async getUserSpaces(): Promise<IApiResponse<ISpace[]>> {
    const response = await request.get('/api/spaces/user')
    return response.data
  },

  // 创建空间
  async createSpace(data: {
    name: string
    description?: string
    type: 'personal' | 'shared' | 'enterprise'
    isPublic?: boolean
  }): Promise<IApiResponse<ISpace>> {
    const response = await request.post('/api/spaces', data)
    return response.data
  },

  // 获取空间详情
  async getSpaceDetail(spaceId: number): Promise<IApiResponse<ISpace>> {
    const response = await request.get(`/api/spaces/${spaceId}`)
    return response.data
  },

  // 更新空间信息
  async updateSpace(spaceId: number, data: {
    name?: string
    description?: string
    isPublic?: boolean
  }): Promise<IApiResponse<ISpace>> {
    const response = await request.put(`/api/spaces/${spaceId}`, data)
    return response.data
  },

  // 删除空间
  async deleteSpace(spaceId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/spaces/${spaceId}`)
    return response.data
  },

  // 获取空间成员列表
  async getSpaceMembers(spaceId: number, params?: {
    page?: number
    size?: number
    role?: string
  }): Promise<IPagedResponse<ISpaceMember>> {
    const response = await request.get(`/api/spaces/${spaceId}/members`, { params })
    return response.data
  },

  // 添加空间成员
  async addSpaceMember(spaceId: number, data: {
    userId: number
    role: string
  }): Promise<IApiResponse<ISpaceMember>> {
    const response = await request.post(`/api/spaces/${spaceId}/members`, data)
    return response.data
  },

  // 更新成员角色
  async updateMemberRole(spaceId: number, userId: number, data: {
    role: string
  }): Promise<IApiResponse<ISpaceMember>> {
    const response = await request.put(`/api/spaces/${spaceId}/members/${userId}`, data)
    return response.data
  },

  // 移除空间成员
  async removeMember(spaceId: number, userId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/spaces/${spaceId}/members/${userId}`)
    return response.data
  },

  // 获取空间统计信息
  async getSpaceStatistics(spaceId: number): Promise<IApiResponse<{
    fileCount: number
    folderCount: number
    totalSize: number
    memberCount: number
    lastActivity: string
  }>> {
    const response = await request.get(`/api/spaces/${spaceId}/statistics`)
    return response.data
  },

  // 获取空间存储使用情况
  async getSpaceStorage(spaceId: number): Promise<IApiResponse<{
    used: number
    total: number
    usagePercentage: number
    breakdown: {
      documents: number
      images: number
      videos: number
      others: number
    }
  }>> {
    const response = await request.get(`/api/spaces/${spaceId}/storage`)
    return response.data
  },

  // 搜索空间
  async searchSpaces(params: {
    keyword?: string
    type?: 'personal' | 'shared' | 'enterprise'
    page?: number
    size?: number
  }): Promise<IPagedResponse<ISpace>> {
    const response = await request.get('/api/spaces/search', { params })
    return response.data
  },

  // 获取空间权限
  async getSpacePermissions(spaceId: number): Promise<IApiResponse<{
    canRead: boolean
    canWrite: boolean
    canDelete: boolean
    canManage: boolean
    canInvite: boolean
  }>> {
    const response = await request.get(`/api/spaces/${spaceId}/permissions`)
    return response.data
  },

  // 获取空间存储配额信息
  async getSpaceQuota(spaceId: number): Promise<IApiResponse<{
    used: number
    total: number
    usedReadable: string
    totalReadable: string
    usagePercentage: number
    fileCount: number
    folderCount: number
  }>> {
    const response = await request.get(`/api/spaces/${spaceId}/quota`)
    return response.data
  },

  // 生成空间邀请链接
  async generateInviteLink(spaceId: number, data: {
    role: string
    expiresIn?: number // 过期时间(小时)
  }): Promise<IApiResponse<{
    inviteCode: string
    inviteUrl: string
    expiresAt: string
  }>> {
    const response = await request.post(`/api/spaces/${spaceId}/invite`, data)
    return response.data
  },

  // 通过邀请码加入空间
  async joinSpaceByInvite(inviteCode: string): Promise<IApiResponse<ISpace>> {
    const response = await request.post('/api/spaces/join', { inviteCode })
    return response.data
  },

  // 离开空间
  async leaveSpace(spaceId: number): Promise<IApiResponse<void>> {
    const response = await request.post(`/api/spaces/${spaceId}/leave`)
    return response.data
  },

  // 获取空间活动日志
  async getSpaceActivity(spaceId: number, params?: {
    page?: number
    size?: number
    type?: string
    userId?: number
  }): Promise<IPagedResponse<{
    id: number
    type: string
    description: string
    userId: number
    username: string
    createdAt: string
    metadata?: Record<string, any>
  }>> {
    const response = await request.get(`/api/spaces/${spaceId}/activity`, { params })
    return response.data
  },
}

// ==================== 空间工具函数 ====================
export const spaceUtils = {
  // 格式化空间类型
  formatSpaceType(type: string): string {
    const typeMap: Record<string, string> = {
      personal: '个人空间',
      shared: '共享空间',
      enterprise: '企业空间'
    }
    return typeMap[type] || type
  },

  // 获取空间类型图标
  getSpaceTypeIcon(type: string): string {
    const iconMap: Record<string, string> = {
      personal: 'lucide:user',
      shared: 'lucide:users',
      enterprise: 'lucide:building'
    }
    return iconMap[type] || 'lucide:folder'
  },

  // 检查空间权限
  hasPermission(permissions: Record<string, boolean>, action: string): boolean {
    return permissions[action] === true
  },

  // 格式化存储使用率
  formatStorageUsage(used: number, total: number): {
    percentage: number
    usedFormatted: string
    totalFormatted: string
    status: 'normal' | 'warning' | 'danger'
  } {
    const percentage = total > 0 ? (used / total) * 100 : 0
    const formatSize = (bytes: number) => {
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    let status: 'normal' | 'warning' | 'danger' = 'normal'
    if (percentage >= 90) status = 'danger'
    else if (percentage >= 75) status = 'warning'

    return {
      percentage: Math.round(percentage),
      usedFormatted: formatSize(used),
      totalFormatted: formatSize(total),
      status
    }
  },

  // 验证空间名称
  validateSpaceName(name: string): { isValid: boolean; error?: string } {
    if (!name.trim()) {
      return { isValid: false, error: '空间名称不能为空' }
    }
    if (name.length > 100) {
      return { isValid: false, error: '空间名称不能超过100个字符' }
    }
    const invalidChars = /[<>:"/\\|?*]/
    if (invalidChars.test(name)) {
      return { isValid: false, error: '空间名称包含非法字符' }
    }
    return { isValid: true }
  },
}
