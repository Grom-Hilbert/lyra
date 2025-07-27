package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserService 测试类
 * 测试用户业务服务的各项功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 业务服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setFailedLoginAttempts(0);
        testUser.setStorageQuota(10737418240L); // 10GB
        testUser.setStorageUsed(0L);
        
        pageable = PageRequest.of(0, 10);
    }

    // ========== CRUD操作测试 ==========

    @Test
    @DisplayName("根据ID查找用户 - 用户存在")
    void should_returnUser_when_userExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("根据ID查找用户 - 用户不存在")
    void should_returnEmpty_when_userNotExists() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("根据ID获取用户 - 用户存在")
    void should_returnUser_when_getUserByIdAndUserExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertEquals(testUser, result);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("根据ID获取用户 - 用户不存在")
    void should_throwException_when_getUserByIdAndUserNotExists() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.getUserById(999L));
        assertEquals("用户不存在: 999", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("分页查询所有用户 - 成功")
    void should_returnPagedUsers_when_findAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User());
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.findAllUsers(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(testUser, result.getContent().get(0));
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("根据状态分页查询用户 - 成功")
    void should_returnPagedUsersByStatus_when_findUsersByStatus() {
        // Arrange
        List<User> activeUsers = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(activeUsers, pageable, activeUsers.size());
        when(userRepository.findByStatus(User.UserStatus.ACTIVE, pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.findUsersByStatus(User.UserStatus.ACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(testUser, result.getContent().get(0));
        verify(userRepository).findByStatus(User.UserStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("搜索用户 - 成功")
    void should_returnSearchResults_when_searchUsers() {
        // Arrange
        String keyword = "test";
        List<User> searchResults = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(searchResults, pageable, searchResults.size());
        when(userRepository.searchUsers(keyword, pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.searchUsers(keyword, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(testUser, result.getContent().get(0));
        verify(userRepository).searchUsers(keyword, pageable);
    }

    @Test
    @DisplayName("创建用户 - 成功")
    void should_createUser_when_validUserProvided() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("rawPassword");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(newUser);

        // Assert
        assertEquals(newUser, result);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("创建用户 - 用户名已存在")
    void should_throwException_when_createUserWithExistingUsername() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("existinguser");
        newUser.setEmail("new@example.com");
        
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.createUser(newUser));
        assertEquals("用户名已存在: existinguser", exception.getMessage());
        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户 - 邮箱已存在")
    void should_throwException_when_createUserWithExistingEmail() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("existing@example.com");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.createUser(newUser));
        assertEquals("邮箱已被注册: existing@example.com", exception.getMessage());
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户信息 - 成功")
    void should_updateUser_when_validDataProvided() {
        // Arrange
        User updateData = new User();
        updateData.setDisplayName("新昵称");
        updateData.setPhone("13800138000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUser(1L, updateData);

        // Assert
        assertEquals("新昵称", testUser.getDisplayName());
        assertEquals("13800138000", testUser.getPhone());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("更新用户密码 - 成功")
    void should_updatePassword_when_validPasswordProvided() {
        // Arrange
        String newPassword = "newPassword123";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUserPassword(1L, newPassword);

        // Assert
        verify(passwordEncoder).encode(newPassword);
        assertTrue(testUser.getCredentialsNonExpired());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("软删除用户 - 成功")
    void should_deleteUser_when_validUserIdProvided() {
        // Arrange
        String reason = "用户申请注销";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.deleteUser(1L, reason);

        // Assert
        assertEquals(User.UserStatus.DEACTIVATED, testUser.getStatus());
        assertFalse(testUser.getEnabled());
        assertTrue(testUser.getDeleted());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("批量更新用户状态 - 成功")
    void should_batchUpdateStatus_when_validUserIdsProvided() {
        // Arrange
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        User.UserStatus newStatus = User.UserStatus.DISABLED;
        when(userRepository.updateStatusByIds(userIds, newStatus.name())).thenReturn(3);

        // Act
        int result = userService.batchUpdateUserStatus(userIds, newStatus);

        // Assert
        assertEquals(3, result);
        verify(userRepository).updateStatusByIds(userIds, newStatus.name());
    }

    // ========== 配额管理测试 ==========

    @Test
    @DisplayName("更新用户存储配额 - 成功")
    void should_updateStorageQuota_when_validQuotaProvided() {
        // Arrange
        Long newQuota = 21474836480L; // 20GB
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateStorageQuota(1L, newQuota);

        // Assert
        assertEquals(newQuota, testUser.getStorageQuota());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("更新用户存储配额 - 配额为负数")
    void should_throwException_when_updateStorageQuotaWithNegativeValue() {
        // Arrange
        Long negativeQuota = -1000L;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.updateStorageQuota(1L, negativeQuota));
        assertEquals("存储配额不能为负数", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("更新用户存储配额 - 新配额小于已使用存储")
    void should_throwException_when_newQuotaLessThanUsedStorage() {
        // Arrange
        testUser.setStorageUsed(5368709120L); // 5GB
        Long smallQuota = 1073741824L; // 1GB
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.updateStorageQuota(1L, smallQuota));
        assertTrue(exception.getMessage().contains("新配额"));
        assertTrue(exception.getMessage().contains("不能小于已使用存储"));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("增加用户存储使用量 - 成功")
    void should_increaseStorageUsage_when_validSizeProvided() {
        // Arrange
        Long additionalSize = 1073741824L; // 1GB
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.increaseStorageUsage(1L, additionalSize);

        // Assert
        assertEquals(additionalSize, testUser.getStorageUsed());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("增加用户存储使用量 - 存储配额不足")
    void should_throwException_when_increaseStorageUsageExceedsQuota() {
        // Arrange
        testUser.setStorageUsed(9663676416L); // 9GB
        Long additionalSize = 2147483648L; // 2GB，总共会超过10GB配额
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.increaseStorageUsage(1L, additionalSize));
        assertTrue(exception.getMessage().contains("存储配额不足"));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("减少用户存储使用量 - 成功")
    void should_decreaseStorageUsage_when_validSizeProvided() {
        // Arrange
        testUser.setStorageUsed(2147483648L); // 2GB
        Long decreaseSize = 1073741824L; // 1GB
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.decreaseStorageUsage(1L, decreaseSize);

        // Assert
        assertEquals(1073741824L, testUser.getStorageUsed()); // 应该剩余1GB
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("查找存储使用率超过阈值的用户 - 成功")
    void should_findUsersWithHighStorageUsage_when_validThresholdProvided() {
        // Arrange
        double threshold = 0.8; // 80%
        List<User> highUsageUsers = Arrays.asList(testUser);
        when(userRepository.findByStorageUsageGreaterThan(threshold)).thenReturn(highUsageUsers);

        // Act
        List<User> result = userService.findUsersWithHighStorageUsage(threshold);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userRepository).findByStorageUsageGreaterThan(threshold);
    }

    @Test
    @DisplayName("查找存储使用率超过阈值的用户 - 无效阈值")
    void should_throwException_when_findUsersWithInvalidThreshold() {
        // Arrange
        double invalidThreshold = 1.5; // 150%，无效值

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> userService.findUsersWithHighStorageUsage(invalidThreshold));
        assertEquals("使用率阈值必须在0.0-1.0之间", exception.getMessage());
        verify(userRepository, never()).findByStorageUsageGreaterThan(anyDouble());
    }

    @Test
    @DisplayName("查找存储配额不足的用户 - 成功")
    void should_findUsersWithInsufficientStorage_when_validSizeProvided() {
        // Arrange
        Long additionalSize = 5368709120L; // 5GB
        List<User> insufficientUsers = Arrays.asList(testUser);
        when(userRepository.findUsersWithInsufficientStorage(additionalSize)).thenReturn(insufficientUsers);

        // Act
        List<User> result = userService.findUsersWithInsufficientStorage(additionalSize);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userRepository).findUsersWithInsufficientStorage(additionalSize);
    }

    // ========== 统计功能测试 ==========

    @Test
    @DisplayName("获取用户统计信息 - 成功")
    void should_getUserStatistics_when_called() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        
        List<Object[]> statusCounts = Arrays.asList(
            new Object[]{User.UserStatus.ACTIVE, 80L},
            new Object[]{User.UserStatus.PENDING, 10L},
            new Object[]{User.UserStatus.DISABLED, 5L},
            new Object[]{User.UserStatus.LOCKED, 3L},
            new Object[]{User.UserStatus.DEACTIVATED, 2L}
        );
        when(userRepository.countByStatus()).thenReturn(statusCounts);
        when(userRepository.countByEnabledTrue()).thenReturn(85L);
        when(userRepository.countByEmailVerifiedTrue()).thenReturn(75L);

        // Act
        UserService.UserStatistics result = userService.getUserStatistics();

        // Assert
        assertEquals(100L, result.getTotalUsers());
        assertEquals(80L, result.getActiveUsers());
        assertEquals(10L, result.getPendingUsers());
        assertEquals(5L, result.getDisabledUsers());
        assertEquals(3L, result.getLockedUsers());
        assertEquals(2L, result.getDeactivatedUsers());
        assertEquals(85L, result.getEnabledUsers());
        assertEquals(75L, result.getEmailVerifiedUsers());
        
        verify(userRepository).count();
        verify(userRepository).countByStatus();
        verify(userRepository).countByEnabledTrue();
        verify(userRepository).countByEmailVerifiedTrue();
    }

    // ========== 原有测试保持不变 ==========

    @Test
    @DisplayName("检查用户是否活跃 - 正常活跃用户")
    void testIsUserActive_ActiveUser() {
        // Given
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setStatus(User.UserStatus.ACTIVE);

        // When
        boolean result = userService.isUserActive(testUser);

        // Then
        assertTrue(result, "活跃用户应该返回true");
    }

    @Test
    @DisplayName("检查用户是否活跃 - 被禁用用户")
    void testIsUserActive_DisabledUser() {
        // Given
        testUser.setEnabled(false);

        // When
        boolean result = userService.isUserActive(testUser);

        // Then
        assertFalse(result, "被禁用用户应该返回false");
    }

    @Test
    @DisplayName("检查用户是否活跃 - 被锁定用户")
    void testIsUserActive_LockedUser() {
        // Given
        testUser.setAccountNonLocked(false);

        // When
        boolean result = userService.isUserActive(testUser);

        // Then
        assertFalse(result, "被锁定用户应该返回false");
    }

    @Test
    @DisplayName("增加用户登录失败次数 - 成功")
    void testIncrementFailedLoginAttempts_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.incrementFailedLoginAttempts(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getFailedLoginAttempts());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("增加用户登录失败次数 - 用户不存在")
    void testIncrementFailedLoginAttempts_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> userService.incrementFailedLoginAttempts(999L),
            "用户不存在时应该抛出异常");
        
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("重置用户登录失败次数 - 成功")
    void testResetFailedLoginAttempts_Success() {
        // Given
        testUser.setFailedLoginAttempts(5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.resetFailedLoginAttempts(1L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getFailedLoginAttempts());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("锁定用户账户 - 成功")
    void testLockUserAccount_Success() {
        // Given
        String reason = "多次登录失败";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.lockUserAccount(1L, reason);

        // Then
        assertNotNull(result);
        assertFalse(result.getAccountNonLocked());
        assertEquals(User.UserStatus.LOCKED, result.getStatus());
        assertNotNull(result.getLockedAt());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("解锁用户账户 - 成功")
    void testUnlockUserAccount_Success() {
        // Given
        String reason = "管理员解锁";
        testUser.setAccountNonLocked(false);
        testUser.setStatus(User.UserStatus.LOCKED);
        testUser.setLockedAt(LocalDateTime.now().minusHours(1));
        testUser.setFailedLoginAttempts(5);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.unlockUserAccount(1L, reason);

        // Then
        assertNotNull(result);
        assertTrue(result.getAccountNonLocked());
        assertEquals(User.UserStatus.ACTIVE, result.getStatus());
        assertNull(result.getLockedAt());
        assertEquals(0, result.getFailedLoginAttempts());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("激活用户账户 - 成功")
    void testActivateUserAccount_Success() {
        // Given
        testUser.setStatus(User.UserStatus.PENDING);
        testUser.setEnabled(false);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.activateUserAccount(1L);

        // Then
        assertNotNull(result);
        assertEquals(User.UserStatus.ACTIVE, result.getStatus());
        assertTrue(result.getEnabled());
        assertTrue(result.getAccountNonLocked());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("禁用用户账户 - 成功")
    void testDisableUserAccount_Success() {
        // Given
        String reason = "违反规定";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.disableUserAccount(1L, reason);

        // Then
        assertNotNull(result);
        assertEquals(User.UserStatus.DISABLED, result.getStatus());
        assertFalse(result.getEnabled());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("验证用户邮箱 - 成功")
    void testVerifyUserEmail_Success() {
        // Given
        testUser.setEmailVerified(false);
        testUser.setEmailVerifiedAt(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.verifyUserEmail(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.getEmailVerified());
        assertNotNull(result.getEmailVerifiedAt());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("检查用户存储配额是否足够 - 有足够空间")
    void testHasEnoughStorage_SufficientSpace() {
        // Given
        testUser.setStorageUsed(5368709120L); // 5GB
        testUser.setStorageQuota(10737418240L); // 10GB
        Long additionalSize = 1073741824L; // 1GB

        // When
        boolean result = userService.hasEnoughStorage(testUser, additionalSize);

        // Then
        assertTrue(result, "应该有足够的存储空间");
    }

    @Test
    @DisplayName("检查用户存储配额是否足够 - 空间不足")
    void testHasEnoughStorage_InsufficientSpace() {
        // Given
        testUser.setStorageUsed(9663676416L); // 9GB
        testUser.setStorageQuota(10737418240L); // 10GB
        Long additionalSize = 2147483648L; // 2GB

        // When
        boolean result = userService.hasEnoughStorage(testUser, additionalSize);

        // Then
        assertFalse(result, "应该没有足够的存储空间");
    }

    @Test
    @DisplayName("计算用户存储使用率 - 正常情况")
    void testCalculateStorageUsageRatio_Normal() {
        // Given
        testUser.setStorageUsed(5368709120L); // 5GB
        testUser.setStorageQuota(10737418240L); // 10GB

        // When
        double result = userService.calculateStorageUsageRatio(testUser);

        // Then
        assertEquals(0.5, result, 0.01, "使用率应该是50%");
    }

    @Test
    @DisplayName("计算用户存储使用率 - 配额为0")
    void testCalculateStorageUsageRatio_ZeroQuota() {
        // Given
        testUser.setStorageUsed(1000L);
        testUser.setStorageQuota(0L);

        // When
        double result = userService.calculateStorageUsageRatio(testUser);

        // Then
        assertEquals(0.0, result, "配额为0时使用率应该是0");
    }

    @Test
    @DisplayName("更新用户最后登录信息 - 成功")
    void testUpdateLastLoginInfo_Success() {
        // Given
        String loginIp = "192.168.1.100";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateLastLoginInfo(1L, loginIp);

        // Then
        assertNotNull(result);
        assertEquals(loginIp, result.getLastLoginIp());
        assertNotNull(result.getLastLoginAt());
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }
} 