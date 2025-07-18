package tslc.beihaiyun.lyra.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件夹权限实体类测试
 */
@DisplayName("FolderPermission Tests")
class FolderPermissionTest {

    private User owner;
    private User otherUser;
    private Role userRole;
    private Role adminRole;
    private FolderEntity rootFolder;
    private FolderEntity subFolder;
    private FolderPermission rootFolderPermission;
    private FolderPermission subFolderPermission;

    @BeforeEach
    void setUp() {
        // 创建角色
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setType(Role.RoleType.USER);
        userRole.setDescription("普通用户角色");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");
        adminRole.setType(Role.RoleType.ADMIN);
        adminRole.setDescription("管理员角色");

        // 创建文件夹所有者
        owner = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@example.com")
                .displayName("Owner User")
                .status(User.UserStatus.ACTIVE)
                .build();
        Set<Role> ownerRoles = new HashSet<>();
        ownerRoles.add(adminRole);
        owner.setRoles(ownerRoles);

        // 创建其他用户
        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .displayName("Other User")
                .status(User.UserStatus.ACTIVE)
                .build();
        Set<Role> otherUserRoles = new HashSet<>();
        otherUserRoles.add(userRole);
        otherUser.setRoles(otherUserRoles);

        // 创建根文件夹
        rootFolder = new FolderEntity();
        rootFolder.setId(1L);
        rootFolder.setName("根文件夹");
        rootFolder.setPath("/root");
        rootFolder.setOwner(owner);
        rootFolder.setSpaceType(FileEntity.SpaceType.ENTERPRISE);
        rootFolder.setFiles(new ArrayList<>());
        rootFolder.setChildren(new ArrayList<>());
        rootFolder.setPermissions(new ArrayList<>());

        // 创建子文件夹
        subFolder = new FolderEntity();
        subFolder.setId(2L);
        subFolder.setName("子文件夹");
        subFolder.setPath("/root/sub");
        subFolder.setOwner(owner);
        subFolder.setSpaceType(FileEntity.SpaceType.ENTERPRISE);
        subFolder.setParent(rootFolder);
        subFolder.setFiles(new ArrayList<>());
        subFolder.setChildren(new ArrayList<>());
        subFolder.setPermissions(new ArrayList<>());

        // 将子文件夹添加到根文件夹的子文件夹列表
        rootFolder.getChildren().add(subFolder);

        // 创建根文件夹权限
        rootFolderPermission = new FolderPermission();
        rootFolderPermission.setId(1L);
        rootFolderPermission.setFolder(rootFolder);
        rootFolderPermission.setUser(otherUser);
        rootFolderPermission.setPermissionType(FilePermission.PermissionType.READ);
        rootFolderPermission.setIsInherited(false);
        rootFolderPermission.setGrantedBy(owner);
        rootFolderPermission.setGrantedAt(LocalDateTime.now());

        // 创建子文件夹继承权限
        subFolderPermission = new FolderPermission();
        subFolderPermission.setId(2L);
        subFolderPermission.setFolder(subFolder);
        subFolderPermission.setUser(otherUser);
        subFolderPermission.setPermissionType(FilePermission.PermissionType.READ);
        subFolderPermission.setIsInherited(true);
        subFolderPermission.setGrantedBy(owner);
        subFolderPermission.setGrantedAt(LocalDateTime.now());

        // 添加权限到文件夹
        rootFolder.getPermissions().add(rootFolderPermission);
        subFolder.getPermissions().add(subFolderPermission);
    }

    @Nested
    @DisplayName("基本属性测试")
    class BasicAttributeTests {

        @Test
        @DisplayName("测试文件夹权限基本属性")
        void testFolderPermissionBasicAttributes() {
            assertEquals(1L, rootFolderPermission.getId());
            assertEquals(rootFolder, rootFolderPermission.getFolder());
            assertEquals(otherUser, rootFolderPermission.getUser());
            assertEquals(FilePermission.PermissionType.READ, rootFolderPermission.getPermissionType());
            assertFalse(rootFolderPermission.getIsInherited());
            assertEquals(owner, rootFolderPermission.getGrantedBy());
            assertNotNull(rootFolderPermission.getGrantedAt());
            assertNull(rootFolderPermission.getExpiresAt());
        }

        @Test
        @DisplayName("测试继承权限属性")
        void testInheritedPermissionAttributes() {
            assertEquals(2L, subFolderPermission.getId());
            assertEquals(subFolder, subFolderPermission.getFolder());
            assertEquals(otherUser, subFolderPermission.getUser());
            assertEquals(FilePermission.PermissionType.READ, subFolderPermission.getPermissionType());
            assertTrue(subFolderPermission.getIsInherited());
            assertEquals(owner, subFolderPermission.getGrantedBy());
            assertNotNull(subFolderPermission.getGrantedAt());
            assertNull(subFolderPermission.getExpiresAt());
        }
    }

    @Nested
    @DisplayName("权限继承测试")
    class PermissionInheritanceTests {

        @Test
        @DisplayName("测试子文件夹权限继承")
        void testSubFolderPermissionInheritance() {
            // 验证子文件夹权限是继承的
            assertTrue(subFolderPermission.getIsInherited());
            
            // 验证子文件夹权限与父文件夹权限的关系
            assertEquals(rootFolderPermission.getUser(), subFolderPermission.getUser());
            assertEquals(rootFolderPermission.getPermissionType(), subFolderPermission.getPermissionType());
            assertEquals(rootFolderPermission.getGrantedBy(), subFolderPermission.getGrantedBy());
        }

        @Test
        @DisplayName("测试临时权限设置")
        void testTemporaryPermissionSetting() {
            // 设置临时权限过期时间
            LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);
            rootFolderPermission.setExpiresAt(expiryTime);
            
            // 验证过期时间设置成功
            assertEquals(expiryTime, rootFolderPermission.getExpiresAt());
            
            // 验证权限未过期
            assertTrue(rootFolderPermission.getExpiresAt().isAfter(LocalDateTime.now()));
            
            // 设置已过期的权限
            LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
            subFolderPermission.setExpiresAt(pastTime);
            
            // 验证权限已过期
            assertTrue(subFolderPermission.getExpiresAt().isBefore(LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("角色权限测试")
    class RolePermissionTests {

        @Test
        @DisplayName("测试角色权限设置")
        void testRolePermissionSetting() {
            // 创建角色权限
            FolderPermission rolePermission = new FolderPermission();
            rolePermission.setId(3L);
            rolePermission.setFolder(rootFolder);
            rolePermission.setRole(userRole);
            rolePermission.setPermissionType(FilePermission.PermissionType.READ);
            rolePermission.setIsInherited(false);
            rolePermission.setGrantedBy(owner);
            rolePermission.setGrantedAt(LocalDateTime.now());
            
            // 添加权限到文件夹
            rootFolder.getPermissions().add(rolePermission);
            
            // 验证角色权限设置成功
            assertEquals(2, rootFolder.getPermissions().size());
            assertTrue(rootFolder.getPermissions().contains(rolePermission));
            assertEquals(userRole, rolePermission.getRole());
            assertNull(rolePermission.getUser());
        }
    }

    @Nested
    @DisplayName("生命周期方法测试")
    class LifecycleMethodTests {

        @Test
        @DisplayName("测试创建时间设置")
        void testCreatedAtSetting() {
            FolderPermission newPermission = new FolderPermission();
            newPermission.onCreate();
            
            assertNotNull(newPermission.getGrantedAt());
        }
    }
}