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
    async login(loginForm: ILoginForm & { rememberMe?: boolean }) {
      this.loading = true
      try {
        const response = await authApi.login(loginForm)

        if (response.success && response.data) {
          this.token = response.data.accessToken || null
          this.refreshToken = response.data.refreshToken || null
          this.user = response.data.user || null
          this.isAuthenticated = true

          // 根据记住我选项决定存储方式
          const storage = loginForm.rememberMe ? localStorage : sessionStorage

          // 保存到存储
          if (response.data.accessToken) {
            storage.setItem('token', response.data.accessToken)
            // 如果选择记住我，也保存到localStorage作为备份
            if (loginForm.rememberMe) {
              localStorage.setItem('rememberMe', 'true')
            }
          }
          if (response.data.refreshToken) {
            storage.setItem('refreshToken', response.data.refreshToken)
          }
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

        // 注册成功，但通常需要邮箱验证，不自动登录
        // 如果后端返回了用户信息，说明注册成功
        
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

        if (response.success && response.data) {
          this.token = response.data.accessToken || null
          this.refreshToken = response.data.refreshToken || null

          if (response.data.accessToken) {
            localStorage.setItem('token', response.data.accessToken)
          }
          if (response.data.refreshToken) {
            localStorage.setItem('refreshToken', response.data.refreshToken)
          }
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
        const response = await authApi.getCurrentUser()
        if (response.success && response.data) {
          this.user = response.data
          this.isAuthenticated = true
          return true
        }
        return false
      } catch (error) {
        this.clearAuth()
        return false
      }
    },

    // 更新用户信息
    async updateProfile(userInfo: Partial<IUser>) {
      try {
        const response = await authApi.updateProfile(userInfo)
        if (response.success && response.data) {
          this.user = response.data
          return response.data
        }
        throw new Error('更新失败')
      } catch (error) {
        throw error
      }
    },

    // 修改密码
    async changePassword(oldPassword: string, newPassword: string) {
      try {
        await authApi.changePassword({
          oldPassword,
          newPassword,
          confirmPassword: newPassword
        })
        return true
      } catch (error) {
        throw error
      }
    },

    // 上传头像
    async uploadAvatar(formData: FormData): Promise<void> {
      try {
        const response = await authApi.uploadAvatar(formData)
        if (response.success && response.data) {
          // 更新用户信息中的头像URL
          if (this.user) {
            this.user.avatar = response.data.avatarUrl
          }
        }
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

      // 清除所有存储
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('rememberMe')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('refreshToken')
    },

    // 初始化认证状态
    async initAuth() {
      // 优先从localStorage获取，然后从sessionStorage获取
      let token = localStorage.getItem('token') || sessionStorage.getItem('token')
      let refreshToken = localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken')

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