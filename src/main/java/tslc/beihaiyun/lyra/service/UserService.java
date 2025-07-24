package tslc.beihaiyun.lyra.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.dto.AuthRequest;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;

/**
 * 用户业务服务类
 * 处理用户相关的业务逻辑
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 临时存储令牌的Map（生产环境应使用Redis等持久化存储）
    private final ConcurrentMap<String, TokenInfo> resetTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TokenInfo> verificationTokens = new ConcurrentHashMap<>();

    /**
     * 令牌信息内部类
     */
    private static class TokenInfo {
        private final String email;
        private final LocalDateTime expiresAt;

        public TokenInfo(String email, LocalDateTime expiresAt) {
            this.email = email;
            this.expiresAt = expiresAt;
        }

        public String getEmail() {
            return email;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    // ========== 用户CRUD操作 ==========

    /**
     * 根据用户ID查找用户
     * 
     * @param userId 用户ID
     * @return 用户实体（可选）
     */
    public Optional<User> findById(Long userId) {
        log.debug("查找用户: {}", userId);
        return userRepository.findById(userId);
    }

    /**
     * 根据用户ID获取用户
     *
     * @param userId 用户ID
     * @return 用户实体
     * @throws IllegalArgumentException 如果用户不存在
     */
    @Cacheable(value = CacheConfig.USER_SESSION_CACHE, key = "'user:' + #userId",
               condition = "#userId != null", unless = "#result == null")
    public User getUserById(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    }

    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户实体（可选）
     */
    public Optional<User> findByUsername(String username) {
        log.debug("根据用户名查找用户: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     * 
     * @param email 邮箱
     * @return 用户实体（可选）
     */
    public Optional<User> findByEmail(String email) {
        log.debug("根据邮箱查找用户: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * 分页查询所有用户
     * 
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    public Page<User> findAllUsers(Pageable pageable) {
        log.debug("分页查询所有用户，页码: {}, 大小: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    /**
     * 根据状态分页查询用户
     * 
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    public Page<User> findUsersByStatus(User.UserStatus status, Pageable pageable) {
        log.debug("根据状态分页查询用户: {}", status);
        return userRepository.findByStatus(status, pageable);
    }

    /**
     * 搜索用户（用户名、邮箱、显示名称）
     * 
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        log.debug("搜索用户: {}", keyword);
        return userRepository.searchUsers(keyword, pageable);
    }

    /**
     * 查找拥有指定角色的用户
     * 
     * @param roleCode 角色代码
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    public Page<User> findUsersByRole(String roleCode, Pageable pageable) {
        log.debug("查找拥有角色的用户: {}", roleCode);
        return userRepository.findByRoleCode(roleCode, pageable);
    }

    /**
     * 创建用户
     * 
     * @param user 用户实体
     * @return 创建后的用户
     */
    @Transactional
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());
        
        // 验证用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        
        // 验证邮箱是否已存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册: " + user.getEmail());
        }
        
        // 加密密码
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        log.info("用户创建成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        
        return savedUser;
    }

    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param updateUser 更新的用户信息
     * @return 更新后的用户
     */
    @Transactional
    public User updateUser(Long userId, User updateUser) {
        log.info("更新用户信息: {}", userId);
        
        User existingUser = getUserById(userId);
        
        // 检查用户名是否被其他用户占用
        if (updateUser.getUsername() != null && 
            !updateUser.getUsername().equals(existingUser.getUsername()) &&
            userRepository.existsByUsernameAndIdNot(updateUser.getUsername(), userId)) {
            throw new IllegalArgumentException("用户名已被占用: " + updateUser.getUsername());
        }
        
        // 检查邮箱是否被其他用户占用
        if (updateUser.getEmail() != null && 
            !updateUser.getEmail().equals(existingUser.getEmail()) &&
            userRepository.existsByEmailAndIdNot(updateUser.getEmail(), userId)) {
            throw new IllegalArgumentException("邮箱已被占用: " + updateUser.getEmail());
        }
        
        // 更新字段
        if (updateUser.getUsername() != null) {
            existingUser.setUsername(updateUser.getUsername());
        }
        if (updateUser.getEmail() != null) {
            existingUser.setEmail(updateUser.getEmail());
            // 如果更换邮箱，需要重新验证
            if (!updateUser.getEmail().equals(existingUser.getEmail())) {
                existingUser.setEmailVerified(false);
                existingUser.setEmailVerifiedAt(null);
            }
        }
        if (updateUser.getDisplayName() != null) {
            existingUser.setDisplayName(updateUser.getDisplayName());
        }
        if (updateUser.getPhone() != null) {
            existingUser.setPhone(updateUser.getPhone());
        }
        if (updateUser.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(updateUser.getAvatarUrl());
        }
        
        User savedUser = userRepository.save(existingUser);
        log.info("用户信息更新成功: {}", savedUser.getUsername());
        
        return savedUser;
    }

    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    @Transactional
    public User updateUserPassword(Long userId, String newPassword) {
        log.info("更新用户密码: {}", userId);
        
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsNonExpired(true);
        
        User savedUser = userRepository.save(user);
        log.info("用户密码更新成功: {}", savedUser.getUsername());
        
        return savedUser;
    }

    /**
     * 软删除用户
     * 
     * @param userId 用户ID
     * @param reason 删除原因
     * @return 删除后的用户
     */
    @Transactional
    public User deleteUser(Long userId, String reason) {
        log.info("删除用户: {}, 原因: {}", userId, reason);
        
        User user = getUserById(userId);
        user.setStatus(User.UserStatus.DEACTIVATED);
        user.setEnabled(false);
        user.setDeleted(true); // 使用BaseEntity的软删除标记
        
        User savedUser = userRepository.save(user);
        log.info("用户删除成功: {}", savedUser.getUsername());
        
        return savedUser;
    }

    /**
     * 批量更新用户状态
     * 
     * @param userIds 用户ID列表
     * @param status 新状态
     * @return 更新的用户数量
     */
    @Transactional
    public int batchUpdateUserStatus(List<Long> userIds, User.UserStatus status) {
        log.info("批量更新用户状态: {} -> {}", userIds.size(), status);
        
        int updatedCount = userRepository.updateStatusByIds(userIds, status);
        log.info("批量状态更新完成，影响行数: {}", updatedCount);
        
        return updatedCount;
    }

    // ========== 存储配额管理 ==========

    /**
     * 更新用户存储配额
     * 
     * @param userId 用户ID
     * @param newQuota 新配额（字节）
     * @return 更新后的用户
     */
    @Transactional
    public User updateStorageQuota(Long userId, Long newQuota) {
        log.info("更新用户存储配额: {} -> {}", userId, newQuota);
        
        if (newQuota < 0) {
            throw new IllegalArgumentException("存储配额不能为负数");
        }
        
        User user = getUserById(userId);
        Long oldQuota = user.getStorageQuota();
        
        // 检查新配额是否小于已使用存储
        if (newQuota < user.getStorageUsed()) {
            throw new IllegalArgumentException(
                String.format("新配额(%d字节)不能小于已使用存储(%d字节)", newQuota, user.getStorageUsed())
            );
        }
        
        user.setStorageQuota(newQuota);
        User savedUser = userRepository.save(user);
        
        log.info("用户[{}]存储配额已更新: {} -> {}", user.getUsername(), oldQuota, newQuota);
        return savedUser;
    }

    /**
     * 增加用户存储使用量
     * 
     * @param userId 用户ID
     * @param additionalSize 增加的大小（字节）
     * @return 更新后的用户
     */
    @Transactional
    public User increaseStorageUsage(Long userId, Long additionalSize) {
        log.debug("增加用户存储使用量: {} + {}", userId, additionalSize);
        
        if (additionalSize < 0) {
            throw new IllegalArgumentException("增加的存储大小不能为负数");
        }
        
        User user = getUserById(userId);
        
        // 检查存储配额
        if (user.isStorageQuotaExceeded(additionalSize)) {
            throw new IllegalArgumentException(
                String.format("存储配额不足，当前已使用: %d字节，配额: %d字节，需要增加: %d字节", 
                    user.getStorageUsed(), user.getStorageQuota(), additionalSize)
            );
        }
        
        user.addStorageUsed(additionalSize);
        User savedUser = userRepository.save(user);
        
        log.debug("用户[{}]存储使用量已增加: +{} 字节，当前使用: {} 字节", 
            user.getUsername(), additionalSize, savedUser.getStorageUsed());
        
        return savedUser;
    }

    /**
     * 减少用户存储使用量
     * 
     * @param userId 用户ID
     * @param decreaseSize 减少的大小（字节）
     * @return 更新后的用户
     */
    @Transactional
    public User decreaseStorageUsage(Long userId, Long decreaseSize) {
        log.debug("减少用户存储使用量: {} - {}", userId, decreaseSize);
        
        if (decreaseSize < 0) {
            throw new IllegalArgumentException("减少的存储大小不能为负数");
        }
        
        User user = getUserById(userId);
        user.subtractStorageUsed(decreaseSize);
        User savedUser = userRepository.save(user);
        
        log.debug("用户[{}]存储使用量已减少: -{} 字节，当前使用: {} 字节", 
            user.getUsername(), decreaseSize, savedUser.getStorageUsed());
        
        return savedUser;
    }

    /**
     * 重新计算用户存储使用量
     * 
     * @param userId 用户ID
     * @return 更新后的用户
     */
    @Transactional
    public User recalculateStorageUsage(Long userId) {
        log.info("重新计算用户存储使用量: {}", userId);
        
        User user = getUserById(userId);
        
        // TODO: 实际计算用户所有文件的总大小
        // 这里需要与文件服务集成，统计用户所有文件的实际大小
        // Long actualUsage = fileService.calculateUserTotalSize(userId);
        // user.setStorageUsed(actualUsage);
        
        User savedUser = userRepository.save(user);
        log.info("用户[{}]存储使用量重新计算完成: {} 字节", user.getUsername(), savedUser.getStorageUsed());
        
        return savedUser;
    }

    /**
     * 获取存储使用率超过阈值的用户
     * 
     * @param usageThreshold 使用率阈值（0.0-1.0）
     * @return 用户列表
     */
    public List<User> findUsersWithHighStorageUsage(double usageThreshold) {
        log.debug("查找存储使用率超过 {}% 的用户", usageThreshold * 100);
        
        if (usageThreshold < 0.0 || usageThreshold > 1.0) {
            throw new IllegalArgumentException("使用率阈值必须在0.0-1.0之间");
        }
        
        return userRepository.findByStorageUsageGreaterThan(usageThreshold);
    }

    /**
     * 获取存储配额不足的用户
     * 
     * @param additionalSize 额外需要的存储大小
     * @return 用户列表
     */
    public List<User> findUsersWithInsufficientStorage(Long additionalSize) {
        log.debug("查找存储配额不足的用户，需要额外: {} 字节", additionalSize);
        return userRepository.findUsersWithInsufficientStorage(additionalSize);
    }

    // ========== 统计功能 ==========

    /**
     * 获取用户统计信息
     * 
     * @return 用户统计信息
     */
    public UserStatistics getUserStatistics() {
        log.debug("获取用户统计信息");
        
        UserStatistics stats = new UserStatistics();
        
        // 总用户数
        stats.setTotalUsers(userRepository.count());
        
        // 各状态用户数
        List<Object[]> statusCounts = userRepository.countByStatus();
        for (Object[] row : statusCounts) {
            User.UserStatus status = (User.UserStatus) row[0];
            Long count = (Long) row[1];
            
            switch (status) {
                case ACTIVE -> stats.setActiveUsers(count);
                case PENDING -> stats.setPendingUsers(count);
                case DISABLED -> stats.setDisabledUsers(count);
                case LOCKED -> stats.setLockedUsers(count);
                case DEACTIVATED -> stats.setDeactivatedUsers(count);
            }
        }
        
        // 其他统计
        stats.setEnabledUsers(userRepository.countByEnabledTrue());
        stats.setEmailVerifiedUsers(userRepository.countByEmailVerifiedTrue());
        
        return stats;
    }

    /**
     * 用户统计信息类
     */
    public static class UserStatistics {
        private Long totalUsers = 0L;
        private Long activeUsers = 0L;
        private Long pendingUsers = 0L;
        private Long disabledUsers = 0L;
        private Long lockedUsers = 0L;
        private Long deactivatedUsers = 0L;
        private Long enabledUsers = 0L;
        private Long emailVerifiedUsers = 0L;

        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        
        public Long getPendingUsers() { return pendingUsers; }
        public void setPendingUsers(Long pendingUsers) { this.pendingUsers = pendingUsers; }
        
        public Long getDisabledUsers() { return disabledUsers; }
        public void setDisabledUsers(Long disabledUsers) { this.disabledUsers = disabledUsers; }
        
        public Long getLockedUsers() { return lockedUsers; }
        public void setLockedUsers(Long lockedUsers) { this.lockedUsers = lockedUsers; }
        
        public Long getDeactivatedUsers() { return deactivatedUsers; }
        public void setDeactivatedUsers(Long deactivatedUsers) { this.deactivatedUsers = deactivatedUsers; }
        
        public Long getEnabledUsers() { return enabledUsers; }
        public void setEnabledUsers(Long enabledUsers) { this.enabledUsers = enabledUsers; }
        
        public Long getEmailVerifiedUsers() { return emailVerifiedUsers; }
        public void setEmailVerifiedUsers(Long emailVerifiedUsers) { this.emailVerifiedUsers = emailVerifiedUsers; }
        
        @Override
        public String toString() {
            return String.format("UserStatistics{总用户数=%d, 活跃=%d, 待审核=%d, 禁用=%d, 锁定=%d, 注销=%d, 启用=%d, 邮箱验证=%d}",
                totalUsers, activeUsers, pendingUsers, disabledUsers, lockedUsers, deactivatedUsers, enabledUsers, emailVerifiedUsers);
        }
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册后的用户
     */
    @Transactional
    public User registerUser(AuthRequest.RegisterRequest registerRequest) {
        log.info("开始注册用户: {}", registerRequest.getUsername());

        // 验证用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 验证邮箱是否已存在
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        // 验证密码确认
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("密码和确认密码不匹配");
        }

        // 验证服务条款同意
        if (!registerRequest.isAgreeToTerms()) {
            throw new IllegalArgumentException("必须同意服务条款");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setDisplayName(registerRequest.getDisplayName());
        user.setPhone(registerRequest.getPhone());
        
        // 设置默认状态（待审核）
        user.setStatus(User.UserStatus.PENDING);
        user.setEnabled(false);
        user.setAccountNonLocked(true);
        user.setEmailVerified(false);

        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    /**
     * 生成邮箱验证令牌
     * 
     * @param email 邮箱地址
     * @return 验证令牌
     */
    public String generateEmailVerificationToken(String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TokenInfo tokenInfo = new TokenInfo(email, LocalDateTime.now().plusHours(24)); // 24小时有效
        verificationTokens.put(token, tokenInfo);
        
        log.info("为邮箱 {} 生成验证令牌", email);
        return token;
    }

    /**
     * 验证邮箱
     * 
     * @param verificationToken 验证令牌
     * @return 是否验证成功
     */
    @Transactional
    public boolean verifyEmail(String verificationToken) {
        log.info("开始验证邮箱令牌: {}", verificationToken);

        TokenInfo tokenInfo = verificationTokens.get(verificationToken);
        if (tokenInfo == null) {
            log.warn("无效的验证令牌: {}", verificationToken);
            return false;
        }

        if (tokenInfo.isExpired()) {
            log.warn("验证令牌已过期: {}", verificationToken);
            verificationTokens.remove(verificationToken);
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(tokenInfo.getEmail());
        if (userOpt.isEmpty()) {
            log.warn("验证令牌对应的用户不存在: {}", tokenInfo.getEmail());
            return false;
        }

        User user = userOpt.get();
        user.verifyEmail();
        userRepository.save(user);

        // 移除已使用的令牌
        verificationTokens.remove(verificationToken);
        
        log.info("邮箱验证成功: {}", user.getEmail());
        return true;
    }

    /**
     * 生成密码重置令牌
     * 
     * @param email 邮箱地址
     * @return 重置令牌（如果用户存在）
     */
    public String generatePasswordResetToken(String email) {
        log.info("为邮箱 {} 生成密码重置令牌", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("密码重置请求的邮箱不存在: {}", email);
            // 为了安全，不透露用户是否存在，统一返回成功
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        TokenInfo tokenInfo = new TokenInfo(email, LocalDateTime.now().plusHours(1)); // 1小时有效
        resetTokens.put(token, tokenInfo);
        
        log.info("密码重置令牌生成成功: {}", email);
        return token;
    }

    /**
     * 重置密码
     * 
     * @param resetToken 重置令牌
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    @Transactional
    public boolean resetPassword(String resetToken, String newPassword) {
        log.info("开始重置密码，令牌: {}", resetToken);

        TokenInfo tokenInfo = resetTokens.get(resetToken);
        if (tokenInfo == null) {
            log.warn("无效的重置令牌: {}", resetToken);
            return false;
        }

        if (tokenInfo.isExpired()) {
            log.warn("重置令牌已过期: {}", resetToken);
            resetTokens.remove(resetToken);
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(tokenInfo.getEmail());
        if (userOpt.isEmpty()) {
            log.warn("重置令牌对应的用户不存在: {}", tokenInfo.getEmail());
            return false;
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsNonExpired(true);
        userRepository.save(user);

        // 移除已使用的令牌
        resetTokens.remove(resetToken);
        
        log.info("密码重置成功: {}", user.getEmail());
        return true;
    }

    /**
     * 审批用户注册
     * 
     * @param userId 用户ID
     * @param approved 是否批准
     * @param approverComment 审批意见
     * @return 更新后的用户
     */
    @Transactional
    public User approveRegistration(Long userId, boolean approved, String approverComment) {
        log.info("开始审批用户注册: {} (批准: {})", userId, approved);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        User user = userOpt.get();
        
        if (!User.UserStatus.PENDING.equals(user.getStatus())) {
            throw new IllegalStateException("用户状态不是待审核: " + user.getStatus());
        }

        if (approved) {
            user.activateAccount();
            log.info("用户注册已批准: {} ({})", user.getUsername(), approverComment);
        } else {
            user.setStatus(User.UserStatus.DISABLED);
            user.setEnabled(false);
            log.info("用户注册已拒绝: {} ({})", user.getUsername(), approverComment);
        }

        return userRepository.save(user);
    }

    /**
     * 检查用户是否活跃（已启用且未锁定）
     * 
     * @param user 用户对象
     * @return 是否活跃
     */
    public boolean isUserActive(User user) {
        return Boolean.TRUE.equals(user.getEnabled()) && 
               Boolean.TRUE.equals(user.getAccountNonLocked()) && 
               User.UserStatus.ACTIVE.equals(user.getStatus());
    }

    /**
     * 增加用户登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新后的用户
     */
    @Transactional
    public User incrementFailedLoginAttempts(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Integer failedAttempts = user.getFailedLoginAttempts();
            user.setFailedLoginAttempts((failedAttempts == null) ? 1 : failedAttempts + 1);
            
            // 检查是否需要锁定账户（连续失败5次）
            if (user.getFailedLoginAttempts() >= 5) {
                user.lockAccount();
                log.warn("用户[{}]因登录失败次数过多被锁定", user.getUsername());
            }
            
            log.info("用户[{}]登录失败次数增加至: {}", user.getUsername(), user.getFailedLoginAttempts());
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 重置用户登录失败次数
     * 
     * @param userId 用户ID
     * @return 更新后的用户
     */
    @Transactional
    public User resetFailedLoginAttempts(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFailedLoginAttempts(0);
            
            log.info("用户[{}]登录失败次数已重置", user.getUsername());
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 锁定用户账户
     * 
     * @param userId 用户ID
     * @param reason 锁定原因
     * @return 更新后的用户
     */
    @Transactional
    public User lockUserAccount(Long userId, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountNonLocked(false);
            user.setLockedAt(LocalDateTime.now());
            user.setStatus(User.UserStatus.LOCKED);
            
            log.warn("用户[{}]账户已锁定，原因: {}", user.getUsername(), reason);
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 解锁用户账户
     * 
     * @param userId 用户ID
     * @param reason 解锁原因
     * @return 更新后的用户
     */
    @Transactional
    public User unlockUserAccount(Long userId, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAccountNonLocked(true);
            user.setLockedAt(null);
            user.setStatus(User.UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0); // 解锁时重置失败次数
            
            log.info("用户[{}]账户已解锁，原因: {}", user.getUsername(), reason);
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 激活用户账户
     * 
     * @param userId 用户ID
     * @return 更新后的用户
     */
    @Transactional
    public User activateUserAccount(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(User.UserStatus.ACTIVE);
            user.setEnabled(true);
            user.setAccountNonLocked(true);
            
            log.info("用户[{}]账户已激活", user.getUsername());
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 禁用用户账户
     * 
     * @param userId 用户ID
     * @param reason 禁用原因
     * @return 更新后的用户
     */
    @Transactional
    public User disableUserAccount(Long userId, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(User.UserStatus.DISABLED);
            user.setEnabled(false);
            
            log.warn("用户[{}]账户已禁用，原因: {}", user.getUsername(), reason);
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 验证用户邮箱
     * 
     * @param userId 用户ID
     * @return 更新后的用户
     */
    @Transactional
    public User verifyUserEmail(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            
            log.info("用户[{}]邮箱验证完成", user.getUsername());
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 检查用户存储配额是否足够
     * 
     * @param user 用户对象
     * @param additionalSize 需要增加的存储大小
     * @return 是否有足够空间
     */
    public boolean hasEnoughStorage(User user, Long additionalSize) {
        Long currentUsed = user.getStorageUsed();
        Long quota = user.getStorageQuota();
        return (currentUsed + additionalSize) <= quota;
    }

    /**
     * 计算用户存储使用率
     * 
     * @param user 用户对象
     * @return 使用率（0.0-1.0）
     */
    public double calculateStorageUsageRatio(User user) {
        Long used = user.getStorageUsed();
        Long quota = user.getStorageQuota();
        
        if (quota == null || quota == 0) {
            return 0.0;
        }
        
        return Math.min(1.0, (double) used / quota);
    }

    /**
     * 更新用户最后登录信息
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @return 更新后的用户
     */
    @Transactional
    public User updateLastLoginInfo(Long userId, String loginIp) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(loginIp);
            
            log.debug("用户[{}]最后登录信息已更新", user.getUsername());
            return userRepository.save(user);
        }
        throw new IllegalArgumentException("用户不存在: " + userId);
    }

    /**
     * 根据用户名或邮箱查找用户
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @return 用户实体（可选）
     */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        // 尝试先按用户名查找
        Optional<User> user = userRepository.findByUsername(usernameOrEmail);
        if (user.isPresent()) {
            return user;
        }
        
        // 如果按用户名找不到，再按邮箱查找
        return userRepository.findByEmail(usernameOrEmail);
    }

    /**
     * 清理过期的令牌
     */
    public void cleanupExpiredTokens() {
        // 清理过期的重置令牌
        resetTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // 清理过期的验证令牌
        verificationTokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        log.debug("已清理过期的令牌");
    }
} 