package tslc.beihaiyun.lyra.storage.impl;

import tslc.beihaiyun.lyra.storage.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存存储服务实现（仅用于测试）
 */
public class InMemoryStorageService implements StorageService {
    
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final Map<String, StorageMetadata> metadata = new ConcurrentHashMap<>();
    private final long maxFileSize;
    
    public InMemoryStorageService() {
        this(1024 * 1024); // 默认1MB
    }
    
    public InMemoryStorageService(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    @Override
    public StorageResult store(String key, InputStream inputStream, long contentLength, String contentType) throws StorageException {
        validateKey(key);
        validateContentLength(contentLength);
        
        try {
            byte[] data = inputStream.readAllBytes();
            
            if (data.length > maxFileSize) {
                throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                    "文件大小超过限制: " + maxFileSize + " 字节");
            }
            
            storage.put(key, data);
            
            StorageMetadata meta = StorageMetadata.builder()
                .key(key)
                .size(data.length)
                .contentType(contentType)
                .createdAt(LocalDateTime.now())
                .lastModified(LocalDateTime.now())
                .storagePath("memory://" + key)
                .storageType(StorageType.IN_MEMORY)
                .customMetadata(new HashMap<>())
                .build();
            
            metadata.put(key, meta);
            
            return StorageResult.builder()
                .key(key)
                .size(data.length)
                .checksum(String.valueOf(data.hashCode()))
                .contentType(contentType)
                .storedAt(LocalDateTime.now())
                .storagePath("memory://" + key)
                .success(true)
                .build();
                
        } catch (IOException e) {
            throw new StorageException(StorageErrorType.IO_ERROR, "读取输入流失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<InputStream> retrieve(String key) throws StorageException {
        validateKey(key);
        
        byte[] data = storage.get(key);
        if (data == null) {
            return Optional.empty();
        }
        
        return Optional.of(new ByteArrayInputStream(data));
    }
    
    @Override
    public boolean delete(String key) throws StorageException {
        validateKey(key);
        
        boolean existed = storage.containsKey(key);
        storage.remove(key);
        metadata.remove(key);
        return existed;
    }
    
    @Override
    public boolean exists(String key) throws StorageException {
        validateKey(key);
        return storage.containsKey(key);
    }
    
    @Override
    public Optional<StorageMetadata> getMetadata(String key) throws StorageException {
        validateKey(key);
        return Optional.ofNullable(metadata.get(key));
    }
    
    @Override
    public StorageResult copy(String sourceKey, String targetKey) throws StorageException {
        validateKey(sourceKey);
        validateKey(targetKey);
        
        byte[] sourceData = storage.get(sourceKey);
        if (sourceData == null) {
            throw new StorageException(StorageErrorType.FILE_NOT_FOUND, "源文件不存在: " + sourceKey);
        }
        
        StorageMetadata sourceMeta = metadata.get(sourceKey);
        
        storage.put(targetKey, sourceData.clone());
        
        StorageMetadata targetMeta = StorageMetadata.builder()
            .key(targetKey)
            .size(sourceData.length)
            .contentType(sourceMeta != null ? sourceMeta.getContentType() : null)
            .createdAt(LocalDateTime.now())
            .lastModified(LocalDateTime.now())
            .storagePath("memory://" + targetKey)
            .storageType(StorageType.IN_MEMORY)
            .customMetadata(new HashMap<>())
            .build();
        
        metadata.put(targetKey, targetMeta);
        
        return StorageResult.builder()
            .key(targetKey)
            .size(sourceData.length)
            .checksum(String.valueOf(sourceData.hashCode()))
            .storedAt(LocalDateTime.now())
            .storagePath("memory://" + targetKey)
            .success(true)
            .build();
    }
    
    @Override
    public StorageResult move(String sourceKey, String targetKey) throws StorageException {
        StorageResult copyResult = copy(sourceKey, targetKey);
        if (copyResult.isSuccess()) {
            delete(sourceKey);
        }
        return copyResult;
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.IN_MEMORY;
    }
    
    @Override
    public StorageStats getStats() {
        long totalSize = storage.values().stream()
            .mapToLong(data -> data.length)
            .sum();
        
        return StorageStats.builder()
            .totalSpace(maxFileSize * 1000) // 假设的总空间
            .usedSpace(totalSize)
            .availableSpace(maxFileSize * 1000 - totalSize)
            .fileCount(storage.size())
            .storageType(StorageType.IN_MEMORY)
            .healthy(true)
            .healthMessage("内存存储正常")
            .build();
    }
    
    private void validateKey(String key) throws StorageException {
        if (key == null || key.trim().isEmpty()) {
            throw new StorageException(StorageErrorType.CONFIGURATION_ERROR, "存储键值不能为空");
        }
    }
    
    private void validateContentLength(long contentLength) throws StorageException {
        if (contentLength > maxFileSize) {
            throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                "文件大小超过限制: " + maxFileSize + " 字节");
        }
    }
    
    /**
     * 清空所有存储的数据（测试用）
     */
    public void clear() {
        storage.clear();
        metadata.clear();
    }
    
    /**
     * 获取存储的文件数量（测试用）
     */
    public int size() {
        return storage.size();
    }
}