import request from './request'
import type {
  IFileInfo,
  IUser,
  IApiResponse
} from '@/types/index'

// ==================== 编辑相关类型 ====================
export interface EditSession {
  sessionId: string
  fileId: number
  userId: number
  startTime: string
  lastActivity: string
  isActive: boolean
  lockExpiry?: string
  collaborators: EditCollaborator[]
}

export interface EditCollaborator {
  userId: number
  username: string
  displayName: string
  joinTime: string
  lastActivity: string
  cursor?: EditorCursor
  selection?: EditorSelection
}

export interface EditorCursor {
  line: number
  column: number
}

export interface EditorSelection {
  start: EditorCursor
  end: EditorCursor
}

export interface StartEditRequest {
  fileId: number
  mode?: 'exclusive' | 'collaborative'
  lockTimeout?: number // 锁定超时时间(秒)
}

export interface EditResult {
  success: boolean
  message?: string
  sessionId?: string
  content?: string
  encoding?: string
  language?: string
  readOnly?: boolean
}

export interface SaveEditRequest {
  sessionId: string
  content: string
  encoding?: string
  createBackup?: boolean
}

export interface EditHistory {
  id: number
  sessionId: string
  userId: number
  username: string
  action: 'create' | 'save' | 'close'
  timestamp: string
  changes?: EditChange[]
  fileSize?: number
  lineCount?: number
}

export interface EditChange {
  type: 'insert' | 'delete' | 'replace'
  position: EditorCursor
  content: string
  length?: number
}

export interface CollaborativeEdit {
  sessionId: string
  userId: number
  timestamp: string
  changes: EditChange[]
  version: number
}

export interface EditorConfig {
  maxFileSize: number
  supportedLanguages: string[]
  autoSaveInterval: number
  lockTimeout: number
  maxCollaborators: number
  enableVersionControl: boolean
}

// ==================== 在线编辑API ====================
export const editorApi = {
  // 开始编辑会话
  async startEditSession(data: { fileId: number }): Promise<IApiResponse<EditResult>> {
    const response = await request.post('/api/editor/start', data)
    return response.data
  },

  // 获取编辑会话信息
  async getEditSession(sessionId: string): Promise<IApiResponse<EditSession>> {
    const response = await request.get(`/api/editor/session/${sessionId}`)
    return response.data
  },

  // 保存编辑内容
  async saveEdit(data: SaveEditRequest): Promise<IApiResponse<{
    success: boolean
    version: number
    fileSize: number
    lastModified: string
  }>> {
    const response = await request.put(`/api/editor/session/${data.sessionId}/save`, data)
    return response.data
  },

  // 结束编辑会话
  async endEditSession(sessionId: string): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/editor/session/${sessionId}`)
    return response.data
  },

  // 获取支持的编辑类型
  async getSupportedTypes(): Promise<IApiResponse<{
    textExtensions: string[]
    codeExtensions: string[]
    configExtensions: string[]
    languages: Array<{
      name: string
      extensions: string[]
      mimeTypes: string[]
    }>
  }>> {
    const response = await request.get('/api/editor/supported-types')
    return response.data
  },

  // 获取编辑历史
  async getEditHistory(fileId: number, params?: {
    page?: number
    size?: number
    userId?: number
    startTime?: string
    endTime?: string
  }): Promise<IApiResponse<EditHistory[]>> {
    const response = await request.get(`/api/editor/history/${fileId}`, { params })
    return response.data
  },

  // 获取活跃编辑会话
  async getActiveSessions(params?: {
    fileId?: number
    userId?: number
    spaceId?: number
  }): Promise<IApiResponse<EditSession[]>> {
    const response = await request.get('/api/editor/sessions/active', { params })
    return response.data
  },

  // 加入协作编辑
  async joinCollaboration(sessionId: string): Promise<IApiResponse<{
    success: boolean
    content: string
    version: number
    collaborators: EditCollaborator[]
  }>> {
    const response = await request.post(`/api/editor/session/${sessionId}/join`)
    return response.data
  },

  // 离开协作编辑
  async leaveCollaboration(sessionId: string): Promise<IApiResponse<void>> {
    const response = await request.post(`/api/editor/session/${sessionId}/leave`)
    return response.data
  },

  // 发送协作编辑操作
  async sendCollaborativeEdit(data: CollaborativeEdit): Promise<IApiResponse<{
    version: number
    conflicts?: EditChange[]
  }>> {
    const response = await request.post(`/api/editor/collaboration/edit`, data)
    return response.data
  },

  // 更新光标位置
  async updateCursor(sessionId: string, cursor: EditorCursor): Promise<IApiResponse<void>> {
    const response = await request.put(`/api/editor/session/${sessionId}/cursor`, { cursor })
    return response.data
  },

  // 更新选择区域
  async updateSelection(sessionId: string, selection: EditorSelection): Promise<IApiResponse<void>> {
    const response = await request.put(`/api/editor/session/${sessionId}/selection`, { selection })
    return response.data
  },

  // 获取编辑器配置
  async getEditorConfig(): Promise<IApiResponse<EditorConfig>> {
    const response = await request.get('/api/editor/config')
    return response.data
  },

  // 检查文件编辑权限
  async checkEditPermission(fileId: number): Promise<IApiResponse<{
    canEdit: boolean
    canCollaborate: boolean
    reason?: string
    currentSessions?: EditSession[]
  }>> {
    const response = await request.get(`/api/editor/permission/${fileId}`)
    return response.data
  },

  // 强制结束编辑会话（管理员功能）
  async forceEndSession(sessionId: string): Promise<IApiResponse<void>> {
    const response = await request.delete(`/api/editor/session/${sessionId}/force`)
    return response.data
  },

  // 创建文件备份
  async createBackup(sessionId: string): Promise<IApiResponse<{
    backupId: string
    backupPath: string
    createdAt: string
  }>> {
    const response = await request.post(`/api/editor/session/${sessionId}/backup`)
    return response.data
  },

  // 恢复文件备份
  async restoreBackup(fileId: number, backupId: string): Promise<IApiResponse<void>> {
    const response = await request.post(`/api/editor/restore/${fileId}`, { backupId })
    return response.data
  },
}

// ==================== WebSocket 连接管理 ====================
export class EditorWebSocket {
  private ws: WebSocket | null = null
  private sessionId: string
  private callbacks: Map<string, Function[]> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000

  constructor(sessionId: string) {
    this.sessionId = sessionId
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const wsUrl = `${protocol}//${window.location.host}/api/editor/ws/${this.sessionId}`
        
        this.ws = new WebSocket(wsUrl)
        
        this.ws.onopen = () => {
          console.log('编辑器WebSocket连接已建立')
          this.reconnectAttempts = 0
          resolve()
        }
        
        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('解析WebSocket消息失败:', error)
          }
        }
        
        this.ws.onclose = () => {
          console.log('编辑器WebSocket连接已关闭')
          this.attemptReconnect()
        }
        
        this.ws.onerror = (error) => {
          console.error('编辑器WebSocket错误:', error)
          reject(error)
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  send(message: any): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  on(event: string, callback: Function): void {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, [])
    }
    this.callbacks.get(event)!.push(callback)
  }

  off(event: string, callback?: Function): void {
    if (!this.callbacks.has(event)) return
    
    if (callback) {
      const callbacks = this.callbacks.get(event)!
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    } else {
      this.callbacks.delete(event)
    }
  }

  private handleMessage(message: any): void {
    const { type, data } = message
    const callbacks = this.callbacks.get(type)
    if (callbacks) {
      callbacks.forEach(callback => callback(data))
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重连编辑器WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('重连失败:', error)
        })
      }, this.reconnectDelay * this.reconnectAttempts)
    }
  }
}

// ==================== 编辑工具函数 ====================
export const editorUtils = {
  // 检查文件是否可编辑
  isEditable(file: IFileInfo): boolean {
    const editableTypes = [
      'text/', 'application/json', 'application/xml',
      'application/javascript', 'application/typescript'
    ]
    return editableTypes.some(type => file.mimeType.startsWith(type))
  },

  // 获取编程语言
  getLanguage(filename: string, _mimeType?: string): string {
    const ext = filename.split('.').pop()?.toLowerCase() || ''
    
    const languageMap: Record<string, string> = {
      js: 'javascript',
      ts: 'typescript',
      jsx: 'javascript',
      tsx: 'typescript',
      py: 'python',
      java: 'java',
      cpp: 'cpp',
      c: 'c',
      cs: 'csharp',
      php: 'php',
      rb: 'ruby',
      go: 'go',
      rs: 'rust',
      kt: 'kotlin',
      swift: 'swift',
      html: 'html',
      css: 'css',
      scss: 'scss',
      less: 'less',
      xml: 'xml',
      json: 'json',
      yaml: 'yaml',
      yml: 'yaml',
      md: 'markdown',
      sql: 'sql',
      sh: 'shell',
      bash: 'shell',
      ps1: 'powershell',
      dockerfile: 'dockerfile'
    }
    
    return languageMap[ext] || 'text'
  },

  // 格式化编辑历史
  formatEditAction(action: string): string {
    const actionMap: Record<string, string> = {
      create: '开始编辑',
      save: '保存文件',
      close: '结束编辑'
    }
    return actionMap[action] || action
  },

  // 计算文本差异
  calculateDiff(oldText: string, newText: string): EditChange[] {
    // 简化的差异计算，实际项目中可以使用更复杂的算法
    const changes: EditChange[] = []
    
    if (oldText !== newText) {
      changes.push({
        type: 'replace',
        position: { line: 0, column: 0 },
        content: newText,
        length: oldText.length
      })
    }
    
    return changes
  },

  // 应用编辑变更
  applyChanges(content: string, changes: EditChange[]): string {
    let result = content
    
    // 按位置倒序排列，避免位置偏移问题
    const sortedChanges = [...changes].sort((a, b) => {
      if (a.position.line !== b.position.line) {
        return b.position.line - a.position.line
      }
      return b.position.column - a.position.column
    })
    
    for (const change of sortedChanges) {
      const lines = result.split('\n')
      const line = lines[change.position.line] || ''
      
      switch (change.type) {
        case 'insert':
          const before = line.substring(0, change.position.column)
          const after = line.substring(change.position.column)
          lines[change.position.line] = before + change.content + after
          break
          
        case 'delete':
          const deleteEnd = change.position.column + (change.length || 0)
          const beforeDelete = line.substring(0, change.position.column)
          const afterDelete = line.substring(deleteEnd)
          lines[change.position.line] = beforeDelete + afterDelete
          break
          
        case 'replace':
          const replaceEnd = change.position.column + (change.length || 0)
          const beforeReplace = line.substring(0, change.position.column)
          const afterReplace = line.substring(replaceEnd)
          lines[change.position.line] = beforeReplace + change.content + afterReplace
          break
      }
      
      result = lines.join('\n')
    }
    
    return result
  },

  // 验证编辑权限
  validateEditPermission(file: IFileInfo, _user?: IUser): {
    canEdit: boolean
    reason?: string
  } {
    // 检查文件状态
    if (file.status === 'DELETED') {
      return { canEdit: false, reason: '文件已被删除' }
    }
    
    if (file.status === 'ARCHIVED') {
      return { canEdit: false, reason: '文件已归档' }
    }
    
    // 检查文件类型
    if (!editorUtils.isEditable(file)) {
      return { canEdit: false, reason: '文件类型不支持编辑' }
    }
    
    // 检查文件大小（假设限制为10MB）
    if (file.sizeBytes > 10 * 1024 * 1024) {
      return { canEdit: false, reason: '文件过大，无法编辑' }
    }
    
    return { canEdit: true }
  },

  // 生成会话ID
  generateSessionId(): string {
    return `edit_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`
  },
}
