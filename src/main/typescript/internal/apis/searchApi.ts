import request from './request'
import type {
  IFileInfo,
  IFolderInfo,
  IApiResponse,
  IPagedResponse
} from '@/types/index'

// ==================== 搜索相关类型 ====================
export interface SearchRequest {
  keyword: string
  spaceId?: number
  type?: 'file' | 'folder' | 'all'
  mimeType?: string
  dateRange?: {
    start: string
    end: string
  }
  sizeRange?: {
    min: number
    max: number
  }
  includeDeleted?: boolean
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

export interface SearchResult {
  files: IFileInfo[]
  folders: IFolderInfo[]
  total: number
  searchTime: number
  suggestions?: string[]
}

export interface SearchSuggestion {
  text: string
  type: 'keyword' | 'filename' | 'tag'
  count: number
}

export interface SearchFilter {
  name: string
  value: string
  count: number
}

export interface AdvancedSearchRequest {
  query: string
  spaceId?: number
  filters: {
    fileType?: string[]
    dateCreated?: {
      start?: string
      end?: string
    }
    dateModified?: {
      start?: string
      end?: string
    }
    fileSize?: {
      min?: number
      max?: number
    }
    owner?: number[]
    tags?: string[]
  }
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

// ==================== 搜索API ====================
export const searchApi = {
  // 快速搜索
  async quickSearch(params: {
    keyword: string
    spaceId?: number
    limit?: number
  }): Promise<IApiResponse<{
    files: IFileInfo[]
    folders: IFolderInfo[]
    total: number
  }>> {
    const response = await request.get('/api/search/quick', { params })
    return response.data
  },

  // 全文搜索
  async fullTextSearch(data: SearchRequest): Promise<IPagedResponse<SearchResult>> {
    const response = await request.post('/api/search/fulltext', data)
    return response.data
  },

  // 高级搜索
  async advancedSearch(data: AdvancedSearchRequest): Promise<IPagedResponse<SearchResult>> {
    const response = await request.post('/api/search/advanced', data)
    return response.data
  },

  // 获取搜索建议
  async getSearchSuggestions(params: {
    keyword: string
    spaceId?: number
    limit?: number
  }): Promise<IApiResponse<SearchSuggestion[]>> {
    const response = await request.get('/api/search/suggestions', { params })
    return response.data
  },

  // 获取搜索过滤器
  async getSearchFilters(params: {
    keyword: string
    spaceId?: number
  }): Promise<IApiResponse<{
    fileTypes: SearchFilter[]
    owners: SearchFilter[]
    tags: SearchFilter[]
    dateRanges: SearchFilter[]
  }>> {
    const response = await request.get('/api/search/filters', { params })
    return response.data
  },

  // 保存搜索历史
  async saveSearchHistory(data: {
    keyword: string
    spaceId?: number
    resultCount: number
  }): Promise<IApiResponse<void>> {
    const response = await request.post('/api/search/history', data)
    return response.data
  },

  // 获取搜索历史
  async getSearchHistory(params?: {
    spaceId?: number
    limit?: number
  }): Promise<IApiResponse<Array<{
    id: number
    keyword: string
    spaceId?: number
    resultCount: number
    searchedAt: string
  }>>> {
    const response = await request.get('/api/search/history', { params })
    return response.data
  },

  // 删除搜索历史
  async deleteSearchHistory(historyId?: number): Promise<IApiResponse<void>> {
    const url = historyId ? `/api/search/history/${historyId}` : '/api/search/history'
    const response = await request.delete(url)
    return response.data
  },

  // 获取热门搜索
  async getPopularSearches(params?: {
    spaceId?: number
    limit?: number
    period?: 'day' | 'week' | 'month'
  }): Promise<IApiResponse<Array<{
    keyword: string
    count: number
    trend: 'up' | 'down' | 'stable'
  }>>> {
    const response = await request.get('/api/search/popular', { params })
    return response.data
  },

  // 搜索文件内容
  async searchFileContent(params: {
    keyword: string
    spaceId?: number
    fileTypes?: string[]
    page?: number
    size?: number
  }): Promise<IPagedResponse<{
    file: IFileInfo
    matches: Array<{
      line: number
      content: string
      highlight: string
    }>
  }>> {
    const response = await request.get('/api/search/content', { params })
    return response.data
  },

  // 按标签搜索
  async searchByTags(params: {
    tags: string[]
    spaceId?: number
    page?: number
    size?: number
  }): Promise<IPagedResponse<IFileInfo>> {
    const response = await request.get('/api/search/tags', { params })
    return response.data
  },

  // 相似文件搜索
  async findSimilarFiles(fileId: number, params?: {
    spaceId?: number
    limit?: number
    similarity?: number
  }): Promise<IApiResponse<Array<{
    file: IFileInfo
    similarity: number
    reason: string
  }>>> {
    const response = await request.get(`/api/search/similar/${fileId}`, { params })
    return response.data
  },

  // 重复文件检测
  async findDuplicateFiles(params?: {
    spaceId?: number
    algorithm?: 'hash' | 'name' | 'content'
    page?: number
    size?: number
  }): Promise<IPagedResponse<Array<{
    hash: string
    files: IFileInfo[]
    totalSize: number
  }>>> {
    const response = await request.get('/api/search/duplicates', { params })
    return response.data
  },
}

// ==================== 搜索工具函数 ====================
export const searchUtils = {
  // 高亮搜索关键词
  highlightKeyword(text: string, keyword: string): string {
    if (!keyword.trim()) return text
    const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
    return text.replace(regex, '<mark>$1</mark>')
  },

  // 构建搜索查询
  buildSearchQuery(filters: Record<string, any>): string {
    const parts: string[] = []
    
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value) && value.length > 0) {
          parts.push(`${key}:(${value.join(' OR ')})`)
        } else if (typeof value === 'object' && value.start && value.end) {
          parts.push(`${key}:[${value.start} TO ${value.end}]`)
        } else {
          parts.push(`${key}:${value}`)
        }
      }
    })
    
    return parts.join(' AND ')
  },

  // 解析搜索查询
  parseSearchQuery(query: string): Record<string, any> {
    const filters: Record<string, any> = {}
    const regex = /(\w+):(\[.*?\]|\(.*?\)|".*?"|[^\s]+)/g
    let match
    
    while ((match = regex.exec(query)) !== null) {
      const [, key, value] = match
      
      if (value.startsWith('[') && value.endsWith(']')) {
        // 范围查询
        const range = value.slice(1, -1).split(' TO ')
        if (range.length === 2) {
          filters[key] = { start: range[0], end: range[1] }
        }
      } else if (value.startsWith('(') && value.endsWith(')')) {
        // OR查询
        filters[key] = value.slice(1, -1).split(' OR ')
      } else {
        // 普通查询
        filters[key] = value.replace(/"/g, '')
      }
    }
    
    return filters
  },

  // 格式化搜索结果数量
  formatResultCount(count: number): string {
    if (count === 0) return '未找到结果'
    if (count === 1) return '找到 1 个结果'
    if (count < 1000) return `找到 ${count} 个结果`
    if (count < 1000000) return `找到约 ${Math.round(count / 1000)}k 个结果`
    return `找到约 ${Math.round(count / 1000000)}M 个结果`
  },

  // 生成搜索建议
  generateSuggestions(keyword: string, history: string[]): string[] {
    const suggestions = new Set<string>()
    
    // 添加历史搜索中的相关项
    history.forEach(item => {
      if (item.toLowerCase().includes(keyword.toLowerCase())) {
        suggestions.add(item)
      }
    })
    
    // 添加常见搜索模式
    const patterns = [
      `${keyword} 文档`,
      `${keyword} 图片`,
      `${keyword} 视频`,
      `最近的 ${keyword}`,
      `我的 ${keyword}`,
    ]
    
    patterns.forEach(pattern => {
      if (pattern !== keyword) {
        suggestions.add(pattern)
      }
    })
    
    return Array.from(suggestions).slice(0, 10)
  },

  // 验证搜索关键词
  validateSearchKeyword(keyword: string): { isValid: boolean; error?: string } {
    if (!keyword.trim()) {
      return { isValid: false, error: '搜索关键词不能为空' }
    }
    if (keyword.length > 200) {
      return { isValid: false, error: '搜索关键词不能超过200个字符' }
    }
    return { isValid: true }
  },
}
