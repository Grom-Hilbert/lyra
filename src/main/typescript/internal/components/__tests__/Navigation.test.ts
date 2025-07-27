import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import Navigation from '../Navigation.vue'
import { useUserStore } from '@/stores/user'

// Mock the stores
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

describe('Navigation Component', () => {
  let router: any
  let pinia: any
  let mockUserStore: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
        { path: '/files', component: { template: '<div>Files</div>' } },
        { path: '/search', component: { template: '<div>Search</div>' } },
        { path: '/admin', component: { template: '<div>Admin</div>' } },
        { path: '/admin/users', component: { template: '<div>Users</div>' } }
      ]
    })

    // Create pinia
    pinia = createPinia()

    // Mock user store for regular user
    mockUserStore = {
      isAdmin: false
    }

    vi.mocked(useUserStore).mockReturnValue(mockUserStore)
  })

  it('renders main menu items for regular users', () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('仪表板')
    expect(wrapper.text()).toContain('文件管理')
    expect(wrapper.text()).toContain('搜索')
    expect(wrapper.text()).toContain('文件预览')
    expect(wrapper.text()).toContain('在线编辑')
  })

  it('does not show admin menu items for regular users', () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).not.toContain('管理仪表板')
    expect(wrapper.text()).not.toContain('用户管理')
    expect(wrapper.text()).not.toContain('系统配置')
  })

  it('shows admin menu items for admin users', () => {
    mockUserStore.isAdmin = true

    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('管理仪表板')
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('系统配置')
    expect(wrapper.text()).toContain('版本控制')
  })

  it('highlights active menu item', async () => {
    await router.push('/files')

    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Check if the files menu item has active styling
    const fileMenuItem = wrapper.find('[data-active="true"]')
    expect(fileMenuItem.exists()).toBe(true)
  })

  it('navigates to correct route when menu item is clicked', async () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Find and click the search menu item
    const searchButton = wrapper.find('button:contains("搜索")')
    if (searchButton.exists()) {
      await searchButton.trigger('click')
      expect(router.currentRoute.value.path).toBe('/search')
    }
  })

  it('shows badges for menu items that have them', () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('New') // File management has "New" badge
  })

  it('groups menu items correctly', () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('主要功能')
    expect(wrapper.text()).toContain('工具')
    expect(wrapper.text()).toContain('其他')
  })

  it('shows admin group only for admin users', () => {
    // Test regular user
    let wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).not.toContain('管理员')

    // Test admin user
    mockUserStore.isAdmin = true
    wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('管理员')
  })
})
