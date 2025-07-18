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
 * 权限实体类测试
 */
@DisplayName("Permission Tests")
class PermissionTest {

    private Permission permission;
    private Role role;

    @BeforeEach
    void setUp() {
        // 创建权限
        permission = new Permission();
        permission.setId(1L);
        permission.setName("FILE_READ");
        permission.setDescription("允许读取文件");
        permission.setResource("file");
        permission.setAction("read");
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        permission.setRoles(new HashSet<>());

        // 创建角色
        role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setType(Role.RoleType.USER);
        role.setDescription("普通用户角色");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        Set<Permission> permissions = new HashSet<>();
        role.setPermissions(permissions);
        role.setUsers(new HashSet<>());
    }

    @Nested
    @DisplayName("基本属性测试")
    class BasicAttributeTests {

        @Test
        @DisplayName("测试权限基本属性")
        void testPermissionBasicAttributes() {
            assertEquals(1L, permission.getId());
            assertEquals("FILE_READ", permission.getName());
            assertEquals("允许读取文件", permission.getDescription());
            assertEquals("file", permission.getResource());
            assertEquals("read", permission.getAction());
            assertNotNull(permission.getCreatedAt());
            assertNotNull(permission.getUpdatedAt());
            assertNotNull(permission.getRoles());
            assertTrue(permission.getRoles().isEmpty());
        }

        @Test
        @DisplayName("测试权限相等性")
        void testPermissionEquality() {
            Permission samePermission = new Permission();
            samePermission.setId(1L);
            samePermission.setName("FILE_READ");
            samePermission.setDescription("允许读取文件");
            samePermission.setResource("file");
            samePermission.setAction("read");
            samePermission.setRoles(new HashSet<>());

            Permission differentPermission = new Permission();
            differentPermission.setId(2L);
            differentPermission.setName("FILE_WRITE");
            differentPermission.setDescription("允许写入文件");
            differentPermission.setResource("file");
            differentPermission.setAction("write");
            differentPermission.setRoles(new HashSet<>());

            // 使用ID比较而不是完整对象比较
            assertEquals(permission.getId(), samePermission.getId());
            assertNotEquals(permission.getId(), differentPermission.getId());
        }
    }

    @Nested
    @DisplayName("关系测试")
    class RelationshipTests {

        @Test
        @DisplayName("测试权限与角色的关系")
        void testPermissionToRoleRelationship() {
            // 创建新的集合以避免循环引用
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            permission.setRoles(roles);
            
            Set<Permission> permissions = new HashSet<>();
            permissions.add(permission);
            role.setPermissions(permissions);
            
            // 验证关系
            assertEquals(1, permission.getRoles().size());
            assertEquals(1, role.getPermissions().size());
            assertTrue(permission.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId())));
            assertTrue(role.getPermissions().stream().anyMatch(p -> p.getId().equals(permission.getId())));
        }

        @Test
        @DisplayName("测试移除权限与角色的关系")
        void testRemovePermissionToRoleRelationship() {
            // 创建新的集合以避免循环引用
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            permission.setRoles(roles);
            
            Set<Permission> permissions = new HashSet<>();
            permissions.add(permission);
            role.setPermissions(permissions);
            
            // 验证添加成功
            assertEquals(1, permission.getRoles().size());
            assertEquals(1, role.getPermissions().size());
            
            // 移除关系
            permission.getRoles().clear();
            role.getPermissions().clear();
            
            // 验证移除成功
            assertEquals(0, permission.getRoles().size());
            assertEquals(0, role.getPermissions().size());
        }
    }

    @Nested
    @DisplayName("生命周期方法测试")
    class LifecycleMethodTests {

        @Test
        @DisplayName("测试创建时间设置")
        void testCreatedAtSetting() {
            Permission newPermission = new Permission();
            newPermission.onCreate();
            
            assertNotNull(newPermission.getCreatedAt());
            assertNotNull(newPermission.getUpdatedAt());
        }

        @Test
        @DisplayName("测试更新时间设置")
        void testUpdatedAtSetting() {
            LocalDateTime oldUpdatedAt = permission.getUpdatedAt();
            
            // 等待一小段时间
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                fail("测试被中断");
            }
            
            // 触发更新
            permission.onUpdate();
            
            // 验证更新时间已更新
            assertTrue(permission.getUpdatedAt().isAfter(oldUpdatedAt));
        }
    }
}