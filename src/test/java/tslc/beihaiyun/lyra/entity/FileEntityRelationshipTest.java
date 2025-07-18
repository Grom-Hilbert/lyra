package tslc.beihaiyun.lyra.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件实体关系测试
 */
@DisplayName("FileEntity Relationship Tests")
class FileEntityRelationshipTest {

    private User owner;
    private FolderEntity rootFolder;
    private FolderEntity subFolder;
    private FileEntity file1;
    private FileEntity file2;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // 创建角色
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
        userRole.setType(Role.RoleType.USER);
        userRole.setDescription("普通用户角色");

        // 创建文件所有者
        owner = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .status(User.UserStatus.ACTIVE)
                .build();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        owner.setRoles(roles);

        // 创建根文件夹
        rootFolder = new FolderEntity();
        rootFolder.setId(1L);
        rootFolder.setName("根文件夹");
        rootFolder.setPath("/root");
        rootFolder.setOwner(owner);
        rootFolder.setSpaceType(FileEntity.SpaceType.ENTERPRISE);
        rootFolder.setFiles(new ArrayList<>());
        rootFolder.setChildren(new ArrayList<>());

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
    }

    @Nested
    @DisplayName("文件夹关系测试")
    class FolderRelationshipTests {

        @Test
        @DisplayName("测试文件与文件夹的关系")
        void testFileToFolderRelationship() {
            // 验证文件1属于根文件夹
            assertEquals(rootFolder, file1.getFolder());
            assertTrue(rootFolder.getFiles().contains(file1));
            
            // 验证文件2属于子文件夹
            assertEquals(subFolder, file2.getFolder());
            assertTrue(subFolder.getFiles().contains(file2));
        }

        @Test
        @DisplayName("测试文件夹层次结构")
        void testFolderHierarchy() {
            // 验证子文件夹的父文件夹是根文件夹
            assertEquals(rootFolder, subFolder.getParent());
            assertTrue(rootFolder.getChildren().contains(subFolder));
            
            // 验证根文件夹没有父文件夹
            assertNull(rootFolder.getParent());
        }

        @Test
        @DisplayName("测试移动文件到不同文件夹")
        void testMoveFileBetweenFolders() {
            // 将文件1从根文件夹移动到子文件夹
            rootFolder.getFiles().remove(file1);
            file1.setFolder(subFolder);
            file1.setPath("/root/sub/file1.txt");
            subFolder.getFiles().add(file1);
            
            // 验证移动后的关系
            assertEquals(subFolder, file1.getFolder());
            assertTrue(subFolder.getFiles().contains(file1));
            assertFalse(rootFolder.getFiles().contains(file1));
            assertEquals("/root/sub/file1.txt", file1.getPath());
        }
    }

    @Nested
    @DisplayName("用户关系测试")
    class UserRelationshipTests {

        @Test
        @DisplayName("测试文件与所有者的关系")
        void testFileToOwnerRelationship() {
            // 验证文件1的所有者
            assertEquals(owner, file1.getOwner());
            
            // 验证文件2的所有者
            assertEquals(owner, file2.getOwner());
        }

        @Test
        @DisplayName("测试文件夹与所有者的关系")
        void testFolderToOwnerRelationship() {
            // 验证根文件夹的所有者
            assertEquals(owner, rootFolder.getOwner());
            
            // 验证子文件夹的所有者
            assertEquals(owner, subFolder.getOwner());
        }

        @Test
        @DisplayName("测试转移文件所有权")
        void testTransferFileOwnership() {
            // 创建新用户
            User newOwner = User.builder()
                    .id(2L)
                    .username("newowner")
                    .email("newowner@example.com")
                    .displayName("New Owner")
                    .status(User.UserStatus.ACTIVE)
                    .build();
            
            // 转移文件1的所有权
            file1.setOwner(newOwner);
            
            // 验证所有权已转移
            assertEquals(newOwner, file1.getOwner());
            assertNotEquals(owner, file1.getOwner());
        }
    }

    @Nested
    @DisplayName("版本关系测试")
    class VersionRelationshipTests {

        @Test
        @DisplayName("测试文件与版本的关系")
        void testFileToVersionRelationship() {
            // 创建版本
            FileVersion version1 = new FileVersion();
            version1.setId(1L);
            version1.setVersionNumber(1);
            version1.setVersionDescription("初始版本");
            version1.setFilePath("/storage/file1.txt");
            version1.setSize(1024L);
            version1.setCreatedBy(owner);
            version1.setIsCurrent(true);
            
            // 添加版本到文件1
            file1.addVersion(version1);
            
            // 验证关系
            assertTrue(file1.getVersions().contains(version1));
            assertEquals(file1, version1.getFile());
        }

        @Test
        @DisplayName("测试版本与用户的关系")
        void testVersionToUserRelationship() {
            // 创建版本
            FileVersion version1 = new FileVersion();
            version1.setId(1L);
            version1.setVersionNumber(1);
            version1.setVersionDescription("初始版本");
            version1.setFilePath("/storage/file1.txt");
            version1.setSize(1024L);
            version1.setCreatedBy(owner);
            version1.setIsCurrent(true);
            
            // 添加版本到文件1
            file1.addVersion(version1);
            
            // 验证版本创建者关系
            assertEquals(owner, version1.getCreatedBy());
        }
    }

    @Nested
    @DisplayName("权限关系测试")
    class PermissionRelationshipTests {

        @Test
        @DisplayName("测试文件与权限的关系")
        void testFileToPermissionRelationship() {
            // 创建新用户
            User otherUser = User.builder()
                    .id(2L)
                    .username("otheruser")
                    .email("other@example.com")
                    .displayName("Other User")
                    .status(User.UserStatus.ACTIVE)
                    .build();
            
            // 创建文件权限
            FilePermission permission = new FilePermission();
            permission.setId(1L);
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限到文件1
            file1.addPermission(permission);
            
            // 验证关系
            assertTrue(file1.getPermissions().contains(permission));
            assertEquals(file1, permission.getFile());
        }

        @Test
        @DisplayName("测试权限与用户的关系")
        void testPermissionToUserRelationship() {
            // 创建新用户
            User otherUser = User.builder()
                    .id(2L)
                    .username("otheruser")
                    .email("other@example.com")
                    .displayName("Other User")
                    .status(User.UserStatus.ACTIVE)
                    .build();
            
            // 创建文件权限
            FilePermission permission = new FilePermission();
            permission.setId(1L);
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限到文件1
            file1.addPermission(permission);
            
            // 验证权限与用户的关系
            assertEquals(otherUser, permission.getUser());
            assertEquals(owner, permission.getGrantedBy());
        }

        @Test
        @DisplayName("测试权限与角色的关系")
        void testPermissionToRoleRelationship() {
            // 创建文件权限
            FilePermission permission = new FilePermission();
            permission.setId(1L);
            permission.setRole(userRole);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限到文件1
            file1.addPermission(permission);
            
            // 验证权限与角色的关系
            assertEquals(userRole, permission.getRole());
        }
    }
}