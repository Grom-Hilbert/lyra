/**
 * TypeScript 数据转换工具
 */

import {
  User, CreateUserRequest, UpdateUserRequest,
  FileEntity, FileInfo, FileUploadRequest,
  FolderEntity, FolderInfo, CreateFolderRequest,
  Template, TemplateInfo, CreateTemplateRequest,
  UserSettings, UpdateUserSettingsRequest
} from '../types';

/**
 * 基础映射器接口
 */
export interface Mapper<T, U> {
  toDTO(entity: T): U;
  toEntity(dto: U): T;
}

/**
 * 用户数据映射器
 */
export class UserMapper {
  /**
   * 将API响应转换为用户对象
   */
  static fromApiResponse(data: any): User {
    return {
      id: data.id,
      username: data.username,
      email: data.email,
      displayName: data.displayName,
      status: data.status,
      authProvider: data.authProvider,
      externalId: data.externalId,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      lastLoginAt: data.lastLoginAt,
      roles: data.roles || []
    };
  }

  /**
   * 将用户创建请求转换为API格式
   */
  static toCreateRequest(request: CreateUserRequest): any {
    return {
      username: request.username,
      email: request.email,
      displayName: request.displayName,
      password: request.password,
      authProvider: request.authProvider || 'LOCAL',
      externalId: request.externalId,
      roleIds: request.roleIds || []
    };
  }

  /**
   * 将用户更新请求转换为API格式
   */
  static toUpdateRequest(request: UpdateUserRequest): any {
    const result: any = {};
    
    if (request.displayName !== undefined) {
      result.displayName = request.displayName;
    }
    if (request.status !== undefined) {
      result.status = request.status;
    }
    if (request.roleIds !== undefined) {
      result.roleIds = request.roleIds;
    }
    
    return result;
  }
}

/**
 * 文件数据映射器
 */
export class FileMapper {
  /**
   * 将API响应转换为文件对象
   */
  static fromApiResponse(data: any): FileEntity {
    return {
      id: data.id,
      name: data.name,
      path: data.path,
      mimeType: data.mimeType,
      size: data.size,
      checksum: data.checksum,
      spaceType: data.spaceType,
      versionControlType: data.versionControlType,
      folderId: data.folderId,
      owner: data.owner ? UserMapper.fromApiResponse(data.owner) : {} as User,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      accessedAt: data.accessedAt,
      versions: data.versions || [],
      permissions: data.permissions || []
    };
  }

  /**
   * 将API响应转换为文件信息对象
   */
  static toFileInfo(data: any): FileInfo {
    return {
      id: data.id,
      name: data.name,
      path: data.path,
      type: data.type || 'FILE',
      mimeType: data.mimeType,
      size: data.size,
      spaceType: data.spaceType,
      versionControlType: data.versionControlType,
      owner: data.owner,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      accessedAt: data.accessedAt,
      permissions: data.permissions || [],
      isShared: data.isShared || false,
      hasVersions: data.hasVersions || false
    };
  }

  /**
   * 将文件上传请求转换为FormData
   */
  static toUploadFormData(request: FileUploadRequest): FormData {
    const formData = new FormData();
    
    formData.append('file', request.file);
    formData.append('path', request.path);
    formData.append('spaceType', request.spaceType);
    
    if (request.versionControlType) {
      formData.append('versionControlType', request.versionControlType);
    }
    if (request.description) {
      formData.append('description', request.description);
    }
    if (request.isVersionUpdate !== undefined) {
      formData.append('isVersionUpdate', request.isVersionUpdate.toString());
    }
    
    return formData;
  }
}

/**
 * 文件夹数据映射器
 */
export class FolderMapper {
  /**
   * 将API响应转换为文件夹对象
   */
  static fromApiResponse(data: any): FolderEntity {
    return {
      id: data.id,
      name: data.name,
      path: data.path,
      description: data.description,
      spaceType: data.spaceType,
      parentId: data.parentId,
      parent: data.parent ? FolderMapper.fromApiResponse(data.parent) : undefined,
      children: data.children ? data.children.map(FolderMapper.fromApiResponse) : [],
      files: data.files ? data.files.map(FileMapper.fromApiResponse) : [],
      owner: data.owner ? UserMapper.fromApiResponse(data.owner) : {} as User,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      permissions: data.permissions || []
    };
  }

  /**
   * 将API响应转换为文件夹信息对象
   */
  static toFolderInfo(data: any): FolderInfo {
    return {
      id: data.id,
      name: data.name,
      path: data.path,
      description: data.description,
      spaceType: data.spaceType,
      parentId: data.parentId,
      owner: data.owner,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      permissions: data.permissions || [],
      childrenCount: data.childrenCount || 0,
      filesCount: data.filesCount || 0,
      totalSize: data.totalSize || 0,
      isShared: data.isShared || false
    };
  }

  /**
   * 将文件夹创建请求转换为API格式
   */
  static toCreateRequest(request: CreateFolderRequest): any {
    return {
      name: request.name,
      parentId: request.parentId,
      parentPath: request.parentPath,
      description: request.description,
      spaceType: request.spaceType,
      templateId: request.templateId,
      versionControlType: request.versionControlType
    };
  }
}

/**
 * 模板数据映射器
 */
export class TemplateMapper {
  /**
   * 将API响应转换为模板对象
   */
  static fromApiResponse(data: any): Template {
    return {
      id: data.id,
      name: data.name,
      description: data.description,
      templateData: data.templateData,
      templateType: data.templateType,
      isPublic: data.isPublic,
      createdBy: data.createdBy ? UserMapper.fromApiResponse(data.createdBy) : {} as User,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      templateFiles: data.templateFiles || []
    };
  }

  /**
   * 将API响应转换为模板信息对象
   */
  static toTemplateInfo(data: any): TemplateInfo {
    return {
      id: data.id,
      name: data.name,
      description: data.description,
      templateType: data.templateType,
      isPublic: data.isPublic,
      createdBy: data.createdBy,
      createdAt: data.createdAt,
      updatedAt: data.updatedAt,
      filesCount: data.filesCount || 0,
      usageCount: data.usageCount || 0
    };
  }

  /**
   * 将模板创建请求转换为API格式
   */
  static toCreateRequest(request: CreateTemplateRequest): any {
    return {
      name: request.name,
      description: request.description,
      templateType: request.templateType,
      isPublic: request.isPublic || false,
      templateFiles: request.templateFiles.map(file => ({
        name: file.name,
        relativePath: file.relativePath,
        fileType: file.fileType,
        content: file.content,
        sourcePath: file.sourcePath,
        permissionsConfig: file.permissionsConfig
      }))
    };
  }
}

/**
 * 用户设置数据映射器
 */
export class UserSettingsMapper {
  /**
   * 将API响应转换为用户设置对象
   */
  static fromApiResponse(data: any): UserSettings {
    return {
      id: data.id,
      userId: data.userId,
      user: data.user ? UserMapper.fromApiResponse(data.user) : {} as User,
      theme: data.theme,
      language: data.language,
      timezone: data.timezone,
      dateFormat: data.dateFormat,
      timeFormat: data.timeFormat,
      fileListView: data.fileListView,
      showHiddenFiles: data.showHiddenFiles,
      autoSave: data.autoSave,
      notificationEnabled: data.notificationEnabled,
      customSettings: data.customSettings ? JSON.parse(data.customSettings) : {},
      createdAt: data.createdAt,
      updatedAt: data.updatedAt
    };
  }

  /**
   * 将用户设置更新请求转换为API格式
   */
  static toUpdateRequest(request: UpdateUserSettingsRequest): any {
    const result: any = {};
    
    Object.keys(request).forEach(key => {
      const value = (request as any)[key];
      if (value !== undefined) {
        if (key === 'customSettings') {
          result[key] = JSON.stringify(value);
        } else {
          result[key] = value;
        }
      }
    });
    
    return result;
  }
}

/**
 * 通用数据转换工具
 */
export class DataTransformer {
  /**
   * 将日期字符串转换为本地化格式
   */
  static formatDate(dateString: string, format: string = 'yyyy-MM-dd HH:mm:ss'): string {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    
    // 简单的日期格式化实现
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return format
      .replace('yyyy', year.toString())
      .replace('MM', month)
      .replace('dd', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds);
  }

  /**
   * 格式化文件大小
   */
  static formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const k = 1024;
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${units[i]}`;
  }

  /**
   * 深拷贝对象
   */
  static deepClone<T>(obj: T): T {
    if (obj === null || typeof obj !== 'object') {
      return obj;
    }
    
    if (obj instanceof Date) {
      return new Date(obj.getTime()) as unknown as T;
    }
    
    if (obj instanceof Array) {
      return obj.map(item => DataTransformer.deepClone(item)) as unknown as T;
    }
    
    const cloned = {} as T;
    Object.keys(obj).forEach(key => {
      (cloned as any)[key] = DataTransformer.deepClone((obj as any)[key]);
    });
    
    return cloned;
  }

  /**
   * 移除对象中的空值
   */
  static removeEmptyValues(obj: Record<string, any>): Record<string, any> {
    const result: Record<string, any> = {};
    
    Object.keys(obj).forEach(key => {
      const value = obj[key];
      if (value !== null && value !== undefined && value !== '') {
        if (typeof value === 'object' && !Array.isArray(value)) {
          const cleaned = DataTransformer.removeEmptyValues(value);
          if (Object.keys(cleaned).length > 0) {
            result[key] = cleaned;
          }
        } else {
          result[key] = value;
        }
      }
    });
    
    return result;
  }
}