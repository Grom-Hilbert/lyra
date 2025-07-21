package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.service.impl.LocalFileStorageService;
import tslc.beihaiyun.lyra.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 文件存储服务测试
 * 测试LocalFileStorageService的所有核心功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("文件存储服务测试")
class StorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileEntityRepository fileEntityRepository;

    private StorageService storageService;
    private LyraProperties lyraProperties;
    private Path basePath;
    private Path tempPath;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 设置临时目录
        basePath = tempDir.resolve("files");
        tempPath = tempDir.resolve("temp");

        // 配置属性
        lyraProperties = new LyraProperties();
        LyraProperties.StorageConfig storageConfig = new LyraProperties.StorageConfig();
        storageConfig.setBasePath(basePath.toString());
        storageConfig.setTempPath(tempPath.toString());
        storageConfig.setMaxFileSize("10MB");
        storageConfig.setAllowedTypes("*");
        storageConfig.setEnableDeduplication(true);
        storageConfig.setBackend("local");
        lyraProperties.setStorage(storageConfig);

        // 创建服务实例
        storageService = new LocalFileStorageService(lyraProperties, fileEntityRepository);
        ((LocalFileStorageService) storageService).init();
    }

    @Test
    @DisplayName("初始化时应创建存储目录")
    void should_CreateStorageDirectories_When_Initialize() {
        assertTrue(Files.exists(basePath));
        assertTrue(Files.exists(tempPath));
        assertTrue(Files.isDirectory(basePath));
        assertTrue(Files.isDirectory(tempPath));
    }

    @Test
    @DisplayName("存储MultipartFile文件应成功")
    void should_StoreMultipartFile_When_ValidFile() throws IOException {
        // Given
        String content = "测试文件内容";
        MultipartFile file = new MockMultipartFile(
            "test", "test.txt", "text/plain", content.getBytes()
        );

        when(fileEntityRepository.findByFileHash(anyString())).thenReturn(Collections.emptyList());

        // When
        StorageService.StorageResult result = storageService.store(file);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStoragePath());
        assertNotNull(result.getFileHash());
        assertEquals(content.getBytes().length, result.getSizeBytes());
        assertFalse(result.isDuplicate());

        // 验证文件确实存在
        assertTrue(storageService.exists(result.getStoragePath()));
    }

    @Test
    @DisplayName("存储InputStream文件应成功")
    void should_StoreInputStream_When_ValidData() throws IOException {
        // Given
        String content = "通过InputStream存储的测试内容";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        when(fileEntityRepository.findByFileHash(anyString())).thenReturn(Collections.emptyList());

        // When
        StorageService.StorageResult result = storageService.store(inputStream, "stream-test.txt", "text/plain");

        // Then
        assertNotNull(result);
        assertEquals(content.getBytes().length, result.getSizeBytes());
        assertFalse(result.isDuplicate());
        assertTrue(storageService.exists(result.getStoragePath()));
    }

    @Test
    @DisplayName("存储字节数组应成功")
    void should_StoreByteArray_When_ValidData() throws IOException {
        // Given
        String content = "字节数组测试内容";
        byte[] data = content.getBytes();

        when(fileEntityRepository.findByFileHash(anyString())).thenReturn(Collections.emptyList());

        // When
        StorageService.StorageResult result = storageService.store(data, "bytes-test.txt", "text/plain");

        // Then
        assertNotNull(result);
        assertEquals(data.length, result.getSizeBytes());
        assertFalse(result.isDuplicate());
        assertTrue(storageService.exists(result.getStoragePath()));
    }

    @Test
    @DisplayName("文件去重应正常工作")
    void should_DetectDuplicateFiles_When_SameContent() throws IOException {
        // Given
        String content = "重复文件测试内容";
        byte[] data = content.getBytes();
        String expectedHash = FileUtils.calculateSHA256(new ByteArrayInputStream(data));

        // 创建一个已存在的文件实体
        User testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        Space testSpace = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        testSpace.setId(1L);

        FileEntity existingFile = new FileEntity();
        existingFile.setId(1L);
        existingFile.setName("existing.txt");
        existingFile.setOriginalName("existing.txt");
        existingFile.setPath("/existing.txt");
        existingFile.setSpace(testSpace);
        existingFile.setSizeBytes((long) data.length);
        existingFile.setFileHash(expectedHash);
        existingFile.setStoragePath("existing/path.txt");

        // 创建实际的文件
        Path existingFilePath = basePath.resolve("existing/path.txt");
        Files.createDirectories(existingFilePath.getParent());
        Files.write(existingFilePath, data);

        when(fileEntityRepository.findByFileHash(expectedHash))
            .thenReturn(List.of(existingFile));

        // When
        StorageService.StorageResult result = storageService.store(data, "duplicate.txt", "text/plain");

        // Then
        assertTrue(result.isDuplicate());
        assertEquals("existing/path.txt", result.getStoragePath());
        assertEquals(expectedHash, result.getFileHash());
    }

    @Test
    @DisplayName("读取文件应返回正确内容")
    void should_LoadFileContent_When_FileExists() throws IOException {
        // Given
        String content = "要读取的文件内容";
        byte[] data = content.getBytes();
        
        StorageService.StorageResult result = storageService.store(data, "read-test.txt", "text/plain");

        // When
        Optional<InputStream> inputStreamOpt = storageService.load(result.getStoragePath());

        // Then
        assertTrue(inputStreamOpt.isPresent());
        try (InputStream inputStream = inputStreamOpt.get()) {
            byte[] readData = inputStream.readAllBytes();
            assertArrayEquals(data, readData);
        }
    }

    @Test
    @DisplayName("读取不存在的文件应返回空")
    void should_ReturnEmpty_When_FileNotExists() throws IOException {
        // When
        Optional<InputStream> inputStreamOpt = storageService.load("non-existent/file.txt");

        // Then
        assertFalse(inputStreamOpt.isPresent());
    }

    @Test
    @DisplayName("检查文件存在性应正确")
    void should_CheckFileExistence_Correctly() throws IOException {
        // Given
        String content = "存在性测试内容";
        StorageService.StorageResult result = storageService.store(content.getBytes(), "exists-test.txt", "text/plain");

        // When & Then
        assertTrue(storageService.exists(result.getStoragePath()));
        assertFalse(storageService.exists("non-existent/file.txt"));
    }

    @Test
    @DisplayName("删除文件应成功")
    void should_DeleteFile_When_FileExists() throws IOException {
        // Given
        String content = "要删除的文件内容";
        StorageService.StorageResult result = storageService.store(content.getBytes(), "delete-test.txt", "text/plain");

        assertTrue(storageService.exists(result.getStoragePath()));

        // When
        boolean deleted = storageService.delete(result.getStoragePath());

        // Then
        assertTrue(deleted);
        assertFalse(storageService.exists(result.getStoragePath()));
    }

    @Test
    @DisplayName("删除不存在的文件应返回false")
    void should_ReturnFalse_When_DeleteNonExistentFile() {
        // When
        boolean deleted = storageService.delete("non-existent/file.txt");

        // Then
        assertFalse(deleted);
    }

    @Test
    @DisplayName("获取文件大小应正确")
    void should_GetCorrectFileSize_When_FileExists() throws IOException {
        // Given
        String content = "文件大小测试内容";
        byte[] data = content.getBytes();
        StorageService.StorageResult result = storageService.store(data, "size-test.txt", "text/plain");

        // When
        long size = storageService.getFileSize(result.getStoragePath());

        // Then
        assertEquals(data.length, size);
    }

    @Test
    @DisplayName("获取不存在文件的大小应返回-1")
    void should_ReturnMinusOne_When_GetSizeOfNonExistentFile() {
        // When
        long size = storageService.getFileSize("non-existent/file.txt");

        // Then
        assertEquals(-1, size);
    }

    @Test
    @DisplayName("计算文件哈希值应正确")
    void should_CalculateCorrectHash_When_ValidInputStream() throws IOException {
        // Given
        String content = "哈希值计算测试";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        // When
        String hash = storageService.calculateHash(inputStream);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 产生64位十六进制字符串
    }

    @Test
    @DisplayName("验证文件完整性应正确")
    void should_VerifyIntegrity_Correctly() throws IOException {
        // Given
        String content = "完整性验证测试内容";
        byte[] data = content.getBytes();
        StorageService.StorageResult result = storageService.store(data, "integrity-test.txt", "text/plain");

        // When & Then
        assertTrue(storageService.verifyIntegrity(result.getStoragePath(), result.getFileHash()));
        assertFalse(storageService.verifyIntegrity(result.getStoragePath(), "wrong-hash"));
        assertFalse(storageService.verifyIntegrity("non-existent/file.txt", result.getFileHash()));
    }

    @Test
    @DisplayName("复制文件应成功")
    void should_CopyFile_When_SourceExists() throws IOException {
        // Given
        String content = "复制测试内容";
        StorageService.StorageResult result = storageService.store(content.getBytes(), "copy-source.txt", "text/plain");
        String targetPath = "copy-target.txt";

        // When
        boolean copied = storageService.copy(result.getStoragePath(), targetPath);

        // Then
        assertTrue(copied);
        assertTrue(storageService.exists(result.getStoragePath())); // 源文件还在
        assertTrue(storageService.exists(targetPath)); // 目标文件存在
        assertEquals(storageService.getFileSize(result.getStoragePath()), storageService.getFileSize(targetPath));
    }

    @Test
    @DisplayName("移动文件应成功")
    void should_MoveFile_When_SourceExists() throws IOException {
        // Given
        String content = "移动测试内容";
        StorageService.StorageResult result = storageService.store(content.getBytes(), "move-source.txt", "text/plain");
        String targetPath = "move-target.txt";

        // When
        boolean moved = storageService.move(result.getStoragePath(), targetPath);

        // Then
        assertTrue(moved);
        assertFalse(storageService.exists(result.getStoragePath())); // 源文件不存在了
        assertTrue(storageService.exists(targetPath)); // 目标文件存在
    }

    @Test
    @DisplayName("清理临时文件应工作")
    void should_CleanupTempFiles_When_OldFilesExist() throws IOException {
        // Given
        Path oldTempFile = tempPath.resolve("old-temp-file.tmp");
        Files.write(oldTempFile, "临时文件内容".getBytes());

        // 修改文件的最后修改时间为2小时前
        Files.setLastModifiedTime(oldTempFile, 
            java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 2 * 60 * 60 * 1000));

        // When
        int cleanedCount = storageService.cleanupTempFiles();

        // Then
        assertEquals(1, cleanedCount);
        assertFalse(Files.exists(oldTempFile));
    }

    @Test
    @DisplayName("获取存储统计信息应成功")
    void should_GetStorageStats_Successfully() throws IOException {
        // Given
        storageService.store("测试文件1".getBytes(), "stats-test1.txt", "text/plain");
        storageService.store("测试文件2".getBytes(), "stats-test2.txt", "text/plain");

        // When
        StorageService.StorageStats stats = storageService.getStorageStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.getTotalSpace() > 0);
        assertTrue(stats.getFreeSpace() >= 0);
        assertTrue(stats.getUsedSpace() >= 0);
        assertTrue(stats.getFileCount() >= 2);
        assertTrue(stats.getUsagePercentage() >= 0 && stats.getUsagePercentage() <= 100);
    }

    @Test
    @DisplayName("存储空文件应抛出异常")
    void should_ThrowException_When_StoreEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("empty", "", "text/plain", new byte[0]);

        // When & Then
        assertThrows(IOException.class, () -> storageService.store(emptyFile));
    }

    @Test
    @DisplayName("存储null文件应抛出异常")
    void should_ThrowException_When_StoreNullFile() {
        // When & Then
        assertThrows(IOException.class, () -> storageService.store((MultipartFile) null));
    }

    @Test
    @DisplayName("获取文件路径应正确")
    void should_GetCorrectPath_When_FileExists() throws IOException {
        // Given
        String content = "路径测试内容";
        StorageService.StorageResult result = storageService.store(content.getBytes(), "path-test.txt", "text/plain");

        // When
        Optional<Path> pathOpt = storageService.getPath(result.getStoragePath());

        // Then
        assertTrue(pathOpt.isPresent());
        assertTrue(Files.exists(pathOpt.get()));
        assertEquals(basePath.resolve(result.getStoragePath()), pathOpt.get());
    }

    @Test
    @DisplayName("获取不存在文件的路径应返回空")
    void should_ReturnEmpty_When_GetPathOfNonExistentFile() {
        // When
        Optional<Path> pathOpt = storageService.getPath("non-existent/file.txt");

        // Then
        assertFalse(pathOpt.isPresent());
    }

    @Test
    @DisplayName("查找重复文件应正确工作")
    void should_FindDuplicateFile_When_Enabled() throws IOException {
        // Given
        String content = "重复文件查找测试";
        byte[] data = content.getBytes();
        String hash = FileUtils.calculateSHA256(new ByteArrayInputStream(data));

        User testUser = new User("testuser", "test@example.com", "password");
        testUser.setId(1L);
        Space testSpace = new Space("测试空间", Space.SpaceType.PERSONAL, testUser);
        testSpace.setId(1L);

        FileEntity existingFile = new FileEntity();
        existingFile.setStoragePath("existing/duplicate.txt");

        // 创建实际文件
        Path existingPath = basePath.resolve("existing/duplicate.txt");
        Files.createDirectories(existingPath.getParent());
        Files.write(existingPath, data);

        when(fileEntityRepository.findByFileHash(hash)).thenReturn(List.of(existingFile));

        // When
        Optional<String> duplicatePath = storageService.findDuplicateFile(hash);

        // Then
        assertTrue(duplicatePath.isPresent());
        assertEquals("existing/duplicate.txt", duplicatePath.get());
    }

    @Test
    @DisplayName("查找重复文件在禁用去重时应返回空")
    void should_ReturnEmpty_When_DeduplicationDisabled() {
        // Given - 重新配置服务禁用去重
        LyraProperties.StorageConfig config = lyraProperties.getStorage();
        config.setEnableDeduplication(false);
        
        StorageService disabledDeduplicationService = new LocalFileStorageService(lyraProperties, fileEntityRepository);
        ((LocalFileStorageService) disabledDeduplicationService).init();

        // When
        Optional<String> duplicatePath = disabledDeduplicationService.findDuplicateFile("any-hash");

        // Then
        assertFalse(duplicatePath.isPresent());
    }
} 