package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.Role;

import java.util.List;
import java.util.Optional;

/**
 * 角色Repository接口
 * 提供角色数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色代码查找角色
     * 
     * @param code 角色代码
     * @return 角色实体（可选）
     */
    Optional<Role> findByCode(String code);

    /**
     * 根据角色名称查找角色
     * 
     * @param name 角色名称
     * @return 角色实体（可选）
     */
    Optional<Role> findByName(String name);

    /**
     * 检查角色代码是否已存在
     * 
     * @param code 角色代码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 检查角色名称是否已存在
     * 
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查角色代码是否已存在（排除指定角色ID）
     * 
     * @param code 角色代码
     * @param roleId 排除的角色ID
     * @return 是否存在
     */
    boolean existsByCodeAndIdNot(String code, Long roleId);

    /**
     * 检查角色名称是否已存在（排除指定角色ID）
     * 
     * @param name 角色名称
     * @param roleId 排除的角色ID
     * @return 是否存在
     */
    boolean existsByNameAndIdNot(String name, Long roleId);

    /**
     * 根据角色类型查找角色
     * 
     * @param type 角色类型
     * @return 角色列表
     */
    List<Role> findByType(Role.RoleType type);

    /**
     * 根据角色类型分页查找角色
     * 
     * @param type 角色类型
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByType(Role.RoleType type, Pageable pageable);

    /**
     * 查找启用的角色
     * 
     * @return 角色列表
     */
    List<Role> findByEnabledTrue();

    /**
     * 分页查找启用的角色
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByEnabledTrue(Pageable pageable);

    /**
     * 查找系统内置角色
     * 
     * @return 角色列表
     */
    List<Role> findBySystemTrue();

    /**
     * 查找自定义角色
     * 
     * @return 角色列表
     */
    List<Role> findBySystemFalse();

    /**
     * 分页查找自定义角色
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findBySystemFalse(Pageable pageable);

    /**
     * 根据启用状态和系统标记查找角色
     * 
     * @param enabled 是否启用
     * @param system 是否系统角色
     * @return 角色列表
     */
    List<Role> findByEnabledAndSystem(Boolean enabled, Boolean system);

    /**
     * 分页根据启用状态和系统标记查找角色
     * 
     * @param enabled 是否启用
     * @param system 是否系统角色
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByEnabledAndSystem(Boolean enabled, Boolean system, Pageable pageable);

    /**
     * 根据排序顺序查找所有启用角色
     * 
     * @return 按排序顺序排列的角色列表
     */
    List<Role> findByEnabledTrueOrderBySortOrderAsc();

    /**
     * 查找管理员角色（所有管理员类型）
     * 
     * @return 管理员角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.type IN ('SYSTEM_ADMIN', 'ORGANIZATION_ADMIN', 'DEPARTMENT_ADMIN')")
    List<Role> findAdminRoles();

    /**
     * 分页查找管理员角色
     * 
     * @param pageable 分页参数
     * @return 管理员角色分页结果
     */
    @Query("SELECT r FROM Role r WHERE r.type IN ('SYSTEM_ADMIN', 'ORGANIZATION_ADMIN', 'DEPARTMENT_ADMIN')")
    Page<Role> findAdminRoles(Pageable pageable);

    /**
     * 根据角色代码模糊搜索
     * 
     * @param code 角色代码关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    /**
     * 根据角色名称模糊搜索
     * 
     * @param name 角色名称关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 根据描述模糊搜索
     * 
     * @param description 描述关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    /**
     * 综合搜索角色（代码、名称、描述）
     * 
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Query("SELECT r FROM Role r WHERE " +
           "LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Role> searchRoles(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找指定用户拥有的角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur " +
           "WHERE ur.userId = :userId AND ur.status = 'ACTIVE' AND ur.deleted = false")
    List<Role> findByUserId(@Param("userId") Long userId);

    /**
     * 查找指定用户拥有的启用角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur " +
           "WHERE ur.userId = :userId AND ur.status = 'ACTIVE' AND ur.deleted = false AND r.enabled = true")
    List<Role> findEnabledRolesByUserId(@Param("userId") Long userId);

    /**
     * 查找指定用户没有拥有的角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.id NOT IN " +
           "(SELECT DISTINCT ur.roleId FROM UserRole ur WHERE ur.userId = :userId AND ur.deleted = false)")
    List<Role> findRolesNotAssignedToUser(@Param("userId") Long userId);

    /**
     * 统计各类型角色数量
     * 
     * @return 类型统计结果
     */
    @Query("SELECT r.type, COUNT(r) FROM Role r GROUP BY r.type")
    List<Object[]> countByType();

    /**
     * 统计启用角色数量
     * 
     * @return 启用角色数量
     */
    long countByEnabledTrue();

    /**
     * 统计系统角色数量
     * 
     * @return 系统角色数量
     */
    long countBySystemTrue();

    /**
     * 统计自定义角色数量
     * 
     * @return 自定义角色数量
     */
    long countBySystemFalse();

    /**
     * 查找具有用户的角色（有用户分配的角色）
     * 
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur WHERE ur.deleted = false")
    List<Role> findRolesWithUsers();

    /**
     * 分页查找具有用户的角色
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.userRoles ur WHERE ur.deleted = false")
    Page<Role> findRolesWithUsers(Pageable pageable);

    /**
     * 查找没有用户的角色（没有用户分配的角色）
     * 
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.id NOT IN " +
           "(SELECT DISTINCT ur.roleId FROM UserRole ur WHERE ur.deleted = false)")
    List<Role> findRolesWithoutUsers();

    /**
     * 分页查找没有用户的角色
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Query("SELECT r FROM Role r WHERE r.id NOT IN " +
           "(SELECT DISTINCT ur.roleId FROM UserRole ur WHERE ur.deleted = false)")
    Page<Role> findRolesWithoutUsers(Pageable pageable);

    /**
     * 批量更新角色状态
     * 
     * @param roleIds 角色ID列表
     * @param enabled 是否启用
     * @return 更新行数
     */
    @Query("UPDATE Role r SET r.enabled = :enabled WHERE r.id IN :roleIds")
    int updateEnabledByIds(@Param("roleIds") List<Long> roleIds, @Param("enabled") Boolean enabled);

    /**
     * 查找排序顺序大于指定值的角色
     * 
     * @param sortOrder 排序顺序
     * @return 角色列表
     */
    List<Role> findBySortOrderGreaterThan(Integer sortOrder);

    /**
     * 获取最大排序顺序
     * 
     * @return 最大排序顺序
     */
    @Query("SELECT MAX(r.sortOrder) FROM Role r")
    Integer findMaxSortOrder();

    /**
     * 更新角色排序顺序
     * 
     * @param roleId 角色ID
     * @param sortOrder 排序顺序
     * @return 更新行数
     */
    @Query("UPDATE Role r SET r.sortOrder = :sortOrder WHERE r.id = :roleId")
    int updateSortOrder(@Param("roleId") Long roleId, @Param("sortOrder") Integer sortOrder);
} 