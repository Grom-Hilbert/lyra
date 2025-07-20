import { defineStore } from 'pinia'
import type { IUser, ILoginForm, IRegisterForm } from '@/types/index.ts'
import { authApi } from '@/apis/auth.ts'

interface UserState {
  user: IUser | null
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  loading: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    user: null,
    token: localStorage.getItem('token'),
    refreshToken: localStorage.getItem('refreshToken'),
    isAuthenticated: false,
    loading: false
  }),

  getters: {
    // 获取用户角色
    userRoles: (state): string[] => {
      return state.user?.roles || []
    },

    // 检查是否具有指定角色
    hasRole: (state) => (role: string): boolean => {
      return state.user?.roles.includes(role) || false
    },

    // 检查是否为管理员
    isAdmin: (state): boolean => {
      return state.user?.roles.includes('admin') || false
    },

    // 获取用户显示名称
    displayName: (state): string => {
      return state.user?.displayName || state.user?.username || '未知用户'
    }
  },

  actions: {
    // 登录
    async login(loginForm: ILoginForm) {
      this.loading = true
      try {
        const response = await authApi.login(loginForm)
        
        this.token = response.token || null
        this.refreshToken = response.refreshToken || null
        this.user = response.user || null
        this.isAuthenticated = true

        // 保存到本地存储
        if (response.token) {
          localStorage.setItem('token', response.token)
        }
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        
        return response
      } catch (error) {
        this.clearAuth()
        throw error
      } finally {
        this.loading = false
      }
    },

    // 注册
    async register(registerForm: IRegisterForm) {
      this.loading = true
      try {
        const response = await authApi.register(registerForm)
        
        // 注册成功后自动登录
        if (response.autoLogin && response.token && response.refreshToken && response.user) {
          this.token = response.token
          this.refreshToken = response.refreshToken
          this.user = response.user
          this.isAuthenticated = true

          localStorage.setItem('token', response.token)
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        
        return response
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 登出
    async logout() {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } catch (error) {
        console.error('登出时发生错误:', error)
      } finally {
        this.clearAuth()
      }
    },

    // 刷新令牌
    async refreshAuthToken() {
      if (!this.refreshToken) {
        this.clearAuth()
        return false
      }

      try {
        const response = await authApi.refreshToken(this.refreshToken)
        
        this.token = response.token || null
        this.refreshToken = response.refreshToken || null
        
        if (response.token) {
          localStorage.setItem('token', response.token)
        }
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        
        return true
      } catch (error) {
        this.clearAuth()
        return false
      }
    },

    // 获取当前用户信息
    async fetchCurrentUser() {
      if (!this.token) {
        return false
      }

      try {
        const user = await authApi.getCurrentUser()
        this.user = user
        this.isAuthenticated = true
        return true
      } catch (error) {
        this.clearAuth()
        return false
      }
    },

    // 更新用户信息
    async updateProfile(userInfo: Partial<IUser>) {
      try {
        const updatedUser = await authApi.updateProfile(userInfo)
        this.user = updatedUser
        return updatedUser
      } catch (error) {
        throw error
      }
    },

    // 修改密码
    async changePassword(oldPassword: string, newPassword: string) {
      try {
        await authApi.changePassword(oldPassword, newPassword)
        return true
      } catch (error) {
        throw error
      }
    },

    // 清除认证信息
    clearAuth() {
      this.user = null
      this.token = null
      this.refreshToken = null
      this.isAuthenticated = false
      
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    },

    // 初始化认证状态
    async initAuth() {
      const token = localStorage.getItem('token')
      const refreshToken = localStorage.getItem('refreshToken')
      
      if (!token || !refreshToken) {
        this.clearAuth()
        return false
      }
      
      this.token = token
      this.refreshToken = refreshToken
      
      // 尝试获取用户信息
      const success = await this.fetchCurrentUser()
      if (!success) {
        // 尝试刷新令牌
        const refreshSuccess = await this.refreshAuthToken()
        if (refreshSuccess) {
          return await this.fetchCurrentUser()
        }
      }
      
      return success
    }
  }
}) 