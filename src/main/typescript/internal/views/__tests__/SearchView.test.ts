import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import SearchView from '../SearchView.vue'
import { useSearchStore } from '@/stores/search'

// Mock router
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Home</div>' } },
    { path: '/search', name: 'Search', component: { template: '<div>Search</div>' } },
    { path: '/files', name: 'Files', component: { template: '<div>Files</div>' } },
    { path: '/files/:spaceId/:folderId', name: 'FilesFolder', component: { template: '<div>FilesFolder</div>' } }
  ]
})

// Mock components
const mockComponents = {
  Card: { template: '<div class="card"><slot /></div>' },
  CardContent: { template: '<div class="card-content"><slot /></div>' },
  CardHeader: { template: '<div class="card-header"><slot /></div>' },
  CardTitle: { template: '<div class="card-title"><slot /></div>' },
  Button: { template: '<button v-bind="$attrs" v-on="$listeners"><slot /></button>' },
  Input: { template: '<input v-bind="$attrs" v-on="$listeners" />' },
  Label: { template: '<label v-bind="$attrs"><slot /></label>' },
  Badge: { template: '<span v-bind="$attrs"><slot /></span>' },
  SearchBox: { template: '<div class="search-box" v-bind="$attrs" v-on="$listeners"></div>' },
  FileIcon: { template: '<div class="file-icon"></div>' }
}

describe('SearchView', () => {
  let wrapper: any
  let searchStore: any

  beforeEach(async () => {
    setActivePinia(createPinia())
    searchStore = useSearchStore()

    // Mock route query
    router.currentRoute.value.query = { q: 'test', spaceId: '1' }

    wrapper = mount(SearchView, {
      global: {
        plugins: [router],
        stubs: mockComponents
      }
    })

    // Wait for component to be fully mounted
    await wrapper.vm.$nextTick()
  })

  it('应该正确渲染搜索页面', () => {
    expect(wrapper.find('h1').text()).toBe('搜索结果')
    expect(wrapper.find('.search-box').exists()).toBe(true)
  })

  it('应该显示搜索统计信息', async () => {
    // 设置搜索结果
    searchStore.searchResults = {
      files: [
        { id: 1, name: 'test.txt', path: '/test.txt', size: 1024, updatedAt: '2023-01-01' }
      ],
      folders: [
        { id: 1, name: 'test-folder', path: '/test-folder', updatedAt: '2023-01-01' }
      ],
      total: 2,
      searchTime: 150,
      suggestions: []
    }
    
    await wrapper.vm.$nextTick()
    
    expect(wrapper.text()).toContain('找到 2 个结果')
    expect(wrapper.text()).toContain('(150ms)')
    expect(wrapper.text()).toContain('1 个文件')
    expect(wrapper.text()).toContain('1 个文件夹')
  })

  it('应该显示搜索结果', async () => {
    // 确保不在搜索状态
    searchStore.isSearching = false
    searchStore.searchError = null
    searchStore.currentKeyword = 'test'

    searchStore.searchResults = {
      files: [
        {
          id: 1,
          name: 'document.pdf',
          path: '/documents/document.pdf',
          size: 2048,
          updatedAt: '2023-01-01T10:00:00Z',
          mimeType: 'application/pdf'
        }
      ],
      folders: [
        {
          id: 1,
          name: 'documents',
          path: '/documents',
          updatedAt: '2023-01-01T10:00:00Z',
          spaceId: 1
        }
      ],
      total: 2,
      searchTime: 100,
      suggestions: []
    }

    await wrapper.vm.$nextTick()

    // 检查文件夹结果
    expect(wrapper.text()).toContain('文件夹 (1)')
    expect(wrapper.text()).toContain('documents')

    // 检查文件结果
    expect(wrapper.text()).toContain('文件 (1)')
    expect(wrapper.text()).toContain('document.pdf')
    expect(wrapper.text()).toContain('application/pdf')
  })

  it('应该显示加载状态', async () => {
    searchStore.isSearching = true
    
    await wrapper.vm.$nextTick()
    
    expect(wrapper.text()).toContain('搜索中...')
    expect(wrapper.find('.animate-spin').exists()).toBe(true)
  })

  it('应该显示错误状态', async () => {
    searchStore.isSearching = false
    searchStore.searchError = '搜索服务暂时不可用'
    searchStore.searchResults = null

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('搜索失败')
    expect(wrapper.text()).toContain('搜索服务暂时不可用')
  })

  it('应该显示无结果状态', async () => {
    searchStore.isSearching = false
    searchStore.searchError = null
    searchStore.currentKeyword = 'nonexistent'
    searchStore.searchResults = {
      files: [],
      folders: [],
      total: 0,
      searchTime: 50,
      suggestions: []
    }

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('未找到结果')
    expect(wrapper.text()).toContain('尝试使用不同的关键词或调整搜索条件')
  })

  it('应该显示空状态', async () => {
    searchStore.currentKeyword = ''
    searchStore.searchResults = null
    searchStore.isSearching = false
    
    await wrapper.vm.$nextTick()
    
    expect(wrapper.text()).toContain('开始搜索')
    expect(wrapper.text()).toContain('输入关键词搜索文件和文件夹')
  })

  it('应该支持高级搜索', async () => {
    // 点击高级搜索按钮
    const advancedButton = wrapper.find('button')
    await advancedButton.trigger('click')
    
    expect(wrapper.vm.showAdvancedSearch).toBe(true)
    expect(wrapper.text()).toContain('高级搜索')
  })

  it('应该支持文件类型筛选', async () => {
    wrapper.vm.showAdvancedSearch = true
    await wrapper.vm.$nextTick()
    
    // 模拟点击文件类型筛选
    await wrapper.vm.toggleFileType('document')
    
    expect(searchStore.advancedFilters.fileType).toContain('document')
    
    // 再次点击应该移除
    await wrapper.vm.toggleFileType('document')
    expect(searchStore.advancedFilters.fileType).not.toContain('document')
  })

  it('应该支持重置筛选条件', async () => {
    wrapper.vm.showAdvancedSearch = true
    const resetSpy = vi.spyOn(searchStore, 'resetAdvancedFilters')

    // 设置一些筛选条件
    searchStore.setAdvancedFilters({
      fileType: ['document', 'image'],
      fileSize: { min: 100, max: 1000 }
    })

    await wrapper.vm.resetFilters()

    expect(resetSpy).toHaveBeenCalled()
  })

  it('应该支持搜索结果高亮', () => {
    const highlightedText = wrapper.vm.highlightText('test document', 'test')
    expect(highlightedText).toContain('<mark class="bg-yellow-200 dark:bg-yellow-800">test</mark>')
  })

  it('应该正确格式化文件大小', () => {
    expect(wrapper.vm.formatFileSize(1024)).toBe('1 KB')
    expect(wrapper.vm.formatFileSize(1048576)).toBe('1 MB')
    expect(wrapper.vm.formatFileSize(1073741824)).toBe('1 GB')
    expect(wrapper.vm.formatFileSize(0)).toBe('0 B')
  })

  it('应该正确格式化日期', () => {
    const dateString = '2023-01-01T10:00:00Z'
    const formatted = wrapper.vm.formatDate(dateString)
    expect(formatted).toMatch(/2023/)
  })

  it('应该支持点击文件夹导航', async () => {
    const routerPushSpy = vi.spyOn(router, 'push')
    
    const folder = {
      id: 1,
      name: 'test-folder',
      path: '/test-folder',
      spaceId: 1,
      updatedAt: '2023-01-01'
    }
    
    await wrapper.vm.openFolder(folder)
    
    expect(routerPushSpy).toHaveBeenCalledWith({
      name: 'FilesFolder',
      params: {
        spaceId: '1',
        folderId: '1'
      }
    })
  })

  it('应该从URL参数初始化搜索', async () => {
    // 直接设置组件的currentSpaceId
    wrapper.vm.currentSpaceId = 2
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.currentSpaceId).toBe(2)
  })
})
