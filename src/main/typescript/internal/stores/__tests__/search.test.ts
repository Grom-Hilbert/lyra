import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSearchStore } from '../search'

// Mock searchApi before importing
vi.mock('@/apis/searchApi', () => ({
  searchApi: {
    quickSearch: vi.fn(),
    fullTextSearch: vi.fn(),
    advancedSearch: vi.fn(),
    getSuggestions: vi.fn()
  }
}))

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn()
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

describe('useSearchStore', () => {
  let searchStore: any
  let mockSearchApi: any

  beforeEach(async () => {
    setActivePinia(createPinia())
    // Get the mocked searchApi
    const { searchApi } = await import('@/apis/searchApi')
    mockSearchApi = searchApi

    searchStore = useSearchStore()
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue('[]')
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      expect(searchStore.currentKeyword).toBe('')
      expect(searchStore.currentSpaceId).toBeNull()
      expect(searchStore.searchResults).toBeNull()
      expect(searchStore.isSearching).toBe(false)
      expect(searchStore.searchError).toBeNull()
      expect(searchStore.suggestions).toEqual([])
      expect(searchStore.loadingSuggestions).toBe(false)
    })
  })

  describe('getters', () => {
    it('hasResults 应该正确判断是否有结果', () => {
      expect(searchStore.hasResults).toBe(false)

      searchStore.searchResults = {
        files: [{ id: 1, name: 'test.txt' }],
        folders: [],
        total: 1,
        searchTime: 100,
        suggestions: []
      }

      expect(searchStore.hasResults).toBe(true)
    })

    it('resultStats 应该返回正确的统计信息', () => {
      searchStore.searchResults = {
        files: [{ id: 1 }, { id: 2 }],
        folders: [{ id: 1 }],
        total: 3,
        searchTime: 100,
        suggestions: []
      }

      const stats = searchStore.resultStats
      expect(stats.files).toBe(2)
      expect(stats.folders).toBe(1)
      expect(stats.total).toBe(3)
    })

    it('recentSearches 应该返回最近的搜索记录', () => {
      const now = Date.now()
      searchStore.searchHistory = [
        { id: '1', keyword: 'old', timestamp: now - 1000, resultCount: 1 },
        { id: '2', keyword: 'new', timestamp: now, resultCount: 2 }
      ]

      const recent = searchStore.recentSearches
      expect(recent[0].keyword).toBe('new')
      expect(recent[1].keyword).toBe('old')
    })

    it('popularKeywords 应该返回热门关键词', () => {
      searchStore.searchHistory = [
        { id: '1', keyword: 'test', timestamp: 1, resultCount: 1 },
        { id: '2', keyword: 'test', timestamp: 2, resultCount: 1 },
        { id: '3', keyword: 'doc', timestamp: 3, resultCount: 1 }
      ]

      const popular = searchStore.popularKeywords
      expect(popular[0].keyword).toBe('test')
      expect(popular[0].count).toBe(2)
      expect(popular[1].keyword).toBe('doc')
      expect(popular[1].count).toBe(1)
    })

    it('hasActiveFilters 应该正确判断是否有活跃筛选', () => {
      expect(searchStore.hasActiveFilters).toBe(false)

      searchStore.advancedFilters.fileType = ['document']
      expect(searchStore.hasActiveFilters).toBe(true)
    })
  })

  describe('actions', () => {
    describe('quickSearch', () => {
      it('应该执行快速搜索', async () => {
        const mockResponse = {
          success: true,
          data: {
            files: [{ id: 1, name: 'test.txt' }],
            folders: [],
            total: 1
          }
        }

        mockSearchApi.quickSearch.mockResolvedValue(mockResponse)

        const result = await searchStore.quickSearch('test', 1, 10)

        expect(mockSearchApi.quickSearch).toHaveBeenCalledWith({
          keyword: 'test',
          spaceId: 1,
          limit: 10
        })

        expect(searchStore.currentKeyword).toBe('test')
        expect(searchStore.currentSpaceId).toBe(1)
        expect(result.files).toHaveLength(1)
      })

      it('应该处理空关键词', async () => {
        await searchStore.quickSearch('', 1, 10)

        expect(mockSearchApi.quickSearch).not.toHaveBeenCalled()
        expect(searchStore.searchResults).toBeNull()
      })

      it('应该处理搜索错误', async () => {
        mockSearchApi.quickSearch.mockRejectedValue(new Error('搜索失败'))

        await searchStore.quickSearch('test', 1, 10)

        expect(searchStore.searchError).toBe('搜索失败')
        expect(searchStore.isSearching).toBe(false)
      })
    })

    describe('fullTextSearch', () => {
      it('应该执行全文搜索', async () => {
        const mockResponse = {
          success: true,
          data: {
            files: [],
            folders: [],
            total: 0,
            searchTime: 50,
            suggestions: []
          }
        }

        mockSearchApi.fullTextSearch.mockResolvedValue(mockResponse)

        const request = {
          keyword: 'test',
          spaceId: 1,
          type: 'all' as const
        }

        await searchStore.fullTextSearch(request)

        expect(mockSearchApi.fullTextSearch).toHaveBeenCalledWith(request)
        expect(searchStore.searchResults).toEqual(mockResponse.data)
      })
    })

    describe('advancedSearch', () => {
      it('应该执行高级搜索', async () => {
        const mockResponse = {
          success: true,
          data: {
            files: [],
            folders: [],
            total: 0,
            searchTime: 75,
            suggestions: []
          }
        }

        mockSearchApi.advancedSearch.mockResolvedValue(mockResponse)

        const request = {
          query: 'test',
          spaceId: 1,
          filters: {
            fileType: ['document']
          }
        }

        await searchStore.advancedSearch(request)

        expect(mockSearchApi.advancedSearch).toHaveBeenCalledWith(request)
        expect(searchStore.searchResults).toEqual(mockResponse.data)
      })
    })

    describe('getSuggestions', () => {
      it('应该获取搜索建议', async () => {
        const mockSuggestions = [
          { text: 'test.txt', type: 'filename', count: 1 }
        ]

        mockSearchApi.getSuggestions.mockResolvedValue({
          success: true,
          data: mockSuggestions
        })

        await searchStore.getSuggestions('test', 1)

        expect(mockSearchApi.getSuggestions).toHaveBeenCalledWith({
          keyword: 'test',
          spaceId: 1,
          limit: 10
        })

        expect(searchStore.suggestions).toEqual(mockSuggestions)
      })

      it('应该处理空关键词', async () => {
        await searchStore.getSuggestions('', 1)

        expect(mockSearchApi.getSuggestions).not.toHaveBeenCalled()
        expect(searchStore.suggestions).toEqual([])
      })
    })

    describe('搜索历史管理', () => {
      it('应该添加搜索历史', () => {
        searchStore.addToHistory('test', 5, 1)

        expect(searchStore.searchHistory).toHaveLength(1)
        expect(searchStore.searchHistory[0].keyword).toBe('test')
        expect(searchStore.searchHistory[0].resultCount).toBe(5)
        expect(searchStore.searchHistory[0].spaceId).toBe(1)
        expect(localStorageMock.setItem).toHaveBeenCalled()
      })

      it('应该移除重复的搜索历史', () => {
        searchStore.addToHistory('test', 5, 1)
        searchStore.addToHistory('test', 3, 1)

        expect(searchStore.searchHistory).toHaveLength(1)
        expect(searchStore.searchHistory[0].resultCount).toBe(3)
      })

      it('应该限制搜索历史数量', () => {
        searchStore.maxHistoryItems = 2

        searchStore.addToHistory('test1', 1, 1)
        searchStore.addToHistory('test2', 2, 1)
        searchStore.addToHistory('test3', 3, 1)

        expect(searchStore.searchHistory).toHaveLength(2)
        expect(searchStore.searchHistory[0].keyword).toBe('test3')
        expect(searchStore.searchHistory[1].keyword).toBe('test2')
      })

      it('应该删除指定的搜索历史', () => {
        searchStore.addToHistory('test', 5, 1)
        const historyId = searchStore.searchHistory[0].id

        searchStore.removeFromHistory(historyId)

        expect(searchStore.searchHistory).toHaveLength(0)
        expect(localStorageMock.setItem).toHaveBeenCalled()
      })

      it('应该清空搜索历史', () => {
        searchStore.addToHistory('test', 5, 1)
        searchStore.clearHistory()

        expect(searchStore.searchHistory).toHaveLength(0)
        expect(localStorageMock.removeItem).toHaveBeenCalledWith('searchHistory')
      })
    })

    describe('高级筛选管理', () => {
      it('应该设置高级筛选条件', () => {
        const filters = {
          fileType: ['document', 'image'],
          fileSize: { min: 100, max: 1000 }
        }

        searchStore.setAdvancedFilters(filters)

        expect(searchStore.advancedFilters.fileType).toEqual(['document', 'image'])
        expect(searchStore.advancedFilters.fileSize).toEqual({ min: 100, max: 1000 })
      })

      it('应该重置高级筛选条件', () => {
        searchStore.advancedFilters.fileType = ['document']
        searchStore.advancedFilters.fileSize = { min: 100 }

        searchStore.resetAdvancedFilters()

        expect(searchStore.advancedFilters.fileType).toEqual([])
        expect(searchStore.advancedFilters.fileSize).toEqual({})
      })
    })

    describe('缓存管理', () => {
      it('应该缓存搜索结果', () => {
        const result = {
          files: [],
          folders: [],
          total: 0,
          searchTime: 100,
          suggestions: []
        }

        searchStore.cacheResult('test-key', result)

        expect(searchStore.resultCache.has('test-key')).toBe(true)
      })

      it('应该获取缓存的搜索结果', () => {
        const result = {
          files: [],
          folders: [],
          total: 0,
          searchTime: 100,
          suggestions: []
        }

        searchStore.cacheResult('test-key', result)
        const cached = searchStore.getCachedResult('test-key')

        expect(cached).toEqual(result)
      })

      it('应该清理过期缓存', () => {
        const result = {
          files: [],
          folders: [],
          total: 0,
          searchTime: 100,
          suggestions: []
        }

        // 设置一个已过期的缓存
        const now = Date.now()
        searchStore.resultCache.set('expired-key', {
          key: 'expired-key',
          result,
          timestamp: now - 10000,
          expiry: now - 5000
        })

        const cached = searchStore.getCachedResult('expired-key')
        expect(cached).toBeNull()
        expect(searchStore.resultCache.has('expired-key')).toBe(false)
      })

      it('应该清空所有缓存', () => {
        searchStore.cacheResult('key1', { files: [], folders: [], total: 0, searchTime: 0, suggestions: [] })
        searchStore.cacheResult('key2', { files: [], folders: [], total: 0, searchTime: 0, suggestions: [] })

        searchStore.clearCache()

        expect(searchStore.resultCache.size).toBe(0)
      })
    })

    describe('clearResults', () => {
      it('应该清除搜索结果', () => {
        searchStore.searchResults = { files: [], folders: [], total: 0, searchTime: 0, suggestions: [] }
        searchStore.searchError = 'error'
        searchStore.currentKeyword = 'test'
        searchStore.suggestions = [{ text: 'test', type: 'keyword', count: 1 }]

        searchStore.clearResults()

        expect(searchStore.searchResults).toBeNull()
        expect(searchStore.searchError).toBeNull()
        expect(searchStore.currentKeyword).toBe('')
        expect(searchStore.suggestions).toEqual([])
      })
    })
  })
})
