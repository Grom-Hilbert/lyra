package tslc.beihaiyun.lyra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

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
} 