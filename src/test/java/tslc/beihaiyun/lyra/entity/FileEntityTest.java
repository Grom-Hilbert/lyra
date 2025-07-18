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
 * 文件实体类测试
 */
@DisplayName("FileEntity Tests")
class FileEntityTest {

    private User owner;
    private FolderEntity folder;
    private FileEntity file;
    private Role userRole;
    private User otherUser;

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

        // 创建其他用户
        otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .displayName("Other User")
                .status(User.UserStatus.ACTIVE)
                .build();
        otherUser.setRoles(roles);

        // 创建文件夹
        folder = new FolderEntity();
        folder.setId(1L);
        folder.setName("测试文件夹");
        folder.setPath("/test");
        folder.setOwner(owner);
        folder.setSpaceType(FileEntity.SpaceType.ENTERPRISE);

        // 创建文件
        file = FileEntity.builder()
                .id(1L)
                .name("test.txt")
                .path("/test/test.txt")
                .mimeType("text/plain")
                .size(1024L)
                .checksum("abcdef123456")
                .spaceType(FileEntity.SpaceType.ENTERPRISE)
                .versionControlType(FileEntity.VersionControlType.BASIC)
                .folder(folder)
                .owner(owner)
                .storageKey("enterprise/1/uuid-test")
                .build();
        file.setCreatedAt(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        file.setAccessedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("基本属性测试")
    class BasicAttributeTests {

        @Test
        @DisplayName("测试文件基本属性")
        void testFileBasicAttributes() {
            assertEquals(1L, file.getId());
            assertEquals("test.txt", file.getName());
            assertEquals("/test/test.txt", file.getPath());
            assertEquals("text/plain", file.getMimeType());
            assertEquals(1024L, file.getSize());
            assertEquals("abcdef123456", file.getChecksum());
            assertEquals(FileEntity.SpaceType.ENTERPRISE, file.getSpaceType());
            assertEquals(FileEntity.VersionControlType.BASIC, file.getVersionControlType());
            assertEquals(folder, file.getFolder());
            assertEquals(owner, file.getOwner());
            assertNotNull(file.getCreatedAt());
            assertNotNull(file.getUpdatedAt());
            assertNotNull(file.getAccessedAt());
        }

        @Test
        @DisplayName("测试文件类型判断")
        void testFileTypeChecks() {
            assertTrue(file.isTextFile());
            assertFalse(file.isBinaryFile());
            assertTrue(file.isPreviewable());
            assertTrue(file.isEditable());
            assertFalse(file.isImageFile());
        }

        @Test
        @DisplayName("测试文件空间类型判断")
        void testFileSpaceTypeChecks() {
            assertTrue(file.isInEnterpriseSpace());
            assertFalse(file.isInPersonalSpace());
        }

        @Test
        @DisplayName("测试文件版本控制判断")
        void testFileVersionControlChecks() {
            assertTrue(file.supportsVersionControl());
            assertFalse(file.supportsAdvancedVersionControl());
            
            file.setVersionControlType(FileEntity.VersionControlType.ADVANCED);
            assertTrue(file.supportsVersionControl());
            assertTrue(file.supportsAdvancedVersionControl());
            
            file.setVersionControlType(FileEntity.VersionControlType.NONE);
            assertFalse(file.supportsVersionControl());
            assertFalse(file.supportsAdvancedVersionControl());
        }
    }

    @Nested
    @DisplayName("版本管理测试")
    class VersionManagementTests {

        @Test
        @DisplayName("测试添加文件版本")
        void testAddFileVersion() {
            // 创建版本
            FileVersion version = new FileVersion();
            version.setVersionNumber(1);
            version.setVersionDescription("初始版本");
            version.setFilePath("/storage/test.txt");
            version.setSize(1024L);
            version.setCreatedBy(owner);
            version.setIsCurrent(true);
            
            // 添加版本
            FileVersion addedVersion = file.addVersion(version);
            
            // 验证
            assertEquals(1, file.getVersions().size());
            assertEquals(version, addedVersion);
            assertEquals(file, addedVersion.getFile());
            assertTrue(addedVersion.getIsCurrent());
        }

        @Test
        @DisplayName("测试添加多个文件版本")
        void testAddMultipleFileVersions() {
            // 创建第一个版本
            FileVersion version1 = new FileVersion();
            version1.setVersionNumber(1);
            version1.setVersionDescription("初始版本");
            version1.setFilePath("/storage/test.txt");
            version1.setSize(1024L);
            version1.setCreatedBy(owner);
            version1.setIsCurrent(true);
            
            // 添加第一个版本
            file.addVersion(version1);
            
            // 创建第二个版本
            FileVersion version2 = new FileVersion();
            version2.setVersionNumber(2);
            version2.setVersionDescription("修改版本");
            version2.setFilePath("/storage/test.txt.v2");
            version2.setSize(1048L);
            version2.setCreatedBy(owner);
            version2.setIsCurrent(true);
            
            // 添加第二个版本
            file.addVersion(version2);
            
            // 验证
            assertEquals(2, file.getVersions().size());
            assertFalse(version1.getIsCurrent());
            assertTrue(version2.getIsCurrent());
            assertEquals(version2, file.getCurrentVersion());
            assertEquals(version1, file.getVersion(1));
            assertEquals(version2, file.getVersion(2));
        }

        @Test
        @DisplayName("测试获取当前版本")
        void testGetCurrentVersion() {
            // 创建版本
            FileVersion version = new FileVersion();
            version.setVersionNumber(1);
            version.setVersionDescription("初始版本");
            version.setFilePath("/storage/test.txt");
            version.setSize(1024L);
            version.setCreatedBy(owner);
            version.setIsCurrent(true);
            
            // 添加版本
            file.addVersion(version);
            
            // 验证
            assertEquals(version, file.getCurrentVersion());
        }

        @Test
        @DisplayName("测试获取指定版本号的版本")
        void testGetVersionByNumber() {
            // 创建多个版本
            FileVersion version1 = new FileVersion();
            version1.setVersionNumber(1);
            version1.setVersionDescription("初始版本");
            version1.setFilePath("/storage/test.txt");
            version1.setSize(1024L);
            version1.setCreatedBy(owner);
            version1.setIsCurrent(false);
            
            FileVersion version2 = new FileVersion();
            version2.setVersionNumber(2);
            version2.setVersionDescription("修改版本");
            version2.setFilePath("/storage/test.txt.v2");
            version2.setSize(1048L);
            version2.setCreatedBy(owner);
            version2.setIsCurrent(true);
            
            // 添加版本
            file.addVersion(version1);
            file.addVersion(version2);
            
            // 验证
            assertEquals(version1, file.getVersion(1));
            assertEquals(version2, file.getVersion(2));
            assertNull(file.getVersion(3));
        }
    }

    @Nested
    @DisplayName("权限管理测试")
    class PermissionManagementTests {

        @Test
        @DisplayName("测试添加文件权限")
        void testAddFilePermission() {
            // 创建权限
            FilePermission permission = new FilePermission();
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限
            FilePermission addedPermission = file.addPermission(permission);
            
            // 验证
            assertEquals(1, file.getPermissions().size());
            assertEquals(permission, addedPermission);
            assertEquals(file, addedPermission.getFile());
        }

        @Test
        @DisplayName("测试移除文件权限")
        void testRemoveFilePermission() {
            // 创建权限
            FilePermission permission = new FilePermission();
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限
            file.addPermission(permission);
            
            // 验证添加成功
            assertEquals(1, file.getPermissions().size());
            
            // 移除权限
            boolean removed = file.removePermission(permission);
            
            // 验证移除成功
            assertTrue(removed);
            assertEquals(0, file.getPermissions().size());
        }

        @Test
        @DisplayName("测试文件所有者权限检查")
        void testOwnerPermissionCheck() {
            // 所有者应该拥有所有权限
            assertTrue(file.hasPermission(owner, FilePermission.PermissionType.READ));
            assertTrue(file.hasPermission(owner, FilePermission.PermissionType.WRITE));
            assertTrue(file.hasPermission(owner, FilePermission.PermissionType.DELETE));
            assertTrue(file.hasPermission(owner, FilePermission.PermissionType.SHARE));
            assertTrue(file.hasPermission(owner, FilePermission.PermissionType.ADMIN));
        }

        @Test
        @DisplayName("测试用户直接权限检查")
        void testUserDirectPermissionCheck() {
            // 创建权限
            FilePermission permission = new FilePermission();
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限
            file.addPermission(permission);
            
            // 验证权限
            assertTrue(file.hasPermission(otherUser, FilePermission.PermissionType.READ));
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.WRITE));
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.DELETE));
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.SHARE));
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.ADMIN));
        }

        @Test
        @DisplayName("测试角色权限检查")
        void testRolePermissionCheck() {
            // 创建角色权限
            FilePermission permission = new FilePermission();
            permission.setRole(userRole);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            
            // 添加权限
            file.addPermission(permission);
            
            // 验证权限
            assertTrue(file.hasPermission(otherUser, FilePermission.PermissionType.READ));
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.WRITE));
        }

        @Test
        @DisplayName("测试过期权限检查")
        void testExpiredPermissionCheck() {
            // 创建已过期的权限
            FilePermission permission = new FilePermission();
            permission.setUser(otherUser);
            permission.setPermissionType(FilePermission.PermissionType.READ);
            permission.setGrantedBy(owner);
            permission.setExpiresAt(LocalDateTime.now().minusDays(1)); // 已过期
            
            // 添加权限
            file.addPermission(permission);
            
            // 验证权限
            assertFalse(file.hasPermission(otherUser, FilePermission.PermissionType.READ));
        }
    }

    @Nested
    @DisplayName("索引和存储测试")
    class IndexingAndStorageTests {

        @Test
        @DisplayName("测试文件索引状态")
        void testFileIndexingStatus() {
            // 初始化索引状态
            file.setIndexedForSearch(false);
            
            // 默认应该是未索引状态
            assertFalse(file.getIndexedForSearch());
            
            // 标记为已索引
            file.markAsIndexed();
            assertTrue(file.getIndexedForSearch());
            
            // 标记为未索引
            file.markAsNotIndexed();
            assertFalse(file.getIndexedForSearch());
        }

        @Test
        @DisplayName("测试生成存储键")
        void testGenerateStorageKey() {
            String storageKey = file.generateStorageKey();
            
            // 验证存储键格式
            assertTrue(storageKey.startsWith("enterprise/1/"));
            assertTrue(storageKey.length() > 20); // 应该包含UUID
        }

        @Test
        @DisplayName("测试更新访问时间")
        void testUpdateAccessTime() {
            // 记录当前访问时间
            LocalDateTime oldAccessTime = file.getAccessedAt();
            
            // 等待一小段时间
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                fail("测试被中断");
            }
            
            // 更新访问时间
            file.updateAccessTime();
            
            // 验证访问时间已更新
            assertTrue(file.getAccessedAt().isAfter(oldAccessTime));
        }
    }
}