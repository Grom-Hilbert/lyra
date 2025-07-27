import request from './request'
import type {
  IFileInfo,
  IUser,
  IApiResponse,
  IPagedResponse
} from '@/types/index'

// ==================== 版本控制相关类型 ====================
export interface FileVersion {
  id: number
  fileId: number
  version: number
  filename: string
  sizeBytes: number
  mimeType: string
  fileHash: string
  comment?: string
  createdBy: number
  createdAt: string
  isActive: boolean
  tags: string[]
  metadata: Record<string, any>
}

export interface VersionDiff {
  type: 'text' | 'binary'
  additions: number
  deletions: number
  changes: number
  diffContent?: string
  isBinary: boolean
}

export interface CreateVersionRequest {
  fileId: number
  comment?: string
  tags?: string[]
  metadata?: Record<string, any>
}

export interface VersionCompareRequest {
  fileId: number
  fromVersion: number
  toVersion: number
  format?: 'unified' | 'side-by-side' | 'json'
}

export interface VersionRestoreRequest {
  fileId: number
  versionId: number
  createBackup?: boolean
  comment?: string
}

export interface VersionBranch {
  id: number
  name: string
  description?: string
  baseVersionId: number
  headVersionId: number
  createdBy: number
  createdAt: string
  isActive: boolean
  fileCount: number
}

export interface VersionMergeRequest {
  sourceBranchId: number
  targetBranchId: number
  strategy: 'auto' | 'manual'
  conflictResolution?: 'source' | 'target' | 'manual'
  comment?: string
}

export interface VersionConflict {
  fileId: number
  filename: string
  sourceVersion: number
  targetVersion: number
  conflictType: 'content' | 'metadata' | 'both'
  conflictDetails: string
}

export interface VersionStatistics {
  totalVersions: number
  totalBranches: number
  storageUsed: number
  averageVersionSize: number
  mostVersionedFiles: Array<{
    fileId: number
    filename: string
    versionCount: number
  }>
  recentActivity: Array<{
    type: 'create' | 'restore' | 'branch' | 'merge'
    timestamp: string
    userId: number
    username: string
    description: string
  }>
}

// ==================== 版本控制API ====================
export const versionApi = {
  // 获取文件版本列表
  async getFileVersions(fileId: number, params?: {
    page?: number
    size?: number
    includeInactive?: boolean
    tags?: string[]
  }): Promise<IPagedResponse<FileVersion>> {
    const response = await request.get(`/api/version/file/${fileId}/versions`, { params })
    return response.data
  },

  // 获取版本详情
  async getVersionDetail(versionId: number): Promise<IApiResponse<FileVersion>> {
    const response = await request.get(`/api/version/${versionId}`)
    return response.data
  },

  // 创建新版本
  async createVersion(data: CreateVersionRequest): Promise<IApiResponse<FileVersion>> {
    const response = await request.post('/api/version/create', data)
    return response.data
  },

  // 恢复到指定版本
  async restoreVersion(data: VersionRestoreRequest): Promise<IApiResponse<IFileInfo>> {
    const response = await request.post('/api/version/restore', data)
    return response.data
  },

  // 比较版本差异
  async compareVersions(data: VersionCompareRequest): Promise<IApiResponse<VersionDiff>> {
    const response = await request.post('/api/version/compare', data)
    return response.data
  },

  // 删除版本
  async deleteVersion(versionId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/version/${versionId}`)
    return response.data
  },

  // 下载指定版本
  getVersionDownloadUrl(versionId: number): string {
    return `/api/version/${versionId}/download`
  },

  // 获取版本内容
  getVersionContentUrl(versionId: number): string {
    return `/api/version/${versionId}/content`
  },

  // 获取分支列表
  async getBranches(params?: {
    fileId?: number
    spaceId?: number
    page?: number
    size?: number
    includeInactive?: boolean
  }): Promise<IPagedResponse<VersionBranch>> {
    const response = await request.get('/api/version/branches', { params })
    return response.data
  },

  // 创建分支
  async createBranch(data: {
    name: string
    description?: string
    baseVersionId: number
    fileIds?: number[]
  }): Promise<IApiResponse<VersionBranch>> {
    const response = await request.post('/api/version/branch/create', data)
    return response.data
  },

  // 切换分支
  async switchBranch(branchId: number, fileIds?: number[]): Promise<IApiResponse<void>> {
    const response = await request.post(`/api/version/branch/${branchId}/switch`, { fileIds })
    return response.data
  },

  // 合并分支
  async mergeBranch(data: VersionMergeRequest): Promise<IApiResponse<{
    success: boolean
    conflicts?: VersionConflict[]
    mergedFiles: number[]
  }>> {
    const response = await request.post('/api/version/branch/merge', data)
    return response.data
  },

  // 删除分支
  async deleteBranch(branchId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/version/branch/${branchId}`)
    return response.data
  },

  // 解决冲突
  async resolveConflicts(data: {
    mergeId: string
    resolutions: Array<{
      fileId: number
      resolution: 'source' | 'target' | 'manual'
      content?: string
    }>
  }): Promise<IApiResponse<void>> {
    const response = await request.post('/api/version/conflicts/resolve', data)
    return response.data
  },

  // 获取版本统计
  async getVersionStatistics(params?: {
    spaceId?: number
    fileId?: number
    period?: 'day' | 'week' | 'month' | 'year'
  }): Promise<IApiResponse<VersionStatistics>> {
    const response = await request.get('/api/version/statistics', { params })
    return response.data
  },

  // 清理旧版本
  async cleanupVersions(data: {
    fileId?: number
    spaceId?: number
    keepCount?: number
    keepDays?: number
    dryRun?: boolean
  }): Promise<IApiResponse<{
    deletedVersions: number
    freedSpace: number
    affectedFiles: number
  }>> {
    const response = await request.post('/api/version/cleanup', data)
    return response.data
  },

  // 导出版本历史
  async exportVersionHistory(fileId: number, format: 'json' | 'csv' | 'xml'): Promise<IApiResponse<{
    downloadUrl: string
    filename: string
    size: number
  }>> {
    const response = await request.post(`/api/version/export/${fileId}`, { format })
    return response.data
  },

  // 批量操作版本
  async batchOperateVersions(data: {
    versionIds: number[]
    operation: 'delete' | 'tag' | 'untag'
    tags?: string[]
  }): Promise<IApiResponse<{
    successCount: number
    failureCount: number
    errors: string[]
  }>> {
    const response = await request.post('/api/version/batch', data)
    return response.data
  },
}

// ==================== 版本控制工具函数 ====================
export const versionUtils = {
  // 格式化版本号
  formatVersion(version: number): string {
    return `v${version.toString().padStart(3, '0')}`
  },

  // 计算版本差异百分比
  calculateDiffPercentage(diff: VersionDiff): number {
    const total = diff.additions + diff.deletions + diff.changes
    if (total === 0) return 0
    return Math.round((diff.changes / total) * 100)
  },

  // 获取版本状态颜色
  getVersionStatusColor(version: FileVersion): string {
    if (!version.isActive) return 'gray'
    if (version.tags.includes('stable')) return 'green'
    if (version.tags.includes('beta')) return 'orange'
    if (version.tags.includes('alpha')) return 'red'
    return 'blue'
  },

  // 格式化版本大小变化
  formatSizeChange(oldSize: number, newSize: number): {
    change: number
    percentage: number
    formatted: string
    trend: 'up' | 'down' | 'same'
  } {
    const change = newSize - oldSize
    const percentage = oldSize > 0 ? (change / oldSize) * 100 : 0
    const trend = change > 0 ? 'up' : change < 0 ? 'down' : 'same'
    
    const formatSize = (bytes: number) => {
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(Math.abs(bytes)) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }
    
    const formatted = change === 0 ? '无变化' : 
      `${change > 0 ? '+' : ''}${formatSize(change)} (${percentage.toFixed(1)}%)`
    
    return { change, percentage, formatted, trend }
  },

  // 验证分支名称
  validateBranchName(name: string): { isValid: boolean; error?: string } {
    if (!name.trim()) {
      return { isValid: false, error: '分支名称不能为空' }
    }
    
    if (name.length > 50) {
      return { isValid: false, error: '分支名称不能超过50个字符' }
    }
    
    const invalidChars = /[<>:"/\\|?*\s]/
    if (invalidChars.test(name)) {
      return { isValid: false, error: '分支名称包含非法字符' }
    }
    
    const reservedNames = ['master', 'main', 'HEAD', 'origin']
    if (reservedNames.includes(name.toLowerCase())) {
      return { isValid: false, error: '分支名称不能使用保留字' }
    }
    
    return { isValid: true }
  },

  // 生成版本标签建议
  generateTagSuggestions(filename: string, comment?: string): string[] {
    const suggestions: string[] = []
    
    // 基于文件类型的标签
    const ext = filename.split('.').pop()?.toLowerCase()
    if (ext) {
      const typeMap: Record<string, string[]> = {
        'js': ['frontend', 'javascript'],
        'ts': ['frontend', 'typescript'],
        'java': ['backend', 'java'],
        'py': ['backend', 'python'],
        'md': ['documentation'],
        'json': ['config'],
        'yaml': ['config'],
        'yml': ['config']
      }
      
      if (typeMap[ext]) {
        suggestions.push(...typeMap[ext])
      }
    }
    
    // 基于注释的标签
    if (comment) {
      const commentLower = comment.toLowerCase()
      if (commentLower.includes('fix') || commentLower.includes('修复')) {
        suggestions.push('bugfix')
      }
      if (commentLower.includes('feature') || commentLower.includes('功能')) {
        suggestions.push('feature')
      }
      if (commentLower.includes('refactor') || commentLower.includes('重构')) {
        suggestions.push('refactor')
      }
      if (commentLower.includes('test') || commentLower.includes('测试')) {
        suggestions.push('test')
      }
    }
    
    return [...new Set(suggestions)]
  },

  // 检查版本兼容性
  checkVersionCompatibility(sourceVersion: FileVersion, targetVersion: FileVersion): {
    compatible: boolean
    issues: string[]
    recommendations: string[]
  } {
    const issues: string[] = []
    const recommendations: string[] = []
    
    // 检查文件类型
    if (sourceVersion.mimeType !== targetVersion.mimeType) {
      issues.push('文件类型不匹配')
      recommendations.push('请确认文件类型变更是否正确')
    }
    
    // 检查文件大小变化
    const sizeChange = Math.abs(targetVersion.sizeBytes - sourceVersion.sizeBytes)
    const sizeChangePercent = (sizeChange / sourceVersion.sizeBytes) * 100
    
    if (sizeChangePercent > 50) {
      issues.push('文件大小变化超过50%')
      recommendations.push('请检查文件内容是否正确')
    }
    
    // 检查版本间隔
    if (targetVersion.version - sourceVersion.version > 10) {
      recommendations.push('版本跨度较大，建议检查中间版本的变更')
    }
    
    return {
      compatible: issues.length === 0,
      issues,
      recommendations
    }
  },

  // 生成版本摘要
  generateVersionSummary(versions: FileVersion[]): {
    totalVersions: number
    sizeRange: { min: number; max: number }
    timeSpan: { start: string; end: string }
    topTags: Array<{ tag: string; count: number }>
    contributors: Array<{ userId: number; count: number }>
  } {
    if (versions.length === 0) {
      return {
        totalVersions: 0,
        sizeRange: { min: 0, max: 0 },
        timeSpan: { start: '', end: '' },
        topTags: [],
        contributors: []
      }
    }
    
    const sizes = versions.map(v => v.sizeBytes)
    const times = versions.map(v => v.createdAt).sort()
    
    // 统计标签
    const tagCounts = new Map<string, number>()
    versions.forEach(v => {
      v.tags.forEach(tag => {
        tagCounts.set(tag, (tagCounts.get(tag) || 0) + 1)
      })
    })
    
    // 统计贡献者
    const contributorCounts = new Map<number, number>()
    versions.forEach(v => {
      contributorCounts.set(v.createdBy, (contributorCounts.get(v.createdBy) || 0) + 1)
    })
    
    return {
      totalVersions: versions.length,
      sizeRange: { min: Math.min(...sizes), max: Math.max(...sizes) },
      timeSpan: { start: times[0], end: times[times.length - 1] },
      topTags: Array.from(tagCounts.entries())
        .map(([tag, count]) => ({ tag, count }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5),
      contributors: Array.from(contributorCounts.entries())
        .map(([userId, count]) => ({ userId, count }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5)
    }
  },
}
