package tslc.beihaiyun.lyra.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tslc.beihaiyun.lyra.storage.*;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 本地文件系统存储服务实现
 */
@Slf4j
@Service
public class LocalFileStorageService implements StorageService {
    
    private final Path storageRoot;
    private final long maxFileSize;
    
    public LocalFileStorageService(
            @Value("${lyra.storage.local.root:./storage}") String storageRootPath,
            @Value("${lyra.storage.local.max-file-size:104857600}") long maxFileSize) { // 默认100MB
        this.storageRoot = Paths.get(storageRootPath).toAbsolutePath();
        this.maxFileSize = maxFileSize;
        initializeStorage();
    }
    
    private void initializeStorage() {
        try {
            Files.createDirectories(storageRoot);
            log.info("本地存储初始化完成，存储路径: {}", storageRoot);
        } catch (IOException e) {
            log.error("本地存储初始化失败", e);
            throw new RuntimeException("无法初始化本地存储", e);
        }
    }
    
    @Override
    public StorageResult store(String key, InputStream inputStream, long contentLength, String contentType) throws StorageException {
        validateKey(key);
        validateContentLength(contentLength);
        
        Path filePath = resolveFilePath(key);
        
        try {
            // 创建父目录
            Files.createDirectories(filePath.getParent());
            
            // 计算校验和并写入文件
            String checksum;
            try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                 BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > maxFileSize) {
                        Files.deleteIfExists(filePath);
                        throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                            "文件大小超过限制: " + maxFileSize + " 字节");
                    }
                    
                    bos.write(buffer, 0, bytesRead);
                    md5.update(buffer, 0, bytesRead);
                }
                
                checksum = HexFormat.of().formatHex(md5.digest());
                
                log.debug("文件存储成功: key={}, size={}, checksum={}", key, totalBytes, checksum);
                
                return StorageResult.builder()
                    .key(key)
                    .size(totalBytes)
                    .checksum(checksum)
                    .contentType(contentType)
                    .storedAt(LocalDateTime.now())
                    .storagePath(filePath.toString())
                    .success(true)
                    .build();
            }
            
        } catch (IOException e) {
            log.error("存储文件失败: key={}", key, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "存储文件失败: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new StorageException(StorageErrorType.UNKNOWN, "校验和计算失败", e);
        }
    }
    
    @Override
    public Optional<InputStream> retrieve(String key) throws StorageException {
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            log.debug("文件检索成功: key={}", key);
            return Optional.of(inputStream);
        } catch (IOException e) {
            log.error("检索文件失败: key={}", key, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "检索文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean delete(String key) throws StorageException {
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("文件删除成功: key={}", key);
                // 尝试删除空的父目录
                cleanupEmptyDirectories(filePath.getParent());
            }
            return deleted;
        } catch (IOException e) {
            log.error("删除文件失败: key={}", key, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "删除文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String key) throws StorageException {
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
    
    @Override
    public Optional<StorageMetadata> getMetadata(String key) throws StorageException {
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        
        try {
            var attrs = Files.readAttributes(filePath, "size,creationTime,lastModifiedTime");
            
            return Optional.of(StorageMetadata.builder()
                .key(key)
                .size((Long) attrs.get("size"))
                .createdAt(LocalDateTime.ofInstant(
                    ((java.nio.file.attribute.FileTime) attrs.get("creationTime")).toInstant(),
                    ZoneId.systemDefault()))
                .lastModified(LocalDateTime.ofInstant(
                    ((java.nio.file.attribute.FileTime) attrs.get("lastModifiedTime")).toInstant(),
                    ZoneId.systemDefault()))
                .storagePath(filePath.toString())
                .storageType(StorageType.LOCAL_FILESYSTEM)
                .customMetadata(new HashMap<>())
                .build());
                
        } catch (IOException e) {
            log.error("获取文件元数据失败: key={}", key, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "获取文件元数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult copy(String sourceKey, String targetKey) throws StorageException {
        validateKey(sourceKey);
        validateKey(targetKey);
        
        Path sourcePath = resolveFilePath(sourceKey);
        Path targetPath = resolveFilePath(targetKey);
        
        if (!Files.exists(sourcePath)) {
            throw new StorageException(StorageErrorType.FILE_NOT_FOUND, "源文件不存在: " + sourceKey);
        }
        
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            long size = Files.size(targetPath);
            log.debug("文件复制成功: {} -> {}", sourceKey, targetKey);
            
            return StorageResult.builder()
                .key(targetKey)
                .size(size)
                .storedAt(LocalDateTime.now())
                .storagePath(targetPath.toString())
                .success(true)
                .build();
                
        } catch (IOException e) {
            log.error("复制文件失败: {} -> {}", sourceKey, targetKey, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "复制文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult move(String sourceKey, String targetKey) throws StorageException {
        validateKey(sourceKey);
        validateKey(targetKey);
        
        Path sourcePath = resolveFilePath(sourceKey);
        Path targetPath = resolveFilePath(targetKey);
        
        if (!Files.exists(sourcePath)) {
            throw new StorageException(StorageErrorType.FILE_NOT_FOUND, "源文件不存在: " + sourceKey);
        }
        
        try {
            Files.createDirectories(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            long size = Files.size(targetPath);
            log.debug("文件移动成功: {} -> {}", sourceKey, targetKey);
            
            // 清理空目录
            cleanupEmptyDirectories(sourcePath.getParent());
            
            return StorageResult.builder()
                .key(targetKey)
                .size(size)
                .storedAt(LocalDateTime.now())
                .storagePath(targetPath.toString())
                .success(true)
                .build();
                
        } catch (IOException e) {
            log.error("移动文件失败: {} -> {}", sourceKey, targetKey, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "移动文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL_FILESYSTEM;
    }
    
    @Override
    public StorageStats getStats() {
        try {
            FileStore fileStore = Files.getFileStore(storageRoot);
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            // 计算文件数量（简单实现，实际可能需要缓存）
            long fileCount = countFiles(storageRoot);
            
            return StorageStats.builder()
                .totalSpace(totalSpace)
                .usedSpace(usedSpace)
                .availableSpace(usableSpace)
                .fileCount(fileCount)
                .storageType(StorageType.LOCAL_FILESYSTEM)
                .healthy(true)
                .healthMessage("本地存储正常")
                .build();
                
        } catch (IOException e) {
            log.error("获取存储统计信息失败", e);
            return StorageStats.builder()
                .storageType(StorageType.LOCAL_FILESYSTEM)
                .healthy(false)
                .healthMessage("获取存储统计信息失败: " + e.getMessage())
                .build();
        }
    }
    
    private void validateKey(String key) throws StorageException {
        if (key == null || key.trim().isEmpty()) {
            throw new StorageException(StorageErrorType.CONFIGURATION_ERROR, "存储键值不能为空");
        }
        
        // 检查路径遍历攻击
        if (key.contains("..") || key.contains("//")) {
            throw new StorageException(StorageErrorType.ACCESS_DENIED, "非法的存储键值: " + key);
        }
    }
    
    private void validateContentLength(long contentLength) throws StorageException {
        if (contentLength > maxFileSize) {
            throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                "文件大小超过限制: " + maxFileSize + " 字节");
        }
    }
    
    private Path resolveFilePath(String key) {
        // 将key转换为安全的文件路径
        String safePath = key.replace('\\', '/');
        if (safePath.startsWith("/")) {
            safePath = safePath.substring(1);
        }
        return storageRoot.resolve(safePath);
    }
    
    private void cleanupEmptyDirectories(Path directory) {
        try {
            if (directory != null && !directory.equals(storageRoot) && Files.isDirectory(directory)) {
                try (var stream = Files.list(directory)) {
                    if (stream.findAny().isEmpty()) {
                        Files.delete(directory);
                        // 递归清理父目录
                        cleanupEmptyDirectories(directory.getParent());
                    }
                }
            }
        } catch (IOException e) {
            // 忽略清理失败，不影响主要功能
            log.debug("清理空目录失败: {}", directory, e);
        }
    }
    
    private long countFiles(Path directory) {
        try (var stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            log.warn("统计文件数量失败", e);
            return 0;
        }
    }
}