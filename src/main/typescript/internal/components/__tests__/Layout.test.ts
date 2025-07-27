import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import Layout from '../Layout.vue'
import { useUserStore } from '@/stores/user'

// Mock the stores
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

// Mock the components that might not be available in test environment
vi.mock('@/components/Navigation.vue', () => ({
  default: {
    name: 'Navigation',
    template: '<div data-testid="navigation">Navigation</div>'
  }
}))

vi.mock('@/components/Breadcrumb.vue', () => ({
  default: {
    name: 'Breadcrumb',
    template: '<div data-testid="breadcrumb">Breadcrumb</div>'
  }
}))

vi.mock('@/components/Notification.vue', () => ({
  default: {
    name: 'Notification',
    template: '<div data-testid="notification">Notification</div>'
  }
}))

describe('Layout Component', () => {
  let router: any
  let pinia: any
  let mockUserStore: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: { template: '<div>Home</div>' } },
        { path: '/login', component: { template: '<div>Login</div>' } },
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' } }
      ]
    })

    // Create pinia
    pinia = createPinia()

    // Mock user store
    mockUserStore = {
      user: {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User',
        roles: ['USER']
      },
      isAuthenticated: true,
      isAdmin: false,
      displayName: 'Test User',
      logout: vi.fn()
    }

    vi.mocked(useUserStore).mockReturnValue(mockUserStore)
  })

  it('renders auth page layout for auth routes', async () => {
    await router.push('/login')
    
    const wrapper = mount(Layout, {
      global: {
        plugins: [router, pinia],
        stubs: {
          'router-view': true
        }
      },
      slots: {
        default: '<div data-testid="auth-content">Auth Content</div>'
      }
    })

    expect(wrapper.find('[data-testid="auth-content"]').exists()).toBe(true)
  })

  it('renders main app layout for authenticated users', async () => {
    await router.push('/dashboard')
    
    const wrapper = mount(Layout, {
      global: {
        plugins: [router, pinia],
        stubs: {
          'router-view': true
        }
      },
      slots: {
        default: '<div data-testid="main-content">Main Content</div>'
      }
    })

    expect(wrapper.find('[data-testid="navigation"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="breadcrumb"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="notification"]').exists()).toBe(true)
  })

  it('renders unauthenticated state for non-auth routes when not logged in', async () => {
    mockUserStore.isAuthenticated = false
    await router.push('/dashboard')
    
    const wrapper = mount(Layout, {
      global: {
        plugins: [router, pinia],
        stubs: {
          'router-view': true
        }
      }
    })

    expect(wrapper.text()).toContain('正在重定向到首页')
  })

  it('displays user information in sidebar footer', async () => {
    await router.push('/dashboard')
    
    const wrapper = mount(Layout, {
      global: {
        plugins: [router, pinia],
        stubs: {
          'router-view': true
        }
      }
    })

    expect(wrapper.text()).toContain('Test User')
    expect(wrapper.text()).toContain('test@example.com')
  })
})
