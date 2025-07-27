import request from './request'
import type {
  IApiResponse
} from '@/types/index'

// ==================== WebDAV相关类型 ====================
export interface WebDavResource {
  path: string
  name: string
  type: 'file' | 'directory'
  size?: number
  mimeType?: string
  lastModified: string
  etag?: string
  properties: Record<string, any>
}

export interface WebDavProperty {
  namespace: string
  name: string
  value: any
}

export interface WebDavPropfindRequest {
  path: string
  depth: '0' | '1' | 'infinity'
  properties?: string[]
}

export interface WebDavPropfindResponse {
  resources: WebDavResource[]
  status: number
}

export interface WebDavLockInfo {
  lockToken: string
  lockType: 'write' | 'read'
  lockScope: 'exclusive' | 'shared'
  owner: string
  timeout: number
  depth: '0' | 'infinity'
}

export interface WebDavMoveRequest {
  source: string
  destination: string
  overwrite?: boolean
}

export interface WebDavCopyRequest {
  source: string
  destination: string
  overwrite?: boolean
  depth?: '0' | 'infinity'
}

export interface WebDavConfig {
  enabled: boolean
  basePath: string
  maxFileSize: number
  allowedMethods: string[]
  lockTimeout: number
  supportedProperties: string[]
}

// ==================== WebDAV API ====================
export const webdavApi = {
  // PROPFIND - 获取资源属性
  async propfind(data: WebDavPropfindRequest): Promise<IApiResponse<WebDavPropfindResponse>> {
    const response = await request({
      method: 'PROPFIND',
      url: `/webdav${data.path}`,
      headers: {
        'Depth': data.depth,
        'Content-Type': 'application/xml'
      },
      data: data.properties ? this.buildPropfindXml(data.properties) : undefined
    })
    return response.data
  },

  // GET - 下载文件
  async getFile(path: string): Promise<Blob> {
    const response = await request({
      method: 'GET',
      url: `/webdav${path}`,
      responseType: 'blob'
    })
    return response.data
  },

  // PUT - 上传文件
  async putFile(path: string, file: File | Blob, onProgress?: (progress: number) => void): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'PUT',
      url: `/webdav${path}`,
      data: file,
      headers: {
        'Content-Type': file instanceof File ? file.type : 'application/octet-stream'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
    return response.data
  },

  // DELETE - 删除资源
  async deleteResource(path: string): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'DELETE',
      url: `/webdav${path}`
    })
    return response.data
  },

  // MKCOL - 创建集合（文件夹）
  async createCollection(path: string): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'MKCOL',
      url: `/webdav${path}`
    })
    return response.data
  },

  // MOVE - 移动资源
  async moveResource(data: WebDavMoveRequest): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'MOVE',
      url: `/webdav${data.source}`,
      headers: {
        'Destination': `/webdav${data.destination}`,
        'Overwrite': data.overwrite ? 'T' : 'F'
      }
    })
    return response.data
  },

  // COPY - 复制资源
  async copyResource(data: WebDavCopyRequest): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'COPY',
      url: `/webdav${data.source}`,
      headers: {
        'Destination': `/webdav${data.destination}`,
        'Overwrite': data.overwrite ? 'T' : 'F',
        'Depth': data.depth || 'infinity'
      }
    })
    return response.data
  },

  // LOCK - 锁定资源
  async lockResource(path: string, lockInfo: Partial<WebDavLockInfo>): Promise<IApiResponse<WebDavLockInfo>> {
    const lockXml = this.buildLockXml(lockInfo)
    const response = await request({
      method: 'LOCK',
      url: `/webdav${path}`,
      headers: {
        'Content-Type': 'application/xml',
        'Depth': lockInfo.depth || '0',
        'Timeout': `Second-${lockInfo.timeout || 3600}`
      },
      data: lockXml
    })
    return response.data
  },

  // UNLOCK - 解锁资源
  async unlockResource(path: string, lockToken: string): Promise<IApiResponse<void>> {
    const response = await request({
      method: 'UNLOCK',
      url: `/webdav${path}`,
      headers: {
        'Lock-Token': `<${lockToken}>`
      }
    })
    return response.data
  },

  // OPTIONS - 获取支持的方法
  async getOptions(path: string = '/'): Promise<IApiResponse<{
    allowedMethods: string[]
    davCompliance: string[]
    features: string[]
  }>> {
    const response = await request({
      method: 'OPTIONS',
      url: `/webdav${path}`
    })
    return response.data
  },

  // PROPPATCH - 修改属性
  async patchProperties(path: string, properties: WebDavProperty[]): Promise<IApiResponse<void>> {
    const proppatchXml = this.buildProppatchXml(properties)
    const response = await request({
      method: 'PROPPATCH',
      url: `/webdav${path}`,
      headers: {
        'Content-Type': 'application/xml'
      },
      data: proppatchXml
    })
    return response.data
  },

  // 获取WebDAV配置
  async getConfig(): Promise<IApiResponse<WebDavConfig>> {
    const response = await request.get('/api/webdav/config')
    return response.data
  },

  // 获取WebDAV状态
  async getStatus(): Promise<IApiResponse<{
    enabled: boolean
    version: string
    activeSessions: number
    totalRequests: number
    errorRate: number
  }>> {
    const response = await request.get('/api/webdav/status')
    return response.data
  },

  // 构建PROPFIND XML
  buildPropfindXml(properties: string[]): string {
    const props = properties.map(prop => `<D:${prop}/>`).join('')
    return `<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    ${props}
  </D:prop>
</D:propfind>`
  },

  // 构建LOCK XML
  buildLockXml(lockInfo: Partial<WebDavLockInfo>): string {
    return `<?xml version="1.0" encoding="utf-8"?>
<D:lockinfo xmlns:D="DAV:">
  <D:lockscope><D:${lockInfo.lockScope || 'exclusive'}/></D:lockscope>
  <D:locktype><D:${lockInfo.lockType || 'write'}/></D:locktype>
  <D:owner>${lockInfo.owner || 'unknown'}</D:owner>
</D:lockinfo>`
  },

  // 构建PROPPATCH XML
  buildProppatchXml(properties: WebDavProperty[]): string {
    const setProps = properties.map(prop => 
      `<${prop.namespace}:${prop.name}>${prop.value}</${prop.namespace}:${prop.name}>`
    ).join('')
    
    return `<?xml version="1.0" encoding="utf-8"?>
<D:propertyupdate xmlns:D="DAV:">
  <D:set>
    <D:prop>
      ${setProps}
    </D:prop>
  </D:set>
</D:propertyupdate>`
  },
}

// ==================== WebDAV客户端类 ====================
export class WebDavClient {
  private basePath: string
  private credentials?: { username: string; password: string }

  constructor(basePath: string = '/webdav', credentials?: { username: string; password: string }) {
    this.basePath = basePath
    this.credentials = credentials
  }

  // 列出目录内容
  async listDirectory(path: string): Promise<WebDavResource[]> {
    const response = await webdavApi.propfind({
      path,
      depth: '1',
      properties: ['resourcetype', 'getcontentlength', 'getlastmodified', 'getcontenttype']
    })
    
    if (response.success && response.data) {
      return response.data.resources.filter(resource => resource.path !== path)
    }
    
    return []
  }

  // 上传文件
  async uploadFile(remotePath: string, file: File, onProgress?: (progress: number) => void): Promise<boolean> {
    try {
      const response = await webdavApi.putFile(remotePath, file, onProgress)
      return response.success
    } catch (error) {
      console.error('WebDAV文件上传失败:', error)
      return false
    }
  }

  // 下载文件
  async downloadFile(remotePath: string): Promise<Blob | null> {
    try {
      return await webdavApi.getFile(remotePath)
    } catch (error) {
      console.error('WebDAV文件下载失败:', error)
      return null
    }
  }

  // 创建文件夹
  async createFolder(remotePath: string): Promise<boolean> {
    try {
      const response = await webdavApi.createCollection(remotePath)
      return response.success
    } catch (error) {
      console.error('WebDAV文件夹创建失败:', error)
      return false
    }
  }

  // 删除文件或文件夹
  async delete(remotePath: string): Promise<boolean> {
    try {
      const response = await webdavApi.deleteResource(remotePath)
      return response.success
    } catch (error) {
      console.error('WebDAV删除失败:', error)
      return false
    }
  }

  // 移动文件或文件夹
  async move(sourcePath: string, destinationPath: string, overwrite: boolean = false): Promise<boolean> {
    try {
      const response = await webdavApi.moveResource({
        source: sourcePath,
        destination: destinationPath,
        overwrite
      })
      return response.success
    } catch (error) {
      console.error('WebDAV移动失败:', error)
      return false
    }
  }

  // 复制文件或文件夹
  async copy(sourcePath: string, destinationPath: string, overwrite: boolean = false): Promise<boolean> {
    try {
      const response = await webdavApi.copyResource({
        source: sourcePath,
        destination: destinationPath,
        overwrite
      })
      return response.success
    } catch (error) {
      console.error('WebDAV复制失败:', error)
      return false
    }
  }

  // 检查资源是否存在
  async exists(remotePath: string): Promise<boolean> {
    try {
      const response = await webdavApi.propfind({
        path: remotePath,
        depth: '0'
      })
      return response.success && (response.data?.resources?.length || 0) > 0
    } catch (error) {
      return false
    }
  }

  // 获取资源信息
  async getResourceInfo(remotePath: string): Promise<WebDavResource | null> {
    try {
      const response = await webdavApi.propfind({
        path: remotePath,
        depth: '0',
        properties: ['resourcetype', 'getcontentlength', 'getlastmodified', 'getcontenttype', 'getetag']
      })

      if (response.success && response.data && (response.data.resources?.length || 0) > 0) {
        return response.data.resources[0]
      }

      return null
    } catch (error) {
      console.error('获取WebDAV资源信息失败:', error)
      return null
    }
  }
}

// ==================== WebDAV工具函数 ====================
export const webdavUtils = {
  // 解析WebDAV路径
  parsePath(path: string): {
    directory: string
    filename: string
    extension: string
  } {
    const normalizedPath = path.replace(/\\/g, '/').replace(/\/+/g, '/')
    const parts = normalizedPath.split('/')
    const filename = parts.pop() || ''
    const directory = parts.join('/')
    const extension = filename.includes('.') ? filename.split('.').pop() || '' : ''
    
    return { directory, filename, extension }
  },

  // 构建WebDAV URL
  buildUrl(basePath: string, resourcePath: string): string {
    const cleanBasePath = basePath.replace(/\/+$/, '')
    const cleanResourcePath = resourcePath.replace(/^\/+/, '')
    return `${cleanBasePath}/${cleanResourcePath}`
  },

  // 验证WebDAV路径
  validatePath(path: string): { isValid: boolean; error?: string } {
    if (!path) {
      return { isValid: false, error: '路径不能为空' }
    }
    
    if (path.length > 1000) {
      return { isValid: false, error: '路径过长' }
    }
    
    const invalidChars = /[<>:"|?*\x00-\x1f]/
    if (invalidChars.test(path)) {
      return { isValid: false, error: '路径包含非法字符' }
    }
    
    return { isValid: true }
  },

  // 格式化WebDAV错误
  formatWebDavError(status: number): string {
    const errorMap: Record<number, string> = {
      400: '请求格式错误',
      401: '认证失败',
      403: '权限不足',
      404: '资源不存在',
      405: '方法不允许',
      409: '资源冲突',
      412: '前置条件失败',
      423: '资源已锁定',
      507: '存储空间不足'
    }
    return errorMap[status] || `WebDAV错误 (${status})`
  },

  // 检查WebDAV功能支持
  checkFeatureSupport(davCompliance: string[]): {
    supportsLocking: boolean
    supportsVersioning: boolean
    supportsSearch: boolean
    supportsACL: boolean
  } {
    return {
      supportsLocking: davCompliance.includes('2'),
      supportsVersioning: davCompliance.includes('version-control'),
      supportsSearch: davCompliance.includes('search'),
      supportsACL: davCompliance.includes('access-control')
    }
  },

  // 生成WebDAV客户端配置
  generateClientConfig(serverUrl: string, credentials?: { username: string; password: string }): {
    url: string
    headers: Record<string, string>
  } {
    const headers: Record<string, string> = {
      'User-Agent': 'Lyra WebDAV Client',
      'Accept': '*/*'
    }
    
    if (credentials) {
      const auth = btoa(`${credentials.username}:${credentials.password}`)
      headers['Authorization'] = `Basic ${auth}`
    }
    
    return { url: serverUrl, headers }
  },
}
