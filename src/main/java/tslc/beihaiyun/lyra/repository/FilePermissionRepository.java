package tslc.beihaiyun.lyra.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FilePermission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件权限数据访问接口
 * 提供文件权限相关的数据库操作
 */
@Repository
public interface FilePermissionRepository extends BaseRepository<FilePermission, Long>, CustomRepository<FilePermission, Long> {

    /**
     * 根据文件查找权限列表
     */
    List<FilePermission> findByFile(FileEntity file);

    /**
     * 根据用户查找权限列表
     */
    List<FilePermission> findByUser(User user);

    /**
     * 根据角色查找权限列表
     */
    List<FilePermission> findByRole(Role role);

    /**
     * 查找用户对特定文件的权限
     */
    List<FilePermission> findByFileAndUser(FileEntity file, User user);

    /**
     * 查找角色对特定文件的权限
     */
    List<FilePermission> findByFileAndRole(FileEntity file, Role role);

    /**
     * 查找特定权限类型的权限记录
     */
    List<FilePermission> findByPermissionType(FilePermission.PermissionType permissionType);

    /**
     * 查找用户对文件的特定权限
     */
    Optional<FilePermission> findByFileAndUserAndPermissionType(
        FileEntity file, User user, FilePermission.PermissionType permissionType);

    /**
     * 查找角色对文件的特定权限
     */
    Optional<FilePermission> findByFileAndRoleAndPermissionType(
        FileEntity file, Role role, FilePermission.PermissionType permissionType);

    /**
     * 查找已过期的权限
     */
    @Query("SELECT fp FROM FilePermission fp WHERE fp.expiresAt IS NOT NULL AND fp.expiresAt < :now")
    List<FilePermission> findExpiredPermissions(@Param("now") LocalDateTime now);

    /**
     * 检查用户是否对文件有特定权限
     */
    @Query("SELECT COUNT(fp) > 0 FROM FilePermission fp WHERE fp.file = :file AND " +
           "(fp.user = :user OR fp.role IN :roles) AND fp.permissionType = :permissionType AND " +
           "(fp.expiresAt IS NULL OR fp.expiresAt > :now)")
    boolean hasPermission(@Param("file") FileEntity file, @Param("user") User user, 
                         @Param("roles") List<Role> roles, @Param("permissionType") FilePermission.PermissionType permissionType,
                         @Param("now") LocalDateTime now);

    /**
     * 删除文件的所有权限
     */
    void deleteByFile(FileEntity file);

    /**
     * 删除用户的所有权限
     */
    void deleteByUser(User user);
}