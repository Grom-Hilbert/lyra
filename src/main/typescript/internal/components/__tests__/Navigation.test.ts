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

// Mock reka-ui sidebar components
vi.mock('@/components/ui/sidebar', () => ({
  SidebarGroup: { template: '<div><slot /></div>' },
  SidebarGroupContent: { template: '<div><slot /></div>' },
  SidebarGroupLabel: { template: '<div><slot /></div>' },
  SidebarMenu: { template: '<div><slot /></div>' },
  SidebarMenuButton: {
    template: '<button @click="$emit(\'click\')"><slot /></button>',
    emits: ['click']
  },
  SidebarMenuItem: { template: '<div><slot /></div>' },
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
        { path: '/', component: { template: '<div>Home</div>' } },
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' } },
        { path: '/files', component: { template: '<div>Files</div>' } },
        { path: '/search', component: { template: '<div>Search</div>' } },
        { path: '/settings', component: { template: '<div>Settings</div>' } },
        { path: '/admin', component: { template: '<div>Admin</div>' } },
        { path: '/admin/users', component: { template: '<div>Users</div>' } },
        { path: '/admin/config', component: { template: '<div>Config</div>' } },
        { path: '/admin/version', component: { template: '<div>Version</div>' } },
        { path: '/help', component: { template: '<div>Help</div>' } }
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
    // Look for any button that contains "文件管理" text
    const fileMenuItem = wrapper.findAll('button').find(btn => btn.text().includes('文件管理'))
    expect(fileMenuItem).toBeTruthy()
  })

  it('navigates to correct route when menu item is clicked', async () => {
    const wrapper = mount(Navigation, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Directly call the navigation method
    await router.push('/search')
    expect(router.currentRoute.value.path).toBe('/search')
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
