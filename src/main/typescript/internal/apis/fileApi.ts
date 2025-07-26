import request from './request'
import type {
  FileInfo,
  FileUploadRequest,
  FileUploadResponse,
  ChunkedUploadInitRequest,
  ChunkedUploadInitResponse,
  FileUpdateRequest,
  FileCopyRequest,
  FileMoveRequest,
  FileSearchRequest,
  FileStatistics,
  FolderInfo,
  CreateFolderRequest,
  UpdateFolderRequest,
  MoveFolderRequest,
  CopyFolderRequest,
  FolderTreeNode,
  FolderContent,
  FolderSearchRequest,
  BatchFolderOperationRequest,
  BatchFileOperationRequest,
  ApiResponse,
  PageResponse,
  PaginationParams,
  FilterOption,
  SortOption
} from '@/types/file'

// 文件相关API
export const fileApi = {
  // 上传文件
  async upload(data: FormData, onProgress?: (progress: number) => void): Promise<FileUploadResponse> {
    const response = await request.post('/files/upload', data, {
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
  async initChunkedUpload(data: ChunkedUploadInitRequest): Promise<ChunkedUploadInitResponse> {
    const response = await request.post('/files/upload/chunked/init', data)
    return response.data
  },

  // 上传分块
  async uploadChunk(uploadId: string, chunkNumber: number, chunk: Blob, chunkMd5?: string): Promise<void> {
    const formData = new FormData()
    formData.append('uploadId', uploadId)
    formData.append('chunkNumber', chunkNumber.toString())
    formData.append('chunk', chunk)
    if (chunkMd5) {
      formData.append('chunkMd5', chunkMd5)
    }

    await request.post('/files/upload/chunked/chunk', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // 完成分块上传
  async completeChunkedUpload(uploadId: string, totalChunks: number): Promise<FileInfo> {
    const response = await request.post('/files/upload/chunked/complete', {
      uploadId,
      totalChunks,
    })
    return response.data
  },

  // 获取文件信息
  async getFileInfo(fileId: number): Promise<FileInfo> {
    const response = await request.get(`/files/${fileId}`)
    return response.data
  },

  // 更新文件信息
  async updateFile(fileId: number, data: FileUpdateRequest): Promise<FileInfo> {
    const response = await request.put(`/files/${fileId}`, data)
    return response.data
  },

  // 删除文件
  async deleteFile(fileId: number): Promise<void> {
    await request.delete(`/files/${fileId}`)
  },

  // 下载文件
  downloadFile(fileId: number): string {
    return `/api/files/${fileId}/download`
  },

  // 复制文件
  async copyFile(fileId: number, data: FileCopyRequest): Promise<FileInfo> {
    const response = await request.post(`/files/${fileId}/copy`, data)
    return response.data
  },

  // 移动文件
  async moveFile(fileId: number, data: FileMoveRequest): Promise<FileInfo> {
    const response = await request.post(`/files/${fileId}/move`, data)
    return response.data
  },

  // 搜索文件
  async searchFiles(
    params: FileSearchRequest & PaginationParams
  ): Promise<PageResponse<FileInfo>> {
    const response = await request.get('/files/search', { params })
    return response.data
  },

  // 获取文件统计
  async getFileStatistics(spaceId?: number): Promise<FileStatistics> {
    const response = await request.get('/files/statistics', {
      params: spaceId ? { spaceId } : undefined,
    })
    return response.data
  },

  // 批量删除文件
  async batchDeleteFiles(fileIds: number[]): Promise<void> {
    await request.post('/files/batch/delete', { fileIds })
  },

  // 批量移动文件
  async batchMoveFiles(fileIds: number[], targetFolderId: number, targetSpaceId?: number): Promise<void> {
    await request.post('/files/batch/move', {
      fileIds,
      targetFolderId,
      targetSpaceId,
    })
  },

  // 批量复制文件
  async batchCopyFiles(fileIds: number[], targetFolderId: number, targetSpaceId?: number): Promise<void> {
    await request.post('/files/batch/copy', {
      fileIds,
      targetFolderId,
      targetSpaceId,
    })
  },

  // 获取文件预览URL
  getPreviewUrl(fileId: number): string {
    return `/api/files/${fileId}/preview`
  },

  // 获取文件缩略图URL
  getThumbnailUrl(fileId: number): string {
    return `/api/files/${fileId}/thumbnail`
  },
}

// 文件夹相关API
export const folderApi = {
  // 创建文件夹
  async createFolder(data: CreateFolderRequest): Promise<FolderInfo> {
    const response = await request.post('/folders', data)
    return response.data
  },

  // 获取文件夹列表
  async getFolders(params: {
    spaceId?: number
    parentId?: number
    page?: number
    size?: number
    sort?: SortOption
  } = {}): Promise<PageResponse<FolderInfo>> {
    const response = await request.get('/folders', { params })
    return response.data
  },

  // 获取文件夹详情
  async getFolderDetail(folderId: number): Promise<FolderInfo> {
    const response = await request.get(`/folders/${folderId}`)
    return response.data
  },

  // 获取文件夹内容
  async getFolderContent(
    folderId: number,
    params: PaginationParams & FilterOption = { page: 0, size: 20 }
  ): Promise<FolderContent> {
    const response = await request.get(`/folders/${folderId}/content`, { params })
    return response.data
  },

  // 更新文件夹
  async updateFolder(folderId: number, data: UpdateFolderRequest): Promise<FolderInfo> {
    const response = await request.put(`/folders/${folderId}`, data)
    return response.data
  },

  // 删除文件夹
  async deleteFolder(folderId: number): Promise<void> {
    await request.delete(`/folders/${folderId}`)
  },

  // 移动文件夹
  async moveFolder(folderId: number, data: MoveFolderRequest): Promise<FolderInfo> {
    const response = await request.post(`/folders/${folderId}/move`, data)
    return response.data
  },

  // 复制文件夹
  async copyFolder(folderId: number, data: CopyFolderRequest): Promise<FolderInfo> {
    const response = await request.post(`/folders/${folderId}/copy`, data)
    return response.data
  },

  // 获取文件夹树
  async getFolderTree(spaceId?: number, maxDepth?: number): Promise<FolderTreeNode[]> {
    const response = await request.get('/folders/tree', {
      params: { spaceId, maxDepth },
    })
    return response.data
  },

  // 搜索文件夹
  async searchFolders(
    params: FolderSearchRequest & PaginationParams
  ): Promise<PageResponse<FolderInfo>> {
    const response = await request.get('/folders/search', { params })
    return response.data
  },

  // 批量删除文件夹
  async batchDeleteFolders(folderIds: number[]): Promise<void> {
    await request.post('/folders/batch/delete', { folderIds })
  },

  // 批量移动文件夹
  async batchMoveFolders(
    folderIds: number[], 
    targetParentId: number, 
    targetSpaceId?: number
  ): Promise<void> {
    await request.post('/folders/batch/move', {
      folderIds,
      targetParentId,
      targetSpaceId,
    })
  },

  // 获取面包屑导航
  async getBreadcrumb(folderId: number): Promise<Array<{ id: number; name: string; path: string }>> {
    const response = await request.get(`/folders/${folderId}/breadcrumb`)
    return response.data
  },
}

// 空间相关API（简化版，用于文件管理）
export const spaceApi = {
  // 获取用户空间列表
  async getUserSpaces(): Promise<Array<{
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
  }>> {
    const response = await request.get('/spaces/user')
    return response.data
  },

  // 获取空间根文件夹
  async getSpaceRootFolder(spaceId: number): Promise<FolderInfo> {
    const response = await request.get(`/spaces/${spaceId}/root`)
    return response.data
  },
}

// 工具函数
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

  // 生成文件缩略图URL
  generateThumbnailUrl(fileId: number, size: 'small' | 'medium' | 'large' = 'medium'): string {
    return `/api/files/${fileId}/thumbnail?size=${size}`
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
} 