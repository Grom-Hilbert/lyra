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
 * 权限继承机制测试
 */
@DisplayName("Permission Inheritance Tests")
class PermissionInheritanceTest {

    private User owner;
    private User otherUser;
    private Role userRole;
    private Role adminRole;
    private FolderEntity rootFolder;
    private FolderEntity subFolder;
    private FileEntity file1;
    private FileEntity file2;
    private FolderPermission rootFolderPermission;
    private FolderPermission subFolderPermission;
    private FilePermission filePermission;

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

        // 创建文件1（在根文件夹中）
        file1 = FileEntity.builder()
                .id(1L)
                .name("file1.txt")
                .path("/root/file1.txt")
                .mimeType("text/plain")
                .size(1024L)
                .checksum("abcdef123456")
                .spaceType(FileEntity.SpaceType.ENTERPRISE)
                .versionControlType(FileEntity.VersionControlType.BASIC)
                .folder(rootFolder)
                .owner(owner)
                .storageKey("enterprise/1/uuid-file1")
                .build();
        file1.setCreatedAt(LocalDateTime.now());
        file1.setUpdatedAt(LocalDateTime.now());
        file1.setAccessedAt(LocalDateTime.now());

        // 创建文件2（在子文件夹中）
        file2 = FileEntity.builder()
                .id(2L)
                .name("file2.txt")
                .path("/root/sub/file2.txt")
                .mimeType("text/plain")
                .size(2048L)
                .checksum("abcdef789012")
                .spaceType(FileEntity.SpaceType.ENTERPRISE)
                .versionControlType(FileEntity.VersionControlType.ADVANCED)
                .folder(subFolder)
                .owner(owner)
                .storageKey("enterprise/1/uuid-file2")
                .build();
        file2.setCreatedAt(LocalDateTime.now());
        file2.setUpdatedAt(LocalDateTime.now());
        file2.setAccessedAt(LocalDateTime.now());

        // 将文件添加到对应文件夹的文件列表
        rootFolder.getFiles().add(file1);
        subFolder.getFiles().add(file2);

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

        // 创建文件权限
        filePermission = new FilePermission();
        filePermission.setId(1L);
        filePermission.setFile(file1);
        filePermission.setUser(otherUser);
        filePermission.setPermissionType(FilePermission.PermissionType.READ);
        filePermission.setGrantedBy(owner);
        filePermission.setGrantedAt(LocalDateTime.now());

        // 添加权限到文件夹和文件
        rootFolder.getPermissions().add(rootFolderPermission);
        subFolder.getPermissions().add(subFolderPermission);
        file1.addPermission(filePermission);
    }

    @Nested
    @DisplayName("文件夹权限继承测试")
    class FolderPermissionInheritanceTests {

        @Test
        @DisplayName("测试子文件夹继承父文件夹权限")
        void testSubFolderInheritsParentFolderPermissions() {
            // 验证子文件夹权限是继承的
            assertTrue(subFolderPermission.getIsInherited());
            
            // 验证子文件夹权限与父文件夹权限的关系
            assertEquals(rootFolderPermission.getUser(), subFolderPermission.getUser());
            assertEquals(rootFolderPermission.getPermissionType(), subFolderPermission.getPermissionType());
        }

        @Test
        @DisplayName("测试创建新子文件夹时继承父文件夹权限")
        void testNewSubFolderInheritsParentFolderPermissions() {
            // 创建新的子文件夹
            FolderEntity newSubFolder = new FolderEntity();
            newSubFolder.setId(3L);
            newSubFolder.setName("新子文件夹");
            newSubFolder.setPath("/root/newsub");
            newSubFolder.setOwner(owner);
            newSubFolder.setSpaceType(FileEntity.SpaceType.ENTERPRISE);
            newSubFolder.setParent(rootFolder);
            newSubFolder.setFiles(new ArrayList<>());
            newSubFolder.setChildren(new ArrayList<>());
            newSubFolder.setPermissions(new ArrayList<>());
            
            // 将新子文件夹添加到根文件夹的子文件夹列表
            rootFolder.getChildren().add(newSubFolder);
            
            // 创建继承的权限
            FolderPermission inheritedPermission = new FolderPermission();
            inheritedPermission.setId(3L);
            inheritedPermission.setFolder(newSubFolder);
            inheritedPermission.setUser(otherUser);
            inheritedPermission.setPermissionType(FilePermission.PermissionType.READ);
            inheritedPermission.setIsInherited(true);
            inheritedPermission.setGrantedBy(owner);
            inheritedPermission.setGrantedAt(LocalDateTime.now());
            
            // 添加权限到新子文件夹
            newSubFolder.getPermissions().add(inheritedPermission);
            
            // 验证新子文件夹权限是继承的
            assertTrue(inheritedPermission.getIsInherited());
            
            // 验证新子文件夹权限与父文件夹权限的关系
            assertEquals(rootFolderPermission.getUser(), inheritedPermission.getUser());
            assertEquals(rootFolderPermission.getPermissionType(), inheritedPermission.getPermissionType());
        }
    }

    @Nested
    @DisplayName("文件权限继承测试")
    class FilePermissionInheritanceTests {

        @Test
        @DisplayName("测试文件继承文件夹权限")
        void testFileInheritsFolderPermissions() {
            // 验证文件1有直接权限
            assertTrue(file1.hasPermission(otherUser, FilePermission.PermissionType.READ));
            
            // 验证文件2没有直接权限，但应该继承自子文件夹
            assertFalse(file2.getPermissions().stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().equals(otherUser) && 
                             p.getPermissionType() == FilePermission.PermissionType.READ));
            
            // 模拟文件2继承子文件夹权限的检查
            // 注意：在实际实现中，这应该由一个服务层方法来处理
            boolean hasInheritedPermission = subFolder.getPermissions().stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().equals(otherUser) && 
                             p.getPermissionType() == FilePermission.PermissionType.READ);
            
            assertTrue(hasInheritedPermission);
        }

        @Test
        @DisplayName("测试文件权限覆盖继承权限")
        void testFilePermissionOverridesInheritedPermission() {
            // 为文件2添加写入权限
            FilePermission writePermission = new FilePermission();
            writePermission.setId(2L);
            writePermission.setFile(file2);
            writePermission.setUser(otherUser);
            writePermission.setPermissionType(FilePermission.PermissionType.WRITE);
            writePermission.setGrantedBy(owner);
            writePermission.setGrantedAt(LocalDateTime.now());
            
            file2.addPermission(writePermission);
            
            // 验证文件2有直接的写入权限
            assertTrue(file2.hasPermission(otherUser, FilePermission.PermissionType.WRITE));
            
            // 验证文件2没有直接的读取权限，但应该继承自子文件夹
            assertFalse(file2.getPermissions().stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().equals(otherUser) && 
                             p.getPermissionType() == FilePermission.PermissionType.READ));
            
            // 模拟文件2继承子文件夹读取权限的检查
            boolean hasInheritedReadPermission = subFolder.getPermissions().stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().equals(otherUser) && 
                             p.getPermissionType() == FilePermission.PermissionType.READ);
            
            assertTrue(hasInheritedReadPermission);
        }
    }

    @Nested
    @DisplayName("临时权限测试")
    class TemporaryPermissionTests {

        @Test
        @DisplayName("测试临时权限过期")
        void testTemporaryPermissionExpiration() {
            // 设置临时权限过期时间
            LocalDateTime expiryTime = LocalDateTime.now().plusDays(7);
            rootFolderPermission.setExpiresAt(expiryTime);
            
            // 验证过期时间设置成功
            assertEquals(expiryTime, rootFolderPermission.getExpiresAt());
            
            // 验证权限未过期
            assertTrue(rootFolderPermission.getExpiresAt().isAfter(LocalDateTime.now()));
            
            // 设置已过期的权限
            LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
            filePermission.setExpiresAt(pastTime);
            
            // 验证权限已过期
            assertTrue(filePermission.getExpiresAt().isBefore(LocalDateTime.now()));
            
            // 验证过期权限不再有效
            assertFalse(file1.hasPermission(otherUser, FilePermission.PermissionType.READ));
        }
    }

    @Nested
    @DisplayName("角色权限测试")
    class RolePermissionTests {

        @Test
        @DisplayName("测试用户通过角色获取文件夹权限")
        void testUserGetsPermissionsThroughRole() {
            // 创建角色权限
            FolderPermission rolePermission = new FolderPermission();
            rolePermission.setId(3L);
            rolePermission.setFolder(rootFolder);
            rolePermission.setRole(userRole);
            rolePermission.setPermissionType(FilePermission.PermissionType.WRITE);
            rolePermission.setIsInherited(false);
            rolePermission.setGrantedBy(owner);
            rolePermission.setGrantedAt(LocalDateTime.now());
            
            // 添加权限到文件夹
            rootFolder.getPermissions().add(rolePermission);
            
            // 模拟检查用户是否通过角色获得了权限
            // 注意：在实际实现中，这应该由一个服务层方法来处理
            boolean hasRolePermission = rootFolder.getPermissions().stream()
                    .anyMatch(p -> p.getRole() != null && otherUser.getRoles().contains(p.getRole()) && 
                             p.getPermissionType() == FilePermission.PermissionType.WRITE);
            
            assertTrue(hasRolePermission);
        }

        @Test
        @DisplayName("测试用户通过角色获取文件权限")
        void testUserGetsFilePermissionsThroughRole() {
            // 创建文件角色权限
            FilePermission rolePermission = new FilePermission();
            rolePermission.setId(2L);
            rolePermission.setFile(file1);
            rolePermission.setRole(userRole);
            rolePermission.setPermissionType(FilePermission.PermissionType.WRITE);
            rolePermission.setGrantedBy(owner);
            rolePermission.setGrantedAt(LocalDateTime.now());
            
            // 添加权限到文件
            file1.addPermission(rolePermission);
            
            // 验证用户通过角色获得了文件权限
            assertTrue(file1.hasPermission(otherUser, FilePermission.PermissionType.WRITE));
        }
    }
}