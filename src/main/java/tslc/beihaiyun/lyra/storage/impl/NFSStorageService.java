package tslc.beihaiyun.lyra.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tslc.beihaiyun.lyra.storage.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.TimeUnit;

/**
 * NFS网络文件系统存储服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "lyra.storage.primary", havingValue = "nfs")
public class NFSStorageService implements StorageService {
    
    private final String nfsServer;
    private final String nfsExportPath;
    private final Path nfsMountPoint;
    private final String mountOptions;
    private final long maxFileSize;
    private final int connectionTimeout;
    private final int readTimeout;
    private final int retryCount;
    
    private volatile boolean mounted = false;
    private volatile boolean healthy = true;
    private String healthMessage = "NFS存储正常";
    
    public NFSStorageService(
            @Value("${lyra.storage.nfs.server}") String nfsServer,
            @Value("${lyra.storage.nfs.export-path}") String nfsExportPath,
            @Value("${lyra.storage.nfs.mount-point:/mnt/nfs-storage}") String mountPoint,
            @Value("${lyra.storage.nfs.mount-options:rw,sync,hard,intr}") String mountOptions,
            @Value("${lyra.storage.nfs.max-file-size:104857600}") long maxFileSize,
            @Value("${lyra.storage.nfs.connection-timeout:30000}") int connectionTimeout,
            @Value("${lyra.storage.nfs.read-timeout:60000}") int readTimeout,
            @Value("${lyra.storage.nfs.retry-count:3}") int retryCount) {
        
        this.nfsServer = nfsServer;
        this.nfsExportPath = nfsExportPath;
        this.nfsMountPoint = Paths.get(mountPoint).toAbsolutePath();
        this.mountOptions = mountOptions;
        this.maxFileSize = maxFileSize;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.retryCount = retryCount;
    }
    
    @PostConstruct
    public void initializeNFS() {
        log.info("初始化NFS存储服务: {}:{}", nfsServer, nfsExportPath);
        
        try {
            if (!isNFSMounted()) {
                mountNFS();
            }
            verifyNFSAccess();
            mounted = true;
            healthy = true;
            healthMessage = "NFS存储正常";
            log.info("NFS存储初始化完成，挂载点: {}", nfsMountPoint);
        } catch (Exception e) {
            mounted = false;
            healthy = false;
            healthMessage = "NFS初始化失败: " + e.getMessage();
            log.error("NFS存储初始化失败", e);
            throw new RuntimeException("无法初始化NFS存储", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (mounted) {
            try {
                unmountNFS();
                log.info("NFS存储清理完成");
            } catch (Exception e) {
                log.warn("NFS卸载失败", e);
            }
        }
    }
    
    private boolean isNFSMounted() {
        try {
            // 检查挂载点是否已挂载
            Process process = new ProcessBuilder("mount")
                .redirectErrorStream(true)
                .start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                    .anyMatch(line -> line.contains(nfsMountPoint.toString()) && 
                                    line.contains(nfsServer));
            }
        } catch (Exception e) {
            log.debug("检查NFS挂载状态失败", e);
            return false;
        }
    }
    
    private void mountNFS() throws IOException, InterruptedException {
        // 创建挂载点目录
        Files.createDirectories(nfsMountPoint);
        
        // 构建挂载命令
        String nfsPath = nfsServer + ":" + nfsExportPath;
        ProcessBuilder pb = new ProcessBuilder(
            "mount", "-t", "nfs", "-o", mountOptions, 
            nfsPath, nfsMountPoint.toString()
        );
        
        log.info("执行NFS挂载命令: mount -t nfs -o {} {} {}", 
                mountOptions, nfsPath, nfsMountPoint);
        
        Process process = pb.start();
        boolean finished = process.waitFor(connectionTimeout / 1000, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("NFS挂载超时");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String error = reader.lines()
                    .reduce("", (a, b) -> a + "\n" + b);
                throw new IOException("NFS挂载失败 (退出码: " + exitCode + "): " + error);
            }
        }
    }
    
    private void unmountNFS() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("umount", nfsMountPoint.toString());
        Process process = pb.start();
        
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            // 尝试强制卸载
            pb = new ProcessBuilder("umount", "-f", nfsMountPoint.toString());
            process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
    
    private void verifyNFSAccess() throws IOException {
        Path testFile = nfsMountPoint.resolve(".lyra-nfs-test");
        try {
            // 测试写入
            Files.write(testFile, "NFS access test".getBytes());
            
            // 测试读取
            byte[] content = Files.readAllBytes(testFile);
            if (content.length == 0) {
                throw new IOException("NFS读取测试失败");
            }
            
            // 清理测试文件
            Files.deleteIfExists(testFile);
            
        } catch (IOException e) {
            throw new IOException("NFS访问验证失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult store(String key, InputStream inputStream, long contentLength, String contentType) throws StorageException {
        validateMounted();
        validateKey(key);
        validateContentLength(contentLength);
        
        Path filePath = resolveFilePath(key);
        
        return executeWithRetry(() -> {
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
                    
                    bos.flush();
                    checksum = HexFormat.of().formatHex(md5.digest());
                    
                    // 验证文件完整性
                    if (Files.size(filePath) != totalBytes) {
                        Files.deleteIfExists(filePath);
                        throw new StorageException(StorageErrorType.IO_ERROR, "文件大小不匹配");
                    }
                    
                    log.debug("NFS文件存储成功: key={}, size={}, checksum={}", key, totalBytes, checksum);
                    
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
                log.error("NFS存储文件失败: key={}", key, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS存储文件失败: " + e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                log.error("MD5算法不可用", e);
                throw new StorageException(StorageErrorType.UNKNOWN, "校验和计算失败", e);
            }
        });
    }
    
    @Override
    public Optional<InputStream> retrieve(String key) throws StorageException {
        validateMounted();
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        
        return executeWithRetry(() -> {
            try {
                InputStream inputStream = Files.newInputStream(filePath);
                log.debug("NFS文件检索成功: key={}", key);
                return Optional.of(inputStream);
            } catch (IOException e) {
                log.error("NFS检索文件失败: key={}", key, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS检索文件失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean delete(String key) throws StorageException {
        validateMounted();
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        return executeWithRetry(() -> {
            try {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.debug("NFS文件删除成功: key={}", key);
                    // 尝试删除空的父目录
                    cleanupEmptyDirectories(filePath.getParent());
                }
                return deleted;
            } catch (IOException e) {
                log.error("NFS删除文件失败: key={}", key, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS删除文件失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public boolean exists(String key) throws StorageException {
        validateMounted();
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
    
    @Override
    public Optional<StorageMetadata> getMetadata(String key) throws StorageException {
        validateMounted();
        validateKey(key);
        
        Path filePath = resolveFilePath(key);
        
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        
        return executeWithRetry(() -> {
            try {
                var attrs = Files.readAttributes(filePath, "size,creationTime,lastModifiedTime");
                
                Map<String, String> customMetadata = new HashMap<>();
                customMetadata.put("nfs.server", nfsServer);
                customMetadata.put("nfs.export", nfsExportPath);
                customMetadata.put("nfs.mount-point", nfsMountPoint.toString());
                
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
                    .storageType(StorageType.NFS)
                    .customMetadata(customMetadata)
                    .build());
                    
            } catch (IOException e) {
                log.error("NFS获取文件元数据失败: key={}", key, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS获取文件元数据失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public StorageResult copy(String sourceKey, String targetKey) throws StorageException {
        validateMounted();
        validateKey(sourceKey);
        validateKey(targetKey);
        
        Path sourcePath = resolveFilePath(sourceKey);
        Path targetPath = resolveFilePath(targetKey);
        
        if (!Files.exists(sourcePath)) {
            throw new StorageException(StorageErrorType.FILE_NOT_FOUND, "源文件不存在: " + sourceKey);
        }
        
        return executeWithRetry(() -> {
            try {
                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                long size = Files.size(targetPath);
                log.debug("NFS文件复制成功: {} -> {}", sourceKey, targetKey);
                
                return StorageResult.builder()
                    .key(targetKey)
                    .size(size)
                    .storedAt(LocalDateTime.now())
                    .storagePath(targetPath.toString())
                    .success(true)
                    .build();
                    
            } catch (IOException e) {
                log.error("NFS复制文件失败: {} -> {}", sourceKey, targetKey, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS复制文件失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public StorageResult move(String sourceKey, String targetKey) throws StorageException {
        validateMounted();
        validateKey(sourceKey);
        validateKey(targetKey);
        
        Path sourcePath = resolveFilePath(sourceKey);
        Path targetPath = resolveFilePath(targetKey);
        
        if (!Files.exists(sourcePath)) {
            throw new StorageException(StorageErrorType.FILE_NOT_FOUND, "源文件不存在: " + sourceKey);
        }
        
        return executeWithRetry(() -> {
            try {
                Files.createDirectories(targetPath.getParent());
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                long size = Files.size(targetPath);
                log.debug("NFS文件移动成功: {} -> {}", sourceKey, targetKey);
                
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
                log.error("NFS移动文件失败: {} -> {}", sourceKey, targetKey, e);
                throw new StorageException(StorageErrorType.IO_ERROR, "NFS移动文件失败: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.NFS;
    }
    
    @Override
    public StorageStats getStats() {
        try {
            if (!mounted) {
                return StorageStats.builder()
                    .storageType(StorageType.NFS)
                    .healthy(false)
                    .healthMessage("NFS未挂载")
                    .build();
            }
            
            FileStore fileStore = Files.getFileStore(nfsMountPoint);
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            // 计算文件数量
            long fileCount = countFiles(nfsMountPoint);
            
            return StorageStats.builder()
                .totalSpace(totalSpace)
                .usedSpace(usedSpace)
                .availableSpace(usableSpace)
                .fileCount(fileCount)
                .storageType(StorageType.NFS)
                .healthy(healthy)
                .healthMessage(healthMessage)
                .build();
                
        } catch (IOException e) {
            log.error("获取NFS存储统计信息失败", e);
            healthy = false;
            healthMessage = "获取存储统计信息失败: " + e.getMessage();
            
            return StorageStats.builder()
                .storageType(StorageType.NFS)
                .healthy(false)
                .healthMessage(healthMessage)
                .build();
        }
    }
    
    private void validateMounted() throws StorageException {
        if (!mounted) {
            throw new StorageException(StorageErrorType.CONFIGURATION_ERROR, "NFS未挂载");
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
        return nfsMountPoint.resolve(safePath);
    }
    
    private void cleanupEmptyDirectories(Path directory) {
        try {
            if (directory != null && !directory.equals(nfsMountPoint) && Files.isDirectory(directory)) {
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
    
    /**
     * 带重试机制的操作执行
     */
    private <T> T executeWithRetry(StorageOperation<T> operation) throws StorageException {
        StorageException lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                return operation.execute();
            } catch (StorageException e) {
                lastException = e;
                
                // 如果是网络相关错误，尝试重新挂载
                if (e.getErrorType() == StorageErrorType.IO_ERROR && attempt < retryCount) {
                    log.warn("NFS操作失败，尝试重新挂载 (尝试 {}/{})", attempt, retryCount);
                    try {
                        if (!isNFSMounted()) {
                            mountNFS();
                            verifyNFSAccess();
                        }
                        // 短暂等待后重试
                        Thread.sleep(1000 * attempt);
                    } catch (Exception remountException) {
                        log.error("重新挂载NFS失败", remountException);
                        mounted = false;
                        healthy = false;
                        healthMessage = "NFS重新挂载失败: " + remountException.getMessage();
                    }
                } else {
                    break;
                }
            }
        }
        
        throw lastException;
    }
    
    @FunctionalInterface
    private interface StorageOperation<T> {
        T execute() throws StorageException;
    }
}