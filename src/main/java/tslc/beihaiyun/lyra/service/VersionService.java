package tslc.beihaiyun.lyra.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 版本控制服务接口
 * 提供文件版本管理的完整功能，包括版本创建、查询、回滚、清理等操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public interface VersionService {

    /**
     * 版本操作结果
     */
    class VersionOperationResult {
        private final boolean success;
        private final String message;
        private final FileVersion version;
        private final Exception exception;

        public VersionOperationResult(boolean success, String message, FileVersion version) {
            this.success = success;
            this.message = message;
            this.version = version;
            this.exception = null;
        }

        public VersionOperationResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.version = null;
            this.exception = exception;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public FileVersion getVersion() { return version; }
        public Exception getException() { return exception; }
    }

    /**
     * 版本清理策略
     */
    enum CleanupStrategy {
        /** 保留指定数量的最新版本 */
        KEEP_COUNT,
        /** 保留指定天数内的版本 */
        KEEP_DAYS,
        /** 保留指定大小限制内的版本 */
        KEEP_SIZE,
        /** 混合策略：同时应用多种策略 */
        MIXED
    }

    /**
     * 版本清理策略配置
     */
    class CleanupConfig {
        private final CleanupStrategy strategy;
        private final Integer keepCount;
        private final Integer keepDays;
        private final Long keepSizeBytes;
        private final boolean preserveFirstVersion;
        private final boolean preserveLastVersion;

        public CleanupConfig(CleanupStrategy strategy, Integer keepCount, Integer keepDays, 
                           Long keepSizeBytes, boolean preserveFirstVersion, boolean preserveLastVersion) {
            this.strategy = strategy;
            this.keepCount = keepCount;
            this.keepDays = keepDays;
            this.keepSizeBytes = keepSizeBytes;
            this.preserveFirstVersion = preserveFirstVersion;
            this.preserveLastVersion = preserveLastVersion;
        }

        public CleanupStrategy getStrategy() { return strategy; }
        public Integer getKeepCount() { return keepCount; }
        public Integer getKeepDays() { return keepDays; }
        public Long getKeepSizeBytes() { return keepSizeBytes; }
        public boolean isPreserveFirstVersion() { return preserveFirstVersion; }
        public boolean isPreserveLastVersion() { return preserveLastVersion; }
    }

    /**
     * 版本清理结果
     */
    class CleanupResult {
        private final int totalVersionsProcessed;
        private final int versionsDeleted;
        private final long spaceFreed;
        private final List<String> errors;

        public CleanupResult(int totalVersionsProcessed, int versionsDeleted, long spaceFreed, List<String> errors) {
            this.totalVersionsProcessed = totalVersionsProcessed;
            this.versionsDeleted = versionsDeleted;
            this.spaceFreed = spaceFreed;
            this.errors = errors;
        }

        public int getTotalVersionsProcessed() { return totalVersionsProcessed; }
        public int getVersionsDeleted() { return versionsDeleted; }
        public long getSpaceFreed() { return spaceFreed; }
        public List<String> getErrors() { return errors; }
        public boolean isSuccessful() { return errors.isEmpty(); }
        public double getCleanupRatio() { 
            return totalVersionsProcessed > 0 ? (double) versionsDeleted / totalVersionsProcessed : 0; 
        }
    }

    /**
     * 版本统计信息
     */
    class VersionStatistics {
        private final long totalVersions;
        private final long totalSizeBytes;
        private final int maxVersionNumber;
        private final LocalDateTime firstVersionTime;
        private final LocalDateTime lastVersionTime;
        private final long averageSizeBytes;
        private final double compressionRatio;

        public VersionStatistics(long totalVersions, long totalSizeBytes, int maxVersionNumber,
                               LocalDateTime firstVersionTime, LocalDateTime lastVersionTime,
                               long averageSizeBytes, double compressionRatio) {
            this.totalVersions = totalVersions;
            this.totalSizeBytes = totalSizeBytes;
            this.maxVersionNumber = maxVersionNumber;
            this.firstVersionTime = firstVersionTime;
            this.lastVersionTime = lastVersionTime;
            this.averageSizeBytes = averageSizeBytes;
            this.compressionRatio = compressionRatio;
        }

        public long getTotalVersions() { return totalVersions; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public int getMaxVersionNumber() { return maxVersionNumber; }
        public LocalDateTime getFirstVersionTime() { return firstVersionTime; }
        public LocalDateTime getLastVersionTime() { return lastVersionTime; }
        public long getAverageSizeBytes() { return averageSizeBytes; }
        public double getCompressionRatio() { return compressionRatio; }
    }

    // ==================== 版本创建和管理 ====================

    /**
     * 为文件创建新版本
     * 
     * @param file 文件实体
     * @param inputStream 新版本的文件内容
     * @param changeComment 变更注释
     * @param creatorId 创建者ID
     * @return 版本操作结果
     */
    VersionOperationResult createVersion(FileEntity file, InputStream inputStream, String changeComment, Long creatorId);

    /**
     * 为文件创建新版本（从存储路径）
     * 
     * @param file 文件实体
     * @param storagePath 存储路径
     * @param fileSize 文件大小
     * @param fileHash 文件哈希值
     * @param changeComment 变更注释
     * @param creatorId 创建者ID
     * @return 版本操作结果
     */
    VersionOperationResult createVersion(FileEntity file, String storagePath, Long fileSize, String fileHash, 
                                       String changeComment, Long creatorId);

    /**
     * 删除指定版本
     * 
     * @param versionId 版本ID
     * @param deleterId 删除者ID
     * @return 操作是否成功
     */
    boolean deleteVersion(Long versionId, Long deleterId);

    /**
     * 删除文件的指定版本号
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @param deleterId 删除者ID
     * @return 操作是否成功
     */
    boolean deleteVersionByNumber(FileEntity file, Integer versionNumber, Long deleterId);

    // ==================== 版本查询 ====================

    /**
     * 根据ID获取版本
     * 
     * @param versionId 版本ID
     * @return 版本实体（可选）
     */
    Optional<FileVersion> getVersionById(Long versionId);

    /**
     * 获取文件的指定版本
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 版本实体（可选）
     */
    Optional<FileVersion> getVersionByNumber(FileEntity file, Integer versionNumber);

    /**
     * 获取文件的最新版本
     * 
     * @param file 文件实体
     * @return 最新版本（可选）
     */
    Optional<FileVersion> getLatestVersion(FileEntity file);

    /**
     * 获取文件的第一个版本
     * 
     * @param file 文件实体
     * @return 第一个版本（可选）
     */
    Optional<FileVersion> getFirstVersion(FileEntity file);

    /**
     * 获取文件的所有版本
     * 
     * @param file 文件实体
     * @param orderByVersionDesc 是否按版本号倒序
     * @return 版本列表
     */
    List<FileVersion> getAllVersions(FileEntity file, boolean orderByVersionDesc);

    /**
     * 分页查询文件版本
     * 
     * @param file 文件实体
     * @param pageable 分页参数
     * @return 版本分页结果
     */
    Page<FileVersion> getVersionsPaged(FileEntity file, Pageable pageable);

    /**
     * 获取文件的指定数量最新版本
     * 
     * @param file 文件实体
     * @param limit 限制数量
     * @return 最新版本列表
     */
    List<FileVersion> getLatestVersions(FileEntity file, int limit);

    /**
     * 获取指定版本号范围的版本
     * 
     * @param file 文件实体
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @return 版本列表
     */
    List<FileVersion> getVersionsInRange(FileEntity file, Integer fromVersion, Integer toVersion);

    /**
     * 获取指定时间范围内的版本
     * 
     * @param file 文件实体
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 版本列表
     */
    List<FileVersion> getVersionsInTimeRange(FileEntity file, LocalDateTime startTime, LocalDateTime endTime);

    // ==================== 版本内容访问 ====================

    /**
     * 获取版本内容流
     * 
     * @param versionId 版本ID
     * @return 内容输入流（可选）
     * @throws IOException 读取异常
     */
    Optional<InputStream> getVersionContent(Long versionId) throws IOException;

    /**
     * 获取指定版本号的内容流
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 内容输入流（可选）
     * @throws IOException 读取异常
     */
    Optional<InputStream> getVersionContent(FileEntity file, Integer versionNumber) throws IOException;

    /**
     * 验证版本内容完整性
     * 
     * @param versionId 版本ID
     * @return 是否完整
     */
    boolean verifyVersionIntegrity(Long versionId);

    // ==================== 版本回滚 ====================

    /**
     * 回滚文件到指定版本
     * 
     * @param file 文件实体
     * @param targetVersionNumber 目标版本号
     * @param operatorId 操作者ID
     * @param createNewVersion 是否创建新版本（true: 创建新版本，false: 直接覆盖当前版本）
     * @return 版本操作结果
     */
    VersionOperationResult rollbackToVersion(FileEntity file, Integer targetVersionNumber, Long operatorId, boolean createNewVersion);

    /**
     * 回滚文件到指定版本ID
     * 
     * @param file 文件实体
     * @param targetVersionId 目标版本ID
     * @param operatorId 操作者ID
     * @param createNewVersion 是否创建新版本
     * @return 版本操作结果
     */
    VersionOperationResult rollbackToVersion(FileEntity file, Long targetVersionId, Long operatorId, boolean createNewVersion);

    /**
     * 回滚文件到最近的版本（撤销最后一次修改）
     * 
     * @param file 文件实体
     * @param operatorId 操作者ID
     * @return 版本操作结果
     */
    VersionOperationResult rollbackToPreviousVersion(FileEntity file, Long operatorId);

    // ==================== 版本比较和差异 ====================

    /**
     * 比较两个版本的差异（文本文件）
     * 
     * @param file 文件实体
     * @param fromVersionNumber 源版本号
     * @param toVersionNumber 目标版本号
     * @return 差异信息（可选）
     */
    Optional<String> compareVersions(FileEntity file, Integer fromVersionNumber, Integer toVersionNumber);

    /**
     * 获取版本变更摘要
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 变更摘要
     */
    String getVersionChangeSummary(FileEntity file, Integer versionNumber);

    // ==================== 版本清理和优化 ====================

    /**
     * 清理文件的旧版本
     * 
     * @param file 文件实体
     * @param config 清理策略配置
     * @return 清理结果
     */
    CleanupResult cleanupFileVersions(FileEntity file, CleanupConfig config);

    /**
     * 批量清理文件版本
     * 
     * @param files 文件列表
     * @param config 清理策略配置
     * @return 清理结果
     */
    CleanupResult batchCleanupVersions(List<FileEntity> files, CleanupConfig config);

    /**
     * 清理系统中的孤立版本
     * 
     * @return 清理的版本数量
     */
    int cleanupOrphanedVersions();

    /**
     * 优化版本存储（去重、压缩等）
     * 
     * @param file 文件实体
     * @return 优化结果
     */
    CleanupResult optimizeVersionStorage(FileEntity file);

    /**
     * 自动清理过期版本（定时任务使用）
     * 
     * @param defaultConfig 默认清理配置
     * @return 清理结果
     */
    CleanupResult autoCleanupExpiredVersions(CleanupConfig defaultConfig);

    // ==================== 版本统计和信息 ====================

    /**
     * 获取文件版本统计信息
     * 
     * @param file 文件实体
     * @return 版本统计信息
     */
    VersionStatistics getVersionStatistics(FileEntity file);

    /**
     * 统计文件版本数量
     * 
     * @param file 文件实体
     * @return 版本数量
     */
    long countVersions(FileEntity file);

    /**
     * 计算文件所有版本的总大小
     * 
     * @param file 文件实体
     * @return 总大小（字节）
     */
    long getTotalVersionsSize(FileEntity file);

    /**
     * 获取版本创建的下一个版本号
     * 
     * @param file 文件实体
     * @return 下一个版本号
     */
    Integer getNextVersionNumber(FileEntity file);

    /**
     * 检查是否可以创建新版本
     * 
     * @param file 文件实体
     * @return 是否可以创建
     */
    boolean canCreateVersion(FileEntity file);

    /**
     * 检查版本是否存在
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 是否存在
     */
    boolean versionExists(FileEntity file, Integer versionNumber);

    // ==================== 批量操作 ====================

    /**
     * 批量删除版本
     * 
     * @param versionIds 版本ID列表
     * @param deleterId 删除者ID
     * @return 删除的版本数量
     */
    int batchDeleteVersions(List<Long> versionIds, Long deleterId);

    /**
     * 批量验证版本完整性
     * 
     * @param versionIds 版本ID列表
     * @return 验证结果映射（版本ID -> 是否完整）
     */
    java.util.Map<Long, Boolean> batchVerifyVersionIntegrity(List<Long> versionIds);

    // ==================== 搜索和过滤 ====================

    /**
     * 根据变更注释搜索版本
     * 
     * @param file 文件实体
     * @param keyword 关键字
     * @return 匹配的版本列表
     */
    List<FileVersion> searchVersionsByComment(FileEntity file, String keyword);

    /**
     * 获取有变更注释的版本
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> getVersionsWithComment(FileEntity file);

    /**
     * 获取没有变更注释的版本
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> getVersionsWithoutComment(FileEntity file);

    /**
     * 获取大版本文件（超过指定大小）
     * 
     * @param file 文件实体
     * @param sizeThreshold 大小阈值（字节）
     * @return 大版本文件列表
     */
    List<FileVersion> getLargeVersions(FileEntity file, Long sizeThreshold);

    /**
     * 获取重复版本（相同哈希值）
     * 
     * @param file 文件实体
     * @return 重复版本分组
     */
    java.util.Map<String, List<FileVersion>> getDuplicateVersions(FileEntity file);
} 