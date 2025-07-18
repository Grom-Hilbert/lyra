import { BaseEntity } from './common';
import { Permission } from './permission';

/**
 * 角色类型枚举
 */
export enum RoleType {
  SUPER_ADMIN = 'SUPER_ADMIN',
  ADMIN = 'ADMIN',
  USER = 'USER',
  GUEST = 'GUEST'
}

/**
 * 角色接口
 */
export interface Role extends BaseEntity {
  name: string;
  description?: string;
  type: RoleType;
  permissions: Permission[];
}

/**
 * 角色创建请求
 */
export interface CreateRoleRequest {
  name: string;
  description?: string;
  type: RoleType;
  permissionIds?: number[];
}

/**
 * 角色更新请求
 */
export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  permissionIds?: number[];
}

/**
 * 角色权限分配请求
 */
export interface AssignRolePermissionsRequest {
  roleId: number;
  permissionIds: number[];
}

/**
 * 角色搜索条件
 */
export interface RoleSearchCriteria {
  keyword?: string;
  type?: RoleType;
}