/**
 * TypeScript 常量定义
 */

/**
 * 文件相关常量
 */
export const FILE_CONSTANTS = {
  MAX_FILE_SIZE: 100 * 1024 * 1024 * 1024, // 100GB
  MAX_FILENAME_LENGTH: 255,
  MAX_PATH_LENGTH: 1000,
  DEFAULT_MIME_TYPE: 'application/octet-stream',
  
  // 支持预览的文件类型
  PREVIEWABLE_TEXT_TYPES: [
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/json', 'application/xml', 'text/xml',
    'text/markdown', 'text/csv'
  ],
  
  PREVIEWABLE_IMAGE_TYPES: [
    'image/jpeg', 'image/png', 'image/gif', 'image/webp',
    'image/svg+xml', 'image/bmp'
  ],
  
  // 可编辑的文件类型
  EDITABLE_TYPES: [
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/json', 'application/xml', 'text/xml',
    'text/markdown', 'text/csv', 'application/yaml'
  ]
} as const;

/**
 * 用户相关常量
 */
export const USER_CONSTANTS = {
  MIN_USERNAME_LENGTH: 3,
  MAX_USERNAME_LENGTH: 50,
  MAX_DISPLAY_NAME_LENGTH: 100,
  MIN_PASSWORD_LENGTH: 8,
  MAX_LOGIN_ATTEMPTS: 5,
  ACCOUNT_LOCK_DURATION_MINUTES: 30
} as const;

/**
 * 权限相关常量
 */
export const PERMISSION_CONSTANTS = {
  TYPES: {
    READ: 'READ',
    WRITE: 'WRITE',
    DELETE: 'DELETE',
    SHARE: 'SHARE',
    ADMIN: 'ADMIN'
  },
  DEFAULT_EXPIRY_DAYS: 30,
  MAX_EXPIRY_DAYS: 365
} as const;

/**
 * 版本控制相关常量
 */
export const VERSION_CONSTANTS = {
  MAX_COMMIT_MESSAGE_LENGTH: 1000,
  MAX_VERSIONS_PER_FILE: 100,
  DEFAULT_BRANCH: 'main',
  GIT_COMMIT_HASH_LENGTH: 40
} as const;

/**
 * 模板相关常量
 */
export const TEMPLATE_CONSTANTS = {
  MAX_TEMPLATE_NAME_LENGTH: 100,
  MAX_TEMPLATE_DESCRIPTION_LENGTH: 500,
  MAX_TEMPLATE_FILES: 1000
} as const;

/**
 * API相关常量
 */
export const API_CONSTANTS = {
  VERSION: 'v1',
  PREFIX: '/api/v1',
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  
  // HTTP头
  HEADERS: {
    AUTHORIZATION: 'Authorization',
    CONTENT_TYPE: 'Content-Type',
    ACCEPT: 'Accept',
    USER_AGENT: 'User-Agent',
    X_FORWARDED_FOR: 'X-Forwarded-For'
  },
  
  // 响应状态码
  RESPONSE_CODES: {
    SUCCESS: 'SUCCESS',
    ERROR: 'ERROR',
    VALIDATION_ERROR: 'VALIDATION_ERROR',
    PERMISSION_DENIED: 'PERMISSION_DENIED',
    NOT_FOUND: 'NOT_FOUND'
  }
} as const;

/**
 * WebDAV相关常量
 */
export const WEBDAV_CONSTANTS = {
  PATH: '/webdav',
  ENTERPRISE_SPACE_PATH: '/enterprise',
  PERSONAL_SPACE_PATH: '/personal',
  
  // WebDAV方法
  METHODS: {
    PROPFIND: 'PROPFIND',
    PROPPATCH: 'PROPPATCH',
    MKCOL: 'MKCOL',
    COPY: 'COPY',
    MOVE: 'MOVE',
    LOCK: 'LOCK',
    UNLOCK: 'UNLOCK'
  }
} as const;

/**
 * 系统配置相关常量
 */
export const CONFIG_CONSTANTS = {
  DEFAULT_TIMEZONE: 'Asia/Shanghai',
  DEFAULT_DATE_FORMAT: 'yyyy-MM-dd',
  DEFAULT_TIME_FORMAT: 'HH:mm:ss',
  DEFAULT_DATETIME_FORMAT: 'yyyy-MM-dd HH:mm:ss'
} as const;

/**
 * 审计日志相关常量
 */
export const AUDIT_CONSTANTS = {
  // 操作类型
  ACTIONS: {
    LOGIN: 'LOGIN',
    LOGOUT: 'LOGOUT',
    FILE_UPLOAD: 'FILE_UPLOAD',
    FILE_DOWNLOAD: 'FILE_DOWNLOAD',
    FILE_DELETE: 'FILE_DELETE',
    FILE_SHARE: 'FILE_SHARE',
    PERMISSION_GRANT: 'PERMISSION_GRANT',
    PERMISSION_REVOKE: 'PERMISSION_REVOKE',
    VERSION_COMMIT: 'VERSION_COMMIT',
    TEMPLATE_CREATE: 'TEMPLATE_CREATE',
    TEMPLATE_APPLY: 'TEMPLATE_APPLY'
  },
  
  // 日志保留时间（天）
  LOG_RETENTION_DAYS: 90,
  SECURITY_LOG_RETENTION_DAYS: 365
} as const;

/**
 * 错误消息常量
 */
export const ERROR_MESSAGES = {
  USER_NOT_FOUND: '用户不存在',
  FILE_NOT_FOUND: '文件不存在',
  FOLDER_NOT_FOUND: '文件夹不存在',
  PERMISSION_DENIED: '权限不足',
  INVALID_CREDENTIALS: '用户名或密码错误',
  ACCOUNT_LOCKED: '账户已被锁定',
  ACCOUNT_DISABLED: '账户已被禁用',
  FILE_TOO_LARGE: '文件大小超出限制',
  INVALID_FILE_TYPE: '不支持的文件类型',
  STORAGE_FULL: '存储空间不足',
  VERSION_CONFLICT: '版本冲突',
  TEMPLATE_NOT_FOUND: '模板不存在',
  INVALID_TEMPLATE: '模板格式不正确'
} as const;

/**
 * 文件大小单位
 */
export const FILE_SIZE_UNITS = ['B', 'KB', 'MB', 'GB', 'TB'] as const;

/**
 * 支持的语言
 */
export const SUPPORTED_LANGUAGES = [
  { code: 'zh-CN', name: '简体中文' },
  { code: 'en-US', name: 'English' },
  { code: 'ja-JP', name: '日本語' }
] as const;

/**
 * 主题选项
 */
export const THEME_OPTIONS = [
  { value: 'LIGHT', label: '浅色主题' },
  { value: 'DARK', label: '深色主题' },
  { value: 'AUTO', label: '自动主题' }
] as const;