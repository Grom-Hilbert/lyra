package tslc.beihaiyun.lyra.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件管理相关实体类测试
 * 测试Space、Folder、FileEntity、FileVersion、ShareLink实体类
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("文件管理实体类测试")
class FileManagementEntityTest {

    private Validator validator;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    // ========== Space 实体测试 ==========

    @Test
    @DisplayName("创建有效的Space实体")
    void should_CreateValidSpace_When_AllFieldsAreValid() {
        // Given
        Space space = new Space();
        space.setName("测试空间");
        space.setType(Space.SpaceType.PERSONAL);
        space.setOwner(testUser);
        space.setDescription("这是一个测试空间");
        space.setQuotaLimit(1073741824L); // 1GB
        space.setQuotaUsed(0L);
        space.setVersionControlEnabled(true);
        space.setVersionControlMode(Space.VersionControlMode.NORMAL);
        space.setStatus(Space.SpaceStatus.ACTIVE);

        // When
        Set<ConstraintViolation<Space>> violations = validator.validate(space);

        // Then
        assertTrue(violations.isEmpty(), "空间实体应该是有效的");
        assertEquals("测试空间", space.getName());
        assertEquals(Space.SpaceType.PERSONAL, space.getType());
        assertEquals(testUser, space.getOwner());
        assertTrue(space.isPersonalSpace());
        assertFalse(space.isEnterpriseSpace());
        assertTrue(space.isActive());
    }

    @Test
    @DisplayName("Space实体验证空名称")
    void should_FailValidation_When_SpaceNameIsBlank() {
        // Given
        Space space = new Space();
        space.setName("");
        space.setType(Space.SpaceType.PERSONAL);
        space.setOwner(testUser);

        // When
        Set<ConstraintViolation<Space>> violations = validator.validate(space);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("空间名称不能为空")));
    }

    @Test
    @DisplayName("测试Space配额管理")
    void should_ManageQuotaCorrectly_When_UsingQuotaMethods() {
        // Given
        Space space = new Space();
        space.setQuotaLimit(1000L);
        space.setQuotaUsed(0L);

        // When & Then
        assertTrue(space.hasEnoughQuota(500L));
        assertFalse(space.hasEnoughQuota(1500L));

        space.addUsedQuota(300L);
        assertEquals(300L, space.getQuotaUsed());
        assertEquals(0.3, space.getQuotaUsageRatio(), 0.01);

        space.reduceUsedQuota(100L);
        assertEquals(200L, space.getQuotaUsed());
    }

    // ========== Folder 实体测试 ==========

    @Test
    @DisplayName("创建有效的Folder实体")
    void should_CreateValidFolder_When_AllFieldsAreValid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        space.setId(1L);

        Folder folder = new Folder();
        folder.setName("测试文件夹");
        folder.setPath("/test-folder");
        folder.setSpace(space);
        folder.setLevel(0);
        folder.setIsRoot(true);
        folder.setSizeBytes(0L);
        folder.setFileCount(0);

        // When
        Set<ConstraintViolation<Folder>> violations = validator.validate(folder);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals("测试文件夹", folder.getName());
        assertEquals("/test-folder", folder.getPath());
        assertEquals(space, folder.getSpace());
        assertTrue(folder.isRootFolder());
        assertFalse(folder.hasParent());
    }

    @Test
    @DisplayName("测试Folder层级关系")
    void should_ManageFolderHierarchy_When_SetParentFolder() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        space.setId(1L);

        Folder rootFolder = new Folder("根文件夹", "/root", space);
        rootFolder.setIsRoot(true);
        rootFolder.setLevel(0);

        Folder childFolder = new Folder();
        childFolder.setName("子文件夹");
        childFolder.setPath("/root/child");
        childFolder.setSpace(space);

        // When
        childFolder.setParent(rootFolder);

        // Then
        assertEquals(rootFolder, childFolder.getParent());
        assertEquals(1, childFolder.getLevel());
        assertTrue(childFolder.hasParent());
        assertEquals("/根文件夹/子文件夹", childFolder.getFullPath());
    }

    @Test
    @DisplayName("测试Folder统计信息更新")
    void should_UpdateStatistics_When_FilesAreAdded() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        Folder folder = new Folder("测试文件夹", "/test", space);
        
        FileEntity file1 = new FileEntity("文件1.txt", "file1.txt", "/test/file1.txt", space, 100L, "/storage/file1");
        FileEntity file2 = new FileEntity("文件2.txt", "file2.txt", "/test/file2.txt", space, 200L, "/storage/file2");

        // When
        folder.addFile(file1);
        folder.addFile(file2);

        // Then
        assertEquals(2, folder.getFileCount());
        assertEquals(300L, folder.getSizeBytes());
        assertTrue(folder.hasFiles());
    }

    // ========== FileEntity 实体测试 ==========

    @Test
    @DisplayName("创建有效的FileEntity实体")
    void should_CreateValidFileEntity_When_AllFieldsAreValid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        space.setId(1L);

        FileEntity file = new FileEntity();
        file.setName("测试文件.txt");
        file.setOriginalName("test-file.txt");
        file.setPath("/test/测试文件.txt");
        file.setSpace(space);
        file.setSizeBytes(1024L);
        file.setMimeType("text/plain");
        file.setFileHash("abc123");
        file.setStoragePath("/storage/abc123");
        file.setVersion(1);
        file.setStatus(FileEntity.FileStatus.ACTIVE);
        file.setIsPublic(false);

        // When
        Set<ConstraintViolation<FileEntity>> violations = validator.validate(file);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals("测试文件.txt", file.getName());
        assertEquals("test-file.txt", file.getOriginalName());
        assertTrue(file.isActive());
        assertFalse(file.isDeleted());
        assertFalse(file.isPublicFile());
        assertEquals("txt", file.getFileExtension());
        assertTrue(file.isTextFile());
    }

    @Test
    @DisplayName("测试FileEntity业务方法")
    void should_HandleBusinessMethods_When_UsingFileEntity() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        FileEntity file = new FileEntity("image.jpg", "photo.jpg", "/images/image.jpg", space, 2048L, "/storage/image.jpg");
        file.setMimeType("image/jpeg");
        file.setDownloadCount(5);

        // When & Then
        assertEquals("jpg", file.getFileExtension());
        assertTrue(file.isImageFile());
        assertFalse(file.isTextFile());
        assertEquals("2.0 KB", file.getHumanReadableSize());

        file.incrementDownloadCount();
        assertEquals(6, file.getDownloadCount());

        file.incrementVersion();
        assertEquals(2, file.getVersion());
    }

    @Test
    @DisplayName("FileEntity验证必填字段")
    void should_FailValidation_When_RequiredFieldsAreMissing() {
        // Given
        FileEntity file = new FileEntity();
        file.setName(""); // 空名称
        file.setOriginalName("test.txt");
        file.setPath("/test.txt");
        // 缺少space和sizeBytes

        // When
        Set<ConstraintViolation<FileEntity>> violations = validator.validate(file);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("文件名不能为空")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("所属空间不能为空")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("文件大小不能为空")));
    }

    // ========== FileVersion 实体测试 ==========

    @Test
    @DisplayName("创建有效的FileVersion实体")
    void should_CreateValidFileVersion_When_AllFieldsAreValid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        FileEntity file = new FileEntity("测试.txt", "test.txt", "/test.txt", space, 1024L, "/storage/test");
        file.setId(1L);

        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(1);
        version.setSizeBytes(1024L);
        version.setFileHash("hash123");
        version.setStoragePath("/storage/v1/test");
        version.setChangeComment("初始版本");

        // When
        Set<ConstraintViolation<FileVersion>> violations = validator.validate(version);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals(file, version.getFile());
        assertEquals(Integer.valueOf(1), version.getVersionNumber());
        assertEquals("初始版本", version.getChangeComment());
        assertTrue(version.hasChangeComment());
        assertTrue(version.isFirstVersion());
        assertEquals("1.0 KB", version.getHumanReadableSize());
    }

    @Test
    @DisplayName("FileVersion验证版本号约束")
    void should_FailValidation_When_VersionNumberIsInvalid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        FileEntity file = new FileEntity("测试.txt", "test.txt", "/test.txt", space, 1024L, "/storage/test");

        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(0); // 无效版本号
        version.setSizeBytes(1024L);
        version.setStoragePath("/storage/v0/test");

        // When
        Set<ConstraintViolation<FileVersion>> violations = validator.validate(version);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("版本号必须大于0")));
    }

    // ========== ShareLink 实体测试 ==========

    @Test
    @DisplayName("创建有效的ShareLink实体")
    void should_CreateValidShareLink_When_AllFieldsAreValid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        FileEntity file = new FileEntity("共享文件.txt", "shared.txt", "/shared.txt", space, 1024L, "/storage/shared");

        ShareLink shareLink = new ShareLink();
        shareLink.setToken("abcdef1234567890abcdef1234567890");
        shareLink.setFile(file);
        shareLink.setSpace(space);
        shareLink.setAccessType(ShareLink.AccessType.READ);
        shareLink.setDownloadCount(0);
        shareLink.setIsActive(true);

        // When
        Set<ConstraintViolation<ShareLink>> violations = validator.validate(shareLink);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals("abcdef1234567890abcdef1234567890", shareLink.getToken());
        assertEquals(file, shareLink.getFile());
        assertTrue(shareLink.isFileShare());
        assertFalse(shareLink.isFolderShare());
        assertTrue(shareLink.isReadOnly());
        assertFalse(shareLink.isReadWrite());
        assertTrue(shareLink.isValid());
        assertEquals("文件分享", shareLink.getShareTypeDescription());
        assertEquals("共享文件.txt", shareLink.getTargetName());
    }

    @Test
    @DisplayName("测试ShareLink业务逻辑")
    void should_HandleBusinessLogic_When_UsingShareLink() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        Folder folder = new Folder("共享文件夹", "/shared", space);

        ShareLink shareLink = new ShareLink("token123456789012345678901234567890", folder, space, ShareLink.AccessType.WRITE);
        shareLink.setDownloadLimit(10);
        shareLink.setDownloadCount(5);
        shareLink.setExpiresAt(LocalDateTime.now().plusDays(7));
        shareLink.setPasswordHash("hashedpassword");

        // When & Then
        assertTrue(shareLink.isFolderShare());
        assertTrue(shareLink.isReadWrite());
        assertFalse(shareLink.isExpired());
        assertTrue(shareLink.requiresPassword());
        assertFalse(shareLink.isDownloadLimitReached());
        assertEquals("文件夹分享", shareLink.getShareTypeDescription());
        assertEquals("共享文件夹", shareLink.getTargetName());

        shareLink.incrementDownloadCount();
        assertEquals(6, shareLink.getDownloadCount());

        shareLink.disable();
        assertFalse(shareLink.getIsActive());
    }

    @Test
    @DisplayName("ShareLink验证令牌长度")
    void should_FailValidation_When_TokenLengthIsInvalid() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        FileEntity file = new FileEntity("文件.txt", "file.txt", "/file.txt", space, 1024L, "/storage/file");

        ShareLink shareLink = new ShareLink();
        shareLink.setToken("shorttoken"); // 令牌太短
        shareLink.setFile(file);
        shareLink.setSpace(space);
        shareLink.setAccessType(ShareLink.AccessType.READ);

        // When
        Set<ConstraintViolation<ShareLink>> violations = validator.validate(shareLink);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("分享令牌长度必须为32个字符")));
    }

    // ========== 关联关系测试 ==========

    @Test
    @DisplayName("测试实体间的关联关系")
    void should_HandleEntityRelationships_When_EntitiesAreAssociated() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        space.setId(1L);

        Folder folder = new Folder("文件夹", "/folder", space);
        folder.setId(1L);

        FileEntity file = new FileEntity("文件.txt", "file.txt", "/folder/file.txt", space, 1024L, "/storage/file");
        file.setFolder(folder);
        file.setId(1L);

        FileVersion version = new FileVersion(file, 1, 1024L, "/storage/v1/file", "初始版本");

        ShareLink shareLink = new ShareLink("token12345678901234567890123456789012", file, space, ShareLink.AccessType.READ);

        // When
        space.getFolders().add(folder);
        space.getFiles().add(file);
        space.getShareLinks().add(shareLink);
        folder.getFiles().add(file);
        file.getVersions().add(version);
        file.getShareLinks().add(shareLink);

        // Then
        assertTrue(space.getFolders().contains(folder));
        assertTrue(space.getFiles().contains(file));
        assertTrue(space.getShareLinks().contains(shareLink));
        assertTrue(folder.getFiles().contains(file));
        assertTrue(file.getVersions().contains(version));
        assertTrue(file.getShareLinks().contains(shareLink));
        assertEquals(folder, file.getFolder());
        assertEquals(file, version.getFile());
        assertEquals(file, shareLink.getFile());
        assertEquals(space, shareLink.getSpace());
    }

    @Test
    @DisplayName("测试实体equals和hashCode方法")
    void should_HandleEqualsAndHashCode_When_ComparingEntities() {
        // Given
        Space space1 = new Space();
        space1.setId(1L);
        space1.setName("空间1");

        Space space2 = new Space();
        space2.setId(1L);
        space2.setName("空间2");

        Space space3 = new Space();
        space3.setId(2L);
        space3.setName("空间1");

        // When & Then
        assertEquals(space1, space2); // 相同ID
        assertNotEquals(space1, space3); // 不同ID
        assertEquals(space1.hashCode(), space2.hashCode());

        // 测试其他实体的equals方法
        FileEntity file1 = new FileEntity();
        file1.setId(1L);
        FileEntity file2 = new FileEntity();
        file2.setId(1L);
        assertEquals(file1, file2);

        Folder folder1 = new Folder();
        folder1.setId(1L);
        Folder folder2 = new Folder();
        folder2.setId(1L);
        assertEquals(folder1, folder2);
    }

    @Test
    @DisplayName("测试实体toString方法")
    void should_GenerateCorrectStringRepresentation_When_CallingToString() {
        // Given
        Space space = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        space.setId(1L);
        space.setQuotaUsed(1000L);
        space.setQuotaLimit(10000L);

        // When
        String spaceString = space.toString();

        // Then
        assertNotNull(spaceString);
        assertTrue(spaceString.contains("Space{"));
        assertTrue(spaceString.contains("id=1"));
        assertTrue(spaceString.contains("name='测试空间'"));
        assertTrue(spaceString.contains("type=PERSONAL"));
        assertTrue(spaceString.contains("quotaUsed=1000"));
        assertTrue(spaceString.contains("quotaLimit=10000"));
    }
} 