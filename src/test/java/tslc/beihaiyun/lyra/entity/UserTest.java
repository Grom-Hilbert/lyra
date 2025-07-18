package tslc.beihaiyun.lyra.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import tslc.beihaiyun.lyra.validation.ValidationGroups;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User实体类单元测试
 * 测试用户模型的验证方法和业务逻辑
 */
@DisplayName("User实体测试")
class UserTest {

    private Validator validator;
    private User validUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // 创建一个有效的用户对象用于测试
        validUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy") // 有效的BCrypt哈希
                .status(User.UserStatus.ACTIVE)
                .authProvider(User.AuthProvider.LOCAL)
                .roles(new HashSet<>())
                .build();
    }

    @Nested
    @DisplayName("用户名验证测试")
    class UsernameValidationTest {

        @Test
        @DisplayName("有效用户名应该通过验证")
        void shouldAcceptValidUsernames() {
            assertTrue(User.isValidUsername("testuser"));
            assertTrue(User.isValidUsername("test_user"));
            assertTrue(User.isValidUsername("test-user"));
            assertTrue(User.isValidUsername("user123"));
            assertTrue(User.isValidUsername("123user"));
            // 50个字符 - 最大有效长度
            String maxLengthUsername = "a".repeat(50);
            assertTrue(User.isValidUsername(maxLengthUsername));
        }

        @Test
        @DisplayName("边界长度用户名测试")
        void shouldTestUsernameBoundaryLengths() {
            // 3个字符 - 最小有效长度
            assertTrue(User.isValidUsername("abc"));
            // 2个字符 - 无效
            assertFalse(User.isValidUsername("ab"));
            // 50个字符 - 最大有效长度
            String maxLengthUsername = "a".repeat(50);
            assertTrue(User.isValidUsername(maxLengthUsername));
            // 51个字符 - 无效
            String tooLongUsername = "a".repeat(51);
            assertFalse(User.isValidUsername(tooLongUsername));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "  ", "ab", "test user", "test@user", "测试用户"})
        @DisplayName("无效用户名应该被拒绝")
        void shouldRejectInvalidUsernames(String username) {
            assertFalse(User.isValidUsername(username));
        }

        @Test
        @DisplayName("超长用户名应该被拒绝")
        void shouldRejectTooLongUsername() {
            // 51个字符的用户名
            String tooLongUsername = "a".repeat(51);
            assertFalse(User.isValidUsername(tooLongUsername));
        }

        @Test
        @DisplayName("用户名验证注解应该正常工作")
        void shouldValidateUsernameWithAnnotations() {
            User user = User.builder()
                    .username("ab") // 太短
                    .email("test@example.com")
                    .displayName("Test User")
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(user, ValidationGroups.Create.class);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        }
    }

    @Nested
    @DisplayName("邮箱验证测试")
    class EmailValidationTest {

        @Test
        @DisplayName("有效邮箱应该通过验证")
        void shouldAcceptValidEmails() {
            assertTrue(User.isValidEmail("test@example.com"));
            assertTrue(User.isValidEmail("user.name@domain.co.uk"));
            assertTrue(User.isValidEmail("user+tag@example.org"));
            assertTrue(User.isValidEmail("123@456.com"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "  ", "invalid", "@example.com", "test@", "test.example.com", "test@.com"})
        @DisplayName("无效邮箱应该被拒绝")
        void shouldRejectInvalidEmails(String email) {
            assertFalse(User.isValidEmail(email));
        }

        @Test
        @DisplayName("超长邮箱应该被拒绝")
        void shouldRejectTooLongEmail() {
            String longEmail = "a".repeat(250) + "@example.com";
            assertFalse(User.isValidEmail(longEmail));
        }
    }

    @Nested
    @DisplayName("显示名称验证测试")
    class DisplayNameValidationTest {

        @Test
        @DisplayName("有效显示名称应该通过验证")
        void shouldAcceptValidDisplayNames() {
            assertTrue(User.isValidDisplayName("Test User"));
            assertTrue(User.isValidDisplayName("张三"));
            assertTrue(User.isValidDisplayName("User123"));
            assertTrue(User.isValidDisplayName("a"));
            // 100个字符 - 最大有效长度
            String maxLengthName = "a".repeat(100);
            assertTrue(User.isValidDisplayName(maxLengthName));
        }

        @Test
        @DisplayName("边界长度显示名称测试")
        void shouldTestDisplayNameBoundaryLengths() {
            // 1个字符 - 最小有效长度
            assertTrue(User.isValidDisplayName("a"));
            // 100个字符 - 最大有效长度
            String maxLengthName = "a".repeat(100);
            assertTrue(User.isValidDisplayName(maxLengthName));
            // 101个字符 - 无效
            String tooLongName = "a".repeat(101);
            assertFalse(User.isValidDisplayName(tooLongName));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "  "})
        @DisplayName("无效显示名称应该被拒绝")
        void shouldRejectInvalidDisplayNames(String displayName) {
            assertFalse(User.isValidDisplayName(displayName));
        }

        @Test
        @DisplayName("超长显示名称应该被拒绝")
        void shouldRejectTooLongDisplayName() {
            // 101个字符的字符串
            String tooLongName = "a".repeat(101);
            assertFalse(User.isValidDisplayName(tooLongName));
        }
    }

    @Nested
    @DisplayName("密码哈希验证测试")
    class PasswordHashValidationTest {

        @Test
        @DisplayName("有效BCrypt哈希应该通过验证")
        void shouldAcceptValidBCryptHashes() {
            assertTrue(User.isValidPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
            assertTrue(User.isValidPasswordHash("$2b$12$EXRkfkdmXn2gzds2SSitu.MW9.gAVqa9eLS1//RYtYCmB1eLHg.9q"));
            assertTrue(User.isValidPasswordHash("$2y$08$Ro0CUfOqk6cXEKf3dyaM7OhSCvnwM9s4wIX9JeLapehKK5YdLxKcm"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "plaintext",
            "$2a$10$short",
            "$2a$10$toolongpasswordhashthatshouldnotbeaccepted123456789",
            "$1a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123"
        })
        @DisplayName("无效密码哈希应该被拒绝")
        void shouldRejectInvalidPasswordHashes(String passwordHash) {
            assertFalse(User.isValidPasswordHash(passwordHash));
        }
    }

    @Nested
    @DisplayName("用户状态检查测试")
    class UserStatusTest {

        @Test
        @DisplayName("应该正确检查用户状态")
        void shouldCheckUserStatusCorrectly() {
            User activeUser = User.builder().status(User.UserStatus.ACTIVE).build();
            User pendingUser = User.builder().status(User.UserStatus.PENDING).build();
            User suspendedUser = User.builder().status(User.UserStatus.SUSPENDED).build();

            assertTrue(activeUser.isActive());
            assertFalse(activeUser.isPending());
            assertFalse(activeUser.isSuspended());

            assertFalse(pendingUser.isActive());
            assertTrue(pendingUser.isPending());
            assertFalse(pendingUser.isSuspended());

            assertFalse(suspendedUser.isActive());
            assertFalse(suspendedUser.isPending());
            assertTrue(suspendedUser.isSuspended());
        }
    }

    @Nested
    @DisplayName("认证提供者检查测试")
    class AuthProviderTest {

        @Test
        @DisplayName("应该正确检查认证提供者类型")
        void shouldCheckAuthProviderCorrectly() {
            User localUser = User.builder().authProvider(User.AuthProvider.LOCAL).build();
            User oauthUser = User.builder().authProvider(User.AuthProvider.OAUTH2).build();
            User ldapUser = User.builder().authProvider(User.AuthProvider.LDAP).build();

            assertTrue(localUser.isLocalAuth());
            assertFalse(localUser.isExternalAuth());

            assertFalse(oauthUser.isLocalAuth());
            assertTrue(oauthUser.isExternalAuth());

            assertFalse(ldapUser.isLocalAuth());
            assertTrue(ldapUser.isExternalAuth());
        }
    }

    @Nested
    @DisplayName("用户操作测试")
    class UserOperationsTest {

        @Test
        @DisplayName("应该正确更新最后登录时间")
        void shouldUpdateLastLoginTime() {
            User user = User.builder().build();
            assertNull(user.getLastLoginAt());

            LocalDateTime beforeUpdate = LocalDateTime.now();
            user.updateLastLoginTime();
            LocalDateTime afterUpdate = LocalDateTime.now();

            assertNotNull(user.getLastLoginAt());
            assertTrue(user.getLastLoginAt().isAfter(beforeUpdate.minusSeconds(1)));
            assertTrue(user.getLastLoginAt().isBefore(afterUpdate.plusSeconds(1)));
        }

        @Test
        @DisplayName("应该正确激活用户")
        void shouldActivateUser() {
            User user = User.builder().status(User.UserStatus.PENDING).build();
            assertFalse(user.isActive());

            user.activate();

            assertTrue(user.isActive());
            assertNotNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("应该正确暂停用户")
        void shouldSuspendUser() {
            User user = User.builder().status(User.UserStatus.ACTIVE).build();
            assertFalse(user.isSuspended());

            user.suspend();

            assertTrue(user.isSuspended());
            assertNotNull(user.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("数据完整性验证测试")
    class DataIntegrityValidationTest {

        @Test
        @DisplayName("有效用户数据应该通过验证")
        void shouldPassValidationForValidUser() {
            String result = validUser.validateUserData();
            assertNull(result);
        }

        @Test
        @DisplayName("无效用户名应该返回错误信息")
        void shouldFailValidationForInvalidUsername() {
            validUser.setUsername("ab");
            String result = validUser.validateUserData();
            assertNotNull(result);
            assertTrue(result.contains("用户名格式不正确"));
        }

        @Test
        @DisplayName("无效邮箱应该返回错误信息")
        void shouldFailValidationForInvalidEmail() {
            validUser.setEmail("invalid-email");
            String result = validUser.validateUserData();
            assertNotNull(result);
            assertTrue(result.contains("邮箱格式不正确"));
        }

        @Test
        @DisplayName("无效显示名称应该返回错误信息")
        void shouldFailValidationForInvalidDisplayName() {
            validUser.setDisplayName("");
            String result = validUser.validateUserData();
            assertNotNull(result);
            assertTrue(result.contains("显示名称"));
        }

        @Test
        @DisplayName("本地认证用户缺少密码哈希应该返回错误信息")
        void shouldFailValidationForLocalUserWithoutPasswordHash() {
            validUser.setAuthProvider(User.AuthProvider.LOCAL);
            validUser.setPasswordHash(null);
            String result = validUser.validateUserData();
            assertNotNull(result);
            assertTrue(result.contains("密码哈希"));
        }

        @Test
        @DisplayName("外部认证用户缺少外部ID应该返回错误信息")
        void shouldFailValidationForExternalUserWithoutExternalId() {
            validUser.setAuthProvider(User.AuthProvider.OAUTH2);
            validUser.setExternalId(null);
            String result = validUser.validateUserData();
            assertNotNull(result);
            assertTrue(result.contains("外部ID"));
        }
    }

    @Nested
    @DisplayName("JPA注解验证测试")
    class JPAValidationTest {

        @Test
        @DisplayName("创建操作验证应该正常工作")
        void shouldValidateForCreateOperation() {
            User invalidUser = User.builder()
                    .username("ab") // 太短
                    .email("invalid-email") // 无效邮箱
                    .displayName("") // 空显示名称
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(invalidUser, ValidationGroups.Create.class);
            
            assertFalse(violations.isEmpty());
            assertTrue(violations.size() >= 3); // 至少3个验证错误
        }

        @Test
        @DisplayName("更新操作验证应该正常工作")
        void shouldValidateForUpdateOperation() {
            User invalidUser = User.builder()
                    .username("test@user") // 包含特殊字符
                    .email("test@example.com")
                    .displayName("a".repeat(101)) // 太长
                    .build();

            Set<ConstraintViolation<User>> violations = validator.validate(invalidUser, ValidationGroups.Update.class);
            
            assertFalse(violations.isEmpty());
            assertTrue(violations.size() >= 2); // 至少2个验证错误
        }

        @Test
        @DisplayName("有效用户应该通过所有验证")
        void shouldPassAllValidationsForValidUser() {
            Set<ConstraintViolation<User>> createViolations = validator.validate(validUser, ValidationGroups.Create.class);
            Set<ConstraintViolation<User>> updateViolations = validator.validate(validUser, ValidationGroups.Update.class);
            
            assertTrue(createViolations.isEmpty());
            assertTrue(updateViolations.isEmpty());
        }
    }

    @Nested
    @DisplayName("生命周期回调测试")
    class LifecycleCallbackTest {

        @Test
        @DisplayName("PrePersist应该设置创建和更新时间")
        void shouldSetTimestampsOnPrePersist() {
            User user = new User();
            user.onCreate();

            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
            assertNotNull(user.getRoles());
            assertTrue(user.getRoles().isEmpty());
        }

        @Test
        @DisplayName("PreUpdate应该更新时间")
        void shouldUpdateTimestampOnPreUpdate() {
            User user = new User();
            user.onCreate();
            LocalDateTime originalUpdatedAt = user.getUpdatedAt();

            // 等待一毫秒确保时间不同
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            user.onUpdate();

            assertNotNull(user.getUpdatedAt());
            assertTrue(user.getUpdatedAt().isAfter(originalUpdatedAt));
        }
    }
}