package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.PermissionRepository;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import org.springframework.test.annotation.Rollback;
import jakarta.persistence.EntityManager;

/**
 * RoleService集成测试类
 * 测试角色管理功能与数据库和其他服务的集成
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("角色管理服务集成测试")
class RoleServiceIntegrationTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Role adminRole;
    private Role userRole;
    private Permission fileReadPermission;
    private Permission fileWritePermission;
    private Permission systemAdminPermission;

    @BeforeEach
    void setUp() {
        // 清理数据
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();

        // 创建测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 创建权限
        fileReadPermission = new Permission("file.read", "文件读取", "FILE", "READ", 10);
        fileReadPermission.setIsSystem(false);
        fileReadPermission = permissionRepository.save(fileReadPermission);

        fileWritePermission = new Permission("file.write", "文件写入", "FILE", "WRITE", 20);
        fileWritePermission.setIsSystem(false);
        fileWritePermission = permissionRepository.save(fileWritePermission);

        systemAdminPermission = new Permission("system.admin", "系统管理", "SYSTEM", "ADMIN", 100);
        systemAdminPermission.setIsSystem(true);
        systemAdminPermission = permissionRepository.save(systemAdminPermission);

        // 创建角色
        adminRole = new Role("ADMIN", "管理员", Role.RoleType.SYSTEM_ADMIN);
        adminRole.setSystem(true);
        adminRole.addPermission(fileReadPermission);
        adminRole.addPermission(fileWritePermission);
        adminRole.addPermission(systemAdminPermission);
        adminRole = roleRepository.save(adminRole);

        userRole = new Role("USER", "普通用户", Role.RoleType.USER);
        userRole.setSystem(false);
        userRole.addPermission(fileReadPermission);
        userRole = roleRepository.save(userRole);

        // 创建用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setDisplayName("测试用户");
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("角色基础操作集成测试")
    class RoleBasicOperationsIntegrationTest {

        @Test
        @DisplayName("应该成功创建并保存角色到数据库")
        void should_CreateAndPersistRole_Successfully() {
            // Given
            Role newRole = new Role("MODERATOR", "版主", Role.RoleType.CUSTOM);
            newRole.setDescription("版主角色");

            // When
            Role savedRole = roleService.createRole(newRole);

            // Then
            assertThat(savedRole.getId()).isNotNull();
            
            // 验证数据库中的数据
            Role foundRole = roleRepository.findById(savedRole.getId()).orElse(null);
            assertThat(foundRole).isNotNull();
            assertThat(foundRole.getCode()).isEqualTo("MODERATOR");
            assertThat(foundRole.getName()).isEqualTo("版主");
            assertThat(foundRole.getSortOrder()).isNotNull();
            assertThat(foundRole.getSortOrder()).isGreaterThan(0);
        }

        @Test
        @DisplayName("应该成功更新角色并保存到数据库")
        void should_UpdateAndPersistRole_Successfully() {
            // Given
            Role updateRole = new Role("UPDATED_USER", "更新用户", Role.RoleType.CUSTOM);
            updateRole.setDescription("更新后的描述");

            // When
            Role result = roleService.updateRole(userRole.getId(), updateRole);

            // Then
            assertThat(result.getCode()).isEqualTo("UPDATED_USER");
            assertThat(result.getName()).isEqualTo("更新用户");
            
            // 验证数据库中的数据
            Role foundRole = roleRepository.findById(userRole.getId()).orElse(null);
            assertThat(foundRole).isNotNull();
            assertThat(foundRole.getCode()).isEqualTo("UPDATED_USER");
            assertThat(foundRole.getDescription()).isEqualTo("更新后的描述");
        }

        @Test
        @DisplayName("应该正确搜索角色")
        void should_SearchRoles_Successfully() {
            // Given
            PageRequest pageRequest = PageRequest.of(0, 10);

            // When
            Page<Role> result = roleService.searchRoles("管理", pageRequest);

            // Then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().get(0).getName()).contains("管理");
        }

        @Test
        @DisplayName("应该正确获取启用的角色")
        void should_GetEnabledRoles_Successfully() {
            // When
            List<Role> result = roleService.getEnabledRoles();

            // Then
            assertThat(result).hasSize(2); // adminRole 和 userRole
            assertThat(result).extracting("enabled").containsOnly(true);
        }
    }

    @Nested
    @DisplayName("角色分配集成测试")
    class RoleAssignmentIntegrationTest {

        @Test
        @DisplayName("应该成功为用户分配角色并保存到数据库")
        void should_AssignRoleToUser_Successfully() {
            // When
            UserRole result = roleService.assignRoleToUser(
                    testUser.getId(), userRole.getId(), 
                    "admin", "初始分配", null);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(UserRole.AssignmentStatus.ACTIVE);
            
            // 验证数据库中的数据
            UserRole foundUserRole = userRoleRepository.findById(result.getId()).orElse(null);
            assertThat(foundUserRole).isNotNull();
            assertThat(foundUserRole.getUserId()).isEqualTo(testUser.getId());
            assertThat(foundUserRole.getRoleId()).isEqualTo(userRole.getId());
            assertThat(foundUserRole.getAssignedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("应该成功撤销用户角色")
        void should_RevokeUserRole_Successfully() {
            // Given
            UserRole userRoleAssignment = roleService.assignRoleToUser(
                    testUser.getId(), userRole.getId(), 
                    "admin", "初始分配", null);

            // When
            roleService.revokeRoleFromUser(
                    testUser.getId(), userRole.getId(), 
                    "admin", "测试撤销");

            // Then
            UserRole foundUserRole = userRoleRepository.findById(userRoleAssignment.getId()).orElse(null);
            assertThat(foundUserRole).isNotNull();
            assertThat(foundUserRole.getStatus()).isEqualTo(UserRole.AssignmentStatus.REVOKED);
        }

        @Test
        @DisplayName("应该成功批量分配角色")
        void should_BatchAssignRoles_Successfully() {
            // Given
            List<Long> roleIds = Arrays.asList(adminRole.getId(), userRole.getId());

            // When
            List<UserRole> results = roleService.assignRolesToUser(
                    testUser.getId(), roleIds, "admin", "批量分配");

            // Then
            assertThat(results).hasSize(2);
            
            // 验证数据库中的数据
            List<UserRole> userRoles = userRoleRepository.findByUserId(testUser.getId());
            assertThat(userRoles).hasSize(2);
            assertThat(userRoles).extracting("status")
                    .containsOnly(UserRole.AssignmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("应该正确处理过期时间")
        void should_HandleExpirationTime_Successfully() {
            // Given
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

            // When
            UserRole result = roleService.assignRoleToUser(
                    testUser.getId(), userRole.getId(), 
                    "admin", "临时分配", expiresAt);

            // Then
            assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
            
            // 验证数据库中的数据
            UserRole foundUserRole = userRoleRepository.findById(result.getId()).orElse(null);
            assertThat(foundUserRole).isNotNull();
            assertThat(foundUserRole.getExpiresAt()).isEqualTo(expiresAt);
        }
    }

    @Nested
    @DisplayName("权限继承集成测试")
    class PermissionInheritanceIntegrationTest {

        @Test
        @DisplayName("应该正确获取用户通过角色继承的权限")
        void should_GetInheritedPermissions_Successfully() {
            // Given
            UserRole assignment = roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "测试", null);
            // 刷新以确保数据一致性
            roleService.clearUserRoleCache(testUser.getId());

            // When
            Set<Permission> result = roleService.getUserEffectivePermissions(testUser.getId());

            // Then
            assertThat(result).hasSize(3); // fileRead, fileWrite, systemAdmin
            assertThat(result).extracting("code")
                    .containsExactlyInAnyOrder("file.read", "file.write", "system.admin");
        }

        @Test
        @DisplayName("应该正确获取用户有效权限代码")
        void should_GetEffectivePermissionCodes_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "测试", null);
            // 刷新以确保数据一致性
            roleService.clearUserRoleCache(testUser.getId());

            // When
            Set<String> result = roleService.getUserEffectivePermissionCodes(testUser.getId());

            // Then
            assertThat(result).contains("file.read");
            assertThat(result).doesNotContain("file.write", "system.admin");
        }

        @Test
        @DisplayName("应该通过角色正确检查权限")
        void should_CheckPermissionThroughRoles_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "测试", null);
            // 刷新以确保数据一致性
            roleService.clearUserRoleCache(testUser.getId());

            // When & Then
            assertThat(roleService.hasPermissionThroughRoles(testUser.getId(), "file.read")).isTrue();
            assertThat(roleService.hasPermissionThroughRoles(testUser.getId(), "file.write")).isTrue();
            assertThat(roleService.hasPermissionThroughRoles(testUser.getId(), "system.admin")).isTrue();
            assertThat(roleService.hasPermissionThroughRoles(testUser.getId(), "nonexistent.permission")).isFalse();
        }

        @Test
        @DisplayName("应该按类型和类别正确检查权限")
        void should_CheckPermissionByTypeAndCategory_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "测试", null);
            // 刷新以确保数据一致性
            roleService.clearUserRoleCache(testUser.getId());

            // When & Then
            assertThat(roleService.hasPermissionByTypeAndCategory(testUser.getId(), "FILE", "READ")).isTrue();
            assertThat(roleService.hasPermissionByTypeAndCategory(testUser.getId(), "FILE", "WRITE")).isFalse();
            assertThat(roleService.hasPermissionByTypeAndCategory(testUser.getId(), "SYSTEM", "ADMIN")).isFalse();
        }
    }

    @Nested
    @DisplayName("权限覆盖逻辑集成测试")
    class PermissionOverrideIntegrationTest {

        @Test
        @DisplayName("应该正确处理权限冲突和覆盖")
        void should_HandlePermissionOverride_Successfully() {
            // Given
            // 创建一个具有更高级别读权限的角色
            Permission highLevelReadPermission = new Permission("file.read.advanced", "高级文件读取", "FILE", "READ", 30);
            highLevelReadPermission = permissionRepository.save(highLevelReadPermission);
            
            Role premiumRole = new Role("PREMIUM_USER", "高级用户", Role.RoleType.CUSTOM);
            premiumRole.addPermission(highLevelReadPermission);
            premiumRole = roleRepository.save(premiumRole);

            // 分配两个角色，它们都有READ权限但级别不同
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "基础角色", null);
            roleService.assignRoleToUser(testUser.getId(), premiumRole.getId(), "admin", "高级角色", null);

            // When
            Set<Permission> effectivePermissions = roleService.getUserEffectivePermissions(testUser.getId());
            Integer highestLevel = roleService.getHighestPermissionLevel(testUser.getId(), "FILE");

            // Then
            assertThat(effectivePermissions).hasSize(2); // file.read 和 file.read.advanced
            assertThat(highestLevel).isEqualTo(30); // 应该是最高级别的权限
        }

        @Test
        @DisplayName("应该正确计算最高权限级别")
        void should_CalculateHighestPermissionLevel_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "管理员", null);

            // When
            Integer filePermissionLevel = roleService.getHighestPermissionLevel(testUser.getId(), "FILE");
            Integer systemPermissionLevel = roleService.getHighestPermissionLevel(testUser.getId(), "SYSTEM");

            // Then
            assertThat(filePermissionLevel).isEqualTo(20); // fileWrite的级别
            assertThat(systemPermissionLevel).isEqualTo(100); // systemAdmin的级别
        }
    }

    @Nested
    @DisplayName("动态权限检查集成测试")
    class DynamicPermissionCheckIntegrationTest {

        @Test
        @DisplayName("应该正确进行动态权限检查")
        void should_PerformDynamicPermissionCheck_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "管理员", null);
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "用户", 
                    LocalDateTime.now().plusDays(7)); // 7天后过期

            // When
            Map<String, Object> result = roleService.checkUserRolePermissions(testUser.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userId")).isEqualTo(testUser.getId());
            assertThat(result.get("totalRoles")).isEqualTo(2);
            assertThat(result.get("validRoles")).isEqualTo(2);
            assertThat(result.get("expiringSoonCount")).isEqualTo(1);
            assertThat(result.get("hasAdminPermission")).isEqualTo(true);
            assertThat(result).containsKey("permissionsByResourceType");
        }

        @Test
        @DisplayName("应该正确检查管理员权限")
        void should_CheckAdminPermission_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "管理员", null);

            // When
            boolean isAdmin = roleService.hasAdminPermission(testUser.getId());

            // Then
            assertThat(isAdmin).isTrue();
        }

        @Test
        @DisplayName("应该正确检查非管理员用户")
        void should_CheckNonAdminUser_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "普通用户", null);

            // When
            boolean isAdmin = roleService.hasAdminPermission(testUser.getId());

            // Then
            assertThat(isAdmin).isFalse();
        }
    }

    @Nested
    @DisplayName("角色层级管理集成测试")
    class RoleHierarchyIntegrationTest {

        @Test
        @DisplayName("应该正确获取角色层级结构")
        void should_GetRoleHierarchy_Successfully() {
            // When
            Map<Role.RoleType, List<Role>> hierarchy = roleService.getRoleHierarchy();

            // Then
            assertThat(hierarchy).isNotEmpty();
            assertThat(hierarchy.get(Role.RoleType.SYSTEM_ADMIN)).hasSize(1);
            assertThat(hierarchy.get(Role.RoleType.USER)).hasSize(1);
            assertThat(hierarchy.get(Role.RoleType.SYSTEM_ADMIN).get(0).getCode()).isEqualTo("ADMIN");
            assertThat(hierarchy.get(Role.RoleType.USER).get(0).getCode()).isEqualTo("USER");
        }

        @Test
        @DisplayName("应该正确检查角色分配权限")
        void should_CheckRoleAssignmentPermission_Successfully() {
            // When & Then
            assertThat(roleService.canAssignRoleToUser(testUser.getId(), userRole.getId())).isTrue();
            assertThat(roleService.canAssignRoleToUser(testUser.getId(), adminRole.getId())).isTrue();
            
            // 禁用角色后应该不能分配
            roleService.disableRole(userRole.getId());
            // 清除缓存以确保获取最新状态
            roleService.clearAllRoleCache();
            assertThat(roleService.canAssignRoleToUser(testUser.getId(), userRole.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("维护操作集成测试")
    class MaintenanceOperationsIntegrationTest {

        @Test
        @DisplayName("应该正确更新过期用户角色")
        @Transactional
        @Rollback
        void should_UpdateExpiredUserRoles_Successfully() {
            // Given
            LocalDateTime expireTime = LocalDateTime.now().minusDays(1);
            
            // 创建一个已过期的用户角色
            UserRole expiredUserRole = roleService.assignRoleToUser(
                    testUser.getId(), userRole.getId(), 
                    "admin", "过期测试", expireTime);
            
            // 强制刷新到数据库，确保数据一致性
            userRoleRepository.flush();
            // 清除持久化上下文，强制从数据库重新查询
            entityManager.clear();

            // 验证数据是否正确设置
            UserRole beforeUpdate = userRoleRepository.findById(expiredUserRole.getId()).orElse(null);
            assertThat(beforeUpdate).isNotNull();
            assertThat(beforeUpdate.getStatus()).isEqualTo(UserRole.AssignmentStatus.ACTIVE);
            assertThat(beforeUpdate.getExpiresAt()).isNotNull();
            assertThat(beforeUpdate.getExpiresAt()).isBefore(LocalDateTime.now());

            // When
            int updated = roleService.updateExpiredUserRoles();

            // Then
            assertThat(updated).isEqualTo(1); // 应该更新1条记录
            
            // 强制刷新，确保UPDATE操作已提交
            userRoleRepository.flush();
            entityManager.clear();
            
            // 验证状态已更新
            UserRole foundUserRole = userRoleRepository.findById(expiredUserRole.getId()).orElse(null);
            assertThat(foundUserRole).isNotNull();
            assertThat(foundUserRole.getStatus()).isEqualTo(UserRole.AssignmentStatus.EXPIRED);
        }

        @Test
        @DisplayName("应该正确激活待生效用户角色")
        @Transactional
        @Rollback
        void should_ActivatePendingUserRoles_Successfully() {
            // Given - 创建一个简单的待生效用户角色
            LocalDateTime effectiveTime = LocalDateTime.now().minusMinutes(5);
            
            UserRole pendingUserRole = new UserRole(testUser.getId(), userRole.getId());
            pendingUserRole.setStatus(UserRole.AssignmentStatus.PENDING);
            pendingUserRole.setEffectiveAt(effectiveTime);
            pendingUserRole.setAssignedBy("test_admin");
            pendingUserRole = userRoleRepository.save(pendingUserRole);
            userRoleRepository.flush();
            entityManager.clear();

            // 首先验证数据是否正确保存
            UserRole beforeActivation = userRoleRepository.findById(pendingUserRole.getId()).orElse(null);
            assertThat(beforeActivation).isNotNull();
            assertThat(beforeActivation.getStatus()).isEqualTo(UserRole.AssignmentStatus.PENDING);
            assertThat(beforeActivation.getEffectiveAt()).isBefore(LocalDateTime.now());

            // When - 执行激活操作
            int activated = roleService.activatePendingUserRoles();

            // Then - 验证结果
            assertThat(activated).isEqualTo(1); // 应该激活1个记录
            
            // 强制刷新，确保UPDATE操作已提交
            userRoleRepository.flush();
            entityManager.clear();
            
            // 验证状态已更新
            UserRole updatedUserRole = userRoleRepository.findById(pendingUserRole.getId()).orElse(null);
            assertThat(updatedUserRole).isNotNull();
            assertThat(updatedUserRole.getStatus()).isEqualTo(UserRole.AssignmentStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("统计和报告集成测试")
    class StatisticsIntegrationTest {

        @Test
        @DisplayName("应该正确获取角色统计信息")
        void should_GetRoleStatistics_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "测试", null);
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "测试", null);

            // When
            Map<String, Object> stats = roleService.getRoleStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.get("totalRoles")).isEqualTo(2L);
            assertThat(stats.get("enabledRoles")).isEqualTo(2L);
            assertThat(stats.get("systemRoles")).isEqualTo(1L);
            assertThat(stats.get("customRoles")).isEqualTo(1L);
            assertThat(stats).containsKey("rolesByType");
            assertThat(stats).containsKey("userRolesByStatus");
        }

        @Test
        @DisplayName("应该正确获取用户角色信息")
        void should_GetUserRoleInfo_Successfully() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), adminRole.getId(), "admin", "管理员权限", null);
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "用户权限", 
                    LocalDateTime.now().plusDays(30));

            // When
            Map<String, Object> info = roleService.getUserRoleInfo(testUser.getId());

            // Then
            assertThat(info).isNotNull();
            assertThat(info.get("userId")).isEqualTo(testUser.getId());
            assertThat(info.get("totalRoles")).isEqualTo(2);
            assertThat(info.get("validRoles")).isEqualTo(2);
            assertThat(info).containsKey("roleDetails");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roleDetails = (List<Map<String, Object>>) info.get("roleDetails");
            assertThat(roleDetails).hasSize(2);
        }
    }

    @Nested
    @DisplayName("边界条件和错误处理集成测试")
    class EdgeCasesIntegrationTest {

        @Test
        @DisplayName("应该正确处理不存在的用户")
        void should_HandleNonExistentUser_Properly() {
            // When & Then
            assertThatThrownBy(() -> roleService.assignRoleToUser(999L, userRole.getId(), "admin", "测试", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("应该正确处理不存在的角色")
        void should_HandleNonExistentRole_Properly() {
            // When & Then
            assertThatThrownBy(() -> roleService.assignRoleToUser(testUser.getId(), 999L, "admin", "测试", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("角色不存在");
        }

        @Test
        @DisplayName("应该防止删除有用户关联的角色")
        void should_PreventDeletingRoleWithUsers_InDatabase() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "测试", null);

            // When & Then
            assertThatThrownBy(() -> roleService.deleteRole(userRole.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("角色仍有用户关联");
        }

        @Test
        @DisplayName("应该防止重复分配相同角色")
        void should_PreventDuplicateRoleAssignment_InDatabase() {
            // Given
            roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "第一次分配", null);

            // When & Then
            assertThatThrownBy(() -> roleService.assignRoleToUser(testUser.getId(), userRole.getId(), "admin", "重复分配", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("用户已拥有该角色");
        }
    }
} 