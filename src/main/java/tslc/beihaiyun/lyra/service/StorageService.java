package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 文件存储服务接口
 * 提供文件上传、下载、删除等核心存储操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public interface StorageService {

    /**
     * 存储结果
     */
    class StorageResult {
        private final String storagePath;
        private final String fileHash;
        private final long sizeBytes;
        private final boolean isDuplicate;

        public StorageResult(String storagePath, String fileHash, long sizeBytes, boolean isDuplicate) {
            this.storagePath = storagePath;
            this.fileHash = fileHash;
            this.sizeBytes = sizeBytes;
            this.isDuplicate = isDuplicate;
        }

        public String getStoragePath() { return storagePath; }
        public String getFileHash() { return fileHash; }
        public long getSizeBytes() { return sizeBytes; }
        public boolean isDuplicate() { return isDuplicate; }
    }

    /**
     * 存储文件从MultipartFile
     * 
     * @param file 上传的文件
     * @return 存储结果
     * @throws IOException 存储异常
     */
    StorageResult store(MultipartFile file) throws IOException;

    /**
     * 存储文件从InputStream
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param contentType 内容类型
     * @return 存储结果
     * @throws IOException 存储异常
     */
    StorageResult store(InputStream inputStream, String filename, String contentType) throws IOException;

    /**
     * 存储文件从字节数组
     * 
     * @param content 文件内容
     * @param filename 文件名
     * @param contentType 内容类型
     * @return 存储结果
     * @throws IOException 存储异常
     */
    StorageResult store(byte[] content, String filename, String contentType) throws IOException;

    /**
     * 读取文件内容
     * 
     * @param storagePath 存储路径
     * @return 文件输入流（可选）
     * @throws IOException 读取异常
     */
    Optional<InputStream> load(String storagePath) throws IOException;

    /**
     * 获取文件路径
     * 
     * @param storagePath 存储路径
     * @return 文件路径（可选）
     */
    Optional<Path> getPath(String storagePath);

    /**
     * 检查文件是否存在
     * 
     * @param storagePath 存储路径
     * @return 是否存在
     */
    boolean exists(String storagePath);

    /**
     * 删除文件
     * 
     * @param storagePath 存储路径
     * @return 是否删除成功
     */
    boolean delete(String storagePath);

    /**
     * 获取文件大小
     * 
     * @param storagePath 存储路径
     * @return 文件大小（字节），不存在返回-1
     */
    long getFileSize(String storagePath);

    /**
     * 计算文件哈希值
     * 
     * @param inputStream 文件输入流
     * @return SHA-256哈希值
     * @throws IOException 计算异常
     */
    String calculateHash(InputStream inputStream) throws IOException;

    /**
     * 验证文件完整性
     * 
     * @param storagePath 存储路径
     * @param expectedHash 期望的哈希值
     * @return 是否完整
     */
    boolean verifyIntegrity(String storagePath, String expectedHash);

    /**
     * 检查是否存在重复文件
     * 
     * @param fileHash 文件哈希值
     * @return 重复文件的存储路径（可选）
     */
    Optional<String> findDuplicateFile(String fileHash);

    /**
     * 复制文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否复制成功
     * @throws IOException 复制异常
     */
    boolean copy(String sourcePath, String targetPath) throws IOException;

    /**
     * 移动文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否移动成功
     * @throws IOException 移动异常
     */
    boolean move(String sourcePath, String targetPath) throws IOException;

    /**
     * 清理临时文件
     * 
     * @return 清理的文件数量
     */
    int cleanupTempFiles();

    /**
     * 获取存储使用情况
     * 
     * @return 存储使用情况统计
     */
    StorageStats getStorageStats();

    /**
     * 存储使用情况统计
     */
    class StorageStats {
        private final long totalSpace;
        private final long usedSpace;
        private final long freeSpace;
        private final long fileCount;

        public StorageStats(long totalSpace, long usedSpace, long freeSpace, long fileCount) {
            this.totalSpace = totalSpace;
            this.usedSpace = usedSpace;
            this.freeSpace = freeSpace;
            this.fileCount = fileCount;
        }

        public long getTotalSpace() { return totalSpace; }
        public long getUsedSpace() { return usedSpace; }
        public long getFreeSpace() { return freeSpace; }
        public long getFileCount() { return fileCount; }
        public double getUsagePercentage() { 
            return totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0; 
        }
    }
} 