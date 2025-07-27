import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import SettingsView from '../SettingsView.vue'
import { useUserStore } from '@/stores/user'

// Mock the stores
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

describe('SettingsView Component', () => {
  let router: any
  let pinia: any
  let mockUserStore: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/settings', component: SettingsView }
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
        displayName: 'Test User'
      },
      changePassword: vi.fn()
    }

    vi.mocked(useUserStore).mockReturnValue(mockUserStore)
  })

  it('renders settings tabs correctly', () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('账户安全')
    expect(wrapper.text()).toContain('个人偏好')
    expect(wrapper.text()).toContain('存储管理')
    expect(wrapper.text()).toContain('关于')
  })

  it('switches between tabs correctly', async () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Default tab should be security
    expect(wrapper.vm.activeTab).toBe('security')
    expect(wrapper.text()).toContain('修改密码')

    // Switch to preferences tab
    const preferencesTab = wrapper.find('button:contains("个人偏好")')
    if (preferencesTab.exists()) {
      await preferencesTab.trigger('click')
      expect(wrapper.vm.activeTab).toBe('preferences')
      expect(wrapper.text()).toContain('界面设置')
    }
  })

  it('validates password form correctly', async () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Test empty fields
    await wrapper.vm.updatePassword()
    expect(wrapper.vm.errorMessage).toContain('请填写所有密码字段')

    // Test password mismatch
    wrapper.vm.passwordFormData.currentPassword = 'oldpass'
    wrapper.vm.passwordFormData.newPassword = 'newpass'
    wrapper.vm.passwordFormData.confirmPassword = 'different'
    
    await wrapper.vm.updatePassword()
    expect(wrapper.vm.errorMessage).toContain('新密码和确认密码不匹配')

    // Test short password
    wrapper.vm.passwordFormData.confirmPassword = 'new'
    wrapper.vm.passwordFormData.newPassword = 'new'
    
    await wrapper.vm.updatePassword()
    expect(wrapper.vm.errorMessage).toContain('新密码长度至少6个字符')
  })

  it('handles password change successfully', async () => {
    mockUserStore.changePassword.mockResolvedValue(true)

    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Set valid password data
    wrapper.vm.passwordFormData.currentPassword = 'oldpassword'
    wrapper.vm.passwordFormData.newPassword = 'newpassword'
    wrapper.vm.passwordFormData.confirmPassword = 'newpassword'

    await wrapper.vm.updatePassword()

    expect(mockUserStore.changePassword).toHaveBeenCalledWith('oldpassword', 'newpassword')
    expect(wrapper.vm.successMessage).toContain('密码修改成功')
    
    // Form should be cleared
    expect(wrapper.vm.passwordFormData.currentPassword).toBe('')
    expect(wrapper.vm.passwordFormData.newPassword).toBe('')
    expect(wrapper.vm.passwordFormData.confirmPassword).toBe('')
  })

  it('handles password change failure', async () => {
    const mockError = {
      response: {
        status: 400,
        data: { message: 'Invalid current password' }
      }
    }

    mockUserStore.changePassword.mockRejectedValue(mockError)

    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    wrapper.vm.passwordFormData.currentPassword = 'wrongpass'
    wrapper.vm.passwordFormData.newPassword = 'newpassword'
    wrapper.vm.passwordFormData.confirmPassword = 'newpassword'

    await wrapper.vm.updatePassword()

    expect(wrapper.vm.errorMessage).toContain('当前密码不正确')
  })

  it('saves preferences correctly', async () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Change preferences
    wrapper.vm.preferences.theme = 'dark'
    wrapper.vm.preferences.language = 'en-US'

    await wrapper.vm.savePreferences()

    expect(wrapper.vm.successMessage).toContain('偏好设置已保存')
  })

  it('applies theme correctly', () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Test dark theme
    wrapper.vm.applyTheme('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    // Test light theme
    wrapper.vm.applyTheme('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('exports user data correctly', async () => {
    // Mock URL.createObjectURL and related methods
    global.URL.createObjectURL = vi.fn(() => 'mock-url')
    global.URL.revokeObjectURL = vi.fn()
    
    // Mock document.createElement and appendChild
    const mockAnchor = {
      href: '',
      download: '',
      click: vi.fn()
    }
    document.createElement = vi.fn(() => mockAnchor)
    document.body.appendChild = vi.fn()
    document.body.removeChild = vi.fn()

    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    await wrapper.vm.exportUserData()

    expect(mockAnchor.click).toHaveBeenCalled()
    expect(wrapper.vm.successMessage).toContain('用户数据导出成功')
  })

  it('formats bytes correctly', () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.vm.formatBytes(0)).toBe('0 Bytes')
    expect(wrapper.vm.formatBytes(1024)).toBe('1 KB')
    expect(wrapper.vm.formatBytes(1024 * 1024)).toBe('1 MB')
    expect(wrapper.vm.formatBytes(1024 * 1024 * 1024)).toBe('1 GB')
  })

  it('displays storage information correctly', () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Switch to storage tab
    wrapper.vm.activeTab = 'storage'

    expect(wrapper.text()).toContain('存储使用情况')
    expect(wrapper.text()).toContain('清理工具')
    expect(wrapper.text()).toContain('数据管理')
  })

  it('shows loading state during password update', async () => {
    mockUserStore.changePassword.mockImplementation(() => 
      new Promise(resolve => setTimeout(resolve, 100))
    )

    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    wrapper.vm.passwordFormData.currentPassword = 'oldpass'
    wrapper.vm.passwordFormData.newPassword = 'newpass'
    wrapper.vm.passwordFormData.confirmPassword = 'newpass'

    const updatePromise = wrapper.vm.updatePassword()

    expect(wrapper.vm.passwordForm.isSubmitting).toBe(true)

    await updatePromise
    expect(wrapper.vm.passwordForm.isSubmitting).toBe(false)
  })

  it('toggles two-factor authentication', async () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const initialState = wrapper.vm.twoFactorEnabled

    await wrapper.vm.toggleTwoFactor()

    expect(wrapper.vm.twoFactorEnabled).toBe(!initialState)
  })

  it('displays about information correctly', () => {
    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Switch to about tab
    wrapper.vm.activeTab = 'about'

    expect(wrapper.text()).toContain('Lyra v1.0.0')
    expect(wrapper.text()).toContain('技术栈')
    expect(wrapper.text()).toContain('支持与帮助')
  })

  it('clears success messages after timeout', async () => {
    vi.useFakeTimers()

    const wrapper = mount(SettingsView, {
      global: {
        plugins: [router, pinia]
      }
    })

    await wrapper.vm.savePreferences()
    expect(wrapper.vm.successMessage).toContain('偏好设置已保存')

    vi.advanceTimersByTime(3000)
    expect(wrapper.vm.successMessage).toBe('')

    vi.useRealTimers()
  })
})
