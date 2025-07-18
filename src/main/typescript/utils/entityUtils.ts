/**
 * 实体工具类
 * 提供实体操作的通用方法
 */

import {
  User, FileEntity, FolderEntity, Template,
  SpaceType, VersionControlType, UserStatus, AuthProvider
} from '../types';

/**
 * 实体工厂类
 */
export class EntityFactory {
  /**
   * 创建默认用户对象
   */
  static createDefaultUser(): Partial<User> {
    return {
      status: UserStatus.PENDING,
      authProvider: AuthProvider.LOCAL,
      roles: []
    };
  }

  /**
   * 创建默认文件对象
   */
  static createDefaultFile(): Partial<FileEntity> {
    return {
      spaceType: SpaceType.PERSONAL,
      versionControlType: VersionControlType.NONE,
      versions: [],
      permissions: []
    };
  }

  /**
   * 创建默认文件夹对象
   */
  static createDefaultFolder(): Partial<FolderEntity> {
    return {
      spaceType: SpaceType.PERSONAL,
      children: [],
      files: [],
      permissions: []
    };
  }

  /**
   * 创建默认模板对象
   */
  static createDefaultTemplate(): Partial<Template> {
    return {
      isPublic: false,
      templateFiles: []
    };
  }
}

/**
 * 实体比较工具
 */
export class EntityComparator {
  /**
   * 比较两个用户是否相同
   */
  static compareUsers(user1: User, user2: User): boolean {
    return user1.id === user2.id &&
           user1.username === user2.username &&
           user1.email === user2.email;
  }

  /**
   * 比较两个文件是否相同
   */
  static compareFiles(file1: FileEntity, file2: FileEntity): boolean {
    return file1.id === file2.id &&
           file1.path === file2.path &&
           file1.checksum === file2.checksum;
  }

  /**
   * 比较两个文件夹是否相同
   */
  static compareFolders(folder1: FolderEntity, folder2: FolderEntity): boolean {
    return folder1.id === folder2.id &&
           folder1.path === folder2.path;
  }
}

/**
 * 实体状态检查工具
 */
export class EntityStatusChecker {
  /**
   * 检查用户是否激活
   */
  static isUserActive(user: User): boolean {
    return user.status === UserStatus.ACTIVE;
  }

  /**
   * 检查用户是否被暂停
   */
  static isUserSuspended(user: User): boolean {
    return user.status === UserStatus.SUSPENDED;
  }

  /**
   * 检查用户是否待审批
   */
  static isUserPending(user: User): boolean {
    return user.status === UserStatus.PENDING;
  }

  /**
   * 检查文件是否有版本控制
   */
  static hasVersionControl(file: FileEntity): boolean {
    return file.versionControlType !== VersionControlType.NONE;
  }

  /**
   * 检查文件是否有高级版本控制
   */
  static hasAdvancedVersionControl(file: FileEntity): boolean {
    return file.versionControlType === VersionControlType.ADVANCED;
  }

  /**
   * 检查文件是否被分享
   */
  static isFileShared(file: FileEntity): boolean {
    return file.permissions && file.permissions.length > 0;
  }

  /**
   * 检查文件夹是否为企业空间
   */
  static isEnterpriseSpace(folder: FolderEntity): boolean {
    return folder.spaceType === SpaceType.ENTERPRISE;
  }

  /**
   * 检查模板是否公开
   */
  static isTemplatePublic(template: Template): boolean {
    return template.isPublic;
  }
}

/**
 * 实体路径工具
 */
export class EntityPathUtils {
  /**
   * 获取文件的目录路径
   */
  static getDirectoryPath(filePath: string): string {
    const lastSlashIndex = filePath.lastIndexOf('/');
    return lastSlashIndex > 0 ? filePath.substring(0, lastSlashIndex) : '/';
  }

  /**
   * 获取文件名（不含路径）
   */
  static getFileName(filePath: string): string {
    const lastSlashIndex = filePath.lastIndexOf('/');
    return lastSlashIndex >= 0 ? filePath.substring(lastSlashIndex + 1) : filePath;
  }

  /**
   * 获取文件扩展名
   */
  static getFileExtension(fileName: string): string {
    const lastDotIndex = fileName.lastIndexOf('.');
    return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : '';
  }

  /**
   * 组合路径
   */
  static joinPaths(...paths: string[]): string {
    return paths
      .filter(path => path && path.trim() !== '')
      .map(path => path.replace(/^\/+|\/+$/g, ''))
      .join('/')
      .replace(/\/+/g, '/');
  }

  /**
   * 规范化路径
   */
  static normalizePath(path: string): string {
    if (!path) return '/';
    
    // 移除多余的斜杠
    let normalized = path.replace(/\/+/g, '/');
    
    // 确保以斜杠开头
    if (!normalized.startsWith('/')) {
      normalized = '/' + normalized;
    }
    
    // 移除末尾的斜杠（除非是根路径）
    if (normalized.length > 1 && normalized.endsWith('/')) {
      normalized = normalized.slice(0, -1);
    }
    
    return normalized;
  }

  /**
   * 检查路径是否为子路径
   */
  static isSubPath(parentPath: string, childPath: string): boolean {
    const normalizedParent = EntityPathUtils.normalizePath(parentPath);
    const normalizedChild = EntityPathUtils.normalizePath(childPath);
    
    if (normalizedParent === normalizedChild) {
      return false;
    }
    
    return normalizedChild.startsWith(normalizedParent + '/');
  }

  /**
   * 获取相对路径
   */
  static getRelativePath(basePath: string, targetPath: string): string {
    const normalizedBase = EntityPathUtils.normalizePath(basePath);
    const normalizedTarget = EntityPathUtils.normalizePath(targetPath);
    
    if (!normalizedTarget.startsWith(normalizedBase + '/')) {
      return targetPath;
    }
    
    return normalizedTarget.substring(normalizedBase.length + 1);
  }
}

/**
 * 实体权限工具
 */
export class EntityPermissionUtils {
  /**
   * 检查用户是否有指定权限
   */
  static hasPermission(user: User, requiredPermission: string): boolean {
    if (!user.roles || user.roles.length === 0) {
      return false;
    }
    
    return user.roles.some(role => 
      role.permissions && role.permissions.some(permission => 
        permission.action === requiredPermission
      )
    );
  }

  /**
   * 检查用户是否为管理员
   */
  static isAdmin(user: User): boolean {
    if (!user.roles || user.roles.length === 0) {
      return false;
    }
    
    return user.roles.some(role => 
      role.type === 'ADMIN' || role.type === 'SUPER_ADMIN'
    );
  }

  /**
   * 检查用户是否为超级管理员
   */
  static isSuperAdmin(user: User): boolean {
    if (!user.roles || user.roles.length === 0) {
      return false;
    }
    
    return user.roles.some(role => role.type === 'SUPER_ADMIN');
  }

  /**
   * 获取用户的所有权限
   */
  static getUserPermissions(user: User): string[] {
    if (!user.roles || user.roles.length === 0) {
      return [];
    }
    
    const permissions = new Set<string>();
    
    user.roles.forEach(role => {
      if (role.permissions) {
        role.permissions.forEach(permission => {
          permissions.add(permission.action);
        });
      }
    });
    
    return Array.from(permissions);
  }
}

/**
 * 实体搜索工具
 */
export class EntitySearchUtils {
  /**
   * 搜索用户
   */
  static searchUsers(users: User[], keyword: string): User[] {
    if (!keyword || keyword.trim() === '') {
      return users;
    }
    
    const lowerKeyword = keyword.toLowerCase();
    
    return users.filter(user => 
      user.username.toLowerCase().includes(lowerKeyword) ||
      user.email.toLowerCase().includes(lowerKeyword) ||
      user.displayName.toLowerCase().includes(lowerKeyword)
    );
  }

  /**
   * 搜索文件
   */
  static searchFiles(files: FileEntity[], keyword: string): FileEntity[] {
    if (!keyword || keyword.trim() === '') {
      return files;
    }
    
    const lowerKeyword = keyword.toLowerCase();
    
    return files.filter(file => 
      file.name.toLowerCase().includes(lowerKeyword) ||
      file.path.toLowerCase().includes(lowerKeyword) ||
      (file.mimeType && file.mimeType.toLowerCase().includes(lowerKeyword))
    );
  }

  /**
   * 搜索文件夹
   */
  static searchFolders(folders: FolderEntity[], keyword: string): FolderEntity[] {
    if (!keyword || keyword.trim() === '') {
      return folders;
    }
    
    const lowerKeyword = keyword.toLowerCase();
    
    return folders.filter(folder => 
      folder.name.toLowerCase().includes(lowerKeyword) ||
      folder.path.toLowerCase().includes(lowerKeyword) ||
      (folder.description && folder.description.toLowerCase().includes(lowerKeyword))
    );
  }
}

/**
 * 实体排序工具
 */
export class EntitySortUtils {
  /**
   * 按名称排序用户
   */
  static sortUsersByName(users: User[], ascending: boolean = true): User[] {
    return [...users].sort((a, b) => {
      const comparison = a.displayName.localeCompare(b.displayName);
      return ascending ? comparison : -comparison;
    });
  }

  /**
   * 按创建时间排序用户
   */
  static sortUsersByCreatedAt(users: User[], ascending: boolean = true): User[] {
    return [...users].sort((a, b) => {
      const dateA = new Date(a.createdAt).getTime();
      const dateB = new Date(b.createdAt).getTime();
      const comparison = dateA - dateB;
      return ascending ? comparison : -comparison;
    });
  }

  /**
   * 按名称排序文件
   */
  static sortFilesByName(files: FileEntity[], ascending: boolean = true): FileEntity[] {
    return [...files].sort((a, b) => {
      const comparison = a.name.localeCompare(b.name);
      return ascending ? comparison : -comparison;
    });
  }

  /**
   * 按大小排序文件
   */
  static sortFilesBySize(files: FileEntity[], ascending: boolean = true): FileEntity[] {
    return [...files].sort((a, b) => {
      const comparison = a.size - b.size;
      return ascending ? comparison : -comparison;
    });
  }

  /**
   * 按修改时间排序文件
   */
  static sortFilesByUpdatedAt(files: FileEntity[], ascending: boolean = true): FileEntity[] {
    return [...files].sort((a, b) => {
      const dateA = new Date(a.updatedAt || a.createdAt).getTime();
      const dateB = new Date(b.updatedAt || b.createdAt).getTime();
      const comparison = dateA - dateB;
      return ascending ? comparison : -comparison;
    });
  }
}