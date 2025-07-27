import { defineStore } from 'pinia'
import type { 
  IFileInfo, 
  IFolderInfo, 
  IApiResponse, 
  IPagedResponse 
} from '@/types/index'
import { 
  searchApi, 
  type SearchRequest, 
  type SearchResult, 
  type SearchSuggestion,
  type AdvancedSearchRequest 
} from '@/apis/searchApi'

// 搜索历史记录项
interface SearchHistoryItem {
  id: string
  keyword: string
  timestamp: number
  resultCount: number
  spaceId?: number
}

// 搜索缓存项
interface SearchCacheItem {
  key: string
  result: SearchResult
  timestamp: number
  expiry: number
}

// 搜索状态
interface SearchState {
  // 当前搜索
  currentKeyword: string
  currentSpaceId: number | null
  
  // 搜索结果
  searchResults: SearchResult | null
  isSearching: boolean
  searchError: string | null
  
  // 搜索历史
  searchHistory: SearchHistoryItem[]
  
  // 搜索建议
  suggestions: SearchSuggestion[]
  loadingSuggestions: boolean
  
  // 高级搜索
  advancedFilters: {
    fileType: string[]
    dateCreated: { start?: string; end?: string }
    dateModified: { start?: string; end?: string }
    fileSize: { min?: number; max?: number }
    owner: number[]
    tags: string[]
  }
  
  // 缓存
  resultCache: Map<string, SearchCacheItem>
  
  // 配置
  maxHistoryItems: number
  cacheExpiry: number // 缓存过期时间（毫秒）
}

export const useSearchStore = defineStore('search', {
  state: (): SearchState => ({
    currentKeyword: '',
    currentSpaceId: null,
    
    searchResults: null,
    isSearching: false,
    searchError: null,
    
    searchHistory: JSON.parse(localStorage.getItem('searchHistory') || '[]'),
    
    suggestions: [],
    loadingSuggestions: false,
    
    advancedFilters: {
      fileType: [],
      dateCreated: {},
      dateModified: {},
      fileSize: {},
      owner: [],
      tags: []
    },
    
    resultCache: new Map(),
    
    maxHistoryItems: 50,
    cacheExpiry: 5 * 60 * 1000 // 5分钟
  }),

  getters: {
    // 是否有搜索结果
    hasResults: (state): boolean => {
      return !!(state.searchResults && (
        state.searchResults.files.length > 0 || 
        state.searchResults.folders.length > 0
      ))
    },

    // 搜索结果统计
    resultStats: (state) => {
      if (!state.searchResults) {
        return { files: 0, folders: 0, total: 0 }
      }
      return {
        files: state.searchResults.files.length,
        folders: state.searchResults.folders.length,
        total: state.searchResults.total
      }
    },

    // 最近搜索历史（限制数量）
    recentSearches: (state) => {
      return state.searchHistory
        .sort((a, b) => b.timestamp - a.timestamp)
        .slice(0, 10)
    },

    // 热门搜索关键词
    popularKeywords: (state) => {
      const keywordCount = new Map<string, number>()
      
      state.searchHistory.forEach(item => {
        const count = keywordCount.get(item.keyword) || 0
        keywordCount.set(item.keyword, count + 1)
      })
      
      return Array.from(keywordCount.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .map(([keyword, count]) => ({ keyword, count }))
    },

    // 是否有活跃的筛选条件
    hasActiveFilters: (state): boolean => {
      const filters = state.advancedFilters
      return !!(
        filters.fileType.length > 0 ||
        filters.dateCreated.start || filters.dateCreated.end ||
        filters.dateModified.start || filters.dateModified.end ||
        filters.fileSize.min !== undefined || filters.fileSize.max !== undefined ||
        filters.owner.length > 0 ||
        filters.tags.length > 0
      )
    }
  },

  actions: {
    // 快速搜索
    async quickSearch(keyword: string, spaceId?: number, limit: number = 10) {
      if (!keyword.trim()) {
        this.clearResults()
        return
      }

      this.isSearching = true
      this.searchError = null
      this.currentKeyword = keyword
      this.currentSpaceId = spaceId || null

      try {
        // 检查缓存
        const cacheKey = `quick_${keyword}_${spaceId || 'all'}_${limit}`
        const cached = this.getCachedResult(cacheKey)
        if (cached) {
          this.searchResults = cached
          return cached
        }

        const response = await searchApi.quickSearch({
          keyword,
          spaceId,
          limit
        })

        if (response.success && response.data) {
          const result: SearchResult = {
            files: response.data.files,
            folders: response.data.folders,
            total: response.data.total,
            searchTime: 0,
            suggestions: []
          }

          this.searchResults = result
          this.cacheResult(cacheKey, result)
          this.addToHistory(keyword, result.total, spaceId)

          return result
        }
      } catch (error) {
        this.searchError = error instanceof Error ? error.message : '搜索失败'
        console.error('Quick search failed:', error)
      } finally {
        this.isSearching = false
      }
    },

    // 全文搜索
    async fullTextSearch(request: SearchRequest) {
      this.isSearching = true
      this.searchError = null
      this.currentKeyword = request.keyword
      this.currentSpaceId = request.spaceId || null

      try {
        const cacheKey = `fulltext_${JSON.stringify(request)}`
        const cached = this.getCachedResult(cacheKey)
        if (cached) {
          this.searchResults = cached
          return cached
        }

        const response = await searchApi.fullTextSearch(request)

        if (response.success && response.data) {
          // 对于分页响应，data 是数组，我们需要构造 SearchResult
          const result: SearchResult = Array.isArray(response.data)
            ? response.data[0] || { files: [], folders: [], total: 0, searchTime: 0 }
            : response.data

          this.searchResults = result
          this.cacheResult(cacheKey, result)
          this.addToHistory(request.keyword, result.total, request.spaceId)

          return result
        }
      } catch (error) {
        this.searchError = error instanceof Error ? error.message : '搜索失败'
        console.error('Full text search failed:', error)
      } finally {
        this.isSearching = false
      }
    },

    // 高级搜索
    async advancedSearch(request: AdvancedSearchRequest) {
      this.isSearching = true
      this.searchError = null
      this.currentKeyword = request.query
      this.currentSpaceId = request.spaceId || null

      try {
        const cacheKey = `advanced_${JSON.stringify(request)}`
        const cached = this.getCachedResult(cacheKey)
        if (cached) {
          this.searchResults = cached
          return cached
        }

        const response = await searchApi.advancedSearch(request)

        if (response.success && response.data) {
          // 对于分页响应，data 是数组，我们需要构造 SearchResult
          const result: SearchResult = Array.isArray(response.data)
            ? response.data[0] || { files: [], folders: [], total: 0, searchTime: 0 }
            : response.data

          this.searchResults = result
          this.cacheResult(cacheKey, result)
          this.addToHistory(request.query, result.total, request.spaceId)

          return result
        }
      } catch (error) {
        this.searchError = error instanceof Error ? error.message : '搜索失败'
        console.error('Advanced search failed:', error)
      } finally {
        this.isSearching = false
      }
    },

    // 获取搜索建议
    async getSuggestions(keyword: string, spaceId?: number) {
      if (!keyword.trim()) {
        this.suggestions = []
        return
      }

      this.loadingSuggestions = true
      try {
        const response = await searchApi.getSuggestions({
          keyword,
          spaceId,
          limit: 10
        })

        if (response.success && response.data) {
          this.suggestions = response.data
        }
      } catch (error) {
        console.error('Get suggestions failed:', error)
      } finally {
        this.loadingSuggestions = false
      }
    },

    // 添加到搜索历史
    addToHistory(keyword: string, resultCount: number, spaceId?: number) {
      const historyItem: SearchHistoryItem = {
        id: `${Date.now()}_${Math.random()}`,
        keyword: keyword.trim(),
        timestamp: Date.now(),
        resultCount,
        spaceId
      }

      // 移除重复的关键词
      this.searchHistory = this.searchHistory.filter(
        item => item.keyword !== historyItem.keyword || item.spaceId !== spaceId
      )

      // 添加到开头
      this.searchHistory.unshift(historyItem)

      // 限制历史记录数量
      if (this.searchHistory.length > this.maxHistoryItems) {
        this.searchHistory = this.searchHistory.slice(0, this.maxHistoryItems)
      }

      // 保存到localStorage
      localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory))
    },

    // 删除搜索历史项
    removeFromHistory(id: string) {
      this.searchHistory = this.searchHistory.filter(item => item.id !== id)
      localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory))
    },

    // 清空搜索历史
    clearHistory() {
      this.searchHistory = []
      localStorage.removeItem('searchHistory')
    },

    // 设置高级筛选条件
    setAdvancedFilters(filters: Partial<SearchState['advancedFilters']>) {
      this.advancedFilters = { ...this.advancedFilters, ...filters }
    },

    // 重置高级筛选条件
    resetAdvancedFilters() {
      this.advancedFilters = {
        fileType: [],
        dateCreated: {},
        dateModified: {},
        fileSize: {},
        owner: [],
        tags: []
      }
    },

    // 清除搜索结果
    clearResults() {
      this.searchResults = null
      this.searchError = null
      this.currentKeyword = ''
      this.suggestions = []
    },

    // 缓存搜索结果
    cacheResult(key: string, result: SearchResult) {
      const now = Date.now()
      this.resultCache.set(key, {
        key,
        result,
        timestamp: now,
        expiry: now + this.cacheExpiry
      })

      // 清理过期缓存
      this.cleanExpiredCache()
    },

    // 获取缓存的搜索结果
    getCachedResult(key: string): SearchResult | null {
      const cached = this.resultCache.get(key)
      if (!cached) return null

      if (Date.now() > cached.expiry) {
        this.resultCache.delete(key)
        return null
      }

      return cached.result
    },

    // 清理过期缓存
    cleanExpiredCache() {
      const now = Date.now()
      for (const [key, cached] of this.resultCache.entries()) {
        if (now > cached.expiry) {
          this.resultCache.delete(key)
        }
      }
    },

    // 清空所有缓存
    clearCache() {
      this.resultCache.clear()
    }
  }
})
