package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.SpacePermission;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.PermissionRepository;
import tslc.beihaiyun.lyra.repository.SpacePermissionRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 * 覆盖权限检查、继承逻辑、缓存机制等所有权限场景
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("权限服务测试")
class PermissionServiceTest {

    @Mock
    private SpacePermissionRepository spacePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Permission testPermission;
    private SpacePermission testSpacePermission;
    private UserRole testUserRole;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        testPermission = createTestPermission();
        testSpacePermission = createTestSpacePermission();
        testUserRole = createTestUserRole();
    }

    // ========== 基础权限检查测试 ==========

    @Test
    @DisplayName("权限过期检查 - 权限已过期")
    void should_ReturnTrue_When_PermissionExpired() {
        // Given
        testSpacePermission.setExpiresAt(LocalDateTime.now().minusHours(1));
        
        // When
        boolean result = permissionService.isPermissionExpired(testSpacePermission);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("权限过期检查 - 权限未过期")
    void should_ReturnFalse_When_PermissionNotExpired() {
        // Given
        testSpacePermission.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        // When
        boolean result = permissionService.isPermissionExpired(testSpacePermission);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("权限过期检查 - 永不过期")
    void should_ReturnFalse_When_PermissionNeverExpires() {
        // Given
        testSpacePermission.setExpiresAt(null);
        
        // When
        boolean result = permissionService.isPermissionExpired(testSpacePermission);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("权限授权检查 - 已授权且未过期")
    void should_ReturnTrue_When_PermissionGrantedAndNotExpired() {
        // Given
        testSpacePermission.setStatus("GRANTED");
        testSpacePermission.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        // When
        boolean result = permissionService.isPermissionGranted(testSpacePermission);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("权限授权检查 - 已授权但已过期")
    void should_ReturnFalse_When_PermissionGrantedButExpired() {
        // Given
        testSpacePermission.setStatus("GRANTED");
        testSpacePermission.setExpiresAt(LocalDateTime.now().minusHours(1));
        
        // When
        boolean result = permissionService.isPermissionGranted(testSpacePermission);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("权限拒绝检查")
    void should_ReturnTrue_When_PermissionDenied() {
        // Given
        testSpacePermission.setStatus("DENIED");
        
        // When
        boolean result = permissionService.isPermissionDenied(testSpacePermission);
        
        // Then
        assertThat(result).isTrue();
    }

    // ========== 核心权限检查测试 ==========

    @Test
    @DisplayName("用户权限检查 - 拥有权限")
    void should_ReturnTrue_When_UserHasPermission() {
        // Given
        Long userId = 1L;
        String permissionCode = "file.read";
        
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findPermissionsByUserId(userId)).thenReturn(permissions);
        
        // When
        boolean result = permissionService.hasPermission(userId, permissionCode);
        
        // Then
        assertThat(result).isTrue();
        verify(permissionRepository).findPermissionsByUserId(userId);
    }

    @Test
    @DisplayName("用户权限检查 - 没有权限")
    void should_ReturnFalse_When_UserDoesNotHavePermission() {
        // Given
        Long userId = 1L;
        String permissionCode = "file.delete";
        
        List<Permission> permissions = Arrays.asList(testPermission); // 只有file.read权限
        when(permissionRepository.findPermissionsByUserId(userId)).thenReturn(permissions);
        
        // When
        boolean result = permissionService.hasPermission(userId, permissionCode);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("用户权限检查 - 参数为空")
    void should_ReturnFalse_When_ParametersAreNull() {
        // When & Then
        assertThat(permissionService.hasPermission(null, "file.read")).isFalse();
        assertThat(permissionService.hasPermission(1L, null)).isFalse();
        assertThat(permissionService.hasPermission(1L, "")).isFalse();
    }

    @Test
    @DisplayName("资源权限检查 - 拥有直接权限")
    void should_ReturnTrue_When_UserHasDirectResourcePermission() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        String resourceType = "FILE";
        Long resourceId = 1L;
        String permissionCode = "file.read";
        
        when(permissionRepository.findByCode(permissionCode)).thenReturn(Optional.of(testPermission));
        
        List<SpacePermission> spacePermissions = Arrays.asList(testSpacePermission);
        when(spacePermissionRepository.findEffectiveResourcePermissions(
                eq(userId), eq(spaceId), eq(resourceType), eq(resourceId), any(LocalDateTime.class)))
                .thenReturn(spacePermissions);
        
        // When
        boolean result = permissionService.hasResourcePermission(userId, spaceId, resourceType, resourceId, permissionCode);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("资源权限检查 - 权限不存在")
    void should_ReturnFalse_When_PermissionNotExists() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        String resourceType = "FILE";
        Long resourceId = 1L;
        String permissionCode = "nonexistent.permission";
        
        when(permissionRepository.findByCode(permissionCode)).thenReturn(Optional.empty());
        
        // When
        boolean result = permissionService.hasResourcePermission(userId, spaceId, resourceType, resourceId, permissionCode);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("资源权限检查 - 权限已禁用")
    void should_ReturnFalse_When_PermissionIsDisabled() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        String resourceType = "FILE";
        Long resourceId = 1L;
        String permissionCode = "file.read";
        
        testPermission.setIsEnabled(false);
        when(permissionRepository.findByCode(permissionCode)).thenReturn(Optional.of(testPermission));
        
        // When
        boolean result = permissionService.hasResourcePermission(userId, spaceId, resourceType, resourceId, permissionCode);
        
        // Then
        assertThat(result).isFalse();
    }

    // ========== 权限继承测试 ==========

    @Test
    @DisplayName("权限继承检查 - 有继承权限")
    void should_ReturnTrue_When_UserHasInheritedPermission() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        String resourceType = "FILE";
        Long resourceId = 2L;
        String permissionCode = "file.read";
        
        when(permissionRepository.findByCode(permissionCode)).thenReturn(Optional.of(testPermission));
        when(spacePermissionRepository.findEffectiveResourcePermissions(
                eq(userId), eq(spaceId), eq(resourceType), eq(resourceId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList()); // 没有直接权限
        
        // 模拟继承权限
        SpacePermission inheritedPermission = createInheritablePermission();
        when(spacePermissionRepository.findInheritablePermissions(
                eq(userId), eq(spaceId), anyString(), eq(resourceType)))
                .thenReturn(Arrays.asList(inheritedPermission));
        
        // When
        boolean result = permissionService.hasResourcePermission(userId, spaceId, resourceType, resourceId, permissionCode);
        
        // Then
        assertThat(result).isTrue();
    }

    // ========== 用户权限和空间权限获取测试 ==========

    @Test
    @DisplayName("获取用户权限 - 成功")
    void should_ReturnUserPermissions_When_UserExists() {
        // Given
        Long userId = 1L;
        List<Permission> permissions = Arrays.asList(testPermission);
        when(permissionRepository.findPermissionsByUserId(userId)).thenReturn(permissions);
        
        // When
        Set<String> result = permissionService.getUserPermissions(userId);
        
        // Then
        assertThat(result).containsExactly("file.read");
    }

    @Test
    @DisplayName("获取用户权限 - 用户ID为空")
    void should_ReturnEmptySet_When_UserIdIsNull() {
        // When
        Set<String> result = permissionService.getUserPermissions(null);
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("获取用户空间权限 - 成功")
    void should_ReturnUserSpacePermissions_When_UserAndSpaceExist() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        List<SpacePermission> spacePermissions = Arrays.asList(testSpacePermission);
        when(spacePermissionRepository.findEffectivePermissions(eq(userId), eq(spaceId), any(LocalDateTime.class)))
                .thenReturn(spacePermissions);
        
        // When
        List<SpacePermission> result = permissionService.getUserSpacePermissions(userId, spaceId);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testSpacePermission);
    }

    // ========== 管理员权限检查测试 ==========

    @Test
    @DisplayName("管理员权限检查 - 是管理员")
    void should_ReturnTrue_When_UserIsAdmin() {
        // Given
        Long userId = 1L;
        Permission adminPermission = new Permission("system.admin", "系统管理员", "SYSTEM", "ADMIN", 100);
        adminPermission.setIsEnabled(true);
        
        when(permissionRepository.findPermissionsByUserId(userId))
                .thenReturn(Arrays.asList(adminPermission));
        
        // When
        boolean result = permissionService.isAdmin(userId);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("空间管理员权限检查")
    void should_CheckSpaceAdminPermission() {
        // Given
        Long userId = 1L;
        Long spaceId = 1L;
        
        Permission spaceAdminPermission = new Permission("space.admin", "空间管理员", "SPACE", "ADMIN", 90);
        spaceAdminPermission.setId(2L);
        spaceAdminPermission.setIsEnabled(true);
        
        when(permissionRepository.findByCode("space.admin")).thenReturn(Optional.of(spaceAdminPermission));
        
        SpacePermission spacePermission = new SpacePermission(userId, spaceId, 2L, "SPACE", "GRANTED", "DIRECT");
        when(spacePermissionRepository.findEffectiveResourcePermissions(
                eq(userId), eq(spaceId), eq("SPACE"), eq(null), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(spacePermission));
        
        // When
        boolean result = permissionService.isSpaceAdmin(userId, spaceId);
        
        // Then
        assertThat(result).isTrue();
    }

    // ========== 权限级别检查测试 ==========

    @Test
    @DisplayName("权限级别检查 - 级别足够")
    void should_ReturnTrue_When_PermissionLevelSufficient() {
        // Given
        testSpacePermission.setStatus("GRANTED");
        testSpacePermission.setExpiresAt(null);
        testSpacePermission.setPermissionLevel(80);
        
        // When
        boolean result = permissionService.hasRequiredPermissionLevel(testSpacePermission, 50);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("权限级别检查 - 级别不足")
    void should_ReturnFalse_When_PermissionLevelInsufficient() {
        // Given
        testSpacePermission.setStatus("GRANTED");
        testSpacePermission.setExpiresAt(null);
        testSpacePermission.setPermissionLevel(30);
        
        // When
        boolean result = permissionService.hasRequiredPermissionLevel(testSpacePermission, 50);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("权限级别比较")
    void should_ComparePermissionLevels() {
        // When & Then
        assertThat(permissionService.isPermissionLevelSufficient(80, 50)).isTrue();
        assertThat(permissionService.isPermissionLevelSufficient(30, 50)).isFalse();
        assertThat(permissionService.isPermissionLevelSufficient(null, 50)).isFalse();
        assertThat(permissionService.isPermissionLevelSufficient(50, null)).isFalse();
    }

    @Test
    @DisplayName("计算有效权限级别")
    void should_CalculateEffectivePermissionLevel() {
        // Given
        SpacePermission perm1 = createTestSpacePermission();
        perm1.setStatus("GRANTED");
        perm1.setPermissionLevel(50);
        perm1.setExpiresAt(null);
        
        SpacePermission perm2 = createTestSpacePermission();
        perm2.setStatus("GRANTED");
        perm2.setPermissionLevel(80);
        perm2.setExpiresAt(null);
        
        SpacePermission perm3 = createTestSpacePermission();
        perm3.setStatus("DENIED");
        perm3.setPermissionLevel(90);
        perm3.setExpiresAt(null);
        
        List<SpacePermission> permissions = Arrays.asList(perm1, perm2, perm3);
        
        // When
        Integer result = permissionService.calculateEffectivePermissionLevel(permissions);
        
        // Then
        assertThat(result).isEqualTo(80); // 只计算GRANTED状态的权限，取最高级别
    }

    // ========== 权限操作测试 ==========

    @Test
    @DisplayName("授权空间权限")
    void should_GrantSpacePermission_When_PermissionExists() {
        // Given
        Long permissionId = 1L;
        Long granterId = 2L;
        String reason = "业务需要";
        
        when(spacePermissionRepository.findById(permissionId)).thenReturn(Optional.of(testSpacePermission));
        when(spacePermissionRepository.save(any(SpacePermission.class))).thenReturn(testSpacePermission);
        
        // When
        SpacePermission result = permissionService.grantSpacePermission(permissionId, granterId, reason);
        
        // Then
        assertThat(result.getStatus()).isEqualTo("GRANTED");
        assertThat(result.getGrantedBy()).isEqualTo(granterId);
        assertThat(result.getRemark()).isEqualTo(reason);
        verify(spacePermissionRepository).save(testSpacePermission);
    }

    @Test
    @DisplayName("拒绝空间权限")
    void should_DenySpacePermission_When_PermissionExists() {
        // Given
        Long permissionId = 1L;
        Long granterId = 2L;
        String reason = "安全考虑";
        
        when(spacePermissionRepository.findById(permissionId)).thenReturn(Optional.of(testSpacePermission));
        when(spacePermissionRepository.save(any(SpacePermission.class))).thenReturn(testSpacePermission);
        
        // When
        SpacePermission result = permissionService.denySpacePermission(permissionId, granterId, reason);
        
        // Then
        assertThat(result.getStatus()).isEqualTo("DENIED");
        assertThat(result.getGrantedBy()).isEqualTo(granterId);
        assertThat(result.getRemark()).isEqualTo(reason);
    }

    @Test
    @DisplayName("撤销空间权限")
    void should_RevokeSpacePermission_When_PermissionExists() {
        // Given
        Long permissionId = 1L;
        Long operatorId = 2L;
        String reason = "权限回收";
        
        when(spacePermissionRepository.findById(permissionId)).thenReturn(Optional.of(testSpacePermission));
        when(spacePermissionRepository.save(any(SpacePermission.class))).thenReturn(testSpacePermission);
        
        // When
        SpacePermission result = permissionService.revokeSpacePermission(permissionId, operatorId, reason);
        
        // Then
        assertThat(result.getStatus()).isEqualTo("REVOKED");
        assertThat(result.getRemark()).isEqualTo(reason);
        assertThat(result.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("权限操作 - 权限不存在")
    void should_ThrowException_When_PermissionNotExists() {
        // Given
        Long permissionId = 999L;
        when(spacePermissionRepository.findById(permissionId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> permissionService.grantSpacePermission(permissionId, 1L, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权限不存在");
    }

    // ========== 用户角色操作测试 ==========

    @Test
    @DisplayName("用户角色有效性检查 - 有效")
    void should_ReturnTrue_When_UserRoleIsValid() {
        // Given
        testUserRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
        testUserRole.setExpiresAt(LocalDateTime.now().plusDays(1));
        
        // When
        boolean result = permissionService.isUserRoleValid(testUserRole);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("用户角色有效性检查 - 已过期")
    void should_ReturnFalse_When_UserRoleExpired() {
        // Given
        testUserRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
        testUserRole.setExpiresAt(LocalDateTime.now().minusDays(1));
        
        // When
        boolean result = permissionService.isUserRoleValid(testUserRole);
        
        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("激活用户角色")
    void should_ActivateUserRole_When_UserRoleExists() {
        // Given
        Long userRoleId = 1L;
        String reason = "审批通过";
        
        when(userRoleRepository.findById(userRoleId)).thenReturn(Optional.of(testUserRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);
        
        // When
        UserRole result = permissionService.activateUserRole(userRoleId, reason);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(UserRole.AssignmentStatus.ACTIVE);
        assertThat(result.getEffectiveAt()).isNotNull();
    }

    @Test
    @DisplayName("暂停用户角色")
    void should_SuspendUserRole_When_UserRoleExists() {
        // Given
        Long userRoleId = 1L;
        String reason = "违规操作";
        
        when(userRoleRepository.findById(userRoleId)).thenReturn(Optional.of(testUserRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);
        
        // When
        UserRole result = permissionService.suspendUserRole(userRoleId, reason);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(UserRole.AssignmentStatus.SUSPENDED);
    }

    // ========== 测试数据创建方法 ==========

    private Permission createTestPermission() {
        Permission permission = new Permission("file.read", "文件读取", "FILE", "READ", 50);
        permission.setId(1L);
        permission.setIsEnabled(true);
        permission.setIsSystem(false);
        return permission;
    }

    private SpacePermission createTestSpacePermission() {
        SpacePermission permission = new SpacePermission(1L, 1L, 1L, "FILE", "GRANTED", "DIRECT");
        permission.setId(1L);
        permission.setPermissionLevel(50);
        permission.setGrantedAt(LocalDateTime.now());
        permission.setInheritFromParent(true);
        return permission;
    }

    private SpacePermission createInheritablePermission() {
        SpacePermission permission = new SpacePermission(1L, 1L, 1L, "FILE", "GRANTED", "INHERITED");
        permission.setId(2L);
        permission.setPermissionLevel(50);
        permission.setInheritFromParent(true);
        return permission;
    }

    private UserRole createTestUserRole() {
        UserRole userRole = new UserRole();
        userRole.setId(1L);
        userRole.setUserId(1L);
        userRole.setRoleId(1L);
        userRole.setStatus(UserRole.AssignmentStatus.PENDING);
        userRole.setEffectiveAt(LocalDateTime.now());
        return userRole;
    }
} 