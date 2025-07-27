import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/apis/auth'

// Mock the auth API
vi.mock('@/apis/auth', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
    getCurrentUser: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    requestPasswordReset: vi.fn(),
    resetPassword: vi.fn(),
    validateResetToken: vi.fn()
  }
}))

// Mock localStorage and sessionStorage
const mockStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

Object.defineProperty(window, 'localStorage', { value: mockStorage })
Object.defineProperty(window, 'sessionStorage', { value: mockStorage })

describe('Authentication Integration Tests', () => {
  let userStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    userStore = useUserStore()
    vi.clearAllMocks()
    mockStorage.getItem.mockReturnValue(null)
  })

  describe('Login Integration', () => {
    it('should complete full login flow successfully', async () => {
      const mockLoginResponse = {
        success: true,
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          user: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            displayName: 'Test User',
            roles: ['USER']
          }
        }
      }

      authApi.login = vi.fn().mockResolvedValue(mockLoginResponse)

      const loginForm = {
        usernameOrEmail: 'testuser',
        password: 'password123',
        rememberMe: true
      }

      await userStore.login(loginForm)

      // Verify API was called correctly
      expect(authApi.login).toHaveBeenCalledWith(loginForm)

      // Verify store state
      expect(userStore.isAuthenticated).toBe(true)
      expect(userStore.user).toEqual(mockLoginResponse.data.user)
      expect(userStore.token).toBe('mock-access-token')
      expect(userStore.refreshToken).toBe('mock-refresh-token')

      // Verify localStorage was called (remember me = true)
      expect(mockStorage.setItem).toHaveBeenCalledWith('token', 'mock-access-token')
      expect(mockStorage.setItem).toHaveBeenCalledWith('refreshToken', 'mock-refresh-token')
      expect(mockStorage.setItem).toHaveBeenCalledWith('rememberMe', 'true')
    })

    it('should use sessionStorage when remember me is false', async () => {
      const mockLoginResponse = {
        success: true,
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          user: { id: 1, username: 'testuser' }
        }
      }

      authApi.login = vi.fn().mockResolvedValue(mockLoginResponse)

      await userStore.login({
        usernameOrEmail: 'testuser',
        password: 'password123',
        rememberMe: false
      })

      // Should use sessionStorage when rememberMe is false
      expect(mockStorage.setItem).toHaveBeenCalledWith('token', 'mock-access-token')
      expect(mockStorage.setItem).toHaveBeenCalledWith('refreshToken', 'mock-refresh-token')
      expect(mockStorage.setItem).not.toHaveBeenCalledWith('rememberMe', 'true')
    })

    it('should handle login failure correctly', async () => {
      const mockError = {
        response: {
          status: 401,
          data: { message: 'Invalid credentials' }
        }
      }

      authApi.login = vi.fn().mockRejectedValue(mockError)

      await expect(userStore.login({
        usernameOrEmail: 'wronguser',
        password: 'wrongpass'
      })).rejects.toEqual(mockError)

      // Verify store state is cleared
      expect(userStore.isAuthenticated).toBe(false)
      expect(userStore.user).toBe(null)
      expect(userStore.token).toBe(null)
    })
  })

  describe('Registration Integration', () => {
    it('should complete registration flow successfully', async () => {
      const mockRegisterResponse = {
        success: true,
        data: {
          message: 'Registration successful',
          user: {
            id: 2,
            username: 'newuser',
            email: 'newuser@example.com'
          }
        }
      }

      authApi.register = vi.fn().mockResolvedValue(mockRegisterResponse)

      const registerForm = {
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        confirmPassword: 'password123',
        displayName: 'New User'
      }

      const result = await userStore.register(registerForm)

      expect(authApi.register).toHaveBeenCalledWith(registerForm)
      expect(result).toEqual(mockRegisterResponse)
      
      // Registration should not auto-login
      expect(userStore.isAuthenticated).toBe(false)
    })
  })

  describe('Token Refresh Integration', () => {
    it('should refresh token successfully', async () => {
      userStore.refreshToken = 'mock-refresh-token'

      const mockRefreshResponse = {
        success: true,
        data: {
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token'
        }
      }

      authApi.refreshToken = vi.fn().mockResolvedValue(mockRefreshResponse)

      const result = await userStore.refreshAuthToken()

      expect(authApi.refreshToken).toHaveBeenCalledWith('mock-refresh-token')
      expect(result).toBe(true)
      expect(userStore.token).toBe('new-access-token')
      expect(userStore.refreshToken).toBe('new-refresh-token')
    })

    it('should clear auth on refresh failure', async () => {
      userStore.refreshToken = 'invalid-refresh-token'

      authApi.refreshToken = vi.fn().mockRejectedValue(new Error('Invalid refresh token'))

      const result = await userStore.refreshAuthToken()

      expect(result).toBe(false)
      expect(userStore.token).toBe(null)
      expect(userStore.refreshToken).toBe(null)
      expect(userStore.isAuthenticated).toBe(false)
    })
  })

  describe('User Profile Integration', () => {
    beforeEach(() => {
      userStore.token = 'mock-token'
      userStore.user = { id: 1, username: 'testuser' }
      userStore.isAuthenticated = true
    })

    it('should fetch current user successfully', async () => {
      const mockUserResponse = {
        success: true,
        data: {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User'
        }
      }

      authApi.getCurrentUser = vi.fn().mockResolvedValue(mockUserResponse)

      const result = await userStore.fetchCurrentUser()

      expect(authApi.getCurrentUser).toHaveBeenCalled()
      expect(result).toBe(true)
      expect(userStore.user).toEqual(mockUserResponse.data)
    })

    it('should update profile successfully', async () => {
      const mockUpdateResponse = {
        success: true,
        data: {
          id: 1,
          username: 'testuser',
          displayName: 'Updated Name'
        }
      }

      authApi.updateProfile = vi.fn().mockResolvedValue(mockUpdateResponse)

      const updateData = { displayName: 'Updated Name' }
      const result = await userStore.updateProfile(updateData)

      expect(authApi.updateProfile).toHaveBeenCalledWith(updateData)
      expect(result).toEqual(mockUpdateResponse.data)
      expect(userStore.user).toEqual(mockUpdateResponse.data)
    })

    it('should change password successfully', async () => {
      authApi.changePassword = vi.fn().mockResolvedValue({ success: true })

      const result = await userStore.changePassword('oldpass', 'newpass')

      expect(authApi.changePassword).toHaveBeenCalledWith({
        oldPassword: 'oldpass',
        newPassword: 'newpass',
        confirmPassword: 'newpass'
      })
      expect(result).toBe(true)
    })
  })

  describe('Logout Integration', () => {
    beforeEach(() => {
      userStore.token = 'mock-token'
      userStore.user = { id: 1, username: 'testuser' }
      userStore.isAuthenticated = true
    })

    it('should logout successfully', async () => {
      authApi.logout = vi.fn().mockResolvedValue({ success: true })

      await userStore.logout()

      expect(authApi.logout).toHaveBeenCalled()
      expect(userStore.isAuthenticated).toBe(false)
      expect(userStore.user).toBe(null)
      expect(userStore.token).toBe(null)
      expect(mockStorage.removeItem).toHaveBeenCalledWith('token')
      expect(mockStorage.removeItem).toHaveBeenCalledWith('refreshToken')
    })
  })

  describe('Auth Initialization Integration', () => {
    it('should initialize auth from localStorage', async () => {
      mockStorage.getItem.mockImplementation((key) => {
        if (key === 'token') return 'stored-token'
        if (key === 'refreshToken') return 'stored-refresh-token'
        return null
      })

      const mockUserResponse = {
        success: true,
        data: { id: 1, username: 'testuser' }
      }

      authApi.getCurrentUser = vi.fn().mockResolvedValue(mockUserResponse)

      const result = await userStore.initAuth()

      expect(result).toBe(true)
      expect(userStore.token).toBe('stored-token')
      expect(userStore.refreshToken).toBe('stored-refresh-token')
      expect(userStore.user).toEqual(mockUserResponse.data)
      expect(userStore.isAuthenticated).toBe(true)
    })

    it('should fallback to sessionStorage if localStorage is empty', async () => {
      mockStorage.getItem.mockImplementation((key) => {
        // localStorage returns null, sessionStorage returns values
        if (key === 'token') return null
        if (key === 'refreshToken') return null
        return null
      })

      // Mock sessionStorage separately
      const mockSessionStorage = {
        getItem: vi.fn().mockImplementation((key) => {
          if (key === 'token') return 'session-token'
          if (key === 'refreshToken') return 'session-refresh-token'
          return null
        })
      }

      Object.defineProperty(window, 'sessionStorage', { value: mockSessionStorage })

      const mockUserResponse = {
        success: true,
        data: { id: 1, username: 'testuser' }
      }

      authApi.getCurrentUser = vi.fn().mockResolvedValue(mockUserResponse)

      const result = await userStore.initAuth()

      expect(result).toBe(true)
      expect(userStore.token).toBe('session-token')
      expect(userStore.refreshToken).toBe('session-refresh-token')
    })
  })
})
