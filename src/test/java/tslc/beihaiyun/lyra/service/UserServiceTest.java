package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserService 测试类
 * 测试用户业务服务的各项功能
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 业务服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

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
    }

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