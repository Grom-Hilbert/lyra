import request from './request'
import type {
  IUser,
  ILoginForm,
  IRegisterForm,
  IApiResponse,
  LoginResponse,
  RegisterResponse,
  RefreshTokenResponse
} from '@/types/index'

// ==================== 认证API ====================
export const authApi = {
  // 用户登录
  async login(data: ILoginForm): Promise<IApiResponse<LoginResponse>> {
    const response = await request.post('/api/auth/login', data)
    return response.data
  },

  // 用户注册
  async register(data: IRegisterForm): Promise<IApiResponse<RegisterResponse>> {
    const response = await request.post('/api/auth/register', data)
    return response.data
  },

  // 用户登出
  async logout(data?: { logoutAllDevices?: boolean }): Promise<IApiResponse<void>> {
    const response = await request.post('/api/auth/logout', data)
    return response.data
  },

  // 刷新令牌
  async refreshToken(refreshToken: string): Promise<IApiResponse<RefreshTokenResponse>> {
    const response = await request.post('/api/auth/refresh', { refreshToken })
    return response.data
  },

  // 获取当前用户信息
  async getCurrentUser(): Promise<IApiResponse<IUser>> {
    const response = await request.get('/api/auth/me')
    return response.data
  },

  // 更新用户资料
  async updateProfile(data: {
    displayName?: string
    avatar?: string
    bio?: string
    preferences?: {
      theme?: string
      language?: string
      timezone?: string
    }
  }): Promise<IApiResponse<IUser>> {
    const response = await request.put('/api/auth/profile', data)
    return response.data
  },

  // 修改密码
  async changePassword(data: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<IApiResponse<void>> {
    const response = await request.put('/api/auth/password', data)
    return response.data
  },

  // 重置密码请求
  async requestPasswordReset(data: { email: string }): Promise<IApiResponse<void>> {
    const response = await request.post('/api/auth/password/reset-request', data)
    return response.data
  },

  // 重置密码确认
  async resetPassword(data: {
    token: string
    newPassword: string
    confirmPassword: string
  }): Promise<IApiResponse<void>> {
    const response = await request.post('/api/auth/password/reset-confirm', data)
    return response.data
  },
}