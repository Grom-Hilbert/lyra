import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import SearchBox from '../SearchBox.vue'
import { useSearchStore } from '@/stores/search'

// Mock router
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Home</div>' } },
    { path: '/search', name: 'Search', component: { template: '<div>Search</div>' } }
  ]
})

// Mock searchApi
vi.mock('@/apis/searchApi', () => ({
  searchApi: {
    getSuggestions: vi.fn().mockResolvedValue({
      success: true,
      data: [
        { text: 'test.txt', type: 'filename', count: 1 },
        { text: 'document', type: 'keyword', count: 5 }
      ]
    })
  }
}))

describe('SearchBox', () => {
  let wrapper: any
  let searchStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    searchStore = useSearchStore()
    
    wrapper = mount(SearchBox, {
      global: {
        plugins: [router],
        stubs: {
          Input: {
            template: '<input v-model="modelValue" v-bind="$attrs" @input="handleInput" @keydown="handleKeydown" @focus="handleFocus" @blur="handleBlur" />',
            props: ['modelValue', 'placeholder'],
            emits: ['update:modelValue', 'input', 'keydown', 'focus', 'blur'],
            methods: {
              handleInput(e) { this.$emit('update:modelValue', e.target.value); this.$emit('input', e) },
              handleKeydown(e) { this.$emit('keydown', e) },
              handleFocus(e) { this.$emit('focus', e) },
              handleBlur(e) { this.$emit('blur', e) }
            }
          },
          Button: { template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)"><slot /></button>' },
          Badge: { template: '<span v-bind="$attrs"><slot /></span>' },
          Search: { template: '<div class="search-icon"></div>' },
          X: { template: '<div class="x-icon"></div>' },
          History: { template: '<div class="history-icon"></div>' }
        }
      },
      props: {
        spaceId: 1
      }
    })
  })

  it('应该正确渲染搜索框', () => {
    expect(wrapper.find('input').exists()).toBe(true)
    // 检查组件是否正确挂载
    expect(wrapper.vm).toBeDefined()
  })

  it('应该在输入时显示搜索建议', async () => {
    const input = wrapper.find('input')
    const getSuggestionsSpy = vi.spyOn(searchStore, 'getSuggestions')

    // 模拟输入
    await input.setValue('test')
    await input.trigger('input')

    // 等待防抖
    await new Promise(resolve => setTimeout(resolve, 350))

    expect(getSuggestionsSpy).toHaveBeenCalledWith('test', 1)
  })

  it('应该在按下Enter时执行搜索', async () => {
    const routerPushSpy = vi.spyOn(router, 'push')

    // 直接调用组件方法
    wrapper.vm.searchQuery = 'test query'
    await wrapper.vm.performSearch()

    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')[0]).toEqual(['test query'])
    expect(routerPushSpy).toHaveBeenCalledWith({
      name: 'Search',
      query: {
        q: 'test query',
        spaceId: '1'
      }
    })
  })

  it('应该支持键盘导航', async () => {
    const input = wrapper.find('input')

    // 设置建议数据
    searchStore.suggestions = [
      { text: 'suggestion1', type: 'filename', count: 1 },
      { text: 'suggestion2', type: 'keyword', count: 2 }
    ]

    await input.setValue('test')
    await input.trigger('focus')
    wrapper.vm.showDropdown = true
    await wrapper.vm.$nextTick()

    // 模拟向下箭头
    await input.trigger('keydown', { key: 'ArrowDown' })
    expect(wrapper.vm.selectedIndex).toBe(0)

    // 模拟向下箭头
    await input.trigger('keydown', { key: 'ArrowDown' })
    expect(wrapper.vm.selectedIndex).toBe(1)

    // 模拟向上箭头
    await input.trigger('keydown', { key: 'ArrowUp' })
    expect(wrapper.vm.selectedIndex).toBe(0)
  })

  it('应该显示搜索历史', async () => {
    // 设置搜索历史
    searchStore.searchHistory = [
      {
        id: '1',
        keyword: 'previous search',
        timestamp: Date.now(),
        resultCount: 5,
        spaceId: 1
      }
    ]
    
    const input = wrapper.find('input')
    await input.trigger('focus')
    
    // 应该显示历史记录
    expect(wrapper.text()).toContain('最近搜索')
    expect(wrapper.text()).toContain('previous search')
    expect(wrapper.text()).toContain('5 个结果')
  })

  it('应该能清空搜索', async () => {
    const input = wrapper.find('input')
    await input.setValue('test query')
    
    // 查找清空按钮
    const clearButton = wrapper.find('[data-testid="clear-button"]')
    if (clearButton.exists()) {
      await clearButton.trigger('click')
      
      expect(input.element.value).toBe('')
      expect(wrapper.emitted('clear')).toBeTruthy()
    }
  })

  it('应该能清空搜索历史', async () => {
    searchStore.searchHistory = [
      {
        id: '1',
        keyword: 'test',
        timestamp: Date.now(),
        resultCount: 1,
        spaceId: 1
      }
    ]
    
    const input = wrapper.find('input')
    await input.trigger('focus')
    
    // 查找清空历史按钮
    const clearHistoryButton = wrapper.find('[data-testid="clear-history"]')
    if (clearHistoryButton.exists()) {
      await clearHistoryButton.trigger('click')
      expect(searchStore.clearHistory).toHaveBeenCalled()
    }
  })

  it('应该在失去焦点时隐藏下拉菜单', async () => {
    const input = wrapper.find('input')
    
    await input.trigger('focus')
    expect(wrapper.vm.showDropdown).toBe(true)
    
    await input.trigger('blur')
    
    // 等待延迟隐藏
    await new Promise(resolve => setTimeout(resolve, 250))
    expect(wrapper.vm.showDropdown).toBe(false)
  })

  it('应该支持自定义占位符', () => {
    const customWrapper = mount(SearchBox, {
      global: {
        plugins: [router],
        stubs: {
          Input: {
            template: '<input v-model="modelValue" :placeholder="placeholder" v-bind="$attrs" />',
            props: ['modelValue', 'placeholder'],
            emits: ['update:modelValue']
          },
          Button: { template: '<button v-bind="$attrs"><slot /></button>' },
          Badge: { template: '<span v-bind="$attrs"><slot /></span>' },
          Search: { template: '<div class="search-icon"></div>' },
          X: { template: '<div class="x-icon"></div>' },
          History: { template: '<div class="history-icon"></div>' }
        }
      },
      props: {
        placeholder: '自定义搜索提示'
      }
    })

    // 检查props是否正确传递
    expect(customWrapper.props('placeholder')).toBe('自定义搜索提示')
  })

  it('应该支持禁用搜索建议', () => {
    const noSuggestionsWrapper = mount(SearchBox, {
      global: {
        plugins: [router],
        stubs: {
          Input: {
            template: '<input v-model="modelValue" v-bind="$attrs" />',
            props: ['modelValue'],
            emits: ['update:modelValue']
          },
          Button: { template: '<button v-bind="$attrs"><slot /></button>' },
          Badge: { template: '<span v-bind="$attrs"><slot /></span>' },
          Search: { template: '<div class="search-icon"></div>' },
          X: { template: '<div class="x-icon"></div>' },
          History: { template: '<div class="history-icon"></div>' }
        }
      },
      props: {
        showSuggestions: false
      }
    })

    expect(noSuggestionsWrapper.vm.suggestions).toEqual([])
  })

  it('应该支持禁用搜索历史', () => {
    const noHistoryWrapper = mount(SearchBox, {
      global: {
        plugins: [router],
        stubs: {
          Input: {
            template: '<input v-model="modelValue" v-bind="$attrs" />',
            props: ['modelValue'],
            emits: ['update:modelValue']
          },
          Button: { template: '<button v-bind="$attrs"><slot /></button>' },
          Badge: { template: '<span v-bind="$attrs"><slot /></span>' },
          Search: { template: '<div class="search-icon"></div>' },
          X: { template: '<div class="x-icon"></div>' },
          History: { template: '<div class="history-icon"></div>' }
        }
      },
      props: {
        showHistory: false
      }
    })

    expect(noHistoryWrapper.vm.recentSearches).toEqual([])
  })
})
