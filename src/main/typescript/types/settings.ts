import { BaseEntity } from './common';
import { User } from './user';

/**
 * 主题枚举
 */
export enum Theme {
  LIGHT = 'LIGHT',
  DARK = 'DARK',
  AUTO = 'AUTO'
}

/**
 * 语言枚举
 */
export enum Language {
  ZH_CN = 'ZH_CN',
  EN_US = 'EN_US',
  JA_JP = 'JA_JP'
}

/**
 * 用户设置接口
 */
export interface UserSettings extends BaseEntity {
  userId: number;
  user: User;
  theme: Theme;
  language: Language;
  timezone: string;
  dateFormat: string;
  timeFormat: string;
  fileListView: boolean;
  showHiddenFiles: boolean;
  autoSave: boolean;
  notificationEnabled: boolean;
  customSettings?: Record<string, any>;
}

/**
 * 用户设置更新请求
 */
export interface UpdateUserSettingsRequest {
  theme?: Theme;
  language?: Language;
  timezone?: string;
  dateFormat?: string;
  timeFormat?: string;
  fileListView?: boolean;
  showHiddenFiles?: boolean;
  autoSave?: boolean;
  notificationEnabled?: boolean;
  customSettings?: Record<string, any>;
}

/**
 * 系统配置接口
 */
export interface SystemConfig {
  // 文件上传配置
  maxFileSize: number;
  allowedFileTypes: string[];
  uploadChunkSize: number;
  
  // 版本控制配置
  defaultVersionControlType: 'NONE' | 'BASIC' | 'ADVANCED';
  maxVersionsPerFile: number;
  autoCommitEnabled: boolean;
  
  // 安全配置
  passwordMinLength: number;
  passwordRequireSpecialChar: boolean;
  sessionTimeout: number;
  maxLoginAttempts: number;
  
  // 存储配置
  storageType: 'LOCAL' | 'NFS' | 'S3';
  storageConfig: Record<string, any>;
  
  // 缓存配置
  cacheEnabled: boolean;
  cacheType: 'MEMORY' | 'REDIS';
  cacheConfig: Record<string, any>;
  
  // 通知配置
  emailEnabled: boolean;
  emailConfig: EmailConfig;
  
  // WebDAV配置
  webdavEnabled: boolean;
  webdavPath: string;
  
  // 搜索配置
  searchEnabled: boolean;
  searchType: 'BASIC' | 'MEILISEARCH' | 'ELASTICSEARCH';
  searchConfig: Record<string, any>;
}

/**
 * 邮件配置
 */
export interface EmailConfig {
  host: string;
  port: number;
  username: string;
  password: string;
  ssl: boolean;
  fromAddress: string;
  fromName: string;
}

/**
 * 系统配置更新请求
 */
export interface UpdateSystemConfigRequest {
  section: string;
  config: Record<string, any>;
}

/**
 * 用户偏好设置
 */
export interface UserPreferences {
  // 界面偏好
  sidebarCollapsed: boolean;
  toolbarVisible: boolean;
  statusBarVisible: boolean;
  
  // 文件管理偏好
  defaultView: 'LIST' | 'GRID' | 'TREE';
  sortBy: 'NAME' | 'SIZE' | 'DATE' | 'TYPE';
  sortDirection: 'ASC' | 'DESC';
  showFileExtensions: boolean;
  showFileSizes: boolean;
  
  // 编辑器偏好
  editorTheme: string;
  fontSize: number;
  tabSize: number;
  wordWrap: boolean;
  lineNumbers: boolean;
  
  // 快捷键配置
  shortcuts: Record<string, string>;
  
  // 其他偏好
  confirmDelete: boolean;
  autoRefresh: boolean;
  refreshInterval: number;
}

/**
 * 通知设置
 */
export interface NotificationSettings {
  // 邮件通知
  emailNotifications: boolean;
  emailOnFileShare: boolean;
  emailOnPermissionChange: boolean;
  emailOnVersionUpdate: boolean;
  
  // 浏览器通知
  browserNotifications: boolean;
  notifyOnUploadComplete: boolean;
  notifyOnDownloadComplete: boolean;
  notifyOnError: boolean;
  
  // 通知频率
  digestFrequency: 'IMMEDIATE' | 'HOURLY' | 'DAILY' | 'WEEKLY' | 'NEVER';
  quietHours: {
    enabled: boolean;
    startTime: string;
    endTime: string;
  };
}

/**
 * 安全设置
 */
export interface SecuritySettings {
  // 密码设置
  changePasswordRequired: boolean;
  passwordExpiryDays: number;
  
  // 会话设置
  rememberMe: boolean;
  sessionTimeout: number;
  
  // 两步验证
  twoFactorEnabled: boolean;
  twoFactorMethod: 'SMS' | 'EMAIL' | 'TOTP';
  
  // 登录历史
  trackLoginHistory: boolean;
  maxLoginHistoryEntries: number;
  
  // API访问
  apiAccessEnabled: boolean;
  apiTokens: Array<{
    id: string;
    name: string;
    createdAt: string;
    lastUsed?: string;
    expiresAt?: string;
  }>;
}