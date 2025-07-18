import { BaseEntity } from './common';
import { Role } from './role';

/**
 * 用户状态枚举
 */
export enum UserStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

/**
 * 认证提供者枚举
 */
export enum AuthProvider {
  LOCAL = 'LOCAL',
  OAUTH2 = 'OAUTH2',
  OIDC = 'OIDC',
  LDAP = 'LDAP'
}

/**
 * 用户接口
 */
export interface User extends BaseEntity {
  username: string;
  email: string;
  displayName: string;
  status: UserStatus;
  authProvider: AuthProvider;
  externalId?: string;
  lastLoginAt?: string;
  roles: Role[];
}

/**
 * 用户创建请求
 */
export interface CreateUserRequest {
  username: string;
  email: string;
  displayName: string;
  password?: string;
  authProvider?: AuthProvider;
  externalId?: string;
  roleIds?: number[];
}

/**
 * 用户更新请求
 */
export interface UpdateUserRequest {
  displayName?: string;
  status?: UserStatus;
  roleIds?: number[];
}

/**
 * 用户登录请求
 */
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

/**
 * 用户登录响应
 */
export interface LoginResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * 刷新令牌请求
 */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/**
 * 用户信息响应
 */
export interface UserInfo {
  id: number;
  username: string;
  email: string;
  displayName: string;
  status: UserStatus;
  roles: string[];
  permissions: string[];
  lastLoginAt?: string;
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * 用户搜索条件
 */
export interface UserSearchCriteria {
  keyword?: string;
  status?: UserStatus;
  authProvider?: AuthProvider;
  roleId?: number;
}