import { BaseEntity, PermissionType } from './common';
import { User } from './user';
import { Role } from './role';

/**
 * 权限接口
 */
export interface Permission extends BaseEntity {
  name: string;
  description?: string;
  resource: string;
  action: string;
}

/**
 * 文件权限接口
 */
export interface FilePermission extends BaseEntity {
  fileId: number;
  user?: User;
  role?: Role;
  permissionType: PermissionType;
  grantedAt: string;
  expiresAt?: string;
  grantedBy: User;
}

/**
 * 文件夹权限接口
 */
export interface FolderPermission extends BaseEntity {
  folderId: number;
  user?: User;
  role?: Role;
  permissionType: PermissionType;
  isInherited: boolean;
  grantedAt: string;
  expiresAt?: string;
  grantedBy: User;
}

/**
 * 权限授予请求
 */
export interface GrantPermissionRequest {
  resourceId: number;
  resourceType: 'FILE' | 'FOLDER';
  userId?: number;
  roleId?: number;
  permissionType: PermissionType;
  expiresAt?: string;
}

/**
 * 权限撤销请求
 */
export interface RevokePermissionRequest {
  resourceId: number;
  resourceType: 'FILE' | 'FOLDER';
  userId?: number;
  roleId?: number;
  permissionType: PermissionType;
}

/**
 * 权限检查请求
 */
export interface CheckPermissionRequest {
  resourceId: number;
  resourceType: 'FILE' | 'FOLDER';
  action: string;
}

/**
 * 权限检查响应
 */
export interface CheckPermissionResponse {
  hasPermission: boolean;
  reason?: string;
}

/**
 * 资源权限摘要
 */
export interface ResourcePermissionSummary {
  resourceId: number;
  resourceType: 'FILE' | 'FOLDER';
  resourceName: string;
  resourcePath: string;
  userPermissions: FilePermission[] | FolderPermission[];
  rolePermissions: FilePermission[] | FolderPermission[];
  effectivePermissions: PermissionType[];
}

/**
 * 权限继承配置
 */
export interface PermissionInheritanceConfig {
  enabled: boolean;
  inheritFromParent: boolean;
  overrideChildPermissions: boolean;
}