package tslc.beihaiyun.lyra.storage.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.storage.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * NFS存储服务测试
 * 注意：这些测试使用临时目录模拟NFS挂载点，实际的NFS功能需要真实的NFS环境
 */
@ExtendWith(MockitoExtension.class)
class NFSStorageServiceTest {
    
    @TempDir
    Path tempDir;
    
    private NFSStorageService nfsStorageService;
    
    @BeforeEach
    void setUp() {
        // 使用临时目录模拟NFS挂载点进行测试
        // 实际环境中需要真实的NFS服务器
        String mockNfsServer = "test-nfs-server";
        String mockExportPath = "/test/export";
        String mockMountPoint = tempDir.toString();
        String mockMountOptions = "rw,sync,hard,intr";
        long maxFileSize = 1024 * 1024; // 1MB for testing
        int connectionTimeout = 5000;
        int readTimeout = 10000;
        int retryCount = 2;
        
        // 创建一个测试用的NFS存储服务实例
        // 注意：这里我们需要创建一个可测试的版本，跳过实际的NFS挂载
        nfsStorageService = new TestableNFSStorageService(
            mockNfsServer, mockExportPath, mockMountPoint, mockMountOptions,
            maxFileSize, connectionTimeout, readTimeout, retryCount
        );
        
        // 手动调用初始化方法，因为@PostConstruct在测试中不会自动调用
        ((TestableNFSStorageService) nfsStorageService).initializeNFS();
    }
    
    @Test
    void shouldStoreFileSuccessfully() throws StorageException, IOException {
        // Given
        String key = "test/file.txt";
        String content = "Hello, NFS World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        
        // When
        StorageResult result = nfsStorageService.store(key, inputStream, content.length(), "text/plain");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getSize()).isEqualTo(content.length());
        assertThat(result.getChecksum()).isNotNull();
        assertThat(result.getContentType()).isEqualTo("text/plain");
        
        // Verify file exists
        assertThat(nfsStorageService.exists(key)).isTrue();
    }
    
    @Test
    void shouldRetrieveFileSuccessfully() throws StorageException, IOException {
        // Given
        String key = "test/retrieve.txt";
        String content = "Content to retrieve";
        InputStream storeStream = new ByteArrayInputStream(content.getBytes());
        nfsStorageService.store(key, storeStream, content.length(), "text/plain");
        
        // When
        Optional<InputStream> result = nfsStorageService.retrieve(key);
        
        // Then
        assertThat(result).isPresent();
        
        try (InputStream retrievedStream = result.get()) {
            String retrievedContent = new String(retrievedStream.readAllBytes());
            assertThat(retrievedContent).isEqualTo(content);
        }
    }
    
    @Test
    void shouldReturnEmptyWhenFileNotExists() throws StorageException {
        // Given
        String nonExistentKey = "non/existent/file.txt";
        
        // When
        Optional<InputStream> result = nfsStorageService.retrieve(nonExistentKey);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldDeleteFileSuccessfully() throws StorageException {
        // Given
        String key = "test/delete.txt";
        String content = "Content to delete";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        nfsStorageService.store(key, inputStream, content.length(), "text/plain");
        
        // Verify file exists
        assertThat(nfsStorageService.exists(key)).isTrue();
        
        // When
        boolean deleted = nfsStorageService.delete(key);
        
        // Then
        assertThat(deleted).isTrue();
        assertThat(nfsStorageService.exists(key)).isFalse();
    }
    
    @Test
    void shouldReturnFalseWhenDeletingNonExistentFile() throws StorageException {
        // Given
        String nonExistentKey = "non/existent/file.txt";
        
        // When
        boolean deleted = nfsStorageService.delete(nonExistentKey);
        
        // Then
        assertThat(deleted).isFalse();
    }
    
    @Test
    void shouldGetMetadataSuccessfully() throws StorageException {
        // Given
        String key = "test/metadata.txt";
        String content = "Content for metadata test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        nfsStorageService.store(key, inputStream, content.length(), "text/plain");
        
        // When
        Optional<StorageMetadata> metadata = nfsStorageService.getMetadata(key);
        
        // Then
        assertThat(metadata).isPresent();
        StorageMetadata meta = metadata.get();
        assertThat(meta.getKey()).isEqualTo(key);
        assertThat(meta.getSize()).isEqualTo(content.length());
        assertThat(meta.getStorageType()).isEqualTo(StorageType.NFS);
        assertThat(meta.getCreatedAt()).isNotNull();
        assertThat(meta.getLastModified()).isNotNull();
        assertThat(meta.getCustomMetadata()).containsKey("nfs.server");
    }
    
    @Test
    void shouldCopyFileSuccessfully() throws StorageException {
        // Given
        String sourceKey = "test/source.txt";
        String targetKey = "test/target.txt";
        String content = "Content to copy";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        nfsStorageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = nfsStorageService.copy(sourceKey, targetKey);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(targetKey);
        assertThat(result.getSize()).isEqualTo(content.length());
        
        // Verify both files exist
        assertThat(nfsStorageService.exists(sourceKey)).isTrue();
        assertThat(nfsStorageService.exists(targetKey)).isTrue();
    }
    
    @Test
    void shouldMoveFileSuccessfully() throws StorageException {
        // Given
        String sourceKey = "test/source-move.txt";
        String targetKey = "test/target-move.txt";
        String content = "Content to move";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        nfsStorageService.store(sourceKey, inputStream, content.length(), "text/plain");
        
        // When
        StorageResult result = nfsStorageService.move(sourceKey, targetKey);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getKey()).isEqualTo(targetKey);
        assertThat(result.getSize()).isEqualTo(content.length());
        
        // Verify source file no longer exists and target file exists
        assertThat(nfsStorageService.exists(sourceKey)).isFalse();
        assertThat(nfsStorageService.exists(targetKey)).isTrue();
    }
    
    @Test
    void shouldThrowExceptionForInvalidKey() {
        // Given
        String invalidKey = "../../../etc/passwd";
        InputStream inputStream = new ByteArrayInputStream("malicious content".getBytes());
        
        // When & Then
        assertThatThrownBy(() -> nfsStorageService.store(invalidKey, inputStream, 16, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("非法的存储键值");
    }
    
    @Test
    void shouldThrowExceptionForFileSizeExceeded() {
        // Given
        String key = "test/large-file.txt";
        long largeSize = 2 * 1024 * 1024; // 2MB, exceeds 1MB limit
        InputStream inputStream = new ByteArrayInputStream(new byte[(int) largeSize]);
        
        // When & Then
        assertThatThrownBy(() -> nfsStorageService.store(key, inputStream, largeSize, "text/plain"))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("文件大小超过限制");
    }
    
    @Test
    void shouldReturnCorrectStorageType() {
        // When
        StorageType type = nfsStorageService.getStorageType();
        
        // Then
        assertThat(type).isEqualTo(StorageType.NFS);
    }
    
    @Test
    void shouldReturnStorageStats() {
        // When
        StorageStats stats = nfsStorageService.getStats();
        
        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getStorageType()).isEqualTo(StorageType.NFS);
        assertThat(stats.isHealthy()).isTrue();
        assertThat(stats.getHealthMessage()).isNotNull();
    }
    
    /**
     * 可测试的NFS存储服务，跳过实际的NFS挂载操作
     */
    private class TestableNFSStorageService extends NFSStorageService {
        
        public TestableNFSStorageService(String nfsServer, String nfsExportPath, 
                                       String mountPoint, String mountOptions,
                                       long maxFileSize, int connectionTimeout, 
                                       int readTimeout, int retryCount) {
            super(nfsServer, nfsExportPath, mountPoint, mountOptions, 
                  maxFileSize, connectionTimeout, readTimeout, retryCount);
        }
        
        public void initializeNFS() {
            // 跳过实际的NFS挂载，直接标记为已挂载用于测试
            // 在真实环境中，这个方法会执行实际的NFS挂载操作
            try {
                // 创建挂载点目录（使用临时目录）
                Files.createDirectories(tempDir);
                
                // 设置挂载状态 - 使用反射访问私有字段
                setPrivateField("mounted", true);
                setPrivateField("healthy", true);
                setPrivateField("healthMessage", "NFS存储正常（测试模式）");
                
            } catch (Exception e) {
                throw new RuntimeException("测试初始化失败", e);
            }
        }
        
        private void setPrivateField(String fieldName, Object value) throws Exception {
            java.lang.reflect.Field field = NFSStorageService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, value);
        }
        
        @Override
        public void cleanup() {
            // 测试环境下不需要实际卸载
        }
    }
}