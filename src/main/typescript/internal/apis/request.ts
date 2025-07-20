import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { IApiResponse } from '@/types'
import { ElMessage } from 'element-plus'

// 创建axios实例
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 自动添加认证token
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 添加请求时间戳（用于调试）
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, config.data)
    }
    
    return config
  },
  (error) => {
    console.error('[API Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<IApiResponse>) => {
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`, response.data)
    }
    
    const { data } = response
    
    // 检查业务状态码
    if (data.success === false) {
      const errorMessage = data.message || '请求失败'
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
    
    // 返回实际数据
    return data.data !== undefined ? data.data : data
  },
  async (error) => {
    console.error('[API Response Error]', error)
    
    if (!error.response) {
      // 网络错误
      ElMessage.error('网络连接失败，请检查您的网络设置')
      return Promise.reject(error)
    }
    
    const { status, data } = error.response
    
    switch (status) {
      case 401:
        // 未授权，尝试刷新token
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken && !error.config._retry) {
          error.config._retry = true
          
          try {
            const refreshResponse = await axios.post('/api/auth/refresh', {
              refreshToken
            })
            
            const newToken = refreshResponse.data.data.token
            const newRefreshToken = refreshResponse.data.data.refreshToken
            
            // 更新本地存储
            localStorage.setItem('token', newToken)
            localStorage.setItem('refreshToken', newRefreshToken)
            
            // 重新发送原始请求
            error.config.headers.Authorization = `Bearer ${newToken}`
            return instance.request(error.config)
          } catch (refreshError) {
            // 刷新token失败，清除认证信息并跳转登录
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
            
            ElMessage.error('登录已过期，请重新登录')
            
            // 跳转到登录页面
            if (window.location.pathname !== '/login') {
              window.location.href = '/login'
            }
            
            return Promise.reject(refreshError)
          }
        } else {
          // 没有refresh token或刷新失败
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          
          ElMessage.error('请先登录')
          
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
        }
        break
        
      case 403:
        ElMessage.error('权限不足，无法访问该资源')
        break
        
      case 404:
        ElMessage.error('请求的资源不存在')
        break
        
      case 422:
        // 表单验证错误
        if (data.errors && Array.isArray(data.errors)) {
          data.errors.forEach((errorMsg: string) => {
            ElMessage.error(errorMsg)
          })
        } else {
          ElMessage.error(data.message || '数据验证失败')
        }
        break
        
      case 429:
        ElMessage.error('请求过于频繁，请稍后再试')
        break
        
      case 500:
        ElMessage.error('服务器内部错误，请稍后再试')
        break
        
      default:
        ElMessage.error(data.message || `请求失败 (${status})`)
    }
    
    return Promise.reject(error)
  }
)

// 封装常用的HTTP方法
export const request = {
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return instance.get(url, config)
  },
  
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return instance.post(url, data, config)
  },
  
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return instance.put(url, data, config)
  },
  
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return instance.patch(url, data, config)
  },
  
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return instance.delete(url, config)
  },
  
  // 文件上传
  upload: <T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> => {
    return instance.post(url, formData, {
      ...config,
      headers: {
        ...config?.headers,
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 文件下载
  download: (url: string, filename?: string, config?: AxiosRequestConfig): Promise<void> => {
    return instance.get(url, {
      ...config,
      responseType: 'blob'
    }).then((response) => {
      const blob = new Blob([response.data])
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
    })
  }
}

export default instance 