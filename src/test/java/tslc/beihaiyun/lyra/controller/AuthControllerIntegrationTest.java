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
import tslc.beihaiyun.lyra.dto.AuthResponse;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.service.JwtService;
import tslc.beihaiyun.lyra.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
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
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String TEST_USERNAME = "testuser";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "TestPass123";

    @BeforeEach
    void setUp() {
        // 清理数据
        userRepository.deleteAll();
        
        // 创建测试用户
        testUser = new User();
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setDisplayName("测试用户");
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);
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
                .andExpect(jsonPath("$.data.userInfo.email").value(TEST_EMAIL))
                .andReturn();

        // 验证返回的JWT令牌
        String responseJson = result.getResponse().getContentAsString();
        AuthResponse.ApiResponse<AuthResponse.LoginResponse> response = objectMapper.readValue(
                responseJson, 
                objectMapper.getTypeFactory().constructParametricType(
                        AuthResponse.ApiResponse.class, 
                        AuthResponse.LoginResponse.class
                )
        );

        String accessToken = response.getData().getAccessToken();
        assertNotNull(accessToken);
        assertTrue(jwtService.isTokenValid(accessToken));
        assertEquals(TEST_USERNAME, jwtService.extractUsername(accessToken));
    }

    @Test
    @DisplayName("用户登录 - 错误密码")
    void testLogin_WrongPassword() throws Exception {
        // 准备错误密码的登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword("WrongPassword123");

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        // 验证失败次数增加
        User user = userRepository.findByUsername(TEST_USERNAME).orElse(null);
        assertNotNull(user);
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("用户登录 - 用户不存在")
    void testLogin_UserNotFound() throws Exception {
        // 准备不存在用户的登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail("nonexistent@example.com");
        loginRequest.setPassword(TEST_PASSWORD);

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("用户登录 - 账户被锁定")
    void testLogin_AccountLocked() throws Exception {
        // 锁定测试用户
        testUser.lockAccount();
        userRepository.save(testUser);

        // 准备登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("账户已被锁定"));
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void testRegister_Success() throws Exception {
        // 准备注册请求
        AuthRequest.RegisterRequest registerRequest = new AuthRequest.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("NewPass123");
        registerRequest.setConfirmPassword("NewPass123");
        registerRequest.setDisplayName("新用户");
        registerRequest.setAgreeToTerms(true);

        // 执行注册请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requiresApproval").value(true))
                .andExpect(jsonPath("$.data.emailVerificationSent").value(true));

        // 验证用户已保存到数据库
        User savedUser = userRepository.findByUsername("newuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals("newuser@example.com", savedUser.getEmail());
        assertEquals(User.UserStatus.PENDING, savedUser.getStatus());
        assertFalse(savedUser.getEnabled());
        assertFalse(savedUser.getEmailVerified());
    }

    @Test
    @DisplayName("用户注册 - 用户名已存在")
    void testRegister_UsernameExists() throws Exception {
        // 准备使用已存在用户名的注册请求
        AuthRequest.RegisterRequest registerRequest = new AuthRequest.RegisterRequest();
        registerRequest.setUsername(TEST_USERNAME); // 使用已存在的用户名
        registerRequest.setEmail("different@example.com");
        registerRequest.setPassword("NewPass123");
        registerRequest.setConfirmPassword("NewPass123");
        registerRequest.setAgreeToTerms(true);

        // 执行注册请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("用户注册 - 邮箱已存在")
    void testRegister_EmailExists() throws Exception {
        // 准备使用已存在邮箱的注册请求
        AuthRequest.RegisterRequest registerRequest = new AuthRequest.RegisterRequest();
        registerRequest.setUsername("differentuser");
        registerRequest.setEmail(TEST_EMAIL); // 使用已存在的邮箱
        registerRequest.setPassword("NewPass123");
        registerRequest.setConfirmPassword("NewPass123");
        registerRequest.setAgreeToTerms(true);

        // 执行注册请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    @DisplayName("用户注册 - 密码不匹配")
    void testRegister_PasswordMismatch() throws Exception {
        // 准备密码不匹配的注册请求
        AuthRequest.RegisterRequest registerRequest = new AuthRequest.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("NewPass123");
        registerRequest.setConfirmPassword("DifferentPass123"); // 不匹配的确认密码
        registerRequest.setAgreeToTerms(true);

        // 执行注册请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("密码和确认密码不匹配"));
    }

    @Test
    @DisplayName("刷新令牌 - 成功")
    void testRefreshToken_Success() throws Exception {
        // 生成刷新令牌
        tslc.beihaiyun.lyra.security.LyraUserPrincipal userPrincipal = 
                tslc.beihaiyun.lyra.security.LyraUserPrincipal.fromUser(testUser);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // 准备刷新请求
        AuthRequest.RefreshTokenRequest refreshRequest = new AuthRequest.RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        // 执行刷新请求
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("令牌刷新成功"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").exists());
    }

    @Test
    @DisplayName("刷新令牌 - 无效令牌")
    void testRefreshToken_InvalidToken() throws Exception {
        // 准备无效令牌的刷新请求
        AuthRequest.RefreshTokenRequest refreshRequest = new AuthRequest.RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid.jwt.token");

        // 执行刷新请求
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无效的刷新令牌"));
    }

    @Test
    @DisplayName("用户登出 - 成功")
    void testLogout_Success() throws Exception {
        // 生成访问令牌
        tslc.beihaiyun.lyra.security.LyraUserPrincipal userPrincipal = 
                tslc.beihaiyun.lyra.security.LyraUserPrincipal.fromUser(testUser);
        String accessToken = jwtService.generateTokenWithUserId(userPrincipal, testUser.getId());

        // 执行登出请求
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登出成功"));

        // 验证令牌已被加入黑名单
        assertTrue(jwtService.isTokenLoggedOut(accessToken));
    }

    @Test
    @DisplayName("密码重置请求 - 成功")
    void testPasswordResetRequest_Success() throws Exception {
        // 准备密码重置请求
        AuthRequest.PasswordResetRequest resetRequest = new AuthRequest.PasswordResetRequest();
        resetRequest.setEmail(TEST_EMAIL);

        // 执行密码重置请求
        mockMvc.perform(post("/api/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("如果邮箱存在，我们已发送密码重置链接到您的邮箱"));
    }

    @Test
    @DisplayName("密码重置确认 - 成功")
    void testPasswordResetConfirm_Success() throws Exception {
        // 生成重置令牌
        String resetToken = userService.generatePasswordResetToken(TEST_EMAIL);
        
        // 准备密码重置确认请求
        AuthRequest.PasswordResetConfirmRequest confirmRequest = new AuthRequest.PasswordResetConfirmRequest();
        confirmRequest.setResetToken(resetToken);
        confirmRequest.setNewPassword("NewPassword123");
        confirmRequest.setConfirmPassword("NewPassword123");

        // 执行密码重置确认请求
        mockMvc.perform(post("/api/auth/password/reset-confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码重置成功"));

        // 验证密码已更新
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertTrue(passwordEncoder.matches("NewPassword123", updatedUser.getPassword()));
    }

    @Test
    @DisplayName("邮箱验证 - 成功")
    void testEmailVerification_Success() throws Exception {
        // 创建未验证的用户
        User unverifiedUser = new User();
        unverifiedUser.setUsername("unverified");
        unverifiedUser.setEmail("unverified@example.com");
        unverifiedUser.setPassword(passwordEncoder.encode("Password123"));
        unverifiedUser.setEmailVerified(false);
        unverifiedUser = userRepository.save(unverifiedUser);

        // 生成验证令牌
        String verificationToken = userService.generateEmailVerificationToken("unverified@example.com");
        
        // 准备邮箱验证请求
        AuthRequest.EmailVerificationRequest verificationRequest = new AuthRequest.EmailVerificationRequest();
        verificationRequest.setVerificationToken(verificationToken);

        // 执行邮箱验证请求
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱验证成功"));

        // 验证邮箱验证状态已更新
        User verifiedUser = userRepository.findById(unverifiedUser.getId()).orElse(null);
        assertNotNull(verifiedUser);
        assertTrue(verifiedUser.getEmailVerified());
        assertNotNull(verifiedUser.getEmailVerifiedAt());
    }

    @Test
    @DisplayName("邮箱验证（GET方式） - 成功")
    void testEmailVerificationByGet_Success() throws Exception {
        // 创建未验证的用户
        User unverifiedUser = new User();
        unverifiedUser.setUsername("unverified2");
        unverifiedUser.setEmail("unverified2@example.com");
        unverifiedUser.setPassword(passwordEncoder.encode("Password123"));
        unverifiedUser.setEmailVerified(false);
        unverifiedUser = userRepository.save(unverifiedUser);

        // 生成验证令牌
        String verificationToken = userService.generateEmailVerificationToken("unverified2@example.com");
        
        // 执行GET方式的邮箱验证请求
        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", verificationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("邮箱验证成功"));

        // 验证邮箱验证状态已更新
        User verifiedUser = userRepository.findById(unverifiedUser.getId()).orElse(null);
        assertNotNull(verifiedUser);
        assertTrue(verifiedUser.getEmailVerified());
    }

    @Test
    @DisplayName("参数验证 - 登录请求缺少必填字段")
    void testValidation_LoginMissingFields() throws Exception {
        // 准备缺少密码的登录请求
        AuthRequest.LoginRequest loginRequest = new AuthRequest.LoginRequest();
        loginRequest.setUsernameOrEmail(TEST_USERNAME);
        // 密码为空

        // 执行登录请求
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请求参数验证失败"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("参数验证 - 注册请求密码格式不正确")
    void testValidation_RegisterInvalidPassword() throws Exception {
        // 准备密码格式不正确的注册请求
        AuthRequest.RegisterRequest registerRequest = new AuthRequest.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("weak"); // 弱密码
        registerRequest.setConfirmPassword("weak");
        registerRequest.setAgreeToTerms(true);

        // 执行注册请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请求参数验证失败"))
                .andExpect(jsonPath("$.errors").isArray());
    }
} 