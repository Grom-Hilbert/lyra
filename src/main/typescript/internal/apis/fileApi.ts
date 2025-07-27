import request from './request'
import type {
  IFileInfo,
  FileUploadRequest,
  FileUploadResponse,
  ChunkedUploadInitRequest,
  ChunkedUploadInitResponse,
  ChunkedUploadChunkRequest,
  ChunkedUploadCompleteRequest,
  FileSearchRequest,
  IFolderInfo,
  CreateFolderRequest,
  FolderTreeNode,
  IApiResponse,
  IPagedResponse
} from '@/types/index'

// ==================== 文件管理API ====================
export const fileApi = {
  // 上传文件
  async upload(data: FileUploadRequest, onProgress?: (progress: number) => void): Promise<IApiResponse<IFileInfo>> {
    const formData = new FormData()
    formData.append('file', data.file)
    formData.append('spaceId', data.spaceId.toString())
    if (data.folderId) {
      formData.append('folderId', data.folderId.toString())
    }
    if (data.description) {
      formData.append('description', data.description)
    }

    const response = await request.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      },
    })
    return response.data
  },

  // 分块上传初始化
  async initChunkedUpload(data: ChunkedUploadInitRequest): Promise<IApiResponse<ChunkedUploadInitResponse>> {
    const response = await request.post('/api/files/upload/chunked/init', data)
    return response.data
  },

  // 上传分块
  async uploadChunk(data: ChunkedUploadChunkRequest): Promise<IApiResponse<void>> {
    const formData = new FormData()
    formData.append('uploadId', data.uploadId)
    formData.append('chunkIndex', data.chunkIndex.toString())
    formData.append('chunk', data.chunk)

    const response = await request.post('/api/files/upload/chunked/chunk', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  },

  // 完成分块上传
  async completeChunkedUpload(data: ChunkedUploadCompleteRequest): Promise<IApiResponse<IFileInfo>> {
    const response = await request.post('/api/files/upload/chunked/complete', data)
    return response.data
  },

  // 获取文件信息
  async getFileInfo(fileId: number): Promise<IApiResponse<IFileInfo>> {
    const response = await request.get(`/api/files/${fileId}`)
    return response.data
  },

  // 更新文件信息
  async updateFile(fileId: number, data: { filename?: string; description?: string }): Promise<IApiResponse<IFileInfo>> {
    const response = await request.put(`/api/files/${fileId}`, data)
    return response.data
  },

  // 删除文件
  async deleteFile(fileId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/files/${fileId}`)
    return response.data
  },

  // 下载文件URL
  getDownloadUrl(fileId: number): string {
    return `/api/files/${fileId}/download`
  },

  // 搜索文件 (GET方式)
  async searchFiles(params: {
    spaceId: number
    query?: string
    mimeType?: string
    includeDeleted?: boolean
    page?: number
    size?: number
    sort?: string
    direction?: 'asc' | 'desc'
  }): Promise<IApiResponse<any>> {
    const response = await request.get('/api/files/search', { params })
    return response.data
  },

  // 搜索文件 (POST方式)
  async searchFilesPost(data: FileSearchRequest): Promise<IApiResponse<any>> {
    const response = await request.post('/api/files/search', data)
    return response.data
  },

  // 获取空间文件列表
  async getFilesBySpace(params: {
    spaceId: number
    folderId?: number
    page?: number
    size?: number
    sort?: string
    direction?: 'asc' | 'desc'
  }): Promise<IApiResponse<any>> {
    const response = await request.get(`/api/files/space/${params.spaceId}`, {
      params: { ...params, spaceId: undefined }
    })
    return response.data
  },

  // 获取文件统计
  async getFileStatistics(spaceId: number): Promise<IApiResponse<any>> {
    const response = await request.get(`/api/files/space/${spaceId}/statistics`)
    return response.data
  },

  // 批量上传文件
  async batchUpload(data: {
    files: File[]
    spaceId: number
    folderId?: number
  }, onProgress?: (progress: number) => void): Promise<IApiResponse<any>> {
    const formData = new FormData()
    data.files.forEach(file => formData.append('files', file))
    formData.append('spaceId', data.spaceId.toString())
    if (data.folderId) {
      formData.append('folderId', data.folderId.toString())
    }

    const response = await request.post('/api/files/batch-upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      },
    })
    return response.data
  },

  // 批量删除文件
  async batchDeleteFiles(data: { fileIds: number[] }): Promise<IApiResponse<any>> {
    const response = await request.post('/api/files/batch/delete', data)
    return response.data
  },

  // 批量移动文件
  async batchMoveFiles(data: {
    fileIds: number[]
    targetFolderId?: number
    targetSpaceId?: number
  }): Promise<IApiResponse<any>> {
    const response = await request.post('/api/files/batch/move', data)
    return response.data
  },

  // 批量复制文件
  async batchCopyFiles(data: {
    fileIds: number[]
    targetFolderId?: number
    targetSpaceId?: number
  }): Promise<IApiResponse<any>> {
    const response = await request.post('/api/files/batch/copy', data)
    return response.data
  },

  // 移动文件
  async moveFile(fileId: number, data: {
    targetFolderId?: number
    targetSpaceId?: number
  }): Promise<IApiResponse<any>> {
    const response = await request.post(`/api/files/${fileId}/move`, data)
    return response.data
  },

  // 复制文件
  async copyFile(fileId: number, data: {
    targetFolderId?: number
    targetSpaceId?: number
    newName?: string
  }): Promise<IApiResponse<any>> {
    const response = await request.post(`/api/files/${fileId}/copy`, data)
    return response.data
  },

  // 重命名文件
  async renameFile(fileId: number, data: { newName: string }): Promise<IApiResponse<any>> {
    const response = await request.post(`/api/files/${fileId}/rename`, data)
    return response.data
  },

  // 获取文件预览URL
  getPreviewUrl(fileId: number): string {
    return `/api/files/${fileId}/preview`
  },

  // 获取文件缩略图URL
  getThumbnailUrl(fileId: number): string {
    return `/api/files/${fileId}/thumbnail`
  },

  // ==================== 分享功能API ====================

  // 创建文件分享链接
  async createFileShare(fileId: number, data: {
    accessType?: 'read' | 'write'
    password?: string
    expiresAt?: string
    downloadLimit?: number
  }): Promise<IApiResponse<{
    id: number
    token: string
    shareUrl: string
    accessType: string
    expiresAt?: string
    downloadLimit?: number
  }>> {
    const response = await request.post(`/api/files/${fileId}/share`, data)
    return response.data
  },

  // 获取文件分享链接列表
  async getFileShares(fileId: number): Promise<IApiResponse<Array<{
    id: number
    token: string
    shareUrl: string
    accessType: string
    downloadCount: number
    downloadLimit?: number
    expiresAt?: string
    isActive: boolean
    createdAt: string
  }>>> {
    const response = await request.get(`/api/files/${fileId}/shares`)
    return response.data
  },

  // 删除文件分享链接
  async deleteFileShare(fileId: number, shareId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/files/${fileId}/shares/${shareId}`)
    return response.data
  },

  // 更新文件分享链接
  async updateFileShare(fileId: number, shareId: number, data: {
    accessType?: 'read' | 'write'
    password?: string
    expiresAt?: string
    downloadLimit?: number
    isActive?: boolean
  }): Promise<IApiResponse<void>> {
    const response = await request.put(`/api/files/${fileId}/shares/${shareId}`, data)
    return response.data
  },
}

// ==================== 文件夹管理API ====================
export const folderApi = {
  // 创建文件夹
  async createFolder(data: CreateFolderRequest): Promise<IApiResponse<IFolderInfo>> {
    const response = await request.post('/api/folders', data)
    return response.data
  },

  // 获取文件夹列表
  async getFolders(params: {
    spaceId: number
    parentId?: number
    page?: number
    size?: number
    sort?: string
    direction?: 'asc' | 'desc'
  }): Promise<IPagedResponse<IFolderInfo>> {
    const response = await request.get('/api/folders', { params })
    return response.data
  },

  // 获取文件夹详情
  async getFolderDetail(folderId: number): Promise<IApiResponse<IFolderInfo>> {
    const response = await request.get(`/api/folders/${folderId}`)
    return response.data
  },

  // 更新文件夹
  async updateFolder(folderId: number, data: { name?: string; description?: string }): Promise<IApiResponse<IFolderInfo>> {
    const response = await request.put(`/api/folders/${folderId}`, data)
    return response.data
  },

  // 删除文件夹
  async deleteFolder(folderId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/folders/${folderId}`)
    return response.data
  },

  // 获取文件夹树结构
  async getFolderTree(params: {
    spaceId: number
    maxDepth?: number
  }): Promise<IApiResponse<FolderTreeNode[]>> {
    const response = await request.get('/api/folders/tree', { params })
    return response.data
  },

  // 移动文件夹
  async moveFolder(folderId: number, data: {
    targetParentFolderId?: number
    targetSpaceId?: number
  }): Promise<IApiResponse<any>> {
    const response = await request.post('/api/folders/move', {
      folderId,
      ...data
    })
    return response.data
  },

  // ==================== 文件夹分享功能API ====================

  // 创建文件夹分享链接
  async createFolderShare(folderId: number, data: {
    accessType?: 'read' | 'write'
    password?: string
    expiresAt?: string
    downloadLimit?: number
  }): Promise<IApiResponse<{
    id: number
    token: string
    shareUrl: string
    accessType: string
    expiresAt?: string
    downloadLimit?: number
  }>> {
    const response = await request.post(`/api/folders/${folderId}/share`, data)
    return response.data
  },

  // 获取文件夹分享链接列表
  async getFolderShares(folderId: number): Promise<IApiResponse<Array<{
    id: number
    token: string
    shareUrl: string
    accessType: string
    downloadCount: number
    downloadLimit?: number
    expiresAt?: string
    isActive: boolean
    createdAt: string
  }>>> {
    const response = await request.get(`/api/folders/${folderId}/shares`)
    return response.data
  },

  // 删除文件夹分享链接
  async deleteFolderShare(folderId: number, shareId: number): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/folders/${folderId}/shares/${shareId}`)
    return response.data
  },

  // 更新文件夹分享链接
  async updateFolderShare(folderId: number, shareId: number, data: {
    accessType?: 'read' | 'write'
    password?: string
    expiresAt?: string
    downloadLimit?: number
    isActive?: boolean
  }): Promise<IApiResponse<void>> {
    const response = await request.put(`/api/folders/${folderId}/shares/${shareId}`, data)
    return response.data
  },
}

// ==================== 空间管理API (简化版) ====================
export const spaceApi = {
  // 获取用户空间列表
  async getUserSpaces(): Promise<IApiResponse<Array<{
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
  }>>> {
    const response = await request.get('/api/spaces/user')
    return response.data
  },

  // 获取空间根文件夹
  async getSpaceRootFolder(spaceId: number): Promise<IApiResponse<IFolderInfo>> {
    const response = await request.get(`/api/spaces/${spaceId}/root`)
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
}

// ==================== 工具函数 ====================
export const fileUtils = {
  // 格式化文件大小
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  },

  // 获取文件类型图标
  getFileTypeIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'lucide:image'
    if (mimeType.startsWith('video/')) return 'lucide:video'
    if (mimeType.startsWith('audio/')) return 'lucide:music'
    if (mimeType.includes('pdf')) return 'lucide:file-text'
    if (mimeType.includes('word')) return 'lucide:file-text'
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return 'lucide:file-spreadsheet'
    if (mimeType.includes('powerpoint') || mimeType.includes('presentation')) return 'lucide:presentation'
    if (mimeType.includes('zip') || mimeType.includes('archive')) return 'lucide:archive'
    if (mimeType.includes('text/')) return 'lucide:file-text'
    return 'lucide:file'
  },

  // 检查文件是否可以预览
  canPreview(mimeType: string): boolean {
    const previewableTypes = [
      'image/',
      'video/',
      'audio/',
      'text/',
      'application/pdf',
      'application/json',
    ]
    return previewableTypes.some(type => mimeType.startsWith(type))
  },

  // 检查文件是否可以编辑
  canEdit(mimeType: string): boolean {
    const editableTypes = [
      'text/',
      'application/json',
      'application/javascript',
      'application/xml',
    ]
    return editableTypes.some(type => mimeType.startsWith(type))
  },

  // 验证文件名
  validateFileName(name: string): { isValid: boolean; error?: string } {
    if (!name.trim()) {
      return { isValid: false, error: '文件名不能为空' }
    }
    if (name.length > 255) {
      return { isValid: false, error: '文件名不能超过255个字符' }
    }
    const invalidChars = /[<>:"/\\|?*]/
    if (invalidChars.test(name)) {
      return { isValid: false, error: '文件名包含非法字符' }
    }
    return { isValid: true }
  },

  // 获取文件扩展名
  getFileExtension(filename: string): string {
    return filename.split('.').pop()?.toLowerCase() || ''
  },

  // 构建下载链接
  buildDownloadUrl(fileId: number, filename?: string): string {
    const baseUrl = `/api/files/${fileId}/download`
    return filename ? `${baseUrl}?filename=${encodeURIComponent(filename)}` : baseUrl
  },

  // 检查上传状态（断点续传）
  async checkUploadStatus(data: {
    fileHash: string
    filename: string
    size: number
    spaceId: number
    folderId?: number
  }): Promise<IApiResponse<{ uploadedBytes: number; uploadId: string }>> {
    const response = await request.post('/api/files/check-upload', data)
    return response.data
  },

  // 分片上传
  async uploadChunk(data: FormData | ChunkedUploadChunkRequest): Promise<IApiResponse<void>> {
    if (data instanceof FormData) {
      const response = await request.post('/api/files/upload-chunk', data, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      return response.data
    } else {
      const response = await request.post('/api/files/upload/chunked/chunk', data)
      return response.data
    }
  },

  // 完成分片上传
  async completeUpload(data: {
    filename: string
    totalChunks: number
    spaceId: number
    folderId?: number
  }): Promise<IApiResponse<IFileInfo>> {
    const response = await request.post('/api/files/complete-upload', data)
    return response.data
  },
}