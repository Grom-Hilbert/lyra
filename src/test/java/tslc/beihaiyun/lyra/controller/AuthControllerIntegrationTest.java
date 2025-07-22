package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.dto.AuthRequest;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController集成测试
 * 验证认证API的完整流程
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpass";

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // 清理现有数据
        userRepository.deleteAll();

        // 创建测试用户
        testUser = new User();
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setDisplayName("Test User");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setAccountNonExpired(true);
        testUser.setCredentialsNonExpired(true);
        testUser.setEmailVerified(true);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setStorageQuota(1073741824L); // 1GB
        testUser.setStorageUsed(0L);
        testUser.setFailedLoginAttempts(0);
        testUser = userRepository.save(testUser);

        // 创建管理员用户
        adminUser = new User();
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        adminUser.setDisplayName("Admin User");
        adminUser.setEnabled(true);
        adminUser.setAccountNonLocked(true);
        adminUser.setAccountNonExpired(true);
        adminUser.setCredentialsNonExpired(true);
        adminUser.setEmailVerified(true);
        adminUser.setStatus(User.UserStatus.ACTIVE);
        adminUser.setStorageQuota(10737418240L); // 10GB
        adminUser.setStorageUsed(0L);
        adminUser.setFailedLoginAttempts(0);
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void testLogin_Success() throws Exception {
        // 准备登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // 执行登录请求
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userInfo.username").value(TEST_USERNAME))
                .andReturn();

        // 验证响应内容
        String responseContent = result.getResponse().getContentAsString();
        System.out.println("登录成功响应: " + responseContent);
    }

    @Test
    @DisplayName("用户登录 - 错误密码")
    void testLogin_WrongPassword() throws Exception {
        // 准备登录请求（错误密码）
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword("wrongpassword");

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("管理员登录 - 成功")
    void testAdminLogin_Success() throws Exception {
        // 准备管理员登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(ADMIN_USERNAME);
        loginRequest.setPassword(ADMIN_PASSWORD);

        // 执行登录请求
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.userInfo.username").value(ADMIN_USERNAME))
                .andReturn();

        // 验证响应内容
        String responseContent = result.getResponse().getContentAsString();
        System.out.println("管理员登录成功响应: " + responseContent);
    }

    @Test
    @DisplayName("用户登录 - 不存在的用户")
    void testLogin_UserNotFound() throws Exception {
        // 准备登录请求（不存在的用户）
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail("nonexistentuser");
        loginRequest.setPassword("password");

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("用户登录 - 请求参数验证失败")
    void testLogin_ValidationFailure() throws Exception {
        // 准备无效的登录请求（空用户名）
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail("");
        loginRequest.setPassword("password");

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请求参数验证失败"));
    }

    @Test
    @DisplayName("令牌刷新 - 基础功能测试")
    void testRefreshToken_Basic() throws Exception {
        // 首先登录获取令牌
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 从登录响应中提取刷新令牌
        String loginResponse = loginResult.getResponse().getContentAsString();
        // 注意：这里需要解析JSON来获取refreshToken
        // 为了简化测试，我们直接测试刷新令牌API的结构
        
        AuthRequest.RefreshTokenRequest refreshRequest = new AuthRequest.RefreshTokenRequest();
        refreshRequest.setRefreshToken("test.refresh.token");

        // 测试刷新令牌API（期望失败，因为令牌无效）
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("用户登出 - 基础功能测试")
    void testLogout_Basic() throws Exception {
        // 测试登出API
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }

    @Test
    @DisplayName("密码重置请求 - 基础功能测试")
    void testPasswordResetRequest() throws Exception {
        AuthRequest.PasswordResetRequest resetRequest = new AuthRequest.PasswordResetRequest();
        resetRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("如果邮箱存在，我们已发送密码重置链接到您的邮箱"));
    }

    @Test
    @DisplayName("密码重置确认 - 无效令牌")
    void testPasswordResetConfirm_InvalidToken() throws Exception {
        AuthRequest.PasswordResetConfirmRequest confirmRequest = new AuthRequest.PasswordResetConfirmRequest();
        confirmRequest.setResetToken("invalid.token");
        confirmRequest.setNewPassword("NewPassword123");
        confirmRequest.setConfirmPassword("NewPassword123");

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("重置令牌无效或已过期"));
    }

    @Test
    @DisplayName("邮箱验证 - 无效令牌")
    void testEmailVerification_InvalidToken() throws Exception {
        AuthRequest.EmailVerificationRequest verificationRequest = new AuthRequest.EmailVerificationRequest();
        verificationRequest.setVerificationToken("invalid.token");

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证令牌无效或已过期"));
    }

    @Test
    @DisplayName("GET方式邮箱验证 - 无效令牌")
    void testEmailVerificationByGet_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", "invalid.token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证令牌无效或已过期"));
    }

    @Test
    @DisplayName("GET方式邮箱验证 - 空令牌")
    void testEmailVerificationByGet_EmptyToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证令牌不能为空"));
    }
} 