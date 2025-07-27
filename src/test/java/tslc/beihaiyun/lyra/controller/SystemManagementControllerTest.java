package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;
import tslc.beihaiyun.lyra.service.JwtService;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemManagementController 安全性和权限测试
 * 验证管理接口的访问控制和权限验证
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SystemManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE_URL = "/api/admin/system";
    private static final String ADMIN_USERNAME = "admin";
    private static final String USER_USERNAME = "normaluser";
    private static final String PASSWORD = "password123";

    private User adminUser;
    private User normalUser;
    private Role adminRole;
    private Role userRole;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // 清理数据 - 使用物理删除避免软删除导致的唯一约束冲突
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM roles");

        // 创建角色
        adminRole = new Role();
        adminRole.setName("管理员");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("系统管理员角色");
        adminRole = roleRepository.save(adminRole);

        userRole = new Role();
        userRole.setName("普通用户");
        userRole.setCode("USER");
        userRole.setDescription("普通用户角色");
        userRole = roleRepository.save(userRole);

        // 创建管理员用户
        adminUser = new User();
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode(PASSWORD));
        adminUser.setDisplayName("Administrator");
        adminUser.setEnabled(true);
        adminUser.setAccountNonLocked(true);
        adminUser.setAccountNonExpired(true);
        adminUser.setCredentialsNonExpired(true);
        adminUser.setEmailVerified(true);
        adminUser.setStatus(User.UserStatus.ACTIVE);
        adminUser.setStorageQuota(1073741824L); // 1GB
        adminUser.setStorageUsed(0L);
        adminUser = userRepository.save(adminUser);

        // 创建普通用户
        normalUser = new User();
        normalUser.setUsername(USER_USERNAME);
        normalUser.setEmail("user@example.com");
        normalUser.setPassword(passwordEncoder.encode(PASSWORD));
        normalUser.setDisplayName("Normal User");
        normalUser.setEnabled(true);
        normalUser.setAccountNonLocked(true);
        normalUser.setAccountNonExpired(true);
        normalUser.setCredentialsNonExpired(true);
        normalUser.setEmailVerified(true);
        normalUser.setStatus(User.UserStatus.ACTIVE);
        normalUser.setStorageQuota(1073741824L); // 1GB
        normalUser.setStorageUsed(0L);
        normalUser = userRepository.save(normalUser);

        // 分配角色
        UserRole adminUserRole = new UserRole(adminUser, adminRole);
        userRoleRepository.save(adminUserRole);

        UserRole normalUserRole = new UserRole(normalUser, userRole);
        userRoleRepository.save(normalUserRole);

        // 生成JWT令牌 - 通过UserDetailsService获取正确的权限
        UserDetails adminUserDetails = userDetailsService.loadUserByUsername(adminUser.getUsername());
        UserDetails normalUserDetails = userDetailsService.loadUserByUsername(normalUser.getUsername());

        adminToken = jwtService.generateToken(adminUserDetails);
        userToken = jwtService.generateToken(normalUserDetails);
    }

    // ========== 权限验证测试 ==========

    @Test
    @DisplayName("未认证用户访问管理接口应被拒绝")
    void should_denyAccess_when_notAuthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("普通用户访问管理接口应被拒绝")
    void should_denyAccess_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员访问管理接口应被允许")
    void should_allowAccess_when_admin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mock管理员用户可以访问用户列表")
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void should_returnUserList_when_adminAccess() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    // ========== 用户管理API测试 ==========

    @Test
    @DisplayName("管理员可以获取用户详情")
    void should_returnUserDetails_when_adminRequestsValidUser() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/" + normalUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(normalUser.getId()))
                .andExpect(jsonPath("$.user.username").value(normalUser.getUsername()))
                .andExpect(jsonPath("$.storageUsageRatio").exists());
    }

    @Test
    @DisplayName("管理员获取不存在的用户应返回404")
    void should_return404_when_userNotExists() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("管理员可以创建新用户")
    void should_createUser_when_adminProvidesValidData() throws Exception {
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("username", "newuser");
        userRequest.put("email", "newuser@example.com");
        userRequest.put("password", "password123");
        userRequest.put("displayName", "New User");
        userRequest.put("storageQuota", 1073741824L);
        userRequest.put("emailVerified", true);

        mockMvc.perform(post(BASE_URL + "/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value("newuser"));

        // 验证用户确实被创建
        assertThat(userRepository.findByUsername("newuser")).isPresent();
    }

    @Test
    @DisplayName("创建用户时用户名重复应返回错误")
    void should_returnError_when_usernameAlreadyExists() throws Exception {
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("username", normalUser.getUsername()); // 使用已存在的用户名
        userRequest.put("email", "another@example.com");
        userRequest.put("password", "password123");

        mockMvc.perform(post(BASE_URL + "/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("管理员可以更新用户信息")
    void should_updateUser_when_adminProvidesValidData() throws Exception {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("displayName", "Updated Name");
        updateRequest.put("email", "updated@example.com");

        mockMvc.perform(put(BASE_URL + "/users/" + normalUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.displayName").value("Updated Name"));
    }

    @Test
    @DisplayName("管理员可以更新用户密码")
    void should_updatePassword_when_adminProvidesValidPassword() throws Exception {
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("password", "newpassword123");

        mockMvc.perform(put(BASE_URL + "/users/" + normalUser.getId() + "/password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("更新密码时空密码应返回错误")
    void should_returnError_when_passwordIsEmpty() throws Exception {
        Map<String, String> passwordRequest = new HashMap<>();
        passwordRequest.put("password", "");

        mockMvc.perform(put(BASE_URL + "/users/" + normalUser.getId() + "/password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== 用户状态管理测试 ==========

    @Test
    @DisplayName("管理员可以激活用户")
    void should_activateUser_when_adminRequests() throws Exception {
        // 先将用户设为禁用状态
        normalUser.setStatus(User.UserStatus.DISABLED);
        normalUser.setEnabled(false);
        userRepository.save(normalUser);

        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("管理员可以禁用用户")
    void should_disableUser_when_adminRequests() throws Exception {
        Map<String, String> reasonRequest = new HashMap<>();
        reasonRequest.put("reason", "违反使用条款");

        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/disable")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reasonRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reason").value("违反使用条款"));
    }

    @Test
    @DisplayName("管理员可以锁定用户")
    void should_lockUser_when_adminRequests() throws Exception {
        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/lock")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("管理员可以解锁用户")
    void should_unlockUser_when_adminRequests() throws Exception {
        // 先锁定用户
        normalUser.setStatus(User.UserStatus.LOCKED);
        normalUser.setAccountNonLocked(false);
        userRepository.save(normalUser);

        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/unlock")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("管理员可以删除用户")
    void should_deleteUser_when_adminRequests() throws Exception {
        Map<String, String> reasonRequest = new HashMap<>();
        reasonRequest.put("reason", "用户申请注销");

        mockMvc.perform(delete(BASE_URL + "/users/" + normalUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reasonRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ========== 批量操作测试 ==========

    @Test
    @DisplayName("管理员可以批量激活用户")
    void should_batchActivateUsers_when_adminRequests() throws Exception {
        // 创建另一个测试用户
        User anotherUser = new User();
        anotherUser.setUsername("testuser2");
        anotherUser.setEmail("test2@example.com");
        anotherUser.setPassword(passwordEncoder.encode(PASSWORD));
        anotherUser.setEnabled(false);
        anotherUser.setStatus(User.UserStatus.DISABLED);
        anotherUser = userRepository.save(anotherUser);

        Map<String, Object> batchRequest = new HashMap<>();
        batchRequest.put("userIds", List.of(normalUser.getId(), anotherUser.getId()));
        batchRequest.put("operation", "activate");

        mockMvc.perform(post(BASE_URL + "/users/batch")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operation").value("activate"));
    }

    @Test
    @DisplayName("批量操作时空用户列表应返回错误")
    void should_returnError_when_batchOperationWithEmptyUserList() throws Exception {
        Map<String, Object> batchRequest = new HashMap<>();
        batchRequest.put("userIds", List.of());
        batchRequest.put("operation", "activate");

        mockMvc.perform(post(BASE_URL + "/users/batch")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("批量操作时不支持的操作类型应返回错误")
    void should_returnError_when_unsupportedBatchOperation() throws Exception {
        Map<String, Object> batchRequest = new HashMap<>();
        batchRequest.put("userIds", List.of(normalUser.getId()));
        batchRequest.put("operation", "invalid_operation");

        mockMvc.perform(post(BASE_URL + "/users/batch")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== 存储配额管理测试 ==========

    @Test
    @DisplayName("管理员可以更新用户存储配额")
    void should_updateStorageQuota_when_adminRequests() throws Exception {
        Map<String, Long> quotaRequest = new HashMap<>();
        quotaRequest.put("quota", 2147483648L); // 2GB

        mockMvc.perform(put(BASE_URL + "/users/" + normalUser.getId() + "/storage-quota")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quotaRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newQuota").value(2147483648L));
    }

    @Test
    @DisplayName("更新存储配额时负数配额应返回错误")
    void should_returnError_when_negativeStorageQuota() throws Exception {
        Map<String, Long> quotaRequest = new HashMap<>();
        quotaRequest.put("quota", -1L);

        mockMvc.perform(put(BASE_URL + "/users/" + normalUser.getId() + "/storage-quota")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(quotaRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("管理员可以重新计算用户存储使用量")
    void should_recalculateStorageUsage_when_adminRequests() throws Exception {
        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/recalculate-storage")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.storageUsed").exists());
    }

    // ========== 用户注册审批测试 ==========

    @Test
    @DisplayName("管理员可以批准用户注册")
    void should_approveRegistration_when_adminRequests() throws Exception {
        // 创建待审核用户
        User pendingUser = new User();
        pendingUser.setUsername("pendinguser");
        pendingUser.setEmail("pending@example.com");
        pendingUser.setPassword(passwordEncoder.encode(PASSWORD));
        pendingUser.setStatus(User.UserStatus.PENDING);
        pendingUser.setEnabled(false);
        pendingUser = userRepository.save(pendingUser);

        Map<String, Object> approvalRequest = new HashMap<>();
        approvalRequest.put("approved", true);
        approvalRequest.put("comment", "用户资料完整，批准注册");

        mockMvc.perform(post(BASE_URL + "/users/" + pendingUser.getId() + "/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    @DisplayName("管理员可以拒绝用户注册")
    void should_rejectRegistration_when_adminRequests() throws Exception {
        // 创建待审核用户
        User pendingUser = new User();
        pendingUser.setUsername("pendinguser2");
        pendingUser.setEmail("pending2@example.com");
        pendingUser.setPassword(passwordEncoder.encode(PASSWORD));
        pendingUser.setStatus(User.UserStatus.PENDING);
        pendingUser.setEnabled(false);
        pendingUser = userRepository.save(pendingUser);

        Map<String, Object> approvalRequest = new HashMap<>();
        approvalRequest.put("approved", false);
        approvalRequest.put("comment", "用户资料不完整");

        mockMvc.perform(post(BASE_URL + "/users/" + pendingUser.getId() + "/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    @DisplayName("审批已激活用户应返回错误")
    void should_returnError_when_approvingNonPendingUser() throws Exception {
        Map<String, Object> approvalRequest = new HashMap<>();
        approvalRequest.put("approved", true);
        approvalRequest.put("comment", "测试");

        mockMvc.perform(post(BASE_URL + "/users/" + normalUser.getId() + "/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ========== 分页和搜索测试 ==========

    @Test
    @DisplayName("管理员可以分页查询用户")
    void should_returnPaginatedUsers_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "username")
                .param("sortDir", "asc")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @DisplayName("管理员可以按状态筛选用户")
    void should_returnFilteredUsers_when_adminRequestsWithStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .param("status", "ACTIVE")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray());
    }

    @Test
    @DisplayName("管理员可以搜索用户")
    void should_returnSearchedUsers_when_adminRequestsWithKeyword() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .param("search", normalUser.getUsername())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users[0].username").value(normalUser.getUsername()));
    }

    @Test
    @DisplayName("无效状态参数应返回错误")
    void should_returnError_when_invalidStatusParameter() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .param("status", "INVALID_STATUS")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
} 