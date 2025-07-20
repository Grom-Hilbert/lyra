package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 权限数据访问接口
 * 提供权限的CRUD操作和复杂查询方法
 *
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限代码查找权限
     *
     * @param code 权限代码
     * @return 权限对象
     */
    Optional<Permission> findByCode(String code);

    /**
     * 根据权限代码查找权限（忽略大小写）
     *
     * @param code 权限代码
     * @return 权限对象
     */
    Optional<Permission> findByCodeIgnoreCase(String code);

    /**
     * 根据权限代码列表查找权限
     *
     * @param codes 权限代码列表
     * @return 权限列表
     */
    List<Permission> findByCodeIn(Set<String> codes);

    /**
     * 根据资源类型查找权限
     *
     * @param resourceType 资源类型
     * @return 权限列表
     */
    List<Permission> findByResourceType(String resourceType);

    /**
     * 根据权限类别查找权限
     *
     * @param category 权限类别
     * @return 权限列表
     */
    List<Permission> findByCategory(String category);

    /**
     * 根据资源类型和权限类别查找权限
     *
     * @param resourceType 资源类型
     * @param category 权限类别
     * @return 权限列表
     */
    List<Permission> findByResourceTypeAndCategory(String resourceType, String category);

    /**
     * 根据权限组查找权限
     *
     * @param permissionGroup 权限组
     * @return 权限列表
     */
    List<Permission> findByPermissionGroup(String permissionGroup);

    /**
     * 查找所有启用的权限
     *
     * @return 权限列表
     */
    List<Permission> findByIsEnabledTrue();

    /**
     * 查找所有系统权限
     *
     * @return 权限列表
     */
    List<Permission> findByIsSystemTrue();

    /**
     * 查找所有非系统权限
     *
     * @return 权限列表
     */
    List<Permission> findByIsSystemFalse();

    /**
     * 根据权限级别范围查找权限
     *
     * @param minLevel 最小级别
     * @param maxLevel 最大级别
     * @return 权限列表
     */
    List<Permission> findByLevelBetween(Integer minLevel, Integer maxLevel);

    /**
     * 根据资源类型和启用状态查找权限
     *
     * @param resourceType 资源类型
     * @param enabled 启用状态
     * @return 权限列表
     */
    List<Permission> findByResourceTypeAndIsEnabled(String resourceType, Boolean enabled);

    /**
     * 根据权限名称模糊查询
     *
     * @param name 权限名称关键字
     * @return 权限列表
     */
    List<Permission> findByNameContainingIgnoreCase(String name);

    /**
     * 根据权限代码模糊查询
     *
     * @param code 权限代码关键字
     * @return 权限列表
     */
    List<Permission> findByCodeContainingIgnoreCase(String code);

    /**
     * 分页查询权限
     *
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<Permission> findAll(Pageable pageable);

    /**
     * 根据资源类型分页查询权限
     *
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<Permission> findByResourceType(String resourceType, Pageable pageable);

    /**
     * 根据启用状态分页查询权限
     *
     * @param enabled 启用状态
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<Permission> findByIsEnabled(Boolean enabled, Pageable pageable);

    /**
     * 检查权限代码是否存在
     *
     * @param code 权限代码
     * @return true如果存在
     */
    boolean existsByCode(String code);

    /**
     * 检查权限代码是否存在（排除指定ID）
     *
     * @param code 权限代码
     * @param id 排除的权限ID
     * @return true如果存在
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 统计指定资源类型的权限数量
     *
     * @param resourceType 资源类型
     * @return 权限数量
     */
    long countByResourceType(String resourceType);

    /**
     * 统计启用的权限数量
     *
     * @return 权限数量
     */
    long countByIsEnabledTrue();

    /**
     * 统计系统权限数量
     *
     * @return 权限数量
     */
    long countByIsSystemTrue();

    /**
     * 查找具有依赖关系的权限
     *
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.dependencies IS NOT NULL AND p.dependencies != ''")
    List<Permission> findPermissionsWithDependencies();

    /**
     * 根据权限级别排序查找权限
     *
     * @param resourceType 资源类型
     * @return 权限列表（按级别降序）
     */
    @Query("SELECT p FROM Permission p WHERE p.resourceType = :resourceType ORDER BY p.level DESC")
    List<Permission> findByResourceTypeOrderByLevelDesc(@Param("resourceType") String resourceType);

    /**
     * 查找用户通过角色拥有的权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN p.roles r " +
           "JOIN r.userRoles ur " +
           "WHERE ur.userId = :userId " +
           "AND ur.status = 'ACTIVE' " +
           "AND r.enabled = true " +
           "AND p.isEnabled = true")
    List<Permission> findPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 查找用户在指定资源类型上的权限
     *
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @return 权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN p.roles r " +
           "JOIN r.userRoles ur " +
           "WHERE ur.userId = :userId " +
           "AND ur.status = 'ACTIVE' " +
           "AND r.enabled = true " +
           "AND p.isEnabled = true " +
           "AND p.resourceType = :resourceType")
    List<Permission> findPermissionsByUserIdAndResourceType(@Param("userId") Long userId, 
                                                           @Param("resourceType") String resourceType);

    /**
     * 查找指定权限类别和级别范围内的权限
     *
     * @param category 权限类别
     * @param minLevel 最小级别
     * @param maxLevel 最大级别
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE p.category = :category " +
           "AND p.level >= :minLevel " +
           "AND p.level <= :maxLevel " +
           "AND p.isEnabled = true " +
           "ORDER BY p.level DESC")
    List<Permission> findByCategoryAndLevelRange(@Param("category") String category,
                                                @Param("minLevel") Integer minLevel,
                                                @Param("maxLevel") Integer maxLevel);

    /**
     * 查找可以被继承的权限
     *
     * @param resourceType 资源类型
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE p.resourceType = :resourceType " +
           "AND p.isEnabled = true " +
           "AND p.category IN ('READ', 'WRITE', 'DELETE') " +
           "ORDER BY p.level ASC")
    List<Permission> findInheritablePermissions(@Param("resourceType") String resourceType);

    /**
     * 根据多个条件查询权限
     *
     * @param resourceType 资源类型（可选）
     * @param category 权限类别（可选）
     * @param enabled 启用状态（可选）
     * @param isSystem 是否系统权限（可选）
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE (:resourceType IS NULL OR p.resourceType = :resourceType) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:enabled IS NULL OR p.isEnabled = :enabled) " +
           "AND (:isSystem IS NULL OR p.isSystem = :isSystem)")
    Page<Permission> findByMultipleConditions(@Param("resourceType") String resourceType,
                                             @Param("category") String category,
                                             @Param("enabled") Boolean enabled,
                                             @Param("isSystem") Boolean isSystem,
                                             Pageable pageable);

    /**
     * 搜索权限（按代码、名称、描述）
     *
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    @Query("SELECT p FROM Permission p " +
           "WHERE LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Permission> searchPermissions(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取权限统计信息
     *
     * @return 统计结果
     */
    @Query("SELECT " +
           "COUNT(p) as total, " +
           "SUM(CASE WHEN p.isEnabled = true THEN 1 ELSE 0 END) as enabled, " +
           "SUM(CASE WHEN p.isSystem = true THEN 1 ELSE 0 END) as system, " +
           "COUNT(DISTINCT p.resourceType) as resourceTypes, " +
           "COUNT(DISTINCT p.category) as categories " +
           "FROM Permission p")
    Object[] getPermissionStatistics();
} 