package tslc.beihaiyun.lyra.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolationException;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.ShareLink;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.SpacePermission;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;

/**
 * 全面的Repository测试套件
 * 验证已完成任务的所有功能要求
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("全面Repository功能测试")
class ComprehensiveRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    // 用户认证相关Repository
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;

    // 文件管理相关Repository
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private FileEntityRepository fileEntityRepository;
    @Autowired
    private FileVersionRepository fileVersionRepository;
    @Autowired
    private ShareLinkRepository shareLinkRepository;

    // 权限管理相关Repository
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private SpacePermissionRepository spacePermissionRepository;

    private User testUser;
    private Role testRole;
    private Space testSpace;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // 创建基础测试数据
        testUser = createTestUser("testuser", "test@example.com");
        testRole = createTestRole("USER", "普通用户");
        testSpace = createTestSpace("testspace", testUser);
        testPermission = createTestPermission("file.read", "文件读取权限");
    }

    // ==================== 任务2.1：用户认证相关实体测试 ====================

    @Test
    @DisplayName("【任务2.1】验证User实体基础字段和验证注解")
    void testUserEntityFieldsAndValidation() {
        // 测试基础字段
        assertThat(testUser.getUsername()).isEqualTo("testuser");
        assertThat(testUser.getEmail()).isEqualTo("test@example.com");
        assertThat(testUser.getEnabled()).isTrue();
        assertThat(testUser.getAccountNonLocked()).isTrue();
        assertThat(testUser.getStorageQuota()).isEqualTo(1000L);

        // 测试审计字段（来自BaseEntity）
        assertThat(testUser.getCreatedAt()).isNotNull();
        assertThat(testUser.getDeleted()).isFalse();

        // 测试用户名唯一约束
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // 重复用户名
        duplicateUser.setEmail("another@example.com");
        duplicateUser.setPassword("password");
        duplicateUser.setEnabled(true);
        duplicateUser.setAccountNonLocked(true);
        duplicateUser.setAccountNonExpired(true);
        duplicateUser.setCredentialsNonExpired(true);
        duplicateUser.setEmailVerified(false);
        duplicateUser.setStatus(User.UserStatus.ACTIVE);
        duplicateUser.setStorageQuota(1000L);
        duplicateUser.setStorageUsed(0L);
        duplicateUser.setFailedLoginAttempts(0);

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicateUser);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("【任务2.1】验证UserRepository基本查询方法")
    void testUserRepositoryBasicQueries() {
        // 测试根据用户名查找
        Optional<User> foundByUsername = userRepository.findByUsername("testuser");
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getEmail()).isEqualTo("test@example.com");

        // 测试根据邮箱查找
        Optional<User> foundByEmail = userRepository.findByEmail("test@example.com");
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getUsername()).isEqualTo("testuser");

        // 测试用户名或邮箱查找
        Optional<User> foundByUsernameOrEmail = userRepository.findByUsernameOrEmail("testuser", "test@example.com");
        assertThat(foundByUsernameOrEmail).isPresent();

        // 测试存在性检查
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("【任务2.1】验证UserRepository复杂查询方法")
    void testUserRepositoryComplexQueries() {
        // 创建更多测试数据
        User adminUser = createTestUser("admin", "admin@example.com");
        adminUser.setStatus(User.UserStatus.ACTIVE);
        adminUser.setStorageUsed(800L); // 80%使用率
        entityManager.persistAndFlush(adminUser);

        // 测试按状态查询
        List<User> activeUsers = userRepository.findByStatus(User.UserStatus.ACTIVE);
        assertThat(activeUsers).hasSize(2);

        // 测试启用用户查询
        List<User> enabledUsers = userRepository.findByEnabledTrue();
        assertThat(enabledUsers).hasSize(2);

        // 测试存储使用率查询
        List<User> highUsageUsers = userRepository.findByStorageUsageGreaterThan(0.7);
        assertThat(highUsageUsers).hasSize(1);
        assertThat(highUsageUsers.get(0).getUsername()).isEqualTo("admin");

        // 测试模糊搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> searchResults = userRepository.searchUsers("test", pageable);
        assertThat(searchResults.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("【任务2.1】验证Role实体和UserRole关联")
    void testRoleAndUserRoleAssociation() {
        // 创建用户角色关联
        UserRole userRole = new UserRole();
        userRole.setUserId(testUser.getId());
        userRole.setRoleId(testRole.getId());
        userRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
        userRole.setEffectiveAt(LocalDateTime.now().minusDays(1));
        userRole.setAssignedBy("admin");
        entityManager.persistAndFlush(userRole);

        // 测试角色查询
        Optional<Role> foundRole = roleRepository.findByCode("USER");
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("普通用户");

        // 测试用户角色关联查询
        Optional<UserRole> foundUserRole = userRoleRepository.findByUserIdAndRoleId(testUser.getId(), testRole.getId());
        assertThat(foundUserRole).isPresent();
        assertThat(foundUserRole.get().getStatus()).isEqualTo(UserRole.AssignmentStatus.ACTIVE);

        // 测试根据用户查找角色
        List<UserRole> userRoles = userRoleRepository.findByUserId(testUser.getId());
        assertThat(userRoles).hasSize(1);

        // 测试根据角色查找用户
        List<Role> userRolesList = roleRepository.findByUserId(testUser.getId());
        assertThat(userRolesList).hasSize(1);
    }

    // ==================== 任务2.2：文件管理相关实体测试 ====================

    @Test
    @DisplayName("【任务2.2】验证Space实体配额管理和版本控制设置")
    void testSpaceEntityQuotaAndVersionControl() {
        // 测试空间基础字段
        assertThat(testSpace.getName()).isEqualTo("testspace");
        assertThat(testSpace.getType()).isEqualTo(Space.SpaceType.PERSONAL);
        assertThat(testSpace.getStatus()).isEqualTo(Space.SpaceStatus.ACTIVE);
        assertThat(testSpace.getQuotaLimit()).isEqualTo(5000L);
        assertThat(testSpace.getQuotaUsed()).isEqualTo(500L);

        // 测试版本控制设置
        assertThat(testSpace.getVersionControlEnabled()).isFalse();
        assertThat(testSpace.getVersionControlMode()).isEqualTo(Space.VersionControlMode.NORMAL);

        // 测试配额管理查询
        List<Space> spacesOverQuota = spaceRepository.findSpacesOverQuota();
        assertThat(spacesOverQuota).isEmpty(); // 当前没有超配额的空间

        // 创建超配额空间
        Space overQuotaSpace = createTestSpace("overquota", testUser);
        overQuotaSpace.setQuotaUsed(6000L); // 超过5000L的限制
        entityManager.persistAndFlush(overQuotaSpace);

        spacesOverQuota = spaceRepository.findSpacesOverQuota();
        assertThat(spacesOverQuota).hasSize(1);
    }

    @Test
    @DisplayName("【任务2.2】验证Folder实体层级结构和路径管理")
    void testFolderHierarchicalStructure() {
        // 创建根文件夹
        Folder rootFolder = new Folder();
        rootFolder.setSpace(testSpace);
        rootFolder.setName("Documents");
        rootFolder.setPath("/Documents");
        rootFolder.setLevel(1);
        rootFolder.setIsRoot(true);
        rootFolder.setSizeBytes(0L);
        rootFolder.setFileCount(0);
        entityManager.persistAndFlush(rootFolder);

        // 创建子文件夹
        Folder subFolder = new Folder();
        subFolder.setSpace(testSpace);
        subFolder.setParent(rootFolder);
        subFolder.setName("Projects");
        subFolder.setPath("/Documents/Projects");
        subFolder.setLevel(2);
        subFolder.setIsRoot(false);
        subFolder.setSizeBytes(0L);
        subFolder.setFileCount(0);
        entityManager.persistAndFlush(subFolder);

        // 测试层级结构查询
        List<Folder> rootFolders = folderRepository.findBySpaceAndIsRootTrue(testSpace);
        assertThat(rootFolders).hasSize(1);
        assertThat(rootFolders.get(0).getName()).isEqualTo("Documents");

        List<Folder> subFolders = folderRepository.findByParent(rootFolder);
        assertThat(subFolders).hasSize(1);
        assertThat(subFolders.get(0).getName()).isEqualTo("Projects");

        // 测试路径查询
        Optional<Folder> foundByPath = folderRepository.findBySpaceAndPath(testSpace, "/Documents/Projects");
        assertThat(foundByPath).isPresent();
        assertThat(foundByPath.get().getLevel()).isEqualTo(2);

        // 测试层级查询
        List<Folder> level2Folders = folderRepository.findBySpaceAndLevel(testSpace, 2);
        assertThat(level2Folders).hasSize(1);
    }

    @Test
    @DisplayName("【任务2.2】验证FileEntity实体状态管理和版本控制")
    void testFileEntityStatusAndVersionControl() {
        // 创建文件夹
        Folder folder = new Folder();
        folder.setSpace(testSpace);
        folder.setName("TestFolder");
        folder.setPath("/TestFolder");
        folder.setLevel(1);
        folder.setIsRoot(true);
        folder.setSizeBytes(0L);
        folder.setFileCount(0);
        entityManager.persistAndFlush(folder);

        // 创建文件
        FileEntity file = new FileEntity();
        file.setSpace(testSpace);
        file.setFolder(folder);
        file.setName("test.txt");
        file.setOriginalName("test.txt");
        file.setPath("/TestFolder/test.txt");
        file.setStoragePath("/storage/test.txt");
        file.setSizeBytes(1024L);
        file.setMimeType("text/plain");
        file.setFileHash("abc123");
        file.setStatus(FileEntity.FileStatus.ACTIVE);
        file.setVersion(1);
        file.setIsPublic(false);
        file.setDownloadCount(0);
        file.setLastModifiedAt(LocalDateTime.now());
        entityManager.persistAndFlush(file);

        // 创建文件版本
        FileVersion version1 = new FileVersion();
        version1.setFile(file);
        version1.setVersionNumber(1);
        version1.setSizeBytes(1024L);
        version1.setFileHash("abc123");
        version1.setStoragePath("/storage/test_v1.txt");
        version1.setChangeComment("初始版本");
        entityManager.persistAndFlush(version1);

        // 测试文件查询
        List<FileEntity> spaceFiles = fileEntityRepository.findBySpace(testSpace);
        assertThat(spaceFiles).hasSize(1);

        List<FileEntity> folderFiles = fileEntityRepository.findByFolder(folder);
        assertThat(folderFiles).hasSize(1);

        List<FileEntity> activeFiles = fileEntityRepository.findByStatus(FileEntity.FileStatus.ACTIVE);
        assertThat(activeFiles).hasSize(1);

        // 测试版本控制
        List<FileVersion> versions = fileVersionRepository.findByFile(file);
        assertThat(versions).hasSize(1);

        Optional<FileVersion> latestVersion = fileVersionRepository.findLatestByFile(file);
        assertThat(latestVersion).isPresent();
        assertThat(latestVersion.get().getVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("【任务2.2】验证ShareLink实体分享功能")
    void testShareLinkSharingFeatures() {
        // 创建文件
        FileEntity file = createTestFile("shared.txt", testSpace);

        // 创建分享链接
        ShareLink shareLink = new ShareLink();
        shareLink.setSpace(testSpace);
        shareLink.setFile(file);
        shareLink.setToken("12345678901234567890123456789012"); // 32个字符
        shareLink.setAccessType(ShareLink.AccessType.READ);
        shareLink.setIsActive(true);
        shareLink.setDownloadCount(0);
        shareLink.setExpiresAt(LocalDateTime.now().plusDays(7));
        entityManager.persistAndFlush(shareLink);

        // 测试分享链接查询
        Optional<ShareLink> foundByToken = shareLinkRepository.findByToken("12345678901234567890123456789012");
        assertThat(foundByToken).isPresent();
        assertThat(foundByToken.get().getAccessType()).isEqualTo(ShareLink.AccessType.READ);

        List<ShareLink> fileShareLinks = shareLinkRepository.findByFile(file);
        assertThat(fileShareLinks).hasSize(1);

        List<ShareLink> spaceShareLinks = shareLinkRepository.findBySpace(testSpace);
        assertThat(spaceShareLinks).hasSize(1);

        List<ShareLink> activeLinks = shareLinkRepository.findByIsActive(true);
        assertThat(activeLinks).hasSize(1);
    }

    // ==================== 任务2.3：权限管理相关实体测试 ====================

    @Test
    @DisplayName("【任务2.3】验证Permission实体权限分类和级别管理")
    void testPermissionEntityCategorization() {
        // 验证权限基础字段
        assertThat(testPermission.getCode()).isEqualTo("file.read");
        assertThat(testPermission.getName()).isEqualTo("文件读取权限");
        assertThat(testPermission.getCategory()).isEqualTo("READ");
        assertThat(testPermission.getResourceType()).isEqualTo("FILE");
        assertThat(testPermission.getLevel()).isEqualTo(50);

        // 创建更多权限进行测试
        Permission writePermission = createTestPermission("file.write", "文件写入权限");
        writePermission.setCategory("WRITE");
        writePermission.setLevel(60);
        entityManager.persistAndFlush(writePermission);

        Permission deletePermission = createTestPermission("file.delete", "文件删除权限");
        deletePermission.setCategory("DELETE");
        deletePermission.setLevel(80);
        entityManager.persistAndFlush(deletePermission);

        // 测试权限查询
        List<Permission> filePermissions = permissionRepository.findByResourceType("FILE");
        assertThat(filePermissions).hasSize(3);

        List<Permission> readPermissions = permissionRepository.findByCategory("READ");
        assertThat(readPermissions).hasSize(1);

        List<Permission> enabledPermissions = permissionRepository.findByIsEnabledTrue();
        assertThat(enabledPermissions).hasSize(3);

        // 测试权限级别查询
        List<Permission> midLevelPermissions = permissionRepository.findByLevelBetween(40, 70);
        assertThat(midLevelPermissions).hasSize(2); // read(50) and write(60)

        // 测试权限搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<Permission> searchResults = permissionRepository.searchPermissions("文件", pageable);
        assertThat(searchResults.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("【任务2.3】验证SpacePermission权限继承和覆盖机制")
    void testSpacePermissionInheritanceAndOverride() {
        // 创建空间权限
        SpacePermission spacePermission = new SpacePermission();
        spacePermission.setUserId(testUser.getId());
        spacePermission.setSpaceId(testSpace.getId());
        spacePermission.setPermissionId(testPermission.getId());
        spacePermission.setResourceType("FILE");
        spacePermission.setResourceId(1L);
        spacePermission.setStatus("GRANTED");
        spacePermission.setGrantType("DIRECT");
        spacePermission.setPermissionLevel(50);
        spacePermission.setInheritFromParent(false);
        spacePermission.setGrantedAt(LocalDateTime.now());
        spacePermission.setGrantedBy(testUser.getId());
        entityManager.persistAndFlush(spacePermission);

        // 创建继承权限
        SpacePermission inheritedPermission = new SpacePermission();
        inheritedPermission.setUserId(testUser.getId());
        inheritedPermission.setSpaceId(testSpace.getId());
        inheritedPermission.setPermissionId(testPermission.getId());
        inheritedPermission.setResourceType("FOLDER");
        inheritedPermission.setResourceId(2L);
        inheritedPermission.setStatus("INHERITED");
        inheritedPermission.setGrantType("INHERITED");
        inheritedPermission.setPermissionLevel(40); // 较低级别
        inheritedPermission.setInheritFromParent(true);
        inheritedPermission.setGrantedAt(LocalDateTime.now());
        inheritedPermission.setGrantedBy(testUser.getId());
        entityManager.persistAndFlush(inheritedPermission);

        // 测试权限查询
        List<SpacePermission> userPermissions = spacePermissionRepository.findByUserIdAndSpaceId(
            testUser.getId(), testSpace.getId()
        );
        assertThat(userPermissions).hasSize(2);

        List<SpacePermission> grantedPermissions = spacePermissionRepository.findByStatus("GRANTED");
        assertThat(grantedPermissions).hasSize(1);

        List<SpacePermission> directPermissions = spacePermissionRepository.findByGrantType("DIRECT");
        assertThat(directPermissions).hasSize(1);

        List<SpacePermission> inheritedPermissions = spacePermissionRepository.findByGrantType("INHERITED");
        assertThat(inheritedPermissions).hasSize(1);

        // 测试权限继承查询
        List<SpacePermission> inheritablePermissions = spacePermissionRepository.findByInheritFromParent(true);
        assertThat(inheritablePermissions).hasSize(1);

        // 测试资源权限查询
        List<SpacePermission> filePermissions = spacePermissionRepository.findByResourceTypeAndResourceId("FILE", 1L);
        assertThat(filePermissions).hasSize(1);
    }

    @Test
    @DisplayName("【任务2.3】验证权限软删除和过期处理")
    void testPermissionSoftDeleteAndExpiration() {
        // 创建即将过期的权限
        SpacePermission expiringPermission = new SpacePermission();
        expiringPermission.setUserId(testUser.getId());
        expiringPermission.setSpaceId(testSpace.getId());
        expiringPermission.setPermissionId(testPermission.getId());
        expiringPermission.setResourceType("FILE");
        expiringPermission.setResourceId(3L);
        expiringPermission.setStatus("GRANTED");
        expiringPermission.setGrantType("DIRECT");
        expiringPermission.setPermissionLevel(50);
        expiringPermission.setInheritFromParent(false);
        expiringPermission.setGrantedAt(LocalDateTime.now().minusDays(10));
        expiringPermission.setGrantedBy(testUser.getId());
        expiringPermission.setExpiresAt(LocalDateTime.now().minusDays(1)); // 已过期
        entityManager.persistAndFlush(expiringPermission);

        // 测试过期权限查询
        List<SpacePermission> expiredPermissions = spacePermissionRepository.findByExpiresAtBefore(LocalDateTime.now());
        assertThat(expiredPermissions).hasSize(1);

        // 测试标记过期权限为删除
        int updatedCount = spacePermissionRepository.markExpiredPermissionsAsDeleted(LocalDateTime.now());
        assertThat(updatedCount).isEqualTo(1);

        // 验证软删除的权限不会被查询到（由于@SQLRestriction）
        List<SpacePermission> foundPermissions = spacePermissionRepository.findByUserIdAndSpaceId(
            testUser.getId(), testSpace.getId()
        );
        // 应该为空，因为权限已被软删除
        assertThat(foundPermissions).isEmpty();
    }

    // ==================== 综合测试 ====================

    @Test
    @DisplayName("验证实体关联关系完整性")
    void testEntityRelationshipsIntegrity() {
        // 创建完整的关联结构
        Folder folder = createTestFolder("TestFolder", testSpace);
        FileEntity file = createTestFile("test.txt", testSpace);
        file.setFolder(folder);
        entityManager.persistAndFlush(file);

        UserRole userRole = new UserRole();
        userRole.setUserId(testUser.getId());
        userRole.setRoleId(testRole.getId());
        userRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
        userRole.setEffectiveAt(LocalDateTime.now());
        entityManager.persistAndFlush(userRole);

        SpacePermission spacePermission = new SpacePermission();
        spacePermission.setUserId(testUser.getId());
        spacePermission.setSpaceId(testSpace.getId());
        spacePermission.setPermissionId(testPermission.getId());
        spacePermission.setResourceType("FILE");
        spacePermission.setResourceId(file.getId());
        spacePermission.setStatus("GRANTED");
        spacePermission.setGrantType("DIRECT");
        spacePermission.setPermissionLevel(50);
        spacePermission.setInheritFromParent(false);
        spacePermission.setGrantedAt(LocalDateTime.now());
        spacePermission.setGrantedBy(testUser.getId());
        entityManager.persistAndFlush(spacePermission);

        // 验证关联关系
        assertThat(file.getSpace()).isEqualTo(testSpace);
        assertThat(file.getFolder()).isEqualTo(folder);
        assertThat(folder.getSpace()).isEqualTo(testSpace);
        assertThat(testSpace.getOwner()).isEqualTo(testUser);

        // 验证查询能正确返回关联数据
        List<FileEntity> spaceFiles = fileEntityRepository.findBySpace(testSpace);
        assertThat(spaceFiles).hasSize(1);

        List<Folder> spaceFolders = folderRepository.findBySpace(testSpace);
        assertThat(spaceFolders).hasSize(1);

        List<SpacePermission> userSpacePermissions = spacePermissionRepository.findByUserIdAndSpaceId(
            testUser.getId(), testSpace.getId()
        );
        assertThat(userSpacePermissions).hasSize(1);
    }

    @Test
    @DisplayName("验证数据约束和验证逻辑")
    void testDataConstraintsAndValidation() {
        // 测试Permission实体的验证约束
        Permission invalidPermission = new Permission();
        invalidPermission.setCode("INVALID_CODE"); // 应该是小写
        invalidPermission.setName("Invalid Permission");
        invalidPermission.setDescription("Test Permission");
        invalidPermission.setCategory("INVALID"); // 不在允许的范围内
        invalidPermission.setResourceType("INVALID"); // 不在允许的范围内
        invalidPermission.setLevel(150); // 超过最大值100
        invalidPermission.setIsEnabled(true);
        invalidPermission.setIsSystem(false);

        // 应该抛出验证异常
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(invalidPermission);
        }).isInstanceOf(ConstraintViolationException.class);

        // 测试Space配额约束
        Space invalidSpace = new Space();
        invalidSpace.setName("invalid");
        invalidSpace.setOwner(testUser);
        invalidSpace.setQuotaUsed(-100L); // 负数，应该不被允许
        invalidSpace.setQuotaLimit(-1000L); // 负数，应该不被允许

        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(invalidSpace);
        }).isInstanceOf(Exception.class);
    }

    // ==================== 辅助方法 ====================

    private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setEmailVerified(false);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setStorageQuota(1000L);
        user.setStorageUsed(100L);
        user.setFailedLoginAttempts(0);
        return entityManager.persistAndFlush(user);
    }

    private Role createTestRole(String code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription("Test Role");
        role.setType(Role.RoleType.USER);
        role.setEnabled(true);
        role.setSystem(false);
        role.setSortOrder(1);
        return entityManager.persistAndFlush(role);
    }

    private Space createTestSpace(String name, User owner) {
        Space space = new Space();
        space.setName(name);
        space.setDescription("Test Space");
        space.setType(Space.SpaceType.PERSONAL);
        space.setStatus(Space.SpaceStatus.ACTIVE);
        space.setOwner(owner);
        space.setQuotaLimit(5000L);
        space.setQuotaUsed(500L);
        space.setVersionControlEnabled(false);
        space.setVersionControlMode(Space.VersionControlMode.NORMAL);
        return entityManager.persistAndFlush(space);
    }

    private Permission createTestPermission(String code, String name) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription("Test Permission");
        permission.setCategory("READ");
        permission.setResourceType("FILE");
        permission.setLevel(50);
        permission.setIsEnabled(true);
        permission.setIsSystem(false);
        return entityManager.persistAndFlush(permission);
    }

    private Folder createTestFolder(String name, Space space) {
        Folder folder = new Folder();
        folder.setSpace(space);
        folder.setName(name);
        folder.setPath("/" + name);
        folder.setLevel(1);
        folder.setIsRoot(true);
        folder.setSizeBytes(0L);
        folder.setFileCount(0);
        return entityManager.persistAndFlush(folder);
    }

    private FileEntity createTestFile(String name, Space space) {
        FileEntity file = new FileEntity();
        file.setSpace(space);
        file.setName(name);
        file.setOriginalName(name);
        file.setPath("/" + name);
        file.setStoragePath("/storage/" + name);
        file.setSizeBytes(1024L);
        file.setMimeType("text/plain");
        file.setFileHash("hash123");
        file.setStatus(FileEntity.FileStatus.ACTIVE);
        file.setVersion(1);
        file.setIsPublic(false);
        file.setDownloadCount(0);
        file.setLastModifiedAt(LocalDateTime.now());
        return entityManager.persistAndFlush(file);
    }
} 