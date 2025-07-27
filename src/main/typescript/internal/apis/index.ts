// ==================== API模块统一导出 ====================

// 认证相关API
export { authApi } from './auth'

// 文件管理API
export { fileApi, folderApi, spaceApi as fileSpaceApi, fileUtils } from './fileApi'

// 空间管理API
export { spaceApi, spaceUtils } from './spaceApi'

// 搜索API
export { searchApi, searchUtils } from './searchApi'

// 文件预览API
export { previewApi, previewUtils, PreviewType } from './previewApi'

// 在线编辑API
export { editorApi, editorUtils, EditorWebSocket } from './editorApi'

// WebDAV API
export { webdavApi, webdavUtils, WebDavClient } from './webdavApi'

// 版本控制API
export { versionApi, versionUtils } from './versionApi'

// 管理员API
export {
  adminUserApi,
  adminStatisticsApi,
  adminConfigApi,
  adminUtils
} from './adminApi'

// 请求工具
export { default as request } from './request'

// ==================== API类型导出 ====================
export type {
  // 搜索相关类型
  SearchRequest,
  SearchResult,
  SearchSuggestion,
  SearchFilter,
  AdvancedSearchRequest
} from './searchApi'

export type {
  // 预览相关类型
  PreviewResult,
  PreviewData,
  TextPreviewResult,
  ImagePreviewResult,
  MediaPreviewResult,
  PdfPreviewResult,
  PreviewConfig
} from './previewApi'

export type {
  // 编辑相关类型
  EditSession,
  EditCollaborator,
  StartEditRequest,
  EditResult,
  SaveEditRequest,
  EditHistory,
  EditChange,
  CollaborativeEdit,
  EditorConfig
} from './editorApi'

export type {
  // WebDAV相关类型
  WebDavResource,
  WebDavProperty,
  WebDavPropfindRequest,
  WebDavPropfindResponse,
  WebDavLockInfo,
  WebDavMoveRequest,
  WebDavCopyRequest,
  WebDavConfig
} from './webdavApi'

export type {
  // 版本控制相关类型
  FileVersion,
  VersionDiff,
  CreateVersionRequest,
  VersionCompareRequest,
  VersionRestoreRequest,
  VersionBranch,
  VersionMergeRequest,
  VersionConflict,
  VersionStatistics
} from './versionApi'

export type {
  // 管理员相关类型
  CreateUserRequest,
  UpdateUserRequest,
  BatchUserOperationRequest,
  SystemConfig
} from './adminApi'

// ==================== API工具函数 ====================
export const apiUtils = {
  // 处理API错误
  handleApiError(error: any): string {
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    if (error.response?.data?.errors?.length > 0) {
      return error.response.data.errors[0]
    }
    if (error.message) {
      return error.message
    }
    return '请求失败，请稍后重试'
  },

  // 构建查询参数
  buildQueryParams(params: Record<string, any>): string {
    const searchParams = new URLSearchParams()
    
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(item => searchParams.append(key, item.toString()))
        } else {
          searchParams.append(key, value.toString())
        }
      }
    })
    
    return searchParams.toString()
  },

  // 格式化API响应
  formatApiResponse<T>(response: any): {
    success: boolean
    data?: T
    message?: string
    errors?: string[]
  } {
    return {
      success: response.success || false,
      data: response.data,
      message: response.message,
      errors: response.errors
    }
  },

  // 检查API响应是否成功
  isApiSuccess(response: any): boolean {
    return response && response.success === true
  },

  // 提取API错误信息
  extractApiError(response: any): string[] {
    if (response?.errors && Array.isArray(response.errors)) {
      return response.errors
    }
    if (response?.message) {
      return [response.message]
    }
    return ['未知错误']
  },

  // 格式化文件大小
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  },

  // 格式化日期
  formatDate(date: string | Date): string {
    const d = new Date(date)
    return d.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  },

  // 格式化相对时间
  formatRelativeTime(date: string | Date): string {
    const now = new Date()
    const target = new Date(date)
    const diff = now.getTime() - target.getTime()
    
    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    
    if (days > 0) return `${days}天前`
    if (hours > 0) return `${hours}小时前`
    if (minutes > 0) return `${minutes}分钟前`
    return '刚刚'
  },

  // 生成唯一ID
  generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substring(2)
  },

  // 防抖函数
  debounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number
  ): (...args: Parameters<T>) => void {
    let timeout: number | undefined
    return (...args: Parameters<T>) => {
      if (timeout) clearTimeout(timeout)
      timeout = window.setTimeout(() => func.apply(this, args), wait)
    }
  },

  // 节流函数
  throttle<T extends (...args: any[]) => any>(
    func: T,
    limit: number
  ): (...args: Parameters<T>) => void {
    let inThrottle: boolean
    return (...args: Parameters<T>) => {
      if (!inThrottle) {
        func.apply(this, args)
        inThrottle = true
        setTimeout(() => inThrottle = false, limit)
      }
    }
  },

  // 深拷贝
  deepClone<T>(obj: T): T {
    if (obj === null || typeof obj !== 'object') return obj
    if (obj instanceof Date) return new Date(obj.getTime()) as any
    if (obj instanceof Array) return obj.map(item => this.deepClone(item)) as any
    if (typeof obj === 'object') {
      const clonedObj: any = {}
      for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
          clonedObj[key] = this.deepClone(obj[key])
        }
      }
      return clonedObj
    }
    return obj
  },

  // 验证邮箱格式
  validateEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  },

  // 验证密码强度
  validatePassword(password: string): {
    isValid: boolean
    strength: 'weak' | 'medium' | 'strong'
    errors: string[]
  } {
    const errors: string[] = []
    let score = 0
    
    if (password.length < 8) {
      errors.push('密码长度至少8位')
    } else {
      score += 1
    }
    
    if (!/[a-z]/.test(password)) {
      errors.push('密码必须包含小写字母')
    } else {
      score += 1
    }
    
    if (!/[A-Z]/.test(password)) {
      errors.push('密码必须包含大写字母')
    } else {
      score += 1
    }
    
    if (!/\d/.test(password)) {
      errors.push('密码必须包含数字')
    } else {
      score += 1
    }
    
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      errors.push('密码必须包含特殊字符')
    } else {
      score += 1
    }
    
    let strength: 'weak' | 'medium' | 'strong' = 'weak'
    if (score >= 4) strength = 'strong'
    else if (score >= 2) strength = 'medium'
    
    return {
      isValid: errors.length === 0,
      strength,
      errors
    }
  },

  // 获取文件类型
  getFileType(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase() || ''
    const typeMap: Record<string, string> = {
      // 图片
      jpg: 'image', jpeg: 'image', png: 'image', gif: 'image', bmp: 'image', svg: 'image', webp: 'image',
      // 视频
      mp4: 'video', avi: 'video', mov: 'video', wmv: 'video', flv: 'video', mkv: 'video', webm: 'video',
      // 音频
      mp3: 'audio', wav: 'audio', flac: 'audio', aac: 'audio', ogg: 'audio', wma: 'audio',
      // 文档
      pdf: 'document', doc: 'document', docx: 'document', xls: 'document', xlsx: 'document', 
      ppt: 'document', pptx: 'document', txt: 'document', rtf: 'document',
      // 压缩包
      zip: 'archive', rar: 'archive', '7z': 'archive', tar: 'archive', gz: 'archive',
      // 代码
      js: 'code', ts: 'code', html: 'code', css: 'code', java: 'code', py: 'code', cpp: 'code', c: 'code'
    }
    return typeMap[ext] || 'file'
  },
}

// ==================== 常量定义 ====================
export const API_CONSTANTS = {
  // 分页默认值
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  
  // 文件上传限制
  MAX_FILE_SIZE: 100 * 1024 * 1024, // 100MB
  CHUNK_SIZE: 1024 * 1024, // 1MB
  
  // 搜索限制
  MAX_SEARCH_KEYWORD_LENGTH: 200,
  SEARCH_DEBOUNCE_DELAY: 300,
  
  // 缓存时间
  CACHE_DURATION: {
    SHORT: 5 * 60 * 1000, // 5分钟
    MEDIUM: 30 * 60 * 1000, // 30分钟
    LONG: 2 * 60 * 60 * 1000, // 2小时
  },
  
  // HTTP状态码
  HTTP_STATUS: {
    OK: 200,
    CREATED: 201,
    BAD_REQUEST: 400,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    CONFLICT: 409,
    TOO_MANY_REQUESTS: 429,
    INTERNAL_SERVER_ERROR: 500,
  },
  
  // 文件类型
  MIME_TYPES: {
    IMAGE: ['image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/svg+xml', 'image/webp'],
    VIDEO: ['video/mp4', 'video/avi', 'video/mov', 'video/wmv', 'video/flv', 'video/mkv', 'video/webm'],
    AUDIO: ['audio/mp3', 'audio/wav', 'audio/flac', 'audio/aac', 'audio/ogg', 'audio/wma'],
    DOCUMENT: ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'],
    ARCHIVE: ['application/zip', 'application/x-rar-compressed', 'application/x-7z-compressed'],
  }
}
