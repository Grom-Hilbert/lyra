package tslc.beihaiyun.lyra.storage.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tslc.beihaiyun.lyra.storage.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 内存存储服务测试
 */
class InMemoryStorageServiceTest {
    
    private InMemoryStorageService storageService;
    
    @BeforeEach
    void setUp() {
        storageService = new InMemoryStorageService(1024); // 1KB限制
    }
    
    @Test
    void shouldStoreAndRetrieveFile() throws StorageException, IOException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, Memory!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        // When - Store
        StorageResult result = storageService.store(key, inputStream, content.length(), "text/plain");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getSize()).isEqualTo(content.length());
        
        // When - Retrieve
        Optional<InputStream> retrieved = storageService.retrieve(key);
        
        // Then
        assertThat(retrieved).isPresent();
        String retrievedContent = new String(retrieved.get().readAllBytes());
        assertThat(retrievedContent).isEqualTo(content);
    }
    
    @Test
    void shouldDeleteFile() throws StorageException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, Memory!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        boolean deleted = storageService.delete(key);
        
        // Then
        assertThat(deleted).isTrue();
        assertThat(storageService.exists(key)).isFalse();
        assertThat(storageService.size()).isEqualTo(0);
    }
    
    @Test
    void shouldCopyFile() throws StorageException, IOException {
        // Given
        String sourceKey = "source.txt";
        String targetKey = "target.txt";
        String content = "Copy test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = storageService.copy(sourceKey, targetKey);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(storageService.exists(sourceKey)).isTrue();
        assertThat(storageService.exists(targetKey)).isTrue();
        assertThat(storageService.size()).isEqualTo(2);
        
        // Verify content
        Optional<InputStream> targetContent = storageService.retrieve(targetKey);
        assertThat(targetContent).isPresent();
        String targetText = new String(targetContent.get().readAllBytes());
        assertThat(targetText).isEqualTo(content);
    }
    
    @Test
    void shouldMoveFile() throws StorageException, IOException {
        // Given
        String sourceKey = "source.txt";
        String targetKey = "target.txt";
        String content = "Move test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = storageService.move(sourceKey, targetKey);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(storageService.exists(sourceKey)).isFalse();
        assertThat(storageService.exists(targetKey)).isTrue();
        assertThat(storageService.size()).isEqualTo(1);
        
        // Verify content
        Optional<InputStream> targetContent = storageService.retrieve(targetKey);
        assertThat(targetContent).isPresent();
        String targetText = new String(targetContent.get().readAllBytes());
        assertThat(targetText).isEqualTo(content);
    }
    
    @Test
    void shouldGetMetadata() throws StorageException {
        // Given
        String key = "test/file.txt";
        String content = "Metadata test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        Optional<StorageMetadata> metadata = storageService.getMetadata(key);
        
        // Then
        assertThat(metadata).isPresent();
        StorageMetadata meta = metadata.get();
        assertThat(meta.getKey()).isEqualTo(key);
        assertThat(meta.getSize()).isEqualTo(content.length());
        assertThat(meta.getContentType()).isEqualTo("text/plain");
        assertThat(meta.getStorageType()).isEqualTo(StorageType.IN_MEMORY);
        assertThat(meta.getStoragePath()).isEqualTo("memory://" + key);
    }
    
    @Test
    void shouldGetStats() throws StorageException {
        // Given
        String content = "Stats test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store("file1.txt", inputStream, content.length(), "text/plain");
        
        inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store("file2.txt", inputStream, content.length(), "text/plain");
        
        // When
        StorageStats stats = storageService.getStats();
        
        // Then
        assertThat(stats.getStorageType()).isEqualTo(StorageType.IN_MEMORY);
        assertThat(stats.getFileCount()).isEqualTo(2);
        assertThat(stats.getUsedSpace()).isEqualTo(content.length() * 2);
        assertThat(stats.isHealthy()).isTrue();
    }
    
    @Test
    void shouldReturnCorrectStorageType() {
        // When
        StorageType type = storageService.getStorageType();
        
        // Then
        assertThat(type).isEqualTo(StorageType.IN_MEMORY);
    }
    
    @Test
    void shouldThrowExceptionForFileSizeExceeded() {
        // Given
        String key = "large.txt";
        byte[] largeContent = new byte[2048]; // 2KB，超过1KB限制
        InputStream inputStream = new ByteArrayInputStream(largeContent);
        
        // When & Then
        assertThatThrownBy(() -> storageService.store(key, inputStream, largeContent.length, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("文件大小超过限制");
    }
    
    @Test
    void shouldClearAllData() throws StorageException {
        // Given
        String content = "Clear test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store("file1.txt", inputStream, content.length(), "text/plain");
        
        inputStream = new ByteArrayInputStream(content.getBytes());
        storageService.store("file2.txt", inputStream, content.length(), "text/plain");
        
        assertThat(storageService.size()).isEqualTo(2);
        
        // When
        storageService.clear();
        
        // Then
        assertThat(storageService.size()).isEqualTo(0);
        assertThat(storageService.exists("file1.txt")).isFalse();
        assertThat(storageService.exists("file2.txt")).isFalse();
    }
}