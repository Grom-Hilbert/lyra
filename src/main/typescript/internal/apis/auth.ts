import { request } from './request'
import type { IUser, ILoginForm, IRegisterForm, IApiResponse } from '@/types'

// 登录响应
export interface LoginResponse {
  token: string
  refreshToken: string
  user: IUser
}

// 注册响应
export interface RegisterResponse {
  message: string
  autoLogin?: boolean
  token?: string
  refreshToken?: string
  user?: IUser
}

// 刷新令牌响应
export interface RefreshTokenResponse {
  token: string
  refreshToken: string
}

export const authApi = {
  // 用户登录
  login: (data: ILoginForm): Promise<LoginResponse> => {
    return request.post('/api/auth/login', data)
  },

  // 用户注册
  register: (data: IRegisterForm): Promise<RegisterResponse> => {
    return request.post('/api/auth/register', data)
  },

  // 用户登出
  logout: (): Promise<IApiResponse> => {
    return request.post('/api/auth/logout')
  },

  // 刷新令牌
  refreshToken: (refreshToken: string): Promise<RefreshTokenResponse> => {
    return request.post('/api/auth/refresh', { refreshToken })
  },

  // 获取当前用户信息
  getCurrentUser: (): Promise<IUser> => {
    return request.get('/api/auth/me')
  },

  // 更新用户资料
  updateProfile: (data: Partial<IUser>): Promise<IUser> => {
    return request.put('/api/auth/profile', data)
  },

  // 修改密码
  changePassword: (oldPassword: string, newPassword: string): Promise<IApiResponse> => {
    return request.put('/api/auth/password', {
      oldPassword,
      newPassword
    })
  },

  // 重置密码请求
  requestPasswordReset: (email: string): Promise<IApiResponse> => {
    return request.post('/api/auth/password/reset-request', { email })
  },

  // 重置密码确认
  resetPassword: (token: string, newPassword: string): Promise<IApiResponse> => {
    return request.post('/api/auth/password/reset-confirm', {
      token,
      newPassword
    })
  },

  // 验证邮箱
  verifyEmail: (token: string): Promise<IApiResponse> => {
    return request.post('/api/auth/email/verify', { token })
  },

  // 重新发送验证邮件
  resendVerificationEmail: (): Promise<IApiResponse> => {
    return request.post('/api/auth/email/resend')
  }
} 