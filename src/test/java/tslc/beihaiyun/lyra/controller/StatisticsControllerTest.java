package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * StatisticsController 安全性和权限测试
 * 验证统计接口的访问控制和权限验证
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatisticsControllerTest {

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

    private static final String BASE_URL = "/api/admin/statistics";
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
        // 清理数据
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

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
        adminUserRole.setAssignedBy("system");
        adminUserRole.setAssignmentReason("测试管理员角色");
        userRoleRepository.save(adminUserRole);

        UserRole normalUserRole = new UserRole(normalUser, userRole);
        normalUserRole.setAssignedBy("system");
        normalUserRole.setAssignmentReason("测试普通用户角色");
        userRoleRepository.save(normalUserRole);

        // 生成JWT令牌
        LyraUserPrincipal adminPrincipal = LyraUserPrincipal.builder()
                .id(adminUser.getId())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .password(adminUser.getPassword())
                .enabled(adminUser.getEnabled())
                .accountNonLocked(adminUser.getAccountNonLocked())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        
        LyraUserPrincipal userPrincipal = LyraUserPrincipal.builder()
                .id(normalUser.getId())
                .username(normalUser.getUsername())
                .email(normalUser.getEmail())
                .password(normalUser.getPassword())
                .enabled(normalUser.getEnabled())
                .accountNonLocked(normalUser.getAccountNonLocked())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        adminToken = jwtService.generateToken(adminPrincipal);
        userToken = jwtService.generateToken(userPrincipal);
    }

    // ========== 权限验证测试 ==========

    @Test
    @DisplayName("未认证用户访问统计接口应被拒绝")
    void should_denyAccess_when_notAuthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("普通用户访问统计接口应被拒绝")
    void should_denyAccess_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/overview")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员访问统计接口应被允许")
    void should_allowAccess_when_admin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mock管理员用户可以访问系统概览")
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void should_returnOverview_when_adminAccess() throws Exception {
        mockMvc.perform(get(BASE_URL + "/overview"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.files").exists())
                .andExpect(jsonPath("$.storage").exists())
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    // ========== 系统概览统计测试 ==========

    @Test
    @DisplayName("管理员可以获取系统概览统计")
    void should_returnSystemOverview_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.total").exists())
                .andExpect(jsonPath("$.users.active").exists())
                .andExpect(jsonPath("$.files.totalFiles").exists())
                .andExpect(jsonPath("$.storage.totalSpace").exists())
                .andExpect(jsonPath("$.system.javaVersion").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    // ========== 用户统计测试 ==========

    @Test
    @DisplayName("管理员可以获取用户统计")
    void should_returnUserStatistics_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basicStats").exists())
                .andExpect(jsonPath("$.statusDistribution").exists())
                .andExpect(jsonPath("$.storageUsage").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("普通用户不能访问用户统计")
    void should_denyUserStatistics_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 文件统计测试 ==========

    @Test
    @DisplayName("管理员可以获取文件统计")
    void should_returnFileStatistics_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/files")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basicStats").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("普通用户不能访问文件统计")
    void should_denyFileStatistics_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/files")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 性能统计测试 ==========

    @Test
    @DisplayName("管理员可以获取性能统计")
    void should_returnPerformanceStatistics_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/performance")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jvm").exists())
                .andExpect(jsonPath("$.storage").exists())
                .andExpect(jsonPath("$.system").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("普通用户不能访问性能统计")
    void should_denyPerformanceStatistics_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/performance")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 系统报告测试 ==========

    @Test
    @DisplayName("管理员可以生成概览报告")
    void should_generateOverviewReport_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "overview")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header").exists())
                .andExpect(jsonPath("$.header.reportType").value("overview"))
                .andExpect(jsonPath("$.users").exists())
                .andExpect(jsonPath("$.files").exists())
                .andExpect(jsonPath("$.storage").exists());
    }

    @Test
    @DisplayName("管理员可以生成用户报告")
    void should_generateUserReport_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.reportType").value("users"))
                .andExpect(jsonPath("$.statistics").exists());
    }

    @Test
    @DisplayName("管理员可以生成存储报告")
    void should_generateStorageReport_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "storage")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.reportType").value("storage"))
                .andExpect(jsonPath("$.totalQuota").exists())
                .andExpect(jsonPath("$.fileStatistics").exists());
    }

    @Test
    @DisplayName("管理员可以生成性能报告")
    void should_generatePerformanceReport_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "performance")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.reportType").value("performance"))
                .andExpect(jsonPath("$.memory").exists())
                .andExpect(jsonPath("$.system").exists());
    }

    @Test
    @DisplayName("不支持的报告类型应返回错误")
    void should_returnError_when_unsupportedReportType() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "invalid_type")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("普通用户不能生成系统报告")
    void should_denyReportGeneration_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/report")
                .param("type", "overview")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 存储趋势测试 ==========

    @Test
    @DisplayName("管理员可以获取存储趋势")
    void should_returnStorageTrends_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/trends/storage")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").exists())
                .andExpect(jsonPath("$.highUsageUserCount").exists())
                .andExpect(jsonPath("$.growth").exists());
    }

    @Test
    @DisplayName("普通用户不能访问存储趋势")
    void should_denyStorageTrends_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/trends/storage")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 健康状态摘要测试 ==========

    @Test
    @DisplayName("管理员可以获取系统健康状态摘要")
    void should_returnHealthSummary_when_adminRequests() throws Exception {
        mockMvc.perform(get(BASE_URL + "/health-summary")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").exists())
                .andExpect(jsonPath("$.warnings").exists())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.metrics").exists())
                .andExpect(jsonPath("$.checkTime").exists());
    }

    @Test
    @DisplayName("普通用户不能访问健康状态摘要")
    void should_denyHealthSummary_when_notAdmin() throws Exception {
        mockMvc.perform(get(BASE_URL + "/health-summary")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ========== 错误处理测试 ==========

    @Test
    @DisplayName("无效的统计接口路径应返回404")
    void should_return404_when_invalidStatisticsPath() throws Exception {
        mockMvc.perform(get(BASE_URL + "/invalid-endpoint")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("所有统计接口都需要管理员权限")
    @WithMockUser(authorities = {"ROLE_USER"})
    void should_denyAllEndpoints_when_notAdmin() throws Exception {
        String[] endpoints = {
                "/overview",
                "/users", 
                "/files",
                "/performance",
                "/report",
                "/trends/storage",
                "/health-summary"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(BASE_URL + endpoint))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("所有统计接口在管理员权限下都应返回成功")
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void should_allowAllEndpoints_when_admin() throws Exception {
        String[] endpoints = {
                "/overview",
                "/users", 
                "/files",
                "/performance",
                "/trends/storage",
                "/health-summary"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(BASE_URL + endpoint))
                    .andExpect(status().isOk());
        }
    }
} 