package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.FileEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FileCacheService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
class FileCacheServiceTest {

    @Mock
    private LyraProperties lyraProperties;
    
    @Mock
    private StorageService storageService;

    private FileCacheService fileCacheService;

    @BeforeEach
    void setUp() {
        fileCacheService = new FileCacheService(lyraProperties, storageService);
    }

    @Test
    void testGetFileMetadata() {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "test.txt", 1024L);

        // 执行测试
        Optional<FileEntity> result = fileCacheService.getFileMetadata(fileId, file);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(file, result.get());
    }

    @Test
    void testGetFileMetadata_NullFile() {
        // 执行测试
        Optional<FileEntity> result = fileCacheService.getFileMetadata(1L, null);

        // 验证结果
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateFileMetadata() {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "test.txt", 1024L);

        // 执行测试
        Optional<FileEntity> result = fileCacheService.updateFileMetadata(fileId, file);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(file, result.get());
    }

    @Test
    void testGetFileContent_SmallFile() throws IOException {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "small.txt", 512L); // 小于1MB
        byte[] testContent = "test content".getBytes();
        
        when(storageService.load(anyString()))
            .thenReturn(Optional.of(new ByteArrayInputStream(testContent)));

        // 执行测试
        Optional<byte[]> result = fileCacheService.getFileContent(fileId, file);

        // 验证结果
        assertTrue(result.isPresent());
        assertArrayEquals(testContent, result.get());
        verify(storageService).load(file.getStoragePath());
    }

    @Test
    void testGetFileContent_LargeFile() {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "large.txt", 2 * 1024 * 1024L); // 大于1MB

        // 执行测试
        Optional<byte[]> result = fileCacheService.getFileContent(fileId, file);

        // 验证结果
        assertFalse(result.isPresent());
        try {
            verify(storageService, never()).load(anyString());
        } catch (IOException e) {
            // 不应该到达这里
            fail("不应该调用storageService.load方法");
        }
    }

    @Test
    void testGetFileContent_NullFile() {
        // 执行测试
        Optional<byte[]> result = fileCacheService.getFileContent(1L, null);

        // 验证结果
        assertFalse(result.isPresent());
        try {
            verify(storageService, never()).load(anyString());
        } catch (IOException e) {
            // 不应该到达这里
            fail("不应该调用storageService.load方法");
        }
    }

    @Test
    void testGetFileContent_StorageException() throws IOException {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "error.txt", 512L);
        
        when(storageService.load(anyString()))
            .thenThrow(new IOException("Storage error"));

        // 执行测试
        Optional<byte[]> result = fileCacheService.getFileContent(fileId, file);

        // 验证结果
        assertFalse(result.isPresent());
        verify(storageService).load(file.getStoragePath());
    }

    @Test
    void testUpdateFileContent() {
        // 准备测试数据
        Long fileId = 1L;
        byte[] content = "updated content".getBytes();

        // 执行测试
        Optional<byte[]> result = fileCacheService.updateFileContent(fileId, content);

        // 验证结果
        assertTrue(result.isPresent());
        assertArrayEquals(content, result.get());
    }

    @Test
    void testUpdateFileContent_LargeContent() {
        // 准备测试数据
        Long fileId = 1L;
        byte[] largeContent = new byte[2 * 1024 * 1024]; // 大于1MB

        // 执行测试 - 应该仍然可以更新，但可能不会被缓存
        Optional<byte[]> result = fileCacheService.updateFileContent(fileId, largeContent);

        // 验证结果
        assertTrue(result.isPresent());
        assertArrayEquals(largeContent, result.get());
    }

    @Test
    void testGetCachedFileStream_SmallFile() throws IOException {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "small.txt", 512L);
        byte[] testContent = "test stream content".getBytes();
        
        when(storageService.load(anyString()))
            .thenReturn(Optional.of(new ByteArrayInputStream(testContent)));

        // 执行测试
        Optional<InputStream> result = fileCacheService.getCachedFileStream(fileId, file);

        // 验证结果
        assertTrue(result.isPresent());
        
        // 读取流内容验证
        try (InputStream inputStream = result.get()) {
            byte[] readContent = inputStream.readAllBytes();
            assertArrayEquals(testContent, readContent);
        }
    }

    @Test
    void testGetCachedFileStream_LargeFile() throws IOException {
        // 准备测试数据
        Long fileId = 1L;
        FileEntity file = createTestFile(fileId, "large.txt", 2 * 1024 * 1024L);
        byte[] testContent = "large file content".getBytes();
        
        when(storageService.load(anyString()))
            .thenReturn(Optional.of(new ByteArrayInputStream(testContent)));

        // 执行测试
        Optional<InputStream> result = fileCacheService.getCachedFileStream(fileId, file);

        // 验证结果
        assertTrue(result.isPresent());
        verify(storageService).load(file.getStoragePath());
    }

    @Test
    void testGetCachedFileStream_NullFile() {
        // 执行测试
        Optional<InputStream> result = fileCacheService.getCachedFileStream(1L, null);

        // 验证结果
        assertFalse(result.isPresent());
        try {
            verify(storageService, never()).load(anyString());
        } catch (IOException e) {
            // 不应该到达这里
            fail("不应该调用storageService.load方法");
        }
    }

    @Test
    void testWarmUpFileCache() {
        // 准备测试数据
        List<FileEntity> hotFiles = Arrays.asList(
            createTestFile(1L, "file1.txt", 512L),
            createTestFile(2L, "file2.txt", 1024L),
            createTestFile(3L, "file3.txt", 2 * 1024 * 1024L) // 大文件
        );

        // 执行测试
        assertDoesNotThrow(() -> fileCacheService.warmUpFileCache(hotFiles));
    }

    @Test
    void testWarmUpFileCache_EmptyList() {
        // 执行测试
        assertDoesNotThrow(() -> fileCacheService.warmUpFileCache(Arrays.asList()));
    }

    @Test
    void testWarmUpFileCache_NullList() {
        // 执行测试
        assertDoesNotThrow(() -> fileCacheService.warmUpFileCache(null));
    }

    @Test
    void testGetFileCacheStats() {
        // 执行测试
        FileCacheService.FileCacheStats stats = fileCacheService.getFileCacheStats();

        // 验证结果
        assertNotNull(stats);
        assertEquals(0, stats.getMetadataCacheHits());
        assertEquals(0, stats.getMetadataCacheMisses());
        assertEquals(0, stats.getContentCacheHits());
        assertEquals(0, stats.getContentCacheMisses());
        assertEquals(0, stats.getCachedFileCount());
        assertEquals(0, stats.getTotalCacheSize());
    }

    /**
     * 创建测试文件实体
     */
    private FileEntity createTestFile(Long id, String name, Long size) {
        FileEntity file = new FileEntity();
        file.setId(id);
        file.setName(name);
        file.setSizeBytes(size);
        file.setStoragePath("/test/path/" + name);
        file.setMimeType("text/plain");
        file.setStatus(FileEntity.FileStatus.ACTIVE);
        file.setCreatedAt(LocalDateTime.now());
        file.setLastModifiedAt(LocalDateTime.now());
        return file;
    }
}
