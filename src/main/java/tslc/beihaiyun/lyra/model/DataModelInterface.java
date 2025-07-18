package tslc.beihaiyun.lyra.model;

import tslc.beihaiyun.lyra.entity.*;
import tslc.beihaiyun.lyra.validation.DataIntegrityValidator;

import java.util.List;

/**
 * 数据模型接口
 * 定义核心数据模型的操作契约
 */
public interface DataModelInterface {

    /**
     * 用户数据模型接口
     */
    interface UserModel {
        /**
         * 验证用户数据完整性
         */
        DataIntegrityValidator.ValidationResult validateUser(User user, boolean isCreate);

        /**
         * 检查用户名是否唯一
         */
        boolean isUsernameUnique(String username, Long excludeUserId);

        /**
         * 检查邮箱是否唯一
         */
        boolean isEmailUnique(String email, Long excludeUserId);

        /**
         * 获取用户的有效权限列表
         */
        List<String> getUserEffectivePermissions(Long userId);

        /**
         * 检查用户是否有指定权限
         */
        boolean hasPermission(Long userId, String permission);
    }

    /**
     * 文件数据模型接口
     */
    interface FileModel {
        /**
         * 验证文件数据完整性
         */
        DataIntegrityValidator.ValidationResult validateFile(FileEntity file, boolean isCreate);

        /**
         * 检查文件路径是否唯一
         */
        boolean isFilePathUnique(String path, Long excludeFileId);

        /**
         * 计算文件校验和
         */
        String calculateChecksum(byte[] fileContent);

        /**
         * 检查文件是否可以被用户访问
         */
        boolean canUserAccessFile(Long userId, Long fileId, String action);

        /**
         * 获取文件的版本历史
         */
        List<FileVersion> getFileVersionHistory(Long fileId);
    }

    /**
     * 文件夹数据模型接口
     */
    interface FolderModel {
        /**
         * 验证文件夹数据完整性
         */
        DataIntegrityValidator.ValidationResult validateFolder(FolderEntity folder, boolean isCreate);

        /**
         * 检查文件夹路径是否唯一
         */
        boolean isFolderPathUnique(String path, Long excludeFolderId);

        /**
         * 检查是否存在循环引用
         */
        boolean hasCircularReference(Long folderId, Long parentId);

        /**
         * 获取文件夹的完整路径
         */
        String getFolderFullPath(Long folderId);

        /**
         * 获取文件夹的所有子文件夹
         */
        List<FolderEntity> getAllSubFolders(Long folderId);
    }

    /**
     * 权限数据模型接口
     */
    interface PermissionModel {
        /**
         * 验证权限数据完整性
         */
        DataIntegrityValidator.ValidationResult validatePermission(FilePermission permission);

        /**
         * 检查权限是否已存在
         */
        boolean permissionExists(Long resourceId, String resourceType, Long userId, Long roleId, String permissionType);

        /**
         * 获取资源的有效权限
         */
        List<FilePermission> getEffectivePermissions(Long resourceId, String resourceType);

        /**
         * 检查权限是否已过期
         */
        boolean isPermissionExpired(FilePermission permission);

        /**
         * 继承父级权限
         */
        void inheritParentPermissions(Long childResourceId, Long parentResourceId, String resourceType);
    }

    /**
     * 角色数据模型接口
     */
    interface RoleModel {
        /**
         * 验证角色数据完整性
         */
        DataIntegrityValidator.ValidationResult validateRole(Role role, boolean isCreate);

        /**
         * 检查角色名称是否唯一
         */
        boolean isRoleNameUnique(String name, Long excludeRoleId);

        /**
         * 获取角色的所有权限
         */
        List<Permission> getRolePermissions(Long roleId);

        /**
         * 检查角色层次结构
         */
        boolean isValidRoleHierarchy(Role.RoleType parentType, Role.RoleType childType);
    }

    /**
     * 模板数据模型接口
     */
    interface TemplateModel {
        /**
         * 验证模板数据完整性
         */
        DataIntegrityValidator.ValidationResult validateTemplate(Template template, boolean isCreate);

        /**
         * 检查模板名称是否唯一
         */
        boolean isTemplateNameUnique(String name, Long excludeTemplateId);

        /**
         * 验证模板结构
         */
        boolean isValidTemplateStructure(String templateData);

        /**
         * 应用模板到指定路径
         */
        void applyTemplate(Long templateId, String targetPath, Long userId);

        /**
         * 获取模板的使用统计
         */
        int getTemplateUsageCount(Long templateId);
    }

    /**
     * 版本控制数据模型接口
     */
    interface VersionControlModel {
        /**
         * 验证版本提交数据完整性
         */
        DataIntegrityValidator.ValidationResult validateVersionCommit(VersionCommit commit);

        /**
         * 检查提交哈希是否唯一
         */
        boolean isCommitHashUnique(String commitHash, String repositoryPath);

        /**
         * 获取文件的版本差异
         */
        String getVersionDiff(Long fileId, Integer fromVersion, Integer toVersion);

        /**
         * 回滚到指定版本
         */
        void revertToVersion(Long fileId, Integer versionNumber, Long userId);

        /**
         * 合并版本冲突
         */
        void mergeVersionConflict(Long fileId, String mergedContent, Long userId);
    }

    /**
     * 审计日志数据模型接口
     */
    interface AuditLogModel {
        /**
         * 记录操作日志
         */
        void logAction(String action, Long userId, String resourcePath, String resourceType, 
                      boolean success, String details);

        /**
         * 获取用户操作历史
         */
        List<AuditLog> getUserActionHistory(Long userId, int limit);

        /**
         * 获取资源访问历史
         */
        List<AuditLog> getResourceAccessHistory(String resourcePath, String resourceType, int limit);

        /**
         * 清理过期日志
         */
        void cleanupExpiredLogs(int retentionDays);
    }

    /**
     * 用户设置数据模型接口
     */
    interface UserSettingsModel {
        /**
         * 验证用户设置数据完整性
         */
        DataIntegrityValidator.ValidationResult validateUserSettings(UserSettings settings);

        /**
         * 获取用户设置（如果不存在则创建默认设置）
         */
        UserSettings getUserSettingsOrDefault(Long userId);

        /**
         * 合并用户设置
         */
        UserSettings mergeUserSettings(UserSettings existing, UserSettings updates);

        /**
         * 重置用户设置为默认值
         */
        void resetUserSettingsToDefault(Long userId);
    }
}