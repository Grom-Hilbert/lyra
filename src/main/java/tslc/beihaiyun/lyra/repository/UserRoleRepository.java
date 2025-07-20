package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户角色关联Repository接口
 * 提供用户角色关联数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 根据用户ID和角色ID查找用户角色关联
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 用户角色关联（可选）
     */
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 检查用户角色关联是否存在
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否存在
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 查找指定用户的所有角色关联
     * 
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 分页查找指定用户的所有角色关联
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    Page<UserRole> findByUserId(Long userId, Pageable pageable);

    /**
     * 查找指定角色的所有用户关联
     * 
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 分页查找指定角色的所有用户关联
     * 
     * @param roleId 角色ID
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    Page<UserRole> findByRoleId(Long roleId, Pageable pageable);

    /**
     * 根据状态查找用户角色关联
     * 
     * @param status 分配状态
     * @return 用户角色关联列表
     */
    List<UserRole> findByStatus(UserRole.AssignmentStatus status);

    /**
     * 分页根据状态查找用户角色关联
     * 
     * @param status 分配状态
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    Page<UserRole> findByStatus(UserRole.AssignmentStatus status, Pageable pageable);

    /**
     * 查找指定用户的活跃角色关联
     * 
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByUserIdAndStatus(Long userId, UserRole.AssignmentStatus status);

    /**
     * 查找指定角色的活跃用户关联
     * 
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByRoleIdAndStatus(Long roleId, UserRole.AssignmentStatus status);

    /**
     * 查找过期的用户角色关联
     * 
     * @param currentTime 当前时间
     * @return 用户角色关联列表
     */
    List<UserRole> findByExpiresAtBefore(LocalDateTime currentTime);

    /**
     * 查找在指定时间之后生效的用户角色关联
     * 
     * @param currentTime 当前时间
     * @return 用户角色关联列表
     */
    List<UserRole> findByEffectiveAtAfter(LocalDateTime currentTime);

    /**
     * 查找有效的用户角色关联（活跃状态且在有效期内）
     * 
     * @param userId 用户ID
     * @param currentTime 当前时间
     * @return 用户角色关联列表
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId " +
           "AND ur.status = 'ACTIVE' " +
           "AND (ur.effectiveAt IS NULL OR ur.effectiveAt <= :currentTime) " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :currentTime)")
    List<UserRole> findValidUserRoles(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找指定用户的有效角色关联（包含角色信息）
     * 
     * @param userId 用户ID
     * @param currentTime 当前时间
     * @return 用户角色关联列表
     */
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r WHERE ur.userId = :userId " +
           "AND ur.status = 'ACTIVE' " +
           "AND (ur.effectiveAt IS NULL OR ur.effectiveAt <= :currentTime) " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :currentTime) " +
           "AND r.enabled = true")
    List<UserRole> findValidUserRolesWithRole(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找即将过期的用户角色关联
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户角色关联列表
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' " +
           "AND ur.expiresAt BETWEEN :startTime AND :endTime")
    List<UserRole> findExpiringSoon(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查找即将过期的用户角色关联
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' " +
           "AND ur.expiresAt BETWEEN :startTime AND :endTime")
    Page<UserRole> findExpiringSoon(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime, 
                                   Pageable pageable);

    /**
     * 查找由指定人员分配的角色关联
     * 
     * @param assignedBy 分配人
     * @return 用户角色关联列表
     */
    List<UserRole> findByAssignedBy(String assignedBy);

    /**
     * 分页查找由指定人员分配的角色关联
     * 
     * @param assignedBy 分配人
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    Page<UserRole> findByAssignedBy(String assignedBy, Pageable pageable);

    /**
     * 统计各状态的用户角色关联数量
     * 
     * @return 状态统计结果
     */
    @Query("SELECT ur.status, COUNT(ur) FROM UserRole ur GROUP BY ur.status")
    List<Object[]> countByStatus();

    /**
     * 统计指定用户的角色数量
     * 
     * @param userId 用户ID
     * @return 角色数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定角色的用户数量
     * 
     * @param roleId 角色ID
     * @return 用户数量
     */
    long countByRoleId(Long roleId);

    /**
     * 统计活跃的用户角色关联数量
     * 
     * @return 活跃关联数量
     */
    long countByStatus(UserRole.AssignmentStatus status);

    /**
     * 统计指定用户的活跃角色数量
     * 
     * @param userId 用户ID
     * @return 活跃角色数量
     */
    long countByUserIdAndStatus(Long userId, UserRole.AssignmentStatus status);

    /**
     * 统计指定角色的活跃用户数量
     * 
     * @param roleId 角色ID
     * @return 活跃用户数量
     */
    long countByRoleIdAndStatus(Long roleId, UserRole.AssignmentStatus status);

    /**
     * 删除指定用户的所有角色关联
     * 
     * @param userId 用户ID
     * @return 删除数量
     */
    long deleteByUserId(Long userId);

    /**
     * 删除指定角色的所有用户关联
     * 
     * @param roleId 角色ID
     * @return 删除数量
     */
    long deleteByRoleId(Long roleId);

    /**
     * 删除指定用户和角色的关联
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 删除数量
     */
    long deleteByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 批量更新用户角色关联状态
     * 
     * @param ids 关联ID列表
     * @param status 新状态
     * @return 更新行数
     */
    @Query("UPDATE UserRole ur SET ur.status = :status WHERE ur.id IN :ids")
    int updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") UserRole.AssignmentStatus status);

    /**
     * 更新过期的用户角色关联状态
     * 
     * @param currentTime 当前时间
     * @return 更新行数
     */
    @Query("UPDATE UserRole ur SET ur.status = 'EXPIRED' WHERE ur.status = 'ACTIVE' " +
           "AND ur.expiresAt IS NOT NULL AND ur.expiresAt <= :currentTime")
    int updateExpiredUserRoles(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 激活到期生效的用户角色关联
     * 
     * @param currentTime 当前时间
     * @return 更新行数
     */
    @Query("UPDATE UserRole ur SET ur.status = 'ACTIVE' WHERE ur.status = 'PENDING' " +
           "AND ur.effectiveAt IS NOT NULL AND ur.effectiveAt <= :currentTime")
    int activatePendingUserRoles(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找在指定时间段内创建的用户角色关联
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户角色关联列表
     */
    List<UserRole> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查找在指定时间段内创建的用户角色关联
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 用户角色关联分页结果
     */
    Page<UserRole> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查找具有特定分配原因的角色关联
     * 
     * @param assignmentReason 分配原因关键字
     * @return 用户角色关联列表
     */
    List<UserRole> findByAssignmentReasonContainingIgnoreCase(String assignmentReason);

    /**
     * 查找同时拥有多个指定角色的用户
     * 
     * @param roleIds 角色ID列表
     * @param requiredRoleCount 必需的角色数量
     * @return 用户ID列表
     */
    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.roleId IN :roleIds " +
           "AND ur.status = 'ACTIVE' AND ur.deleted = false " +
           "GROUP BY ur.userId HAVING COUNT(DISTINCT ur.roleId) = :requiredRoleCount")
    List<Long> findUsersWithAllRoles(@Param("roleIds") List<Long> roleIds, 
                                    @Param("requiredRoleCount") long requiredRoleCount);

    /**
     * 查找拥有任一指定角色的用户
     * 
     * @param roleIds 角色ID列表
     * @return 用户ID列表
     */
    @Query("SELECT DISTINCT ur.userId FROM UserRole ur WHERE ur.roleId IN :roleIds " +
           "AND ur.status = 'ACTIVE' AND ur.deleted = false")
    List<Long> findUsersWithAnyRole(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据用户ID和角色代码查找用户角色关联
     * 
     * @param userId 用户ID
     * @param roleCode 角色代码
     * @return 用户角色关联（可选）
     */
    @Query("SELECT ur FROM UserRole ur JOIN ur.role r WHERE ur.userId = :userId AND r.code = :roleCode")
    Optional<UserRole> findByUserIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    /**
     * 检查用户是否拥有指定角色
     * 
     * @param userId 用户ID
     * @param roleCode 角色代码
     * @param currentTime 当前时间
     * @return 是否拥有角色
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur JOIN ur.role r WHERE ur.userId = :userId " +
           "AND r.code = :roleCode AND ur.status = 'ACTIVE' " +
           "AND (ur.effectiveAt IS NULL OR ur.effectiveAt <= :currentTime) " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :currentTime)")
    boolean hasRole(@Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("currentTime") LocalDateTime currentTime);
} 