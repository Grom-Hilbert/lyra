package tslc.beihaiyun.lyra.storage.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tslc.beihaiyun.lyra.storage.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 本地文件存储服务测试
 */
class LocalFileStorageServiceTest {
    
    @TempDir
    Path tempDir;
    
    private LocalFileStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDir.toString(), 1024 * 1024); // 1MB限制
    }
    
    @Test
    void shouldStoreFileSuccessfully() throws StorageException, IOException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        // When
        StorageResult result = storageService.store(key, inputStream, content.length(), "text/plain");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getSize()).isEqualTo(content.length());
        assertThat(result.getChecksum()).isNotNull();
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getStoredAt()).isNotNull();
        
        // 验证文件确实被存储
        assertThat(storageService.exists(key)).isTrue();
    }
    
    @Test
    void shouldRetrieveFileSuccessfully() throws StorageException, IOException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        Optional<InputStream> retrieved = storageService.retrieve(key);
        
        // Then
        assertThat(retrieved).isPresent();
        String retrievedContent = new String(retrieved.get().readAllBytes());
        assertThat(retrievedContent).isEqualTo(content);
    }
    
    @Test
    void shouldReturnEmptyWhenFileNotExists() throws StorageException {
        // Given
        String key = "nonexistent/file.txt";
        
        // When
        Optional<InputStream> result = storageService.retrieve(key);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldDeleteFileSuccessfully() throws StorageException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        boolean deleted = storageService.delete(key);
        
        // Then
        assertThat(deleted).isTrue();
        assertThat(storageService.exists(key)).isFalse();
    }
    
    @Test
    void shouldReturnFalseWhenDeletingNonexistentFile() throws StorageException {
        // Given
        String key = "nonexistent/file.txt";
        
        // When
        boolean deleted = storageService.delete(key);
        
        // Then
        assertThat(deleted).isFalse();
    }
    
    @Test
    void shouldCheckFileExistence() throws StorageException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        // When & Then - 文件不存在
        assertThat(storageService.exists(key)).isFalse();
        
        // 存储文件
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When & Then - 文件存在
        assertThat(storageService.exists(key)).isTrue();
    }
    
    @Test
    void shouldGetFileMetadata() throws StorageException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        Optional<StorageMetadata> metadata = storageService.getMetadata(key);
        
        // Then
        assertThat(metadata).isPresent();
        StorageMetadata meta = metadata.get();
        assertThat(meta.getKey()).isEqualTo(key);
        assertThat(meta.getSize()).isEqualTo(content.length());
        assertThat(meta.getStorageType()).isEqualTo(StorageType.LOCAL_FILESYSTEM);
        assertThat(meta.getCreatedAt()).isNotNull();
        assertThat(meta.getLastModified()).isNotNull();
        assertThat(meta.getStoragePath()).contains(key.replace('/', File.separatorChar));
    }
    
    @Test
    void shouldReturnEmptyMetadataForNonexistentFile() throws StorageException {
        // Given
        String key = "nonexistent/file.txt";
        
        // When
        Optional<StorageMetadata> metadata = storageService.getMetadata(key);
        
        // Then
        assertThat(metadata).isEmpty();
    }
    
    @Test
    void shouldCopyFileSuccessfully() throws StorageException, IOException {
        // Given
        String sourceKey = "source/file.txt";
        String targetKey = "target/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = storageService.copy(sourceKey, targetKey);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(targetKey);
        
        // 验证两个文件都存在
        assertThat(storageService.exists(sourceKey)).isTrue();
        assertThat(storageService.exists(targetKey)).isTrue();
        
        // 验证内容相同
        Optional<InputStream> sourceContent = storageService.retrieve(sourceKey);
        Optional<InputStream> targetContent = storageService.retrieve(targetKey);
        assertThat(sourceContent).isPresent();
        assertThat(targetContent).isPresent();
        
        String sourceText = new String(sourceContent.get().readAllBytes());
        String targetText = new String(targetContent.get().readAllBytes());
        assertThat(sourceText).isEqualTo(targetText);
    }
    
    @Test
    void shouldMoveFileSuccessfully() throws StorageException, IOException {
        // Given
        String sourceKey = "source/file.txt";
        String targetKey = "target/file.txt";
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = storageService.move(sourceKey, targetKey);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(targetKey);
        
        // 验证源文件不存在，目标文件存在
        assertThat(storageService.exists(sourceKey)).isFalse();
        assertThat(storageService.exists(targetKey)).isTrue();
        
        // 验证内容正确
        Optional<InputStream> targetContent = storageService.retrieve(targetKey);
        assertThat(targetContent).isPresent();
        String targetText = new String(targetContent.get().readAllBytes());
        assertThat(targetText).isEqualTo(content);
    }
    
    @Test
    void shouldThrowExceptionForInvalidKey() {
        // Given
        String invalidKey = "../../../etc/passwd";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        
        // When & Then
        assertThatThrownBy(() -> storageService.store(invalidKey, inputStream, 7, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("非法的存储键值");
    }
    
    @Test
    void shouldThrowExceptionForEmptyKey() {
        // Given
        String emptyKey = "";
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        
        // When & Then
        assertThatThrownBy(() -> storageService.store(emptyKey, inputStream, 7, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("存储键值不能为空");
    }
    
    @Test
    void shouldThrowExceptionForFileSizeExceeded() {
        // Given
        String key = "large/file.txt";
        byte[] largeContent = new byte[2 * 1024 * 1024]; // 2MB，超过1MB限制
        InputStream inputStream = new ByteArrayInputStream(largeContent);
        
        // When & Then
        assertThatThrownBy(() -> storageService.store(key, inputStream, largeContent.length, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("文件大小超过限制");
    }
    
    @Test
    void shouldGetStorageStats() {
        // When
        StorageStats stats = storageService.getStats();
        
        // Then
        assertThat(stats.getStorageType()).isEqualTo(StorageType.LOCAL_FILESYSTEM);
        assertThat(stats.isHealthy()).isTrue();
        assertThat(stats.getTotalSpace()).isGreaterThan(0);
        assertThat(stats.getAvailableSpace()).isGreaterThan(0);
        assertThat(stats.getFileCount()).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void shouldReturnCorrectStorageType() {
        // When
        StorageType type = storageService.getStorageType();
        
        // Then
        assertThat(type).isEqualTo(StorageType.LOCAL_FILESYSTEM);
    }
}