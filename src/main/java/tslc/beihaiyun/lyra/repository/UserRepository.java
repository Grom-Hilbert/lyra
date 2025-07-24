package tslc.beihaiyun.lyra.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;

/**
 * 用户Repository接口
 * 提供用户数据访问操作
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户实体（可选）
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * 
     * @param email 邮箱
     * @return 用户实体（可选）
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     * 
     * @param username 用户名
     * @param email 邮箱
     * @return 用户实体（可选）
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否已存在（排除指定用户ID）
     * 
     * @param username 用户名
     * @param userId 排除的用户ID
     * @return 是否存在
     */
    boolean existsByUsernameAndIdNot(String username, Long userId);

    /**
     * 检查邮箱是否已存在（排除指定用户ID）
     * 
     * @param email 邮箱
     * @param userId 排除的用户ID
     * @return 是否存在
     */
    boolean existsByEmailAndIdNot(String email, Long userId);

    /**
     * 根据状态查找用户
     * 
     * @param status 用户状态
     * @return 用户列表
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 根据状态分页查找用户
     * 
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    /**
     * 查找启用的用户
     * 
     * @return 用户列表
     */
    List<User> findByEnabledTrue();

    /**
     * 分页查找启用的用户
     * 
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByEnabledTrue(Pageable pageable);

    /**
     * 查找被锁定的用户
     * 
     * @return 用户列表
     */
    List<User> findByAccountNonLockedFalse();

    /**
     * 分页查找被锁定的用户
     * 
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByAccountNonLockedFalse(Pageable pageable);

    /**
     * 查找登录失败次数超过指定值的用户
     * 
     * @param maxFailedAttempts 最大失败次数
     * @return 用户列表
     */
    List<User> findByFailedLoginAttemptsGreaterThan(Integer maxFailedAttempts);

    /**
     * 查找在指定时间之后最后登录的用户
     * 
     * @param since 指定时间
     * @return 用户列表
     */
    List<User> findByLastLoginAtAfter(LocalDateTime since);

    /**
     * 查找在指定时间之前最后登录的用户
     * 
     * @param before 指定时间
     * @return 用户列表
     */
    List<User> findByLastLoginAtBefore(LocalDateTime before);

    /**
     * 查找未验证邮箱的用户
     * 
     * @return 用户列表
     */
    List<User> findByEmailVerifiedFalse();

    /**
     * 分页查找未验证邮箱的用户
     * 
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByEmailVerifiedFalse(Pageable pageable);

    /**
     * 查找存储使用率超过指定百分比的用户
     * 
     * @param usageThreshold 使用率阈值（0.0-1.0）
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE (u.storageUsed * 1.0 / u.storageQuota) > :usageThreshold")
    List<User> findByStorageUsageGreaterThan(@Param("usageThreshold") double usageThreshold);

    /**
     * 查找存储配额不足的用户
     * 
     * @param additionalSize 额外需要的存储大小
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE (u.storageUsed + :additionalSize) > u.storageQuota")
    List<User> findUsersWithInsufficientStorage(@Param("additionalSize") long additionalSize);

    /**
     * 统计各状态用户数量
     * 
     * @return 状态统计结果
     */
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countByStatus();

    /**
     * 统计启用用户数量
     * 
     * @return 启用用户数量
     */
    long countByEnabledTrue();

    /**
     * 统计锁定用户数量
     * 
     * @return 锁定用户数量
     */
    long countByAccountNonLockedFalse();

    /**
     * 统计已验证邮箱用户数量
     * 
     * @return 已验证邮箱用户数量
     */
    long countByEmailVerifiedTrue();

    /**
     * 根据用户名模糊搜索
     * 
     * @param username 用户名关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /**
     * 根据邮箱模糊搜索
     * 
     * @param email 邮箱关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * 根据显示名称模糊搜索
     * 
     * @param displayName 显示名称关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByDisplayNameContainingIgnoreCase(String displayName, Pageable pageable);

    /**
     * 综合搜索用户（用户名、邮箱、显示名称）
     * 
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找拥有指定角色的用户
     * 
     * @param roleCode 角色代码
     * @return 用户列表
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r " +
           "WHERE r.code = :roleCode AND ur.status = 'ACTIVE' AND ur.deleted = false")
    List<User> findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 分页查找拥有指定角色的用户
     * 
     * @param roleCode 角色代码
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r " +
           "WHERE r.code = :roleCode AND ur.status = 'ACTIVE' AND ur.deleted = false")
    Page<User> findByRoleCode(@Param("roleCode") String roleCode, Pageable pageable);

    /**
     * 查找在指定时间段内创建的用户
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户列表
     */
    List<User> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 更新用户最后登录信息
     * 
     * @param userId 用户ID
     * @param lastLoginAt 最后登录时间
     * @param lastLoginIp 最后登录IP
     * @return 更新行数
     */
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :lastLoginIp, " +
           "u.failedLoginAttempts = 0 WHERE u.id = :userId")
    int updateLastLoginInfo(@Param("userId") Long userId, 
                           @Param("lastLoginAt") LocalDateTime lastLoginAt,
                           @Param("lastLoginIp") String lastLoginIp);

    /**
     * 增加用户登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新行数
     */
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * 重置用户登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新行数
     */
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    int resetFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * 更新用户存储使用量
     * 
     * @param userId 用户ID
     * @param storageUsed 已使用存储
     * @return 更新行数
     */
    @Query("UPDATE User u SET u.storageUsed = :storageUsed WHERE u.id = :userId")
    int updateStorageUsed(@Param("userId") Long userId, @Param("storageUsed") Long storageUsed);

    /**
     * 批量更新用户状态
     *
     * @param userIds 用户ID列表
     * @param status 新状态
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :userIds")
    int updateStatusByIds(@Param("userIds") List<Long> userIds, @Param("status") User.UserStatus status);

    /**
     * 查找活跃用户（指定天数内有登录记录的用户）
     *
     * @param days 天数
     * @return 活跃用户列表
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since AND u.enabled = true ORDER BY u.lastLoginAt DESC")
    List<User> findActiveUsers(@Param("since") LocalDateTime since);

    /**
     * 查找活跃用户（指定天数内有登录记录的用户）
     *
     * @param days 天数
     * @return 活跃用户列表
     */
    default List<User> findActiveUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return findActiveUsers(since);
    }

    /**
     * 统计管理员用户数量
     * 
     * @return 管理员用户数量
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur JOIN ur.role r " +
           "WHERE r.code = :roleCode AND ur.status = :status AND ur.deleted = false")
    long countUsersByRoleAndStatus(@Param("roleCode") String roleCode, @Param("status") UserRole.AssignmentStatus status);
    
    /**
     * 统计管理员用户数量
     * 
     * @return 管理员用户数量
     */
    default long countAdminUsers() {
        return countUsersByRoleAndStatus("ADMIN", UserRole.AssignmentStatus.ACTIVE);
    }

    /**
     * 获取所有用户的总存储配额
     * 
     * @return 总存储配额（字节）
     */
    @Query("SELECT COALESCE(SUM(u.storageQuota), 0) FROM User u WHERE u.deleted = false")
    Long getTotalStorageQuota();

    /**
     * 获取所有用户的总存储使用量
     * 
     * @return 总存储使用量（字节）
     */
    @Query("SELECT COALESCE(SUM(u.storageUsed), 0) FROM User u WHERE u.deleted = false")
    Long getTotalStorageUsed();
} 