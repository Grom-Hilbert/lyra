package tslc.beihaiyun.lyra.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FilePermission;
import tslc.beihaiyun.lyra.entity.FolderEntity;
import tslc.beihaiyun.lyra.entity.FolderPermission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件夹权限数据访问接口
 * 提供文件夹权限相关的数据库操作
 */
@Repository
public interface FolderPermissionRepository extends BaseRepository<FolderPermission, Long>, CustomRepository<FolderPermission, Long> {

    /**
     * 根据文件夹查找权限列表
     */
    List<FolderPermission> findByFolder(FolderEntity folder);

    /**
     * 根据用户查找权限列表
     */
    List<FolderPermission> findByUser(User user);

    /**
     * 根据角色查找权限列表
     */
    List<FolderPermission> findByRole(Role role);

    /**
     * 查找用户对特定文件夹的权限
     */
    List<FolderPermission> findByFolderAndUser(FolderEntity folder, User user);

    /**
     * 查找角色对特定文件夹的权限
     */
    List<FolderPermission> findByFolderAndRole(FolderEntity folder, Role role);

    /**
     * 查找继承的权限
     */
    List<FolderPermission> findByIsInheritedTrue();

    /**
     * 查找非继承的权限
     */
    List<FolderPermission> findByIsInheritedFalse();

    /**
     * 查找用户对文件夹的特定权限
     */
    Optional<FolderPermission> findByFolderAndUserAndPermissionType(
        FolderEntity folder, User user, FilePermission.PermissionType permissionType);

    /**
     * 查找角色对文件夹的特定权限
     */
    Optional<FolderPermission> findByFolderAndRoleAndPermissionType(
        FolderEntity folder, Role role, FilePermission.PermissionType permissionType);

    /**
     * 查找已过期的权限
     */
    @Query("SELECT fp FROM FolderPermission fp WHERE fp.expiresAt IS NOT NULL AND fp.expiresAt < :now")
    List<FolderPermission> findExpiredPermissions(@Param("now") LocalDateTime now);

    /**
     * 检查用户是否对文件夹有特定权限（包括继承权限）
     */
    @Query("SELECT COUNT(fp) > 0 FROM FolderPermission fp WHERE fp.folder = :folder AND " +
           "(fp.user = :user OR fp.role IN :roles) AND fp.permissionType = :permissionType AND " +
           "(fp.expiresAt IS NULL OR fp.expiresAt > :now)")
    boolean hasPermission(@Param("folder") FolderEntity folder, @Param("user") User user, 
                         @Param("roles") List<Role> roles, @Param("permissionType") FilePermission.PermissionType permissionType,
                         @Param("now") LocalDateTime now);

    /**
     * 删除文件夹的所有权限
     */
    void deleteByFolder(FolderEntity folder);

    /**
     * 删除用户的所有权限
     */
    void deleteByUser(User user);

    /**
     * 查找文件夹的继承权限
     */
    @Query("SELECT fp FROM FolderPermission fp WHERE fp.folder = :folder AND fp.isInherited = true")
    List<FolderPermission> findInheritedPermissions(@Param("folder") FolderEntity folder);
}