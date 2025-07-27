package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FileVersionRepository;
import tslc.beihaiyun.lyra.service.impl.VersionServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 版本控制服务测试类
 * 测试版本创建、查询、回滚、清理等核心功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("版本控制服务测试")
class VersionServiceTest {

    @Mock
    private FileVersionRepository fileVersionRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private VersionServiceImpl versionService;

    private User testUser;
    private Space testSpace;
    private FileEntity testFile;
    private FileVersion testVersion;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // 创建测试空间
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("测试空间");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);

        // 创建测试文件
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setOriginalName("test.txt");
        testFile.setPath("/test.txt");
        testFile.setSpace(testSpace);
        testFile.setSizeBytes(1024L);
        testFile.setMimeType("text/plain");
        testFile.setFileHash("abc123");
        testFile.setStoragePath("/storage/test.txt");
        testFile.setVersion(1);

        // 创建测试版本
        testVersion = new FileVersion();
        testVersion.setId(1L);
        testVersion.setFile(testFile);
        testVersion.setVersionNumber(1);
        testVersion.setSizeBytes(1024L);
        testVersion.setFileHash("abc123");
        testVersion.setStoragePath("/storage/test_v1.txt");
        testVersion.setChangeComment("初始版本");
        testVersion.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 版本创建测试 ====================

    @Test
    @DisplayName("创建版本成功 - 从输入流")
    void should_CreateVersionSuccessfully_When_ValidInputStreamProvided() throws Exception {
        // Given
        String content = "测试文件内容";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String changeComment = "添加测试内容";
        Long creatorId = 1L;

        StorageService.StorageResult storageResult = new StorageService.StorageResult(
            "/storage/test_v2.txt", "def456", 1024L, false);

        Optional<FileVersion> latestVersion = Optional.of(testVersion);
        FileVersion newVersion = new FileVersion();
        newVersion.setId(2L);
        newVersion.setFile(testFile);
        newVersion.setVersionNumber(2);
        newVersion.setSizeBytes(1024L);
        newVersion.setFileHash("def456");
        newVersion.setStoragePath("/storage/test_v2.txt");
        newVersion.setChangeComment(changeComment);

        when(storageService.store(any(InputStream.class), eq(testFile.getName()), eq(testFile.getMimeType())))
            .thenReturn(storageResult);
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(latestVersion);
        when(fileVersionRepository.save(any(FileVersion.class))).thenReturn(newVersion);

        // When
        VersionService.VersionOperationResult result = versionService.createVersion(
            testFile, inputStream, changeComment, creatorId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("版本创建成功");
        assertThat(result.getVersion()).isNotNull();
        assertThat(result.getVersion().getVersionNumber()).isEqualTo(2);
        assertThat(result.getVersion().getChangeComment()).isEqualTo(changeComment);

        verify(storageService).store(any(InputStream.class), eq(testFile.getName()), eq(testFile.getMimeType()));
        verify(fileVersionRepository).save(any(FileVersion.class));
    }

    @Test
    @DisplayName("创建版本失败 - 文件实体为空")
    void should_FailToCreateVersion_When_FileEntityIsNull() {
        // Given
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        // When
        VersionService.VersionOperationResult result = versionService.createVersion(
            null, inputStream, "comment", 1L);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件实体不能为空");
        assertThat(result.getVersion()).isNull();

        verifyNoInteractions(storageService);
        verify(fileVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建版本失败 - 输入流为空")
    void should_FailToCreateVersion_When_InputStreamIsNull() {
        // When
        VersionService.VersionOperationResult result = versionService.createVersion(
            testFile, null, "comment", 1L);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文件内容不能为空");
        assertThat(result.getVersion()).isNull();

        verifyNoInteractions(storageService);
        verify(fileVersionRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建版本成功 - 从存储路径")
    void should_CreateVersionSuccessfully_When_ValidStoragePathProvided() {
        // Given
        String storagePath = "/storage/test_v2.txt";
        Long fileSize = 2048L;
        String fileHash = "def456";
        String changeComment = "从存储路径创建";
        Long creatorId = 1L;

        Optional<FileVersion> latestVersion = Optional.of(testVersion);
        FileVersion newVersion = new FileVersion();
        newVersion.setId(2L);
        newVersion.setFile(testFile);
        newVersion.setVersionNumber(2);
        newVersion.setSizeBytes(fileSize);
        newVersion.setFileHash(fileHash);
        newVersion.setStoragePath(storagePath);
        newVersion.setChangeComment(changeComment);

        when(storageService.exists(storagePath)).thenReturn(true);
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(latestVersion);
        when(fileVersionRepository.save(any(FileVersion.class))).thenReturn(newVersion);

        // When
        VersionService.VersionOperationResult result = versionService.createVersion(
            testFile, storagePath, fileSize, fileHash, changeComment, creatorId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("版本创建成功");
        assertThat(result.getVersion()).isNotNull();
        assertThat(result.getVersion().getVersionNumber()).isEqualTo(2);
        assertThat(result.getVersion().getStoragePath()).isEqualTo(storagePath);

        verify(storageService).exists(storagePath);
        verify(fileVersionRepository).save(any(FileVersion.class));
    }

    // ==================== 版本查询测试 ====================

    @Test
    @DisplayName("根据ID获取版本成功")
    void should_GetVersionByIdSuccessfully_When_VersionExists() {
        // Given
        Long versionId = 1L;
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));

        // When
        Optional<FileVersion> result = versionService.getVersionById(versionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testVersion);

        verify(fileVersionRepository).findById(versionId);
    }

    @Test
    @DisplayName("根据ID获取版本失败 - 版本不存在")
    void should_ReturnEmpty_When_VersionNotExists() {
        // Given
        Long versionId = 999L;
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.empty());

        // When
        Optional<FileVersion> result = versionService.getVersionById(versionId);

        // Then
        assertThat(result).isEmpty();

        verify(fileVersionRepository).findById(versionId);
    }

    @Test
    @DisplayName("根据版本号获取版本成功")
    void should_GetVersionByNumberSuccessfully_When_VersionExists() {
        // Given
        Integer versionNumber = 1;
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, versionNumber))
            .thenReturn(Optional.of(testVersion));

        // When
        Optional<FileVersion> result = versionService.getVersionByNumber(testFile, versionNumber);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testVersion);

        verify(fileVersionRepository).findByFileAndVersionNumber(testFile, versionNumber);
    }

    @Test
    @DisplayName("获取最新版本成功")
    void should_GetLatestVersionSuccessfully_When_VersionExists() {
        // Given
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(Optional.of(testVersion));

        // When
        Optional<FileVersion> result = versionService.getLatestVersion(testFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testVersion);

        verify(fileVersionRepository).findFirstByFileOrderByVersionNumberDesc(testFile);
    }

    @Test
    @DisplayName("获取所有版本成功")
    void should_GetAllVersionsSuccessfully_When_VersionsExist() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);

        // When
        List<FileVersion> result = versionService.getAllVersions(testFile, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testVersion);

        verify(fileVersionRepository).findByFileOrderByVersionNumberDesc(testFile);
    }

    @Test
    @DisplayName("分页查询版本成功")
    void should_GetVersionsPagedSuccessfully_When_VersionsExist() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<FileVersion> page = new PageImpl<>(Arrays.asList(testVersion), pageable, 1);
        when(fileVersionRepository.findByFile(testFile, pageable)).thenReturn(page);

        // When
        Page<FileVersion> result = versionService.getVersionsPaged(testFile, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testVersion);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(fileVersionRepository).findByFile(testFile, pageable);
    }

    // ==================== 版本内容访问测试 ====================

    @Test
    @DisplayName("获取版本内容成功")
    void should_GetVersionContentSuccessfully_When_VersionExists() throws IOException {
        // Given
        Long versionId = 1L;
        InputStream expectedStream = new ByteArrayInputStream("content".getBytes());
        
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
        when(storageService.load(testVersion.getStoragePath())).thenReturn(Optional.of(expectedStream));

        // When
        Optional<InputStream> result = versionService.getVersionContent(versionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedStream);

        verify(fileVersionRepository).findById(versionId);
        verify(storageService).load(testVersion.getStoragePath());
    }

    @Test
    @DisplayName("验证版本完整性成功")
    void should_VerifyVersionIntegritySuccessfully_When_VersionIsValid() {
        // Given
        Long versionId = 1L;
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
        when(storageService.exists(testVersion.getStoragePath())).thenReturn(true);
        when(storageService.verifyIntegrity(testVersion.getStoragePath(), testVersion.getFileHash()))
            .thenReturn(true);

        // When
        boolean result = versionService.verifyVersionIntegrity(versionId);

        // Then
        assertThat(result).isTrue();

        verify(fileVersionRepository).findById(versionId);
        verify(storageService).exists(testVersion.getStoragePath());
        verify(storageService).verifyIntegrity(testVersion.getStoragePath(), testVersion.getFileHash());
    }

    // ==================== 版本回滚测试 ====================

    @Test
    @DisplayName("回滚到指定版本成功 - 创建新版本")
    void should_RollbackToVersionSuccessfully_When_CreateNewVersion() throws IOException {
        // Given
        Integer targetVersionNumber = 1;
        Long operatorId = 1L;
        boolean createNewVersion = true;
        
        InputStream contentStream = new ByteArrayInputStream("content".getBytes());
        StorageService.StorageResult storageResult = new StorageService.StorageResult(
            "/storage/test_v2.txt", "def456", 1024L, false);

        FileVersion newVersion = new FileVersion();
        newVersion.setId(2L);
        newVersion.setFile(testFile);
        newVersion.setVersionNumber(2);
        newVersion.setChangeComment("回滚到版本 1");

        when(fileVersionRepository.findByFileAndVersionNumber(testFile, targetVersionNumber))
            .thenReturn(Optional.of(testVersion));
        when(storageService.load(testVersion.getStoragePath())).thenReturn(Optional.of(contentStream));
        when(storageService.store(any(InputStream.class), eq(testFile.getName()), eq(testFile.getMimeType())))
            .thenReturn(storageResult);
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(Optional.of(testVersion));
        when(fileVersionRepository.save(any(FileVersion.class))).thenReturn(newVersion);

        // When
        VersionService.VersionOperationResult result = versionService.rollbackToVersion(
            testFile, targetVersionNumber, operatorId, createNewVersion);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("版本创建成功");
        assertThat(result.getVersion()).isNotNull();

        verify(fileVersionRepository).findByFileAndVersionNumber(testFile, targetVersionNumber);
        verify(storageService).load(testVersion.getStoragePath());
        verify(storageService).store(any(InputStream.class), eq(testFile.getName()), eq(testFile.getMimeType()));
    }

    @Test
    @DisplayName("回滚到上一版本失败 - 没有可回滚的版本")
    void should_FailToRollbackToPrevious_When_NoVersionToRollback() {
        // Given
        testFile.setVersion(1); // 当前是第一个版本
        Long operatorId = 1L;

        // When
        VersionService.VersionOperationResult result = versionService.rollbackToPreviousVersion(testFile, operatorId);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("没有可回滚的版本");
        assertThat(result.getVersion()).isNull();
    }

    // ==================== 版本删除测试 ====================

    @Test
    @DisplayName("删除版本成功")
    void should_DeleteVersionSuccessfully_When_VersionExists() {
        // Given
        Long versionId = 1L;
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
        when(fileVersionRepository.countByFile(testVersion.getFile())).thenReturn(2L); // 有多个版本
        when(storageService.delete(testVersion.getStoragePath())).thenReturn(true);

        // When
        boolean result = versionService.deleteVersion(versionId, 1L);

        // Then
        assertThat(result).isTrue();

        verify(fileVersionRepository).findById(versionId);
        verify(fileVersionRepository).countByFile(testVersion.getFile());
        verify(storageService).delete(testVersion.getStoragePath());
        verify(fileVersionRepository).delete(testVersion);
    }

    @Test
    @DisplayName("删除版本失败 - 唯一版本不能删除")
    void should_FailToDeleteVersion_When_OnlyVersionExists() {
        // Given
        Long versionId = 1L;
        when(fileVersionRepository.findById(versionId)).thenReturn(Optional.of(testVersion));
        when(fileVersionRepository.countByFile(testVersion.getFile())).thenReturn(1L); // 只有一个版本

        // When
        boolean result = versionService.deleteVersion(versionId, 1L);

        // Then
        assertThat(result).isFalse();

        verify(fileVersionRepository).findById(versionId);
        verify(fileVersionRepository).countByFile(testVersion.getFile());
        verify(storageService, never()).delete(any());
        verify(fileVersionRepository, never()).delete(any());
    }

    // ==================== 版本统计测试 ====================

    @Test
    @DisplayName("获取版本统计信息成功")
    void should_GetVersionStatisticsSuccessfully_When_VersionsExist() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion);
        when(fileVersionRepository.findByFileOrderByVersionNumberAsc(testFile)).thenReturn(versions);

        // When
        VersionService.VersionStatistics stats = versionService.getVersionStatistics(testFile);

        // Then
        assertThat(stats.getTotalVersions()).isEqualTo(1);
        assertThat(stats.getTotalSizeBytes()).isEqualTo(1024L);
        assertThat(stats.getMaxVersionNumber()).isEqualTo(1);
        assertThat(stats.getAverageSizeBytes()).isEqualTo(1024L);

        verify(fileVersionRepository).findByFileOrderByVersionNumberAsc(testFile);
    }

    @Test
    @DisplayName("统计版本数量成功")
    void should_CountVersionsSuccessfully_When_VersionsExist() {
        // Given
        when(fileVersionRepository.countByFile(testFile)).thenReturn(1L);

        // When
        long count = versionService.countVersions(testFile);

        // Then
        assertThat(count).isEqualTo(1L);

        verify(fileVersionRepository).countByFile(testFile);
    }

    @Test
    @DisplayName("计算版本总大小成功")
    void should_GetTotalVersionsSizeSuccessfully_When_VersionsExist() {
        // Given
        when(fileVersionRepository.sumSizeBytesByFile(testFile)).thenReturn(1024L);

        // When
        long totalSize = versionService.getTotalVersionsSize(testFile);

        // Then
        assertThat(totalSize).isEqualTo(1024L);

        verify(fileVersionRepository).sumSizeBytesByFile(testFile);
    }

    @Test
    @DisplayName("获取下一个版本号成功")
    void should_GetNextVersionNumberSuccessfully_When_VersionsExist() {
        // Given
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(Optional.of(testVersion));

        // When
        Integer nextVersion = versionService.getNextVersionNumber(testFile);

        // Then
        assertThat(nextVersion).isEqualTo(2);

        verify(fileVersionRepository).findFirstByFileOrderByVersionNumberDesc(testFile);
    }

    @Test
    @DisplayName("获取下一个版本号 - 无版本时返回1")
    void should_ReturnVersionOne_When_NoVersionsExist() {
        // Given
        when(fileVersionRepository.findFirstByFileOrderByVersionNumberDesc(testFile)).thenReturn(Optional.empty());

        // When
        Integer nextVersion = versionService.getNextVersionNumber(testFile);

        // Then
        assertThat(nextVersion).isEqualTo(1);

        verify(fileVersionRepository).findFirstByFileOrderByVersionNumberDesc(testFile);
    }

    // ==================== 版本清理测试 ====================

    @Test
    @DisplayName("按数量清理版本成功")
    void should_CleanupVersionsByCountSuccessfully_When_ExcessVersionsExist() {
        // Given
        FileVersion version2 = new FileVersion();
        version2.setId(2L);
        version2.setFile(testFile);
        version2.setVersionNumber(2);
        version2.setSizeBytes(1024L);
        version2.setStoragePath("/storage/test_v2.txt");

        FileVersion version3 = new FileVersion();
        version3.setId(3L);
        version3.setFile(testFile);
        version3.setVersionNumber(3);
        version3.setSizeBytes(1024L);
        version3.setStoragePath("/storage/test_v3.txt");

        List<FileVersion> allVersions = Arrays.asList(version3, version2, testVersion); // 按版本号倒序

        VersionService.CleanupConfig config = new VersionService.CleanupConfig(
            VersionService.CleanupStrategy.KEEP_COUNT, 2, null, null, false, false);

        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(allVersions);
        when(storageService.delete(testVersion.getStoragePath())).thenReturn(true);

        // When
        VersionService.CleanupResult result = versionService.cleanupFileVersions(testFile, config);

        // Then
        assertThat(result.getTotalVersionsProcessed()).isEqualTo(3);
        assertThat(result.getVersionsDeleted()).isEqualTo(1);
        assertThat(result.getSpaceFreed()).isEqualTo(1024L);
        assertThat(result.isSuccessful()).isTrue();

        verify(fileVersionRepository).findByFileOrderByVersionNumberDesc(testFile);
        verify(storageService).delete(testVersion.getStoragePath());
        verify(fileVersionRepository).delete(testVersion);
    }

    // ==================== 搜索和过滤测试 ====================

    @Test
    @DisplayName("根据注释搜索版本成功")
    void should_SearchVersionsByCommentSuccessfully_When_MatchingVersionsExist() {
        // Given
        String keyword = "初始";
        List<FileVersion> matchingVersions = Arrays.asList(testVersion);
        when(fileVersionRepository.findByFileAndChangeCommentContaining(testFile, keyword))
            .thenReturn(matchingVersions);

        // When
        List<FileVersion> result = versionService.searchVersionsByComment(testFile, keyword);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testVersion);

        verify(fileVersionRepository).findByFileAndChangeCommentContaining(testFile, keyword);
    }

    @Test
    @DisplayName("获取有注释的版本成功")
    void should_GetVersionsWithCommentSuccessfully_When_VersionsExist() {
        // Given
        List<FileVersion> versionsWithComment = Arrays.asList(testVersion);
        when(fileVersionRepository.findVersionsWithCommentByFile(testFile)).thenReturn(versionsWithComment);

        // When
        List<FileVersion> result = versionService.getVersionsWithComment(testFile);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testVersion);

        verify(fileVersionRepository).findVersionsWithCommentByFile(testFile);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("空参数处理 - 文件为空时返回空结果")
    void should_HandleNullFile_When_FileIsNull() {
        // When & Then
        assertThat(versionService.getVersionById(null)).isEmpty();
        assertThat(versionService.getVersionByNumber(null, 1)).isEmpty();
        assertThat(versionService.getLatestVersion(null)).isEmpty();
        assertThat(versionService.getAllVersions(null, true)).isEmpty();
        assertThat(versionService.countVersions(null)).isEqualTo(0);
        assertThat(versionService.getTotalVersionsSize(null)).isEqualTo(0);
        assertThat(versionService.canCreateVersion(null)).isFalse();
        assertThat(versionService.versionExists(null, 1)).isFalse();
    }

    @Test
    @DisplayName("异常处理 - 存储服务异常时正确处理")
    void should_HandleStorageException_When_StorageServiceThrowsException() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        when(storageService.store(any(InputStream.class), any(), any()))
            .thenThrow(new IOException("存储服务异常"));

        // When
        VersionService.VersionOperationResult result = versionService.createVersion(
            testFile, inputStream, "comment", 1L);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("版本创建失败");
        assertThat(result.getException()).isInstanceOf(IOException.class);

        verify(storageService).store(any(InputStream.class), any(), any());
        verify(fileVersionRepository, never()).save(any());
    }
} 