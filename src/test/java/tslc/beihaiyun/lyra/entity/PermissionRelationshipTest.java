package tslc.beihaiyun.lyra.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限实体关系测试
 */
@DisplayName("Permission Relationship Tests")
class PermissionRelationshipTest {

    private Permission fileReadPermission;
    private Permission fileWritePermission;
    private Permission fileDeletePermission;
    private Role userRole;
    private Role adminRole;
    private User user;

    @BeforeEach
    void setUp() {
        // 创建权限
        fileReadPermission = new Permission();
        fileReadPermission.setId(1L);
        fileReadPermission.setName("FILE_READ");
        fileReadPermission.setDescription("允许读取文件");
        fileReadPermission.setResource("file");
        fileReadPermission.setAction("read");
        fileReadPermission.setCreatedAt(LocalDateTime.now());
        fileReadPermission.setUpdatedAt(LocalDateTime.now());
        fileReadPermission.setRoles(new HashSet<>());

        fileWritePermission = new Permission();
        fileWritePermission.setId(2L);
        fileWritePermission.setName("FILE_WRITE");
        fileWritePermission.setDescription("允许写入文件");
        fileWritePermission.setResource("file");
        fileWritePermission.setAction("write");
        fileWritePermission.setCreatedAt(LocalDateTime.now());
        fileWritePermission.setUpdatedAt(LocalDateTime.now());
        fileWritePermission.setRoles(new HashSet<>());

        fileDeletePermission = new Permission();
        fileDeletePermission.setId(3L);
        fileDeletePermission.setName("FILE_DELETE");
        fileDeletePermission.setDescription("允许删除文件");
        fileDeletePermission.setResource("file");
        fileDeletePermission.setAction("delete");
        fileDeletePermission.setCreatedAt(LocalDateTime.now());
        fileDeletePermission.setUpdatedAt(LocalDateTime.now());
        fileDeletePermission.setRoles(new HashSet<>());

        // 创建角色
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setType(Role.RoleType.USER);
        userRole.setDescription("普通用户角色");
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRole.setPermissions(new HashSet<>());
        userRole.setUsers(new HashSet<>());

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");
        adminRole.setType(Role.RoleType.ADMIN);
        adminRole.setDescription("管理员角色");
        adminRole.setCreatedAt(LocalDateTime.now());
        adminRole.setUpdatedAt(LocalDateTime.now());
        adminRole.setPermissions(new HashSet<>());
        adminRole.setUsers(new HashSet<>());

        // 创建用户
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .status(User.UserStatus.ACTIVE)
                .build();
        user.setRoles(new HashSet<>());
    }

    @Nested
    @DisplayName("角色权限关系测试")
    class RolePermissionRelationshipTests {

        @Test
        @DisplayName("测试角色分配权限")
        void testAssignPermissionsToRole() {
            // 为用户角色分配读取权限
            userRole.getPermissions().add(fileReadPermission);
            
            // 为管理员角色分配所有权限
            adminRole.getPermissions().add(fileReadPermission);
            adminRole.getPermissions().add(fileWritePermission);
            adminRole.getPermissions().add(fileDeletePermission);
            
            // 验证用户角色权限
            assertEquals(1, userRole.getPermissions().size());
            assertTrue(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertFalse(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileWritePermission.getId())));
            assertFalse(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileDeletePermission.getId())));
            
            // 验证管理员角色权限
            assertEquals(3, adminRole.getPermissions().size());
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileWritePermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileDeletePermission.getId())));
        }

        @Test
        @DisplayName("测试移除角色权限")
        void testRemovePermissionsFromRole() {
            // 为管理员角色分配所有权限
            adminRole.getPermissions().add(fileReadPermission);
            adminRole.getPermissions().add(fileWritePermission);
            adminRole.getPermissions().add(fileDeletePermission);
            
            // 验证初始权限
            assertEquals(3, adminRole.getPermissions().size());
            
            // 移除删除权限
            adminRole.getPermissions().removeIf(p -> p.getId().equals(fileDeletePermission.getId()));
            
            // 验证权限移除后的状态
            assertEquals(2, adminRole.getPermissions().size());
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileWritePermission.getId())));
            assertFalse(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileDeletePermission.getId())));
        }
    }

    @Nested
    @DisplayName("用户角色权限关系测试")
    class UserRolePermissionRelationshipTests {

        @Test
        @DisplayName("测试用户通过角色获取权限")
        void testUserPermissionsThroughRoles() {
            // 为用户角色分配读取权限
            userRole.getPermissions().add(fileReadPermission);
            
            // 为管理员角色分配所有权限
            adminRole.getPermissions().add(fileReadPermission);
            adminRole.getPermissions().add(fileWritePermission);
            adminRole.getPermissions().add(fileDeletePermission);
            
            // 将用户分配到用户角色
            user.getRoles().add(userRole);
            
            // 验证用户权限
            assertEquals(1, user.getRoles().size());
            assertTrue(user.getRoles().stream().anyMatch(r -> r.getId().equals(userRole.getId())));
            
            // 验证用户通过角色获取的权限
            assertTrue(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertFalse(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileWritePermission.getId())));
            assertFalse(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileDeletePermission.getId())));
            
            // 将用户升级为管理员
            user.getRoles().add(adminRole);
            
            // 验证用户权限
            assertEquals(2, user.getRoles().size());
            assertTrue(user.getRoles().stream().anyMatch(r -> r.getId().equals(userRole.getId())));
            assertTrue(user.getRoles().stream().anyMatch(r -> r.getId().equals(adminRole.getId())));
            
            // 验证用户通过角色获取的权限
            assertTrue(userRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileReadPermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileWritePermission.getId())));
            assertTrue(adminRole.getPermissions().stream().anyMatch(p -> p.getId().equals(fileDeletePermission.getId())));
        }
    }

    @Nested
    @DisplayName("文件权限与系统权限关系测试")
    class FilePermissionSystemPermissionRelationshipTests {

        @Test
        @DisplayName("测试文件权限类型与系统权限的映射")
        void testFilePermissionTypeToSystemPermissionMapping() {
            // 验证文件权限类型与系统权限的映射关系
            assertEquals("read", fileReadPermission.getAction());
            assertEquals("write", fileWritePermission.getAction());
            assertEquals("delete", fileDeletePermission.getAction());
            
            // 验证文件权限类型枚举与系统权限的映射
            assertEquals(FilePermission.PermissionType.READ.name().toLowerCase(), fileReadPermission.getAction());
            assertEquals(FilePermission.PermissionType.WRITE.name().toLowerCase(), fileWritePermission.getAction());
            assertEquals(FilePermission.PermissionType.DELETE.name().toLowerCase(), fileDeletePermission.getAction());
        }
    }
}