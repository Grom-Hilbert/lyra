import request from './request'
import type {
  IFileInfo,
  IApiResponse
} from '@/types/index'

// ==================== 预览相关类型 ====================
export interface PreviewResult {
  success: boolean
  type: PreviewType
  message?: string
  data?: PreviewData
}

export enum PreviewType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  PDF = 'PDF',
  MEDIA = 'MEDIA',
  UNSUPPORTED = 'UNSUPPORTED'
}

export interface PreviewData {
  contentUrl?: string
  thumbnailUrl?: string
  metadata?: Record<string, any>
}

export interface TextPreviewResult extends PreviewData {
  content: string
  encoding: string
  lineCount: number
  language?: string
}

export interface ImagePreviewResult extends PreviewData {
  contentUrl: string
  thumbnailUrl: string
  width: number
  height: number
  format: string
  fileSize: number
}

export interface MediaPreviewResult extends PreviewData {
  contentUrl: string
  previewImageUrl?: string
  mediaType: 'audio' | 'video'
  duration: number
  metadata: Record<string, any>
}

export interface PdfPreviewResult extends PreviewData {
  contentUrl: string
  pageCount: number
  title?: string
  author?: string
  subject?: string
}

export interface PreviewConfig {
  maxTextFileSize: number
  supportedImageFormats: string[]
  supportedVideoFormats: string[]
  supportedAudioFormats: string[]
  thumbnailSizes: string[]
}

// ==================== 文件预览API ====================
export const previewApi = {
  // 获取文件预览
  async getFilePreview(fileId: number): Promise<IApiResponse<PreviewResult>> {
    const response = await request.get(`/api/preview/file/${fileId}`)
    return response.data
  },

  // 批量获取文件预览信息
  async getFilePreviewBatch(data: { fileId: number }): Promise<IApiResponse<PreviewResult>> {
    const response = await request.post('/api/preview/batch', data)
    return response.data
  },

  // 检查文件是否支持预览
  async checkPreviewSupport(params: {
    filename: string
    mimeType?: string
  }): Promise<IApiResponse<{
    supported: boolean
    type: PreviewType
    message: string
  }>> {
    const response = await request.get('/api/preview/check', { params })
    return response.data
  },

  // 获取支持的预览类型
  async getSupportedTypes(): Promise<IApiResponse<{
    textExtensions: string[]
    imageExtensions: string[]
    mediaExtensions: string[]
  }>> {
    const response = await request.get('/api/preview/supported-types')
    return response.data
  },

  // 获取文件预览类型
  async getPreviewType(params: {
    filename: string
    mimeType?: string
  }): Promise<IApiResponse<{
    type: PreviewType
    supported: boolean
  }>> {
    const response = await request.get('/api/preview/type', { params })
    return response.data
  },

  // 获取文本文件预览
  async getTextPreview(fileId: number, params?: {
    encoding?: string
    maxLines?: number
  }): Promise<IApiResponse<TextPreviewResult>> {
    const response = await request.get(`/api/preview/text/${fileId}`, { params })
    return response.data
  },

  // 获取图片预览信息
  async getImagePreview(fileId: number): Promise<IApiResponse<ImagePreviewResult>> {
    const response = await request.get(`/api/preview/image/${fileId}`)
    return response.data
  },

  // 获取PDF预览信息
  async getPdfPreview(fileId: number): Promise<IApiResponse<PdfPreviewResult>> {
    const response = await request.get(`/api/preview/pdf/${fileId}`)
    return response.data
  },

  // 获取媒体文件预览信息
  async getMediaPreview(fileId: number): Promise<IApiResponse<MediaPreviewResult>> {
    const response = await request.get(`/api/preview/media/${fileId}`)
    return response.data
  },

  // 获取预览配置
  async getPreviewConfig(): Promise<IApiResponse<PreviewConfig>> {
    const response = await request.get('/api/preview/config')
    return response.data
  },

  // 生成缩略图
  async generateThumbnail(fileId: number, params?: {
    size?: string
    quality?: number
    format?: 'jpg' | 'png' | 'webp'
  }): Promise<IApiResponse<{
    thumbnailUrl: string
    size: number
    format: string
  }>> {
    const response = await request.post(`/api/preview/thumbnail/${fileId}/generate`, params)
    return response.data
  },

  // 批量生成缩略图
  async batchGenerateThumbnails(fileIds: number[], params?: {
    size?: string
    quality?: number
    format?: 'jpg' | 'png' | 'webp'
  }): Promise<IApiResponse<Array<{
    fileId: number
    success: boolean
    thumbnailUrl?: string
    error?: string
  }>>> {
    const response = await request.post('/api/preview/thumbnail/batch', {
      fileIds,
      ...params
    })
    return response.data
  },

  // 获取文件内容URL（用于直接访问）
  getContentUrl(fileId: number): string {
    return `/api/files/${fileId}/content`
  },

  // 获取PDF页面预览
  getPdfPageUrl(fileId: number, page: number, params?: {
    scale?: number
    format?: 'jpg' | 'png'
  }): string {
    const searchParams = new URLSearchParams()
    if (params?.scale) searchParams.append('scale', params.scale.toString())
    if (params?.format) searchParams.append('format', params.format)
    
    const queryString = searchParams.toString()
    return `/api/preview/pdf/${fileId}/page/${page}${queryString ? '?' + queryString : ''}`
  },

  // 获取视频帧预览
  getVideoFrameUrl(fileId: number, time: number, params?: {
    width?: number
    height?: number
    format?: 'jpg' | 'png'
  }): string {
    const searchParams = new URLSearchParams()
    searchParams.append('time', time.toString())
    if (params?.width) searchParams.append('width', params.width.toString())
    if (params?.height) searchParams.append('height', params.height.toString())
    if (params?.format) searchParams.append('format', params.format)
    
    return `/api/preview/video/${fileId}/frame?${searchParams.toString()}`
  },
}

// ==================== 预览工具函数 ====================
export const previewUtils = {
  // 检查文件是否支持预览
  isPreviewSupported(file: IFileInfo): boolean {
    const supportedTypes = [
      'text/', 'image/', 'video/', 'audio/',
      'application/pdf', 'application/json', 'application/xml'
    ]
    return supportedTypes.some(type => file.mimeType.startsWith(type))
  },

  // 获取预览类型
  getPreviewType(file: IFileInfo): PreviewType {
    const mimeType = file.mimeType.toLowerCase()
    
    if (mimeType.startsWith('text/') || 
        mimeType === 'application/json' || 
        mimeType === 'application/xml') {
      return PreviewType.TEXT
    }
    
    if (mimeType.startsWith('image/')) {
      return PreviewType.IMAGE
    }
    
    if (mimeType === 'application/pdf') {
      return PreviewType.PDF
    }
    
    if (mimeType.startsWith('video/') || mimeType.startsWith('audio/')) {
      return PreviewType.MEDIA
    }
    
    return PreviewType.UNSUPPORTED
  },

  // 获取预览图标
  getPreviewIcon(type: PreviewType): string {
    const iconMap: Record<PreviewType, string> = {
      [PreviewType.TEXT]: 'lucide:file-text',
      [PreviewType.IMAGE]: 'lucide:image',
      [PreviewType.PDF]: 'lucide:file-text',
      [PreviewType.MEDIA]: 'lucide:play-circle',
      [PreviewType.UNSUPPORTED]: 'lucide:file'
    }
    return iconMap[type]
  },

  // 格式化预览错误信息
  formatPreviewError(error: string): string {
    const errorMap: Record<string, string> = {
      'FILE_NOT_FOUND': '文件不存在',
      'PERMISSION_DENIED': '没有预览权限',
      'UNSUPPORTED_FORMAT': '不支持的文件格式',
      'FILE_TOO_LARGE': '文件过大，无法预览',
      'PROCESSING_ERROR': '预览处理失败'
    }
    return errorMap[error] || error
  },

  // 检查是否可以生成缩略图
  canGenerateThumbnail(file: IFileInfo): boolean {
    const supportedTypes = ['image/', 'video/', 'application/pdf']
    return supportedTypes.some(type => file.mimeType.startsWith(type))
  },

  // 计算最佳预览尺寸
  calculateOptimalSize(originalWidth: number, originalHeight: number, maxSize: number): {
    width: number
    height: number
    scale: number
  } {
    const aspectRatio = originalWidth / originalHeight
    let width = originalWidth
    let height = originalHeight
    
    if (width > maxSize || height > maxSize) {
      if (aspectRatio > 1) {
        // 宽度大于高度
        width = maxSize
        height = Math.round(maxSize / aspectRatio)
      } else {
        // 高度大于宽度
        height = maxSize
        width = Math.round(maxSize * aspectRatio)
      }
    }
    
    const scale = width / originalWidth
    
    return { width, height, scale }
  },

  // 格式化媒体时长
  formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = Math.floor(seconds % 60)
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    } else {
      return `${minutes}:${secs.toString().padStart(2, '0')}`
    }
  },

  // 检查浏览器是否支持某种媒体格式
  isBrowserSupported(mimeType: string): boolean {
    const video = document.createElement('video')
    const audio = document.createElement('audio')
    
    if (mimeType.startsWith('video/')) {
      return video.canPlayType(mimeType) !== ''
    }
    
    if (mimeType.startsWith('audio/')) {
      return audio.canPlayType(mimeType) !== ''
    }
    
    return false
  },

  // 生成预览URL的缓存键
  generateCacheKey(fileId: number, type: string, params?: Record<string, any>): string {
    const paramString = params ? JSON.stringify(params) : ''
    return `preview_${fileId}_${type}_${btoa(paramString)}`
  },
}
