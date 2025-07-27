import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import ProfileView from '../ProfileView.vue'
import { useUserStore } from '@/stores/user'

// Mock the stores
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

describe('ProfileView Component', () => {
  let router: any
  let pinia: any
  let mockUserStore: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/profile', component: ProfileView },
        { path: '/settings', component: { template: '<div>Settings</div>' } }
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
        avatar: null,
        roles: ['USER'],
        createdAt: '2024-01-01T00:00:00Z'
      },
      updateProfile: vi.fn(),
      uploadAvatar: vi.fn()
    }

    vi.mocked(useUserStore).mockReturnValue(mockUserStore)
  })

  it('renders user information correctly', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('Test User')
    expect(wrapper.text()).toContain('@testuser')
    expect(wrapper.text()).toContain('test@example.com')
  })

  it('displays user avatar or initials', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Should show initials when no avatar
    expect(wrapper.text()).toContain('T') // First letter of display name
  })

  it('shows user roles as badges', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.text()).toContain('普通用户')
  })

  it('displays account statistics', async () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Wait for component to mount and fetch stats
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('文件数量')
    expect(wrapper.text()).toContain('已用空间')
    expect(wrapper.text()).toContain('共享文件')
    expect(wrapper.text()).toContain('最近活动')
  })

  it('handles profile update successfully', async () => {
    mockUserStore.updateProfile.mockResolvedValue({
      id: 1,
      displayName: 'Updated Name',
      email: 'updated@example.com'
    })

    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Simulate form submission
    await wrapper.vm.handleUpdateProfile({
      displayName: 'Updated Name',
      email: 'updated@example.com'
    })

    expect(mockUserStore.updateProfile).toHaveBeenCalledWith({
      displayName: 'Updated Name',
      email: 'updated@example.com'
    })
  })

  it('handles profile update failure', async () => {
    const mockError = {
      response: {
        data: { message: 'Update failed' }
      }
    }

    mockUserStore.updateProfile.mockRejectedValue(mockError)

    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    await wrapper.vm.handleUpdateProfile({
      displayName: 'Test',
      email: 'test@example.com'
    })

    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Update failed')
  })

  it('handles avatar upload successfully', async () => {
    mockUserStore.uploadAvatar.mockResolvedValue({
      success: true,
      data: { avatarUrl: 'https://example.com/avatar.jpg' }
    })

    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Create a mock file
    const file = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' })
    const event = {
      target: {
        files: [file],
        value: ''
      }
    }

    await wrapper.vm.handleAvatarChange(event)

    expect(mockUserStore.uploadAvatar).toHaveBeenCalled()
  })

  it('validates avatar file type', async () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Create a mock non-image file
    const file = new File(['document'], 'document.txt', { type: 'text/plain' })
    const event = {
      target: {
        files: [file]
      }
    }

    await wrapper.vm.handleAvatarChange(event)

    expect(wrapper.vm.errorMessage).toContain('请选择图片文件')
    expect(mockUserStore.uploadAvatar).not.toHaveBeenCalled()
  })

  it('validates avatar file size', async () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Create a mock large file (6MB)
    const largeFile = new File(['x'.repeat(6 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' })
    const event = {
      target: {
        files: [largeFile]
      }
    }

    await wrapper.vm.handleAvatarChange(event)

    expect(wrapper.vm.errorMessage).toContain('图片文件不能超过5MB')
    expect(mockUserStore.uploadAvatar).not.toHaveBeenCalled()
  })

  it('formats file size correctly', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    expect(wrapper.vm.formatFileSize(0)).toBe('0 B')
    expect(wrapper.vm.formatFileSize(1024)).toBe('1 KB')
    expect(wrapper.vm.formatFileSize(1024 * 1024)).toBe('1 MB')
    expect(wrapper.vm.formatFileSize(1024 * 1024 * 1024)).toBe('1 GB')
  })

  it('formats dates correctly', () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const testDate = '2024-01-01T00:00:00Z'
    const formatted = wrapper.vm.formatDate(testDate)
    expect(formatted).toContain('2024年01月01日')
  })

  it('navigates to settings page', async () => {
    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const settingsButton = wrapper.find('button:contains("设置")')
    if (settingsButton.exists()) {
      await settingsButton.trigger('click')
      expect(router.currentRoute.value.path).toBe('/settings')
    }
  })

  it('shows loading state during operations', async () => {
    mockUserStore.updateProfile.mockImplementation(() => 
      new Promise(resolve => setTimeout(resolve, 100))
    )

    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Start profile update
    const updatePromise = wrapper.vm.handleUpdateProfile({
      displayName: 'Test',
      email: 'test@example.com'
    })

    // Check loading state
    expect(wrapper.vm.loading).toBe(true)

    await updatePromise
    expect(wrapper.vm.loading).toBe(false)
  })

  it('clears messages after timeout', async () => {
    vi.useFakeTimers()

    mockUserStore.updateProfile.mockResolvedValue({})

    const wrapper = mount(ProfileView, {
      global: {
        plugins: [router, pinia]
      }
    })

    await wrapper.vm.handleUpdateProfile({
      displayName: 'Test',
      email: 'test@example.com'
    })

    expect(wrapper.vm.successMessage).toContain('个人信息更新成功')

    // Fast-forward time
    vi.advanceTimersByTime(3000)

    expect(wrapper.vm.successMessage).toBe('')

    vi.useRealTimers()
  })
})
