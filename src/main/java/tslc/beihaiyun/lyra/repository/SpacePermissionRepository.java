package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.SpacePermission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 空间权限数据访问接口
 * 提供空间权限的CRUD操作和复杂查询方法
 * 支持权限继承和覆盖逻辑的数据库查询
 *
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Repository
public interface SpacePermissionRepository extends JpaRepository<SpacePermission, Long> {

    /**
     * 根据用户ID和空间ID查找权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @return 权限列表
     */
    List<SpacePermission> findByUserIdAndSpaceId(Long userId, Long spaceId);

    /**
     * 根据用户ID、空间ID和权限ID查找唯一权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param permissionId 权限ID
     * @return 权限对象
     */
    Optional<SpacePermission> findByUserIdAndSpaceIdAndPermissionId(Long userId, Long spaceId, Long permissionId);

    /**
     * 根据用户ID、空间ID、权限ID和资源查找权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param permissionId 权限ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 权限对象
     */
    Optional<SpacePermission> findByUserIdAndSpaceIdAndPermissionIdAndResourceTypeAndResourceId(
            Long userId, Long spaceId, Long permissionId, String resourceType, Long resourceId);

    /**
     * 根据用户ID查找所有空间权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<SpacePermission> findByUserId(Long userId);

    /**
     * 根据空间ID查找所有权限
     *
     * @param spaceId 空间ID
     * @return 权限列表
     */
    List<SpacePermission> findBySpaceId(Long spaceId);

    /**
     * 根据资源类型和资源ID查找权限
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 权限列表
     */
    List<SpacePermission> findByResourceTypeAndResourceId(String resourceType, Long resourceId);

    /**
     * 根据授权状态查找权限
     *
     * @param status 授权状态
     * @return 权限列表
     */
    List<SpacePermission> findByStatus(String status);

    /**
     * 根据授权类型查找权限
     *
     * @param grantType 授权类型
     * @return 权限列表
     */
    List<SpacePermission> findByGrantType(String grantType);

    /**
     * 查找已过期的权限
     *
     * @param currentTime 当前时间
     * @return 权限列表
     */
    List<SpacePermission> findByExpiresAtBefore(LocalDateTime currentTime);

    /**
     * 查找未过期的权限
     *
     * @param currentTime 当前时间
     * @return 权限列表
     */
    List<SpacePermission> findByExpiresAtAfterOrExpiresAtIsNull(LocalDateTime currentTime);

    /**
     * 根据授权者查找权限
     *
     * @param grantedBy 授权者ID
     * @return 权限列表
     */
    List<SpacePermission> findByGrantedBy(Long grantedBy);

    /**
     * 根据权限路径查找权限
     *
     * @param permissionPath 权限路径
     * @return 权限列表
     */
    List<SpacePermission> findByPermissionPath(String permissionPath);

    /**
     * 根据权限路径前缀查找权限（用于查找子路径权限）
     *
     * @param pathPrefix 路径前缀
     * @return 权限列表
     */
    List<SpacePermission> findByPermissionPathStartingWith(String pathPrefix);

    /**
     * 根据继承标识查找权限
     *
     * @param inheritFromParent 是否继承
     * @return 权限列表
     */
    List<SpacePermission> findByInheritFromParent(Boolean inheritFromParent);

    /**
     * 分页查询用户在指定空间的权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<SpacePermission> findByUserIdAndSpaceId(Long userId, Long spaceId, Pageable pageable);

    /**
     * 检查权限是否存在
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param permissionId 权限ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return true如果存在
     */
    boolean existsByUserIdAndSpaceIdAndPermissionIdAndResourceTypeAndResourceId(
            Long userId, Long spaceId, Long permissionId, String resourceType, Long resourceId);

    /**
     * 统计用户在空间的权限数量
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @return 权限数量
     */
    long countByUserIdAndSpaceId(Long userId, Long spaceId);

    /**
     * 统计空间的总权限数量
     *
     * @param spaceId 空间ID
     * @return 权限数量
     */
    long countBySpaceId(Long spaceId);

    /**
     * 统计指定状态的权限数量
     *
     * @param status 授权状态
     * @return 权限数量
     */
    long countByStatus(String status);

    /**
     * 查找用户的有效权限（未过期且为授权状态）
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param currentTime 当前时间
     * @return 权限列表
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.status = 'GRANTED' " +
           "AND (sp.expiresAt IS NULL OR sp.expiresAt > :currentTime)")
    List<SpacePermission> findEffectivePermissions(@Param("userId") Long userId,
                                                  @Param("spaceId") Long spaceId,
                                                  @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找用户在特定资源上的有效权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param currentTime 当前时间
     * @return 权限列表
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.resourceType = :resourceType " +
           "AND (sp.resourceId = :resourceId OR sp.resourceId IS NULL) " +
           "AND sp.status = 'GRANTED' " +
           "AND (sp.expiresAt IS NULL OR sp.expiresAt > :currentTime) " +
           "ORDER BY sp.permissionLevel DESC")
    List<SpacePermission> findEffectiveResourcePermissions(@Param("userId") Long userId,
                                                          @Param("spaceId") Long spaceId,
                                                          @Param("resourceType") String resourceType,
                                                          @Param("resourceId") Long resourceId,
                                                          @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找可继承的权限（用于权限继承计算）
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param parentPath 父路径
     * @param resourceType 资源类型
     * @return 权限列表
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.resourceType = :resourceType " +
           "AND sp.status IN ('GRANTED', 'INHERITED') " +
           "AND sp.inheritFromParent = true " +
           "AND (:parentPath IS NULL OR sp.permissionPath = :parentPath OR " +
           "     (:parentPath != '' AND (sp.permissionPath IS NULL OR " +
           "      :parentPath LIKE CONCAT(sp.permissionPath, '%')))) " +
           "ORDER BY sp.permissionLevel DESC")
    List<SpacePermission> findInheritablePermissions(@Param("userId") Long userId,
                                                    @Param("spaceId") Long spaceId,
                                                    @Param("parentPath") String parentPath,
                                                    @Param("resourceType") String resourceType);

    /**
     * 查找权限冲突（同一用户、空间、资源的相同权限类型）
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param permissionId 权限ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 权限列表
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.permissionId = :permissionId " +
           "AND sp.resourceType = :resourceType " +
           "AND (sp.resourceId = :resourceId OR (:resourceId IS NULL AND sp.resourceId IS NULL)) " +
           "ORDER BY sp.permissionLevel DESC, sp.grantedAt DESC")
    List<SpacePermission> findConflictingPermissions(@Param("userId") Long userId,
                                                    @Param("spaceId") Long spaceId,
                                                    @Param("permissionId") Long permissionId,
                                                    @Param("resourceType") String resourceType,
                                                    @Param("resourceId") Long resourceId);

    /**
     * 根据权限级别范围查找权限
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param minLevel 最小级别
     * @param maxLevel 最大级别
     * @return 权限列表
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.permissionLevel >= :minLevel " +
           "AND sp.permissionLevel <= :maxLevel " +
           "ORDER BY sp.permissionLevel DESC")
    List<SpacePermission> findByPermissionLevelRange(@Param("userId") Long userId,
                                                    @Param("spaceId") Long spaceId,
                                                    @Param("minLevel") Integer minLevel,
                                                    @Param("maxLevel") Integer maxLevel);

    /**
     * 获取用户权限统计
     *
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @return 统计结果
     */
    @Query("SELECT " +
           "COUNT(sp) as total, " +
           "SUM(CASE WHEN sp.status = 'GRANTED' THEN 1 ELSE 0 END) as granted, " +
           "SUM(CASE WHEN sp.status = 'DENIED' THEN 1 ELSE 0 END) as denied, " +
           "SUM(CASE WHEN sp.status = 'INHERITED' THEN 1 ELSE 0 END) as inherited, " +
           "SUM(CASE WHEN sp.grantType = 'DIRECT' THEN 1 ELSE 0 END) as direct, " +
           "SUM(CASE WHEN sp.grantType = 'ROLE_BASED' THEN 1 ELSE 0 END) as roleBased " +
           "FROM SpacePermission sp " +
           "WHERE sp.userId = :userId " +
           "AND sp.spaceId = :spaceId")
    Object[] getUserPermissionStatistics(@Param("userId") Long userId, @Param("spaceId") Long spaceId);

    /**
     * 批量删除过期权限
     *
     * @param currentTime 当前时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("UPDATE SpacePermission sp SET sp.isDeleted = true " +
           "WHERE sp.expiresAt IS NOT NULL " +
           "AND sp.expiresAt < :currentTime " +
           "AND sp.isDeleted = false")
    int markExpiredPermissionsAsDeleted(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 批量更新权限状态
     *
     * @param userIds 用户ID列表
     * @param spaceId 空间ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE SpacePermission sp SET sp.status = :newStatus " +
           "WHERE sp.userId IN :userIds " +
           "AND sp.spaceId = :spaceId " +
           "AND sp.status = :oldStatus")
    int batchUpdatePermissionStatus(@Param("userIds") Set<Long> userIds,
                                   @Param("spaceId") Long spaceId,
                                   @Param("oldStatus") String oldStatus,
                                   @Param("newStatus") String newStatus);

    /**
     * 删除指定资源的所有权限
     *
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("UPDATE SpacePermission sp SET sp.isDeleted = true " +
           "WHERE sp.resourceType = :resourceType " +
           "AND sp.resourceId = :resourceId " +
           "AND sp.isDeleted = false")
    int markResourcePermissionsAsDeleted(@Param("resourceType") String resourceType,
                                        @Param("resourceId") Long resourceId);

    /**
     * 复制权限（用于资源复制时的权限复制）
     *
     * @param sourceResourceType 源资源类型
     * @param sourceResourceId 源资源ID
     * @param targetResourceType 目标资源类型
     * @param targetResourceId 目标资源ID
     * @param newGrantedBy 新的授权者
     * @return 复制的记录数
     */
    @Modifying
    @Query(value = "INSERT INTO space_permissions " +
                   "(user_id, space_id, permission_id, resource_type, resource_id, " +
                   "status, grant_type, inherit_from_parent, permission_path, " +
                   "permission_level, granted_by, granted_at, expires_at, " +
                   "remark, conditions, created_at, updated_at, is_deleted) " +
                   "SELECT user_id, space_id, permission_id, :targetResourceType, :targetResourceId, " +
                   "status, grant_type, inherit_from_parent, " +
                   "REPLACE(permission_path, CONCAT('/', :sourceResourceId), CONCAT('/', :targetResourceId)), " +
                   "permission_level, :newGrantedBy, NOW(), expires_at, " +
                   "CONCAT('Copied from ', resource_type, ' ', :sourceResourceId), conditions, " +
                   "NOW(), NOW(), false " +
                   "FROM space_permissions " +
                   "WHERE resource_type = :sourceResourceType " +
                   "AND resource_id = :sourceResourceId " +
                   "AND is_deleted = false",
           nativeQuery = true)
    int copyPermissions(@Param("sourceResourceType") String sourceResourceType,
                       @Param("sourceResourceId") Long sourceResourceId,
                       @Param("targetResourceType") String targetResourceType,
                       @Param("targetResourceId") Long targetResourceId,
                       @Param("newGrantedBy") Long newGrantedBy);

    /**
     * 根据多个条件查询权限
     *
     * @param userId 用户ID（可选）
     * @param spaceId 空间ID（可选）
     * @param resourceType 资源类型（可选）
     * @param status 授权状态（可选）
     * @param grantType 授权类型（可选）
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    @Query("SELECT sp FROM SpacePermission sp " +
           "WHERE (:userId IS NULL OR sp.userId = :userId) " +
           "AND (:spaceId IS NULL OR sp.spaceId = :spaceId) " +
           "AND (:resourceType IS NULL OR sp.resourceType = :resourceType) " +
           "AND (:status IS NULL OR sp.status = :status) " +
           "AND (:grantType IS NULL OR sp.grantType = :grantType)")
    Page<SpacePermission> findByMultipleConditions(@Param("userId") Long userId,
                                                  @Param("spaceId") Long spaceId,
                                                  @Param("resourceType") String resourceType,
                                                  @Param("status") String status,
                                                  @Param("grantType") String grantType,
                                                  Pageable pageable);
} 