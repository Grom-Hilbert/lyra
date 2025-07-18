import { BaseEntity } from './common';
import { User } from './user';

/**
 * 资源类型枚举
 */
export enum ResourceType {
  FILE = 'FILE',
  FOLDER = 'FOLDER',
  USER = 'USER',
  ROLE = 'ROLE',
  PERMISSION = 'PERMISSION',
  TEMPLATE = 'TEMPLATE',
  SYSTEM = 'SYSTEM'
}

/**
 * 操作结果枚举
 */
export enum ActionResult {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  PARTIAL = 'PARTIAL'
}

/**
 * 审计日志接口
 */
export interface AuditLog extends BaseEntity {
  user?: User;
  action: string;
  resourcePath?: string;
  resourceType?: ResourceType;
  ipAddress?: string;
  userAgent?: string;
  result: ActionResult;
  errorMessage?: string;
  requestData?: string;
  responseData?: string;
  executionTime?: number;
}

/**
 * 审计日志查询请求
 */
export interface AuditLogSearchRequest {
  userId?: number;
  action?: string;
  resourceType?: ResourceType;
  resourcePath?: string;
  result?: ActionResult;
  ipAddress?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  sortBy?: 'CREATED_AT' | 'ACTION' | 'USER' | 'RESULT';
  sortDirection?: 'ASC' | 'DESC';
}

/**
 * 审计日志统计请求
 */
export interface AuditLogStatisticsRequest {
  dateFrom?: string;
  dateTo?: string;
  groupBy: 'ACTION' | 'USER' | 'RESOURCE_TYPE' | 'RESULT' | 'HOUR' | 'DAY' | 'MONTH';
  userId?: number;
  resourceType?: ResourceType;
}

/**
 * 审计日志统计响应
 */
export interface AuditLogStatisticsResponse {
  totalLogs: number;
  successCount: number;
  failureCount: number;
  partialCount: number;
  topActions: Array<{
    action: string;
    count: number;
    percentage: number;
  }>;
  topUsers: Array<{
    userId: number;
    username: string;
    count: number;
    percentage: number;
  }>;
  resourceTypeDistribution: Array<{
    resourceType: ResourceType;
    count: number;
    percentage: number;
  }>;
  timeSeriesData: Array<{
    timestamp: string;
    count: number;
    successCount: number;
    failureCount: number;
  }>;
}

/**
 * 用户活动摘要
 */
export interface UserActivitySummary {
  userId: number;
  username: string;
  totalActions: number;
  successfulActions: number;
  failedActions: number;
  lastActivity: string;
  mostCommonActions: Array<{
    action: string;
    count: number;
  }>;
  resourceAccess: Array<{
    resourceType: ResourceType;
    count: number;
  }>;
  loginHistory: Array<{
    timestamp: string;
    ipAddress: string;
    userAgent: string;
    success: boolean;
  }>;
}

/**
 * 系统活动摘要
 */
export interface SystemActivitySummary {
  totalUsers: number;
  activeUsers: number;
  totalActions: number;
  successRate: number;
  averageResponseTime: number;
  peakHours: Array<{
    hour: number;
    count: number;
  }>;
  errorTrends: Array<{
    date: string;
    errorCount: number;
    errorRate: number;
  }>;
  resourceUsage: Array<{
    resourceType: ResourceType;
    totalAccess: number;
    uniqueUsers: number;
  }>;
}

/**
 * 安全事件
 */
export interface SecurityEvent {
  id: number;
  eventType: 'FAILED_LOGIN' | 'PERMISSION_DENIED' | 'SUSPICIOUS_ACTIVITY' | 'DATA_BREACH' | 'UNAUTHORIZED_ACCESS';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  userId?: number;
  username?: string;
  ipAddress: string;
  userAgent?: string;
  description: string;
  details: Record<string, any>;
  resolved: boolean;
  resolvedBy?: number;
  resolvedAt?: string;
  createdAt: string;
}

/**
 * 安全事件查询请求
 */
export interface SecurityEventSearchRequest {
  eventType?: string;
  severity?: string;
  userId?: number;
  ipAddress?: string;
  resolved?: boolean;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

/**
 * 合规报告请求
 */
export interface ComplianceReportRequest {
  reportType: 'ACCESS_LOG' | 'PERMISSION_CHANGES' | 'DATA_EXPORT' | 'USER_ACTIVITY' | 'SECURITY_EVENTS';
  dateFrom: string;
  dateTo: string;
  userId?: number;
  resourceType?: ResourceType;
  format: 'PDF' | 'EXCEL' | 'CSV' | 'JSON';
  includeDetails?: boolean;
}

/**
 * 合规报告响应
 */
export interface ComplianceReportResponse {
  reportId: string;
  reportType: string;
  dateRange: {
    from: string;
    to: string;
  };
  summary: {
    totalRecords: number;
    uniqueUsers: number;
    resourcesAccessed: number;
    securityEvents: number;
  };
  downloadUrl: string;
  expiresAt: string;
  createdAt: string;
}