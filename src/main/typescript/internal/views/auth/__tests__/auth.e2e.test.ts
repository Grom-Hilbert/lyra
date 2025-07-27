import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import LoginView from '../LoginView.vue'
import RegisterView from '../RegisterView.vue'
import ForgotPasswordView from '../ForgotPasswordView.vue'
import ResetPasswordView from '../ResetPasswordView.vue'
import { useUserStore } from '@/stores/user'

// Mock the APIs
vi.mock('@/apis/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    requestPasswordReset: vi.fn(),
    resetPassword: vi.fn(),
    validateResetToken: vi.fn()
  }
}))

describe('Authentication E2E Tests', () => {
  let router: any
  let pinia: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/login', component: LoginView },
        { path: '/register', component: RegisterView },
        { path: '/forgot-password', component: ForgotPasswordView },
        { path: '/reset-password', component: ResetPasswordView },
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' } }
      ]
    })

    // Create pinia
    pinia = createPinia()

    // Clear all mocks
    vi.clearAllMocks()
  })

  describe('Login Flow', () => {
    it('should login successfully with valid credentials', async () => {
      const wrapper = mount(LoginView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.login = vi.fn().mockResolvedValue({
        success: true,
        data: {
          accessToken: 'mock-token',
          user: { id: 1, username: 'testuser' }
        }
      })

      // Fill in the form
      const usernameInput = wrapper.find('input[type="text"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const submitButton = wrapper.find('button[type="submit"]')

      await usernameInput.setValue('testuser')
      await passwordInput.setValue('password123')
      await submitButton.trigger('click')

      // Wait for async operations
      await wrapper.vm.$nextTick()

      expect(userStore.login).toHaveBeenCalledWith({
        usernameOrEmail: 'testuser',
        password: 'password123',
        rememberMe: false
      })
    })

    it('should show error message for invalid credentials', async () => {
      const wrapper = mount(LoginView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.login = vi.fn().mockRejectedValue({
        response: { status: 401, data: { message: 'Invalid credentials' } }
      })

      // Fill in the form
      const usernameInput = wrapper.find('input[type="text"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const submitButton = wrapper.find('button[type="submit"]')

      await usernameInput.setValue('wronguser')
      await passwordInput.setValue('wrongpass')
      await submitButton.trigger('click')

      // Wait for async operations
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('用户名或密码错误')
    })

    it('should handle remember me functionality', async () => {
      const wrapper = mount(LoginView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.login = vi.fn().mockResolvedValue({
        success: true,
        data: { accessToken: 'mock-token' }
      })

      // Fill in the form and check remember me
      const usernameInput = wrapper.find('input[type="text"]')
      const passwordInput = wrapper.find('input[type="password"]')
      const rememberMeCheckbox = wrapper.find('input[type="checkbox"]')
      const submitButton = wrapper.find('button[type="submit"]')

      await usernameInput.setValue('testuser')
      await passwordInput.setValue('password123')
      await rememberMeCheckbox.setChecked(true)
      await submitButton.trigger('click')

      expect(userStore.login).toHaveBeenCalledWith({
        usernameOrEmail: 'testuser',
        password: 'password123',
        rememberMe: true
      })
    })
  })

  describe('Registration Flow', () => {
    it('should register successfully with valid data', async () => {
      const wrapper = mount(RegisterView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.register = vi.fn().mockResolvedValue({
        success: true,
        data: { message: 'Registration successful' }
      })

      // Fill in the registration form
      const inputs = wrapper.findAll('input')
      const usernameInput = inputs.find(input => input.attributes('placeholder')?.includes('用户名'))
      const displayNameInput = inputs.find(input => input.attributes('placeholder')?.includes('显示名称'))
      const emailInput = inputs.find(input => input.attributes('type') === 'email')
      const passwordInputs = inputs.filter(input => input.attributes('type') === 'password')
      const agreeCheckbox = wrapper.find('input[type="checkbox"]')
      const submitButton = wrapper.find('button[type="submit"]')

      if (usernameInput) await usernameInput.setValue('newuser')
      if (displayNameInput) await displayNameInput.setValue('New User')
      if (emailInput) await emailInput.setValue('newuser@example.com')
      if (passwordInputs[0]) await passwordInputs[0].setValue('password123')
      if (passwordInputs[1]) await passwordInputs[1].setValue('password123')
      await agreeCheckbox.setChecked(true)
      await submitButton.trigger('click')

      expect(userStore.register).toHaveBeenCalledWith({
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        confirmPassword: 'password123',
        displayName: 'New User'
      })
    })

    it('should show error for duplicate username', async () => {
      const wrapper = mount(RegisterView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.register = vi.fn().mockRejectedValue({
        response: { 
          status: 409, 
          data: { message: 'Username already exists' } 
        }
      })

      // Simulate form submission
      await wrapper.vm.handleRegister({
        username: 'existinguser',
        email: 'test@example.com',
        password: 'password123',
        confirmPassword: 'password123',
        displayName: 'Test User'
      })

      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('用户名已存在')
    })
  })

  describe('Password Reset Flow', () => {
    it('should send reset email successfully', async () => {
      const { authApi } = await import('@/apis/auth')
      authApi.requestPasswordReset = vi.fn().mockResolvedValue({
        success: true,
        data: { message: 'Reset email sent' }
      })

      const wrapper = mount(ForgotPasswordView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const emailInput = wrapper.find('input[type="email"]')
      const submitButton = wrapper.find('button[type="submit"]')

      await emailInput.setValue('test@example.com')
      await submitButton.trigger('click')

      await wrapper.vm.$nextTick()

      expect(authApi.requestPasswordReset).toHaveBeenCalledWith('test@example.com')
      expect(wrapper.text()).toContain('邮件已发送')
    })

    it('should reset password successfully with valid token', async () => {
      const { authApi } = await import('@/apis/auth')
      authApi.validateResetToken = vi.fn().mockResolvedValue({ success: true })
      authApi.resetPassword = vi.fn().mockResolvedValue({
        success: true,
        data: { message: 'Password reset successful' }
      })

      await router.push('/reset-password?token=valid-token')

      const wrapper = mount(ResetPasswordView, {
        global: {
          plugins: [router, pinia]
        }
      })

      await wrapper.vm.$nextTick()

      // Fill in new password
      const passwordInputs = wrapper.findAll('input[type="password"]')
      if (passwordInputs[0]) await passwordInputs[0].setValue('newpassword123')
      if (passwordInputs[1]) await passwordInputs[1].setValue('newpassword123')

      const submitButton = wrapper.find('button[type="submit"]')
      await submitButton.trigger('click')

      await wrapper.vm.$nextTick()

      expect(authApi.resetPassword).toHaveBeenCalledWith({
        token: 'valid-token',
        newPassword: 'newpassword123',
        confirmPassword: 'newpassword123'
      })
    })

    it('should show error for invalid reset token', async () => {
      const { authApi } = await import('@/apis/auth')
      authApi.validateResetToken = vi.fn().mockRejectedValue({
        response: { status: 404, data: { message: 'Invalid token' } }
      })

      await router.push('/reset-password?token=invalid-token')

      const wrapper = mount(ResetPasswordView, {
        global: {
          plugins: [router, pinia]
        }
      })

      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('重置链接无效或已过期')
    })
  })

  describe('Navigation Flow', () => {
    it('should navigate between auth pages correctly', async () => {
      const wrapper = mount(LoginView, {
        global: {
          plugins: [router, pinia]
        }
      })

      // Test navigation to register
      const registerLink = wrapper.find('a[href="/register"]')
      expect(registerLink.exists()).toBe(true)

      // Test navigation to forgot password
      const forgotPasswordLink = wrapper.find('button:contains("忘记密码")')
      expect(forgotPasswordLink.exists()).toBe(true)
    })

    it('should redirect to intended page after login', async () => {
      // Set up redirect query parameter
      await router.push('/login?redirect=/dashboard')

      const wrapper = mount(LoginView, {
        global: {
          plugins: [router, pinia]
        }
      })

      const userStore = useUserStore()
      userStore.login = vi.fn().mockResolvedValue({
        success: true,
        data: { accessToken: 'mock-token' }
      })

      // Simulate successful login
      await wrapper.vm.handleLogin({
        username: 'testuser',
        password: 'password123',
        rememberMe: false
      })

      // Should redirect to dashboard
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })
  })
})
