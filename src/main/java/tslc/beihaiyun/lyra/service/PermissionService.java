package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限服务接口
 * 定义权限管理的核心业务逻辑
 */
public interface PermissionService {

    /**
     * 为用户授予文件权限
     */
    FilePermission grantFilePermission(Long fileId, Long userId, 
                                     FilePermission.PermissionType permissionType, 
                                     LocalDateTime expiresAt, User grantedBy);

    /**
     * 为角色授予文件权限
     */
    FilePermission grantFilePermissionToRole(Long fileId, Long roleId, 
                                           FilePermission.PermissionType permissionType, 
                                           LocalDateTime expiresAt, User grantedBy);

    /**
     * 撤销用户的文件权限
     */
    void revokeFilePermission(Long fileId, Long userId, FilePermission.PermissionType permissionType);

    /**
     * 撤销角色的文件权限
     */
    void revokeFilePermissionFromRole(Long fileId, Long roleId, FilePermission.PermissionType permissionType);

    /**
     * 为用户授予文件夹权限
     */
    FolderPermission grantFolderPermission(Long folderId, Long userId, 
                                         FilePermission.PermissionType permissionType, 
                                         LocalDateTime expiresAt, User grantedBy);

    /**
     * 为角色授予文件夹权限
     */
    FolderPermission grantFolderPermissionToRole(Long folderId, Long roleId, 
                                               FilePermission.PermissionType permissionType, 
                                               LocalDateTime expiresAt, User grantedBy);

    /**
     * 撤销用户的文件夹权限
     */
    void revokeFolderPermission(Long folderId, Long userId, FilePermission.PermissionType permissionType);

    /**
     * 撤销角色的文件夹权限
     */
    void revokeFolderPermissionFromRole(Long folderId, Long roleId, FilePermission.PermissionType permissionType);

    /**
     * 检查用户对文件的权限
     */
    boolean hasFilePermission(Long fileId, User user, FilePermission.PermissionType permissionType);

    /**
     * 检查用户对文件夹的权限
     */
    boolean hasFolderPermission(Long folderId, User user, FilePermission.PermissionType permissionType);

    /**
     * 获取文件的所有权限
     */
    List<FilePermission> getFilePermissions(Long fileId);

    /**
     * 获取文件夹的所有权限
     */
    List<FolderPermission> getFolderPermissions(Long folderId);

    /**
     * 获取用户的文件权限列表
     */
    List<FilePermission> getUserFilePermissions(Long userId);

    /**
     * 获取用户的文件夹权限列表
     */
    List<FolderPermission> getUserFolderPermissions(Long userId);

    /**
     * 继承父文件夹权限到子文件夹
     */
    void inheritFolderPermissions(Long parentFolderId, Long childFolderId);

    /**
     * 继承文件夹权限到文件
     */
    void inheritFolderPermissionsToFile(Long folderId, Long fileId);

    /**
     * 清理过期权限
     */
    void cleanupExpiredPermissions();

    /**
     * 复制权限（从源文件/文件夹到目标文件/文件夹）
     */
    void copyFilePermissions(Long sourceFileId, Long targetFileId);

    /**
     * 复制文件夹权限
     */
    void copyFolderPermissions(Long sourceFolderId, Long targetFolderId);

    /**
     * 获取用户有权限访问的文件列表
     */
    List<FileEntity> getAccessibleFiles(User user, FilePermission.PermissionType permissionType);

    /**
     * 获取用户有权限访问的文件夹列表
     */
    List<FolderEntity> getAccessibleFolders(User user, FilePermission.PermissionType permissionType);
}