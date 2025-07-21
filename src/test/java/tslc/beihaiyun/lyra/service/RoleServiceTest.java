package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.PermissionRepository;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoleService单元测试类
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色管理服务测试")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private User testUser;
    private Permission testPermission;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testRole = new Role("TEST_ROLE", "测试角色", Role.RoleType.CUSTOM);
        testRole.setId(1L);
        testRole.setEnabled(true);
        testRole.setSystem(false);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testPermission = new Permission("file.read", "文件读取", "FILE", "READ", 10);
        testPermission.setId(1L);
        testPermission.setIsEnabled(true);

        testUserRole = new UserRole(1L, 1L);
        testUserRole.setId(1L);
        testUserRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
        testUserRole.setEffectiveAt(LocalDateTime.now().minusDays(1));
    }

    @Nested
    @DisplayName("角色基础CRUD操作")
    class RoleBasicOperationsTest {

        @Test
        @DisplayName("应该成功创建角色")
        void should_CreateRole_Successfully() {
            // Given
            // 确保testRole的sortOrder为null，以便测试设置逻辑
            testRole.setSortOrder(null);
            when(roleRepository.existsByCode(testRole.getCode())).thenReturn(false);
            when(roleRepository.existsByName(testRole.getName())).thenReturn(false);
            when(roleRepository.findMaxSortOrder()).thenReturn(5);
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
                Role role = invocation.getArgument(0);
                role.setId(1L); // 模拟数据库分配ID
                return role; // 返回传入的角色对象（已被服务层修改）
            });

            // When
            Role result = roleService.createRole(testRole);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(testRole.getCode());
            assertThat(result.getSortOrder()).isNotNull(); // 验证sortOrder已设置
            assertThat(result.getSortOrder()).isEqualTo(6); // 验证sortOrder = maxOrder + 1
            assertThat(result.getId()).isEqualTo(1L); // 验证ID已设置
            verify(roleRepository).save(testRole);
        }

        @Test
        @DisplayName("应该在角色代码重复时抛出异常")
        void should_ThrowException_WhenRoleCodeExists() {
            // Given
            when(roleRepository.existsByCode(testRole.getCode())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> roleService.createRole(testRole))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("角色代码已存在");
        }

        @Test
        @DisplayName("应该成功更新角色")
        void should_UpdateRole_Successfully() {
            // Given
            Role updateRole = new Role("UPDATED_ROLE", "更新角色", Role.RoleType.CUSTOM);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.existsByCodeAndIdNot(updateRole.getCode(), 1L)).thenReturn(false);
            when(roleRepository.existsByNameAndIdNot(updateRole.getName(), 1L)).thenReturn(false);
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            // When
            Role result = roleService.updateRole(1L, updateRole);

            // Then
            assertThat(result).isNotNull();
            verify(roleRepository).save(testRole);
        }

        @Test
        @DisplayName("应该防止系统角色的关键信息被修改")
        void should_PreventSystemRoleModification() {
            // Given
            testRole.setSystem(true);
            Role updateRole = new Role("NEW_CODE", "新名称", Role.RoleType.CUSTOM);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When & Then
            assertThatThrownBy(() -> roleService.updateRole(1L, updateRole))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("系统角色的代码和名称不能修改");
        }

        @Test
        @DisplayName("应该成功获取角色")
        void should_GetRole_Successfully() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When
            Role result = roleService.getRoleById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("应该在角色不存在时抛出异常")
        void should_ThrowException_WhenRoleNotFound() {
            // Given
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roleService.getRoleById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("角色不存在");
        }

        @Test
        @DisplayName("应该成功删除角色")
        void should_DeleteRole_Successfully() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.countByRoleId(1L)).thenReturn(0L);
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            // When
            roleService.deleteRole(1L);

            // Then
            verify(roleRepository).save(argThat(role -> role.isDeleted()));
        }

        @Test
        @DisplayName("应该防止删除有用户关联的角色")
        void should_PreventDeletingRoleWithUsers() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.countByRoleId(1L)).thenReturn(5L);

            // When & Then
            assertThatThrownBy(() -> roleService.deleteRole(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("角色仍有用户关联");
        }

        @Test
        @DisplayName("应该防止删除系统角色")
        void should_PreventDeletingSystemRole() {
            // Given
            testRole.setSystem(true);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When & Then
            assertThatThrownBy(() -> roleService.deleteRole(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("系统角色不能删除");
        }
    }

    @Nested
    @DisplayName("角色分配管理")
    class RoleAssignmentTest {

        @Test
        @DisplayName("应该成功为用户分配角色")
        void should_AssignRoleToUser_Successfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.findByUserIdAndRoleId(1L, 1L)).thenReturn(Optional.empty());
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

            // When
            UserRole result = roleService.assignRoleToUser(1L, 1L, "admin", "测试分配", null);

            // Then
            assertThat(result).isNotNull();
            verify(userRoleRepository).save(any(UserRole.class));
            verify(permissionService).clearUserPermissionCache(1L);
        }

        @Test
        @DisplayName("应该在角色不可用时抛出异常")
        void should_ThrowException_WhenRoleNotAvailable() {
            // Given
            testRole.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When & Then
            assertThatThrownBy(() -> roleService.assignRoleToUser(1L, 1L, "admin", "测试", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("角色不可用");
        }

        @Test
        @DisplayName("应该在重复分配时抛出异常")
        void should_ThrowException_WhenRoleAlreadyAssigned() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.findByUserIdAndRoleId(1L, 1L))
                    .thenReturn(Optional.of(testUserRole));

            // When & Then
            assertThatThrownBy(() -> roleService.assignRoleToUser(1L, 1L, "admin", "测试", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("用户已拥有该角色");
        }

        @Test
        @DisplayName("应该成功撤销用户角色")
        void should_RevokeRoleFromUser_Successfully() {
            // Given
            when(userRoleRepository.findByUserIdAndRoleId(1L, 1L))
                    .thenReturn(Optional.of(testUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

            // When
            roleService.revokeRoleFromUser(1L, 1L, "admin", "测试撤销");

            // Then
            verify(userRoleRepository).save(argThat(ur -> 
                    UserRole.AssignmentStatus.REVOKED.equals(ur.getStatus())));
            verify(permissionService).clearUserPermissionCache(1L);
        }

        @Test
        @DisplayName("应该批量为用户分配角色")
        void should_AssignMultipleRolesToUser_Successfully() {
            // Given
            List<Long> roleIds = Arrays.asList(1L, 2L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(anyLong())).thenReturn(Optional.of(testRole));
            when(userRoleRepository.findByUserIdAndRoleId(anyLong(), anyLong()))
                    .thenReturn(Optional.empty());
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

            // When
            List<UserRole> results = roleService.assignRolesToUser(1L, roleIds, "admin", "批量分配");

            // Then
            assertThat(results).hasSize(2);
            verify(userRoleRepository, times(2)).save(any(UserRole.class));
        }
    }

    @Nested
    @DisplayName("权限继承策略")
    class PermissionInheritanceTest {

        @Test
        @DisplayName("应该正确获取用户有效权限")
        void should_GetUserEffectivePermissions_Successfully() {
            // Given
            testRole.addPermission(testPermission);
            testUserRole.setRole(testRole);
            
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            Set<Permission> result = roleService.getUserEffectivePermissions(1L);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains(testPermission);
        }

        @Test
        @DisplayName("应该正确获取用户有效权限代码")
        void should_GetUserEffectivePermissionCodes_Successfully() {
            // Given
            testRole.addPermission(testPermission);
            testUserRole.setRole(testRole);
            
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            Set<String> result = roleService.getUserEffectivePermissionCodes(1L);

            // Then
            assertThat(result).contains("file.read");
        }

        @Test
        @DisplayName("应该通过角色检查权限")
        void should_CheckPermissionThroughRoles_Successfully() {
            // Given
            testRole.addPermission(testPermission);
            testUserRole.setRole(testRole);
            
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            boolean result = roleService.hasPermissionThroughRoles(1L, "file.read");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该按类型和类别检查权限")
        void should_CheckPermissionByTypeAndCategory_Successfully() {
            // Given
            testRole.addPermission(testPermission);
            testUserRole.setRole(testRole);
            
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            boolean result = roleService.hasPermissionByTypeAndCategory(1L, "FILE", "READ");

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("权限覆盖逻辑")
    class PermissionOverrideTest {

        @Test
        @DisplayName("应该正确解析权限冲突")
        void should_ResolvePermissionConflicts_Successfully() {
            // Given - 使用不同的权限代码来测试权限合并
            Permission readPermission = new Permission("file.read", "文件读取", "FILE", "READ", 10);
            Permission writePermission = new Permission("file.write", "文件写入", "FILE", "WRITE", 20);
            
            Set<Permission> permissions = new HashSet<>(Arrays.asList(readPermission, writePermission));

            // When
            Map<String, Permission> result = roleService.resolvePermissionConflicts(permissions);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("file.read")).isNotNull();
            assertThat(result.get("file.write")).isNotNull();
            assertThat(result.get("file.read").getLevel()).isEqualTo(10);
            assertThat(result.get("file.write").getLevel()).isEqualTo(20);
        }

        @Test
        @DisplayName("应该正确计算最高权限级别")
        void should_CalculateHighestPermissionLevel_Successfully() {
            // Given
            Permission permission1 = new Permission("file.read", "文件读取", "FILE", "READ", 10);
            Permission permission2 = new Permission("file.write", "文件写入", "FILE", "WRITE", 20);
            
            testRole.addPermission(permission1);
            testRole.addPermission(permission2);
            testUserRole.setRole(testRole);
            
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            Integer result = roleService.getHighestPermissionLevel(1L, "FILE");

            // Then
            assertThat(result).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("角色权限动态检查")
    class DynamicPermissionCheckTest {

        @Test
        @DisplayName("应该正确检查用户角色权限")
        void should_CheckUserRolePermissions_Successfully() {
            // Given
            when(userRoleRepository.findByUserId(1L))
                    .thenReturn(Arrays.asList(testUserRole));
            when(userRoleRepository.findValidUserRoles(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));
            when(userRoleRepository.findValidUserRolesWithRole(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));

            // When
            Map<String, Object> result = roleService.checkUserRolePermissions(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userId")).isEqualTo(1L);
            assertThat(result.get("totalRoles")).isEqualTo(1);
            assertThat(result.get("validRoles")).isEqualTo(1);
            assertThat(result).containsKey("checkTime");
        }

        @Test
        @DisplayName("应该正确检查管理员权限")
        void should_CheckAdminPermission_Successfully() {
            // Given
            Role adminRole = new Role("ADMIN", "管理员", Role.RoleType.SYSTEM_ADMIN);
            adminRole.setId(2L);
            testUserRole.setRoleId(2L);
            
            when(userRoleRepository.findValidUserRoles(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

            // When
            boolean result = roleService.hasAdminPermission(1L);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("角色层级管理")
    class RoleHierarchyTest {

        @Test
        @DisplayName("应该正确获取角色层级结构")
        void should_GetRoleHierarchy_Successfully() {
            // Given
            Role adminRole = new Role("ADMIN", "管理员", Role.RoleType.SYSTEM_ADMIN);
            Role userRole = new Role("USER", "用户", Role.RoleType.USER);
            
            when(roleRepository.findByEnabledTrueOrderBySortOrderAsc())
                    .thenReturn(Arrays.asList(adminRole, userRole));

            // When
            Map<Role.RoleType, List<Role>> result = roleService.getRoleHierarchy();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(Role.RoleType.SYSTEM_ADMIN)).contains(adminRole);
            assertThat(result.get(Role.RoleType.USER)).contains(userRole);
        }

        @Test
        @DisplayName("应该正确检查角色分配权限")
        void should_CheckCanAssignRoleToUser_Successfully() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(userRoleRepository.findValidUserRoles(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList());

            // When
            boolean result = roleService.canAssignRoleToUser(1L, 1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该在角色不可用时拒绝分配")
        void should_RejectAssignmentWhenRoleNotAvailable() {
            // Given
            testRole.setEnabled(false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When
            boolean result = roleService.canAssignRoleToUser(1L, 1L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("角色状态管理")
    class RoleStatusManagementTest {

        @Test
        @DisplayName("应该成功启用角色")
        void should_EnableRole_Successfully() {
            // Given
            testRole.setEnabled(false);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            // When
            roleService.enableRole(1L);

            // Then
            verify(roleRepository).save(argThat(role -> role.getEnabled()));
        }

        @Test
        @DisplayName("应该成功禁用角色")
        void should_DisableRole_Successfully() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenReturn(testRole);

            // When
            roleService.disableRole(1L);

            // Then
            verify(roleRepository).save(argThat(role -> !role.getEnabled()));
        }

        @Test
        @DisplayName("应该防止禁用系统角色")
        void should_PreventDisablingSystemRole() {
            // Given
            testRole.setSystem(true);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When & Then
            assertThatThrownBy(() -> roleService.disableRole(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("系统角色不能禁用");
        }

        @Test
        @DisplayName("应该成功批量更新角色状态")
        void should_BatchUpdateRoleStatus_Successfully() {
            // Given
            List<Long> roleIds = Arrays.asList(1L, 2L);
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(testRole));
            when(roleRepository.updateEnabledByIds(anyList(), eq(true))).thenReturn(2);

            // When
            int result = roleService.batchUpdateRoleStatus(roleIds, true);

            // Then
            assertThat(result).isEqualTo(2);
            verify(roleRepository).updateEnabledByIds(roleIds, true);
        }
    }

    @Nested
    @DisplayName("定时任务和维护操作")
    class MaintenanceOperationsTest {

        @Test
        @DisplayName("应该成功更新过期用户角色")
        void should_UpdateExpiredUserRoles_Successfully() {
            // Given
            when(userRoleRepository.updateExpiredUserRoles(
                any(LocalDateTime.class),
                any(UserRole.AssignmentStatus.class),
                any(UserRole.AssignmentStatus.class))).thenReturn(5);

            // When
            int result = roleService.updateExpiredUserRoles();

            // Then
            assertThat(result).isEqualTo(5);
            verify(userRoleRepository).updateExpiredUserRoles(
                any(LocalDateTime.class),
                eq(UserRole.AssignmentStatus.ACTIVE),
                eq(UserRole.AssignmentStatus.EXPIRED));
        }

        @Test
        @DisplayName("应该成功激活待生效用户角色")
        void should_ActivatePendingUserRoles_Successfully() {
            // Given
            when(userRoleRepository.activatePendingUserRoles(
                any(LocalDateTime.class),
                any(UserRole.AssignmentStatus.class),
                any(UserRole.AssignmentStatus.class))).thenReturn(3);

            // When
            int result = roleService.activatePendingUserRoles();

            // Then
            assertThat(result).isEqualTo(3);
            verify(userRoleRepository).activatePendingUserRoles(
                any(LocalDateTime.class),
                eq(UserRole.AssignmentStatus.PENDING),
                eq(UserRole.AssignmentStatus.ACTIVE));
        }
    }

    @Nested
    @DisplayName("查询和搜索功能")
    class QueryAndSearchTest {

        @Test
        @DisplayName("应该成功获取启用的角色")
        void should_GetEnabledRoles_Successfully() {
            // Given
            when(roleRepository.findByEnabledTrueOrderBySortOrderAsc())
                    .thenReturn(Arrays.asList(testRole));

            // When
            List<Role> result = roleService.getEnabledRoles();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testRole);
        }

        @Test
        @DisplayName("应该成功分页查询角色")
        void should_GetRolesWithPagination_Successfully() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Role> rolePage = new PageImpl<>(Arrays.asList(testRole), pageable, 1);
            when(roleRepository.findAll(pageable)).thenReturn(rolePage);

            // When
            Page<Role> result = roleService.getRoles(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该成功搜索角色")
        void should_SearchRoles_Successfully() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Role> rolePage = new PageImpl<>(Arrays.asList(testRole), pageable, 1);
            when(roleRepository.searchRoles("测试", pageable)).thenReturn(rolePage);

            // When
            Page<Role> result = roleService.searchRoles("测试", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(roleRepository).searchRoles("测试", pageable);
        }
    }

    @Nested
    @DisplayName("统计和报告")
    class StatisticsAndReportsTest {

        @Test
        @DisplayName("应该成功获取角色统计信息")
        void should_GetRoleStatistics_Successfully() {
            // Given
            when(roleRepository.count()).thenReturn(10L);
            when(roleRepository.countByEnabledTrue()).thenReturn(8L);
            when(roleRepository.countBySystemTrue()).thenReturn(3L);
            when(roleRepository.countBySystemFalse()).thenReturn(7L);
            when(roleRepository.countByType()).thenReturn(Arrays.asList(
                    new Object[]{Role.RoleType.SYSTEM_ADMIN, 2L},
                    new Object[]{Role.RoleType.USER, 5L}
            ));
            when(userRoleRepository.countByStatus()).thenReturn(Arrays.asList(
                    new Object[]{UserRole.AssignmentStatus.ACTIVE, 15L},
                    new Object[]{UserRole.AssignmentStatus.SUSPENDED, 2L}
            ));

            // When
            Map<String, Object> result = roleService.getRoleStatistics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("totalRoles")).isEqualTo(10L);
            assertThat(result.get("enabledRoles")).isEqualTo(8L);
            assertThat(result).containsKey("rolesByType");
            assertThat(result).containsKey("userRolesByStatus");
        }

        @Test
        @DisplayName("应该成功获取用户角色信息")
        void should_GetUserRoleInfo_Successfully() {
            // Given
            when(userRoleRepository.findByUserId(1L)).thenReturn(Arrays.asList(testUserRole));
            when(userRoleRepository.findValidUserRoles(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(testUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When
            Map<String, Object> result = roleService.getUserRoleInfo(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("userId")).isEqualTo(1L);
            assertThat(result.get("totalRoles")).isEqualTo(1);
            assertThat(result.get("validRoles")).isEqualTo(1);
            assertThat(result).containsKey("roleDetails");
        }
    }
} 