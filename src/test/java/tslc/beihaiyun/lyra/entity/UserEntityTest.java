package tslc.beihaiyun.lyra.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User实体类单元测试
 * 验证字段约束、验证规则和业务方法
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("User实体类测试")
class UserEntityTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // 创建一个有效的用户对象
        user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
    }

    @Test
    @DisplayName("创建有效用户 - 无验证错误")
    void testValidUser() {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "有效用户不应该有验证错误");
    }

    @Test
    @DisplayName("用户名验证 - 空值验证")
    void testUsernameNotBlank() {
        user.setUsername("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        boolean hasUsernameError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertTrue(hasUsernameError, "空用户名应该产生验证错误");
    }

    @Test
    @DisplayName("用户名验证 - 长度限制")
    void testUsernameLength() {
        // 测试用户名过短
        user.setUsername("ab");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "过短用户名应该产生验证错误");

        // 测试用户名过长
        user.setUsername("a".repeat(51));
        violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "过长用户名应该产生验证错误");

        // 测试有效长度
        user.setUsername("validuser");
        violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "有效长度用户名不应该产生验证错误");
    }

    @Test
    @DisplayName("用户名验证 - 格式验证")
    void testUsernamePattern() {
        // 测试包含特殊字符
        user.setUsername("test@user");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "包含特殊字符的用户名应该产生验证错误");

        // 测试包含空格
        user.setUsername("test user");
        violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "包含空格的用户名应该产生验证错误");

        // 测试有效格式
        user.setUsername("test_user-123");
        violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("username")), "有效格式用户名不应该产生验证错误");
    }

    @Test
    @DisplayName("邮箱验证 - 空值验证")
    void testEmailNotBlank() {
        user.setEmail("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        boolean hasEmailError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailError, "空邮箱应该产生验证错误");
    }

    @Test
    @DisplayName("邮箱验证 - 格式验证")
    void testEmailFormat() {
        // 测试无效邮箱格式
        user.setEmail("invalid-email");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("email")), "无效邮箱格式应该产生验证错误");

        // 测试有效邮箱格式
        user.setEmail("valid@example.com");
        violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("email")), "有效邮箱格式不应该产生验证错误");
    }

    @Test
    @DisplayName("密码验证 - 空值验证")
    void testPasswordNotBlank() {
        user.setPassword("");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        boolean hasPasswordError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertTrue(hasPasswordError, "空密码应该产生验证错误");
    }

    @Test
    @DisplayName("用户状态枚举测试")
    void testUserStatusEnum() {
        assertNotNull(User.UserStatus.PENDING, "PENDING状态应该存在");
        assertNotNull(User.UserStatus.ACTIVE, "ACTIVE状态应该存在");
        assertNotNull(User.UserStatus.DISABLED, "DISABLED状态应该存在");
        assertNotNull(User.UserStatus.LOCKED, "LOCKED状态应该存在");
        assertNotNull(User.UserStatus.DEACTIVATED, "DEACTIVATED状态应该存在");
        
        // 测试默认状态
        User newUser = new User();
        assertEquals(User.UserStatus.PENDING, newUser.getStatus(), "新用户默认状态应该是PENDING");
    }

    @Test
    @DisplayName("业务方法测试 - isActive")
    void testIsActiveMethod() {
        // 测试活跃用户
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setStatus(User.UserStatus.ACTIVE);
        assertTrue(user.isActive(), "启用且未锁定的活跃用户应该返回true");

        // 测试未启用用户
        user.setEnabled(false);
        assertFalse(user.isActive(), "未启用用户应该返回false");

        // 测试锁定用户
        user.setEnabled(true);
        user.setAccountNonLocked(false);
        assertFalse(user.isActive(), "锁定用户应该返回false");

        // 测试非活跃状态用户
        user.setAccountNonLocked(true);
        user.setStatus(User.UserStatus.DISABLED);
        assertFalse(user.isActive(), "非活跃状态用户应该返回false");
    }

    @Test
    @DisplayName("业务方法测试 - 登录失败次数管理")
    void testFailedLoginAttemptsManagement() {
        // 测试初始状态
        assertEquals(0, user.getFailedLoginAttempts(), "初始登录失败次数应该为0");

        // 测试增加失败次数
        user.incrementFailedLoginAttempts();
        assertEquals(1, user.getFailedLoginAttempts(), "增加后失败次数应该为1");

        user.incrementFailedLoginAttempts();
        assertEquals(2, user.getFailedLoginAttempts(), "再次增加后失败次数应该为2");

        // 测试重置失败次数
        user.resetFailedLoginAttempts();
        assertEquals(0, user.getFailedLoginAttempts(), "重置后失败次数应该为0");

        // 测试null值处理
        user.setFailedLoginAttempts(null);
        user.incrementFailedLoginAttempts();
        assertEquals(1, user.getFailedLoginAttempts(), "null值时增加应该设置为1");
    }

    @Test
    @DisplayName("业务方法测试 - 账户锁定管理")
    void testAccountLockManagement() {
        // 测试锁定账户
        user.lockAccount();
        assertFalse(user.getAccountNonLocked(), "锁定后账户应该处于锁定状态");
        assertNotNull(user.getLockedAt(), "锁定时间应该被设置");
        assertEquals(User.UserStatus.LOCKED, user.getStatus(), "用户状态应该变为LOCKED");

        // 测试解锁账户
        user.unlockAccount();
        assertTrue(user.getAccountNonLocked(), "解锁后账户应该处于非锁定状态");
        assertNull(user.getLockedAt(), "解锁后锁定时间应该被清空");
        assertEquals(0, user.getFailedLoginAttempts(), "解锁后失败次数应该被重置");
        assertEquals(User.UserStatus.ACTIVE, user.getStatus(), "解锁后状态应该变为ACTIVE");
    }

    @Test
    @DisplayName("业务方法测试 - 账户激活和禁用")
    void testAccountActivationAndDisabling() {
        // 测试激活账户
        user.activateAccount();
        assertTrue(user.getEnabled(), "激活后账户应该被启用");
        assertEquals(User.UserStatus.ACTIVE, user.getStatus(), "激活后状态应该为ACTIVE");

        // 测试禁用账户
        user.disableAccount();
        assertFalse(user.getEnabled(), "禁用后账户应该被禁用");
        assertEquals(User.UserStatus.DISABLED, user.getStatus(), "禁用后状态应该为DISABLED");
    }

    @Test
    @DisplayName("业务方法测试 - 邮箱验证")
    void testEmailVerification() {
        // 初始状态
        assertFalse(user.getEmailVerified(), "初始邮箱验证状态应该为false");
        assertNull(user.getEmailVerifiedAt(), "初始邮箱验证时间应该为null");

        // 验证邮箱
        user.verifyEmail();
        assertTrue(user.getEmailVerified(), "验证后邮箱验证状态应该为true");
        assertNotNull(user.getEmailVerifiedAt(), "验证后邮箱验证时间应该被设置");
    }

    @Test
    @DisplayName("业务方法测试 - 最后登录信息更新")
    void testLastLoginInfoUpdate() {
        String testIp = "192.168.1.1";
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        // 设置一些失败次数用于测试重置
        user.setFailedLoginAttempts(3);
        
        user.updateLastLogin(testIp);
        
        LocalDateTime afterUpdate = LocalDateTime.now();
        
        assertEquals(testIp, user.getLastLoginIp(), "登录IP应该被更新");
        assertNotNull(user.getLastLoginAt(), "最后登录时间应该被设置");
        assertTrue(user.getLastLoginAt().isAfter(beforeUpdate) || 
                  user.getLastLoginAt().isEqual(beforeUpdate), "最后登录时间应该在更新之后");
        assertTrue(user.getLastLoginAt().isBefore(afterUpdate) || 
                  user.getLastLoginAt().isEqual(afterUpdate), "最后登录时间应该在当前时间之前");
        assertEquals(0, user.getFailedLoginAttempts(), "登录成功后失败次数应该被重置");
    }

    @Test
    @DisplayName("业务方法测试 - 存储配额管理")
    void testStorageQuotaManagement() {
        // 设置配额和使用量
        user.setStorageQuota(1000L);
        user.setStorageUsed(800L);

        // 测试配额检查
        assertFalse(user.isStorageQuotaExceeded(100L), "未超过配额应该返回false");
        assertTrue(user.isStorageQuotaExceeded(300L), "超过配额应该返回true");

        // 测试增加使用量
        user.addStorageUsed(100L);
        assertEquals(900L, user.getStorageUsed(), "增加使用量后应该正确计算");

        // 测试减少使用量
        user.subtractStorageUsed(200L);
        assertEquals(700L, user.getStorageUsed(), "减少使用量后应该正确计算");

        // 测试减少到负数的保护
        user.subtractStorageUsed(1000L);
        assertEquals(0L, user.getStorageUsed(), "减少使用量不应该变为负数");

        // 测试增加负数的保护
        user.setStorageUsed(500L);
        user.addStorageUsed(-100L);
        assertEquals(400L, user.getStorageUsed(), "增加负数应该正确处理");
    }

    @Test
    @DisplayName("构造函数测试")
    void testConstructors() {
        // 测试无参构造函数
        User emptyUser = new User();
        assertNotNull(emptyUser, "无参构造函数应该创建对象");
        assertEquals(User.UserStatus.PENDING, emptyUser.getStatus(), "默认状态应该为PENDING");
        assertFalse(emptyUser.getEnabled(), "默认启用状态应该为false");

        // 测试有参构造函数
        User paramUser = new User("testuser", "password", "test@example.com");
        assertEquals("testuser", paramUser.getUsername(), "用户名应该被正确设置");
        assertEquals("password", paramUser.getPassword(), "密码应该被正确设置");
        assertEquals("test@example.com", paramUser.getEmail(), "邮箱应该被正确设置");
    }

    @Test
    @DisplayName("默认值测试")
    void testDefaultValues() {
        User newUser = new User();
        
        assertEquals(User.UserStatus.PENDING, newUser.getStatus(), "默认状态应该为PENDING");
        assertFalse(newUser.getEnabled(), "默认启用状态应该为false");
        assertTrue(newUser.getAccountNonExpired(), "默认账户未过期状态应该为true");
        assertTrue(newUser.getAccountNonLocked(), "默认账户未锁定状态应该为true");
        assertTrue(newUser.getCredentialsNonExpired(), "默认凭证未过期状态应该为true");
        assertEquals(0, newUser.getFailedLoginAttempts(), "默认失败次数应该为0");
        assertFalse(newUser.getEmailVerified(), "默认邮箱验证状态应该为false");
        assertEquals(10737418240L, newUser.getStorageQuota(), "默认存储配额应该为10GB");
        assertEquals(0L, newUser.getStorageUsed(), "默认已用存储应该为0");
        assertNotNull(newUser.getUserRoles(), "用户角色集合应该被初始化");
        assertTrue(newUser.getUserRoles().isEmpty(), "用户角色集合应该为空");
    }

    @Test
    @DisplayName("equals和hashCode测试")
    void testEqualsAndHashCode() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("user2"); // 不同用户名但相同ID

        User user3 = new User();
        user3.setId(2L);
        user3.setUsername("user1"); // 相同用户名但不同ID

        User user4 = new User();
        // ID为null

        // 测试相同ID的对象
        assertEquals(user1, user2, "相同ID的用户应该相等");
        assertEquals(user1.hashCode(), user2.hashCode(), "相同ID的用户hashCode应该相等");

        // 测试不同ID的对象
        assertNotEquals(user1, user3, "不同ID的用户应该不相等");

        // 测试null ID的对象
        assertNotEquals(user1, user4, "ID为null的用户与有ID的用户应该不相等");
        assertEquals(user4, user4, "对象与自身应该相等");

        // 测试与null的比较
        assertNotEquals(user1, null, "用户与null应该不相等");

        // 测试与不同类型对象的比较
        assertNotEquals(user1, "string", "用户与字符串应该不相等");
    }

    @Test
    @DisplayName("toString测试")
    void testToString() {
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setEmailVerified(true);

        String toString = user.toString();
        
        assertNotNull(toString, "toString不应该返回null");
        assertTrue(toString.contains("testuser"), "toString应该包含用户名");
        assertTrue(toString.contains("test@example.com"), "toString应该包含邮箱");
        assertTrue(toString.contains("Test User"), "toString应该包含显示名称");
        assertTrue(toString.contains("ACTIVE"), "toString应该包含状态");
    }

    @Test
    @DisplayName("手机号格式验证")
    void testPhoneValidation() {
        // 测试有效手机号格式
        user.setPhone("13800138000");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("phone")), "有效手机号不应该产生验证错误");

        user.setPhone("+86 138-0013-8000");
        violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("phone")), "带格式的手机号不应该产生验证错误");

        user.setPhone("(010) 12345678");
        violations = validator.validate(user);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("phone")), "座机号格式不应该产生验证错误");

        // 测试无效手机号格式
        user.setPhone("abc123");
        violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("phone")), "包含字母的手机号应该产生验证错误");
    }

    @Test
    @DisplayName("字段长度限制测试")
    void testFieldLengthLimits() {
        // 测试显示名称长度限制
        user.setDisplayName("a".repeat(101));
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("displayName")), "过长显示名称应该产生验证错误");

        // 测试头像URL长度限制
        user.setDisplayName("Valid Name");
        user.setAvatarUrl("http://example.com/" + "a".repeat(500));
        violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("avatarUrl")), "过长头像URL应该产生验证错误");

        // 测试邮箱长度限制
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setEmail("a".repeat(95) + "@example.com"); // 超过100字符
        violations = validator.validate(user);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("email")), "过长邮箱应该产生验证错误");
    }
} 