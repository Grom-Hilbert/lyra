package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.repository.FileVersionRepository;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.service.VersionService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 版本控制服务实现
 * 提供文件版本管理的完整功能，包括版本创建、查询、回滚、清理等操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
@Transactional
public class VersionServiceImpl implements VersionService {

    private static final Logger logger = LoggerFactory.getLogger(VersionServiceImpl.class);

    private final FileVersionRepository fileVersionRepository;
    private final StorageService storageService;

    @Autowired
    public VersionServiceImpl(FileVersionRepository fileVersionRepository, StorageService storageService) {
        this.fileVersionRepository = fileVersionRepository;
        this.storageService = storageService;
    }

    // ==================== 版本创建和管理 ====================

    @Override
    public VersionOperationResult createVersion(FileEntity file, InputStream inputStream, String changeComment, Long creatorId) {
        try {
            if (file == null) {
                return new VersionOperationResult(false, "文件实体不能为空", (FileVersion) null);
            }
            
            if (inputStream == null) {
                return new VersionOperationResult(false, "文件内容不能为空", (FileVersion) null);
            }

            // 存储新版本内容
            StorageService.StorageResult storageResult = storageService.store(
                inputStream, file.getName(), file.getMimeType());

            // 获取下一个版本号
            Integer nextVersionNumber = getNextVersionNumber(file);
            
            // 创建版本实体
            FileVersion version = new FileVersion();
            version.setFile(file);
            version.setVersionNumber(nextVersionNumber);
            version.setSizeBytes(storageResult.getSizeBytes());
            version.setFileHash(storageResult.getFileHash());
            version.setStoragePath(storageResult.getStoragePath());
            version.setChangeComment(changeComment);
            version.setCreatedBy(creatorId != null ? creatorId.toString() : "system");

            // 保存版本
            version = fileVersionRepository.save(version);

            logger.info("文件版本创建成功: 文件ID={}, 版本号={}, 创建者={}", 
                       file.getId(), nextVersionNumber, creatorId);
            
            return new VersionOperationResult(true, "版本创建成功", version);
        } catch (IOException e) {
            logger.error("文件版本创建失败: 文件ID={}", file != null ? file.getId() : "null", e);
            return new VersionOperationResult(false, "版本创建失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件版本创建过程中发生未知错误: 文件ID={}", file != null ? file.getId() : "null", e);
            return new VersionOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public VersionOperationResult createVersion(FileEntity file, String storagePath, Long fileSize, String fileHash, 
                                              String changeComment, Long creatorId) {
        try {
            if (file == null) {
                return new VersionOperationResult(false, "文件实体不能为空", (FileVersion) null);
            }
            
            if (storagePath == null || storagePath.trim().isEmpty()) {
                return new VersionOperationResult(false, "存储路径不能为空", (FileVersion) null);
            }

            // 验证存储文件是否存在
            if (!storageService.exists(storagePath)) {
                return new VersionOperationResult(false, "存储文件不存在: " + storagePath, (FileVersion) null);
            }

            // 获取下一个版本号
            Integer nextVersionNumber = getNextVersionNumber(file);
            
            // 创建版本实体
            FileVersion version = new FileVersion();
            version.setFile(file);
            version.setVersionNumber(nextVersionNumber);
            version.setSizeBytes(fileSize);
            version.setFileHash(fileHash);
            version.setStoragePath(storagePath);
            version.setChangeComment(changeComment);
            version.setCreatedBy(creatorId != null ? creatorId.toString() : "system");

            // 保存版本
            version = fileVersionRepository.save(version);

            logger.info("文件版本创建成功: 文件ID={}, 版本号={}, 存储路径={}", 
                       file.getId(), nextVersionNumber, storagePath);
            
            return new VersionOperationResult(true, "版本创建成功", version);
        } catch (Exception e) {
            logger.error("文件版本创建失败: 文件ID={}, 存储路径={}", 
                        file != null ? file.getId() : "null", storagePath, e);
            return new VersionOperationResult(false, "版本创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteVersion(Long versionId, Long deleterId) {
        try {
            if (versionId == null) {
                return false;
            }

            Optional<FileVersion> versionOpt = fileVersionRepository.findById(versionId);
            if (!versionOpt.isPresent()) {
                logger.warn("要删除的版本不存在: ID={}", versionId);
                return false;
            }

            FileVersion version = versionOpt.get();
            
            // 检查是否为文件的唯一版本
            long versionCount = fileVersionRepository.countByFile(version.getFile());
            if (versionCount <= 1) {
                logger.warn("无法删除文件的唯一版本: 文件ID={}, 版本号={}", 
                           version.getFile().getId(), version.getVersionNumber());
                return false;
            }

            // 删除存储文件
            boolean storageDeleted = storageService.delete(version.getStoragePath());
            if (!storageDeleted) {
                logger.warn("删除版本存储文件失败: 路径={}", version.getStoragePath());
            }

            // 删除版本记录
            fileVersionRepository.delete(version);

            logger.info("版本删除成功: 版本ID={}, 文件ID={}, 版本号={}, 删除者={}", 
                       versionId, version.getFile().getId(), version.getVersionNumber(), deleterId);
            
            return true;

        } catch (Exception e) {
            logger.error("删除版本失败: 版本ID={}", versionId, e);
            return false;
        }
    }

    @Override
    public boolean deleteVersionByNumber(FileEntity file, Integer versionNumber, Long deleterId) {
        try {
            if (file == null || versionNumber == null) {
                return false;
            }

            Optional<FileVersion> versionOpt = fileVersionRepository.findByFileAndVersionNumber(file, versionNumber);
            if (!versionOpt.isPresent()) {
                logger.warn("要删除的版本不存在: 文件ID={}, 版本号={}", file.getId(), versionNumber);
                return false;
            }

            return deleteVersion(versionOpt.get().getId(), deleterId);

        } catch (Exception e) {
            logger.error("删除版本失败: 文件ID={}, 版本号={}", 
                        file != null ? file.getId() : "null", versionNumber, e);
            return false;
        }
    }

    // ==================== 版本查询 ====================

    @Override
    @Transactional(readOnly = true)
    public Optional<FileVersion> getVersionById(Long versionId) {
        if (versionId == null) {
            return Optional.empty();
        }
        return fileVersionRepository.findById(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileVersion> getVersionByNumber(FileEntity file, Integer versionNumber) {
        if (file == null || versionNumber == null) {
            return Optional.empty();
        }
        return fileVersionRepository.findByFileAndVersionNumber(file, versionNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileVersion> getLatestVersion(FileEntity file) {
        if (file == null) {
            return Optional.empty();
        }
        return fileVersionRepository.findLatestByFile(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileVersion> getFirstVersion(FileEntity file) {
        if (file == null) {
            return Optional.empty();
        }
        return fileVersionRepository.findFirstByFile(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getAllVersions(FileEntity file, boolean orderByVersionDesc) {
        if (file == null) {
            return new ArrayList<>();
        }
        
        if (orderByVersionDesc) {
            return fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
        } else {
            return fileVersionRepository.findByFileOrderByVersionNumberAsc(file);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileVersion> getVersionsPaged(FileEntity file, Pageable pageable) {
        if (file == null) {
            return Page.empty(pageable);
        }
        return fileVersionRepository.findByFile(file, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getLatestVersions(FileEntity file, int limit) {
        if (file == null || limit <= 0) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findLatestVersionsByFile(file, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getVersionsInRange(FileEntity file, Integer fromVersion, Integer toVersion) {
        if (file == null || fromVersion == null || toVersion == null || fromVersion > toVersion) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findByFileAndVersionNumberBetween(file, fromVersion, toVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getVersionsInTimeRange(FileEntity file, LocalDateTime startTime, LocalDateTime endTime) {
        if (file == null || startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findByFileAndCreatedAtBetween(file, startTime, endTime);
    }

    // ==================== 版本内容访问 ====================

    @Override
    @Transactional(readOnly = true)
    public Optional<InputStream> getVersionContent(Long versionId) throws IOException {
        Optional<FileVersion> versionOpt = getVersionById(versionId);
        if (!versionOpt.isPresent()) {
            return Optional.empty();
        }

        FileVersion version = versionOpt.get();
        return storageService.load(version.getStoragePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InputStream> getVersionContent(FileEntity file, Integer versionNumber) throws IOException {
        Optional<FileVersion> versionOpt = getVersionByNumber(file, versionNumber);
        if (!versionOpt.isPresent()) {
            return Optional.empty();
        }

        return getVersionContent(versionOpt.get().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyVersionIntegrity(Long versionId) {
        Optional<FileVersion> versionOpt = getVersionById(versionId);
        if (!versionOpt.isPresent()) {
            return false;
        }

        FileVersion version = versionOpt.get();
        
        // 检查存储文件是否存在
        if (!storageService.exists(version.getStoragePath())) {
            logger.warn("版本存储文件不存在: 版本ID={}, 路径={}", versionId, version.getStoragePath());
            return false;
        }

        // 验证文件哈希值
        if (version.getFileHash() != null && !version.getFileHash().isEmpty()) {
            return storageService.verifyIntegrity(version.getStoragePath(), version.getFileHash());
        }

        return true;
    }

    // ==================== 版本回滚 ====================

    @Override
    public VersionOperationResult rollbackToVersion(FileEntity file, Integer targetVersionNumber, Long operatorId, boolean createNewVersion) {
        try {
            if (file == null || targetVersionNumber == null) {
                return new VersionOperationResult(false, "参数不能为空", (FileVersion) null);
            }

            // 获取目标版本
            Optional<FileVersion> targetVersionOpt = getVersionByNumber(file, targetVersionNumber);
            if (!targetVersionOpt.isPresent()) {
                return new VersionOperationResult(false, "目标版本不存在: " + targetVersionNumber, (FileVersion) null);
            }

            FileVersion targetVersion = targetVersionOpt.get();

            if (createNewVersion) {
                // 创建新版本
                try (InputStream contentStream = storageService.load(targetVersion.getStoragePath()).orElse(null)) {
                    if (contentStream == null) {
                        return new VersionOperationResult(false, "无法读取目标版本内容", (FileVersion) null);
                    }

                    String rollbackComment = String.format("回滚到版本 %d", targetVersionNumber);
                    return createVersion(file, contentStream, rollbackComment, operatorId);
                }
            } else {
                // 直接更新文件信息
                file.setSizeBytes(targetVersion.getSizeBytes());
                file.setFileHash(targetVersion.getFileHash());
                file.setStoragePath(targetVersion.getStoragePath());
                file.setLastModifiedAt(LocalDateTime.now());
                file.setUpdatedBy(operatorId != null ? operatorId.toString() : "system");

                logger.info("文件回滚成功: 文件ID={}, 目标版本={}, 操作者={}", 
                           file.getId(), targetVersionNumber, operatorId);
                
                return new VersionOperationResult(true, "回滚成功", targetVersion);
            }

        } catch (Exception e) {
            logger.error("版本回滚失败: 文件ID={}, 目标版本={}", 
                        file != null ? file.getId() : "null", targetVersionNumber, e);
            return new VersionOperationResult(false, "回滚失败: " + e.getMessage(), e);
        }
    }

    @Override
    public VersionOperationResult rollbackToVersion(FileEntity file, Long targetVersionId, Long operatorId, boolean createNewVersion) {
        Optional<FileVersion> targetVersionOpt = getVersionById(targetVersionId);
        if (!targetVersionOpt.isPresent()) {
            return new VersionOperationResult(false, "目标版本不存在: " + targetVersionId, (FileVersion) null);
        }

        return rollbackToVersion(file, targetVersionOpt.get().getVersionNumber(), operatorId, createNewVersion);
    }

    @Override
    public VersionOperationResult rollbackToPreviousVersion(FileEntity file, Long operatorId) {
        try {
            if (file == null) {
                return new VersionOperationResult(false, "文件实体不能为空", (FileVersion) null);
            }

            // 获取当前版本号
            Integer currentVersion = file.getVersion();
            if (currentVersion == null || currentVersion <= 1) {
                return new VersionOperationResult(false, "没有可回滚的版本", (FileVersion) null);
            }

            // 回滚到上一个版本
            return rollbackToVersion(file, currentVersion - 1, operatorId, false);

        } catch (Exception e) {
            logger.error("回滚到上一版本失败: 文件ID={}", file != null ? file.getId() : "null", e);
            return new VersionOperationResult(false, "回滚失败: " + e.getMessage(), e);
        }
    }

    // ==================== 版本比较和差异 ====================

    @Override
    @Transactional(readOnly = true)
    public Optional<String> compareVersions(FileEntity file, Integer fromVersionNumber, Integer toVersionNumber) {
        // 基础实现：返回简单的版本信息对比
        // 在实际项目中，这里应该实现具体的文本差异对比算法
        try {
            if (file == null || fromVersionNumber == null || toVersionNumber == null) {
                return Optional.empty();
            }

            Optional<FileVersion> fromVersionOpt = getVersionByNumber(file, fromVersionNumber);
            Optional<FileVersion> toVersionOpt = getVersionByNumber(file, toVersionNumber);

            if (!fromVersionOpt.isPresent() || !toVersionOpt.isPresent()) {
                return Optional.empty();
            }

            FileVersion fromVersion = fromVersionOpt.get();
            FileVersion toVersion = toVersionOpt.get();

            StringBuilder diff = new StringBuilder();
            diff.append("版本对比：").append("\n");
            diff.append("源版本：").append(fromVersionNumber).append("\n");
            diff.append("目标版本：").append(toVersionNumber).append("\n");
            diff.append("大小变化：").append(fromVersion.getSizeBytes()).append(" -> ").append(toVersion.getSizeBytes()).append("\n");
            diff.append("创建时间：").append(fromVersion.getCreatedAt()).append(" -> ").append(toVersion.getCreatedAt()).append("\n");
            
            if (fromVersion.getChangeComment() != null) {
                diff.append("源版本注释：").append(fromVersion.getChangeComment()).append("\n");
            }
            if (toVersion.getChangeComment() != null) {
                diff.append("目标版本注释：").append(toVersion.getChangeComment()).append("\n");
            }

            return Optional.of(diff.toString());

        } catch (Exception e) {
            logger.error("版本对比失败: 文件ID={}, 从版本={}, 到版本={}", 
                        file != null ? file.getId() : "null", fromVersionNumber, toVersionNumber, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVersionChangeSummary(FileEntity file, Integer versionNumber) {
        try {
            Optional<FileVersion> versionOpt = getVersionByNumber(file, versionNumber);
            if (!versionOpt.isPresent()) {
                return "版本不存在";
            }

            FileVersion version = versionOpt.get();
            StringBuilder summary = new StringBuilder();
            
            summary.append("版本 ").append(versionNumber);
            if (version.hasChangeComment()) {
                summary.append(": ").append(version.getChangeComment());
            }
            summary.append(" (").append(version.getHumanReadableSize()).append(")");
            
            return summary.toString();

        } catch (Exception e) {
            logger.error("获取版本变更摘要失败: 文件ID={}, 版本号={}", 
                        file != null ? file.getId() : "null", versionNumber, e);
            return "获取失败";
        }
    }

    // ==================== 版本清理和优化 ====================

    @Override
    public CleanupResult cleanupFileVersions(FileEntity file, CleanupConfig config) {
        try {
            if (file == null || config == null) {
                return new CleanupResult(0, 0, 0, Arrays.asList("参数不能为空"));
            }

            List<FileVersion> allVersions = getAllVersions(file, true); // 按版本号倒序
            List<FileVersion> versionsToDelete = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            switch (config.getStrategy()) {
                case KEEP_COUNT -> versionsToDelete = selectVersionsByCount(allVersions, config);
                case KEEP_DAYS -> versionsToDelete = selectVersionsByDays(allVersions, config);
                case KEEP_SIZE -> versionsToDelete = selectVersionsBySize(allVersions, config);
                case MIXED -> versionsToDelete = selectVersionsByMixed(allVersions, config);
                default -> {
                    errors.add("不支持的清理策略: " + config.getStrategy());
                    return new CleanupResult(0, 0, 0, errors);
                }
            }

            // 执行删除
            int deletedCount = 0;
            long spaceFreed = 0;

            for (FileVersion version : versionsToDelete) {
                try {
                    // 删除存储文件
                    boolean storageDeleted = storageService.delete(version.getStoragePath());
                    if (storageDeleted) {
                        spaceFreed += version.getSizeBytes();
                    }

                    // 删除版本记录
                    fileVersionRepository.delete(version);
                    deletedCount++;

                    logger.debug("清理版本成功: 文件ID={}, 版本号={}", file.getId(), version.getVersionNumber());

                } catch (Exception e) {
                    String error = String.format("删除版本失败: 版本号=%d, 错误=%s", version.getVersionNumber(), e.getMessage());
                    errors.add(error);
                    logger.warn(error, e);
                }
            }

            logger.info("文件版本清理完成: 文件ID={}, 处理版本数={}, 删除版本数={}, 释放空间={}字节", 
                       file.getId(), allVersions.size(), deletedCount, spaceFreed);

            return new CleanupResult(allVersions.size(), deletedCount, spaceFreed, errors);

        } catch (Exception e) {
            logger.error("文件版本清理失败: 文件ID={}", file != null ? file.getId() : "null", e);
            return new CleanupResult(0, 0, 0, Arrays.asList("清理失败: " + e.getMessage()));
        }
    }

    @Override
    public CleanupResult batchCleanupVersions(List<FileEntity> files, CleanupConfig config) {
        int totalProcessed = 0;
        int totalDeleted = 0;
        long totalSpaceFreed = 0;
        List<String> allErrors = new ArrayList<>();

        for (FileEntity file : files) {
            CleanupResult result = cleanupFileVersions(file, config);
            totalProcessed += result.getTotalVersionsProcessed();
            totalDeleted += result.getVersionsDeleted();
            totalSpaceFreed += result.getSpaceFreed();
            allErrors.addAll(result.getErrors());
        }

        logger.info("批量版本清理完成: 文件数={}, 处理版本数={}, 删除版本数={}, 释放空间={}字节", 
                   files.size(), totalProcessed, totalDeleted, totalSpaceFreed);

        return new CleanupResult(totalProcessed, totalDeleted, totalSpaceFreed, allErrors);
    }

    @Override
    public int cleanupOrphanedVersions() {
        try {
            // 查找孤立版本（文件实体不存在的版本）
            List<FileVersion> allVersions = fileVersionRepository.findAll();
            List<FileVersion> orphanedVersions = allVersions.stream()
                .filter(version -> version.getFile() == null)
                .collect(Collectors.toList());

            int deletedCount = 0;
            for (FileVersion version : orphanedVersions) {
                try {
                    // 删除存储文件
                    storageService.delete(version.getStoragePath());
                    // 删除版本记录
                    fileVersionRepository.delete(version);
                    deletedCount++;
                } catch (Exception e) {
                    logger.warn("删除孤立版本失败: 版本ID={}", version.getId(), e);
                }
            }

            logger.info("孤立版本清理完成: 删除数量={}", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("孤立版本清理失败", e);
            return 0;
        }
    }

    @Override
    public CleanupResult optimizeVersionStorage(FileEntity file) {
        try {
            if (file == null) {
                return new CleanupResult(0, 0, 0, Arrays.asList("文件实体不能为空"));
            }

            // 查找重复版本（相同哈希值）
            Map<String, List<FileVersion>> duplicateVersions = getDuplicateVersions(file);
            
            int totalVersions = (int) countVersions(file);
            int deletedCount = 0;
            long spaceFreed = 0;
            List<String> errors = new ArrayList<>();

            // 处理重复版本，保留最新的
            for (Map.Entry<String, List<FileVersion>> entry : duplicateVersions.entrySet()) {
                List<FileVersion> versions = entry.getValue();
                if (versions.size() > 1) {
                    // 按版本号排序，保留最新的
                    versions.sort(Comparator.comparing(FileVersion::getVersionNumber).reversed());
                    
                    // 删除除了第一个之外的所有版本
                    for (int i = 1; i < versions.size(); i++) {
                        FileVersion versionToDelete = versions.get(i);
                        try {
                            // 注意：不删除存储文件，因为可能被其他版本引用
                            fileVersionRepository.delete(versionToDelete);
                            spaceFreed += versionToDelete.getSizeBytes();
                            deletedCount++;
                        } catch (Exception e) {
                            errors.add("删除重复版本失败: 版本号=" + versionToDelete.getVersionNumber());
                        }
                    }
                }
            }

            logger.info("版本存储优化完成: 文件ID={}, 删除重复版本数={}, 释放空间={}字节", 
                       file.getId(), deletedCount, spaceFreed);

            return new CleanupResult(totalVersions, deletedCount, spaceFreed, errors);

        } catch (Exception e) {
            logger.error("版本存储优化失败: 文件ID={}", file != null ? file.getId() : "null", e);
            return new CleanupResult(0, 0, 0, Arrays.asList("优化失败: " + e.getMessage()));
        }
    }

    @Override
    public CleanupResult autoCleanupExpiredVersions(CleanupConfig defaultConfig) {
        try {
            // 获取所有有版本的文件
            List<FileEntity> files = fileVersionRepository.findAll().stream()
                .map(FileVersion::getFile)
                .distinct()
                .collect(Collectors.toList());

            return batchCleanupVersions(files, defaultConfig);

        } catch (Exception e) {
            logger.error("自动清理过期版本失败", e);
            return new CleanupResult(0, 0, 0, Arrays.asList("自动清理失败: " + e.getMessage()));
        }
    }

    // ==================== 版本统计和信息 ====================

    @Override
    @Transactional(readOnly = true)
    public VersionStatistics getVersionStatistics(FileEntity file) {
        try {
            if (file == null) {
                return new VersionStatistics(0, 0, 0, null, null, 0, 0.0);
            }

            List<FileVersion> versions = getAllVersions(file, false);
            if (versions.isEmpty()) {
                return new VersionStatistics(0, 0, 0, null, null, 0, 0.0);
            }

            long totalVersions = versions.size();
            long totalSize = versions.stream().mapToLong(FileVersion::getSizeBytes).sum();
            int maxVersionNumber = versions.stream().mapToInt(FileVersion::getVersionNumber).max().orElse(0);
            LocalDateTime firstVersionTime = versions.get(0).getCreatedAt();
            LocalDateTime lastVersionTime = versions.get(versions.size() - 1).getCreatedAt();
            long averageSize = totalSize / totalVersions;
            
            // 简单的压缩率计算（基于重复内容）
            long uniqueSize = versions.stream()
                .collect(Collectors.groupingBy(FileVersion::getFileHash))
                .values().stream()
                .mapToLong(list -> list.get(0).getSizeBytes())
                .sum();
            double compressionRatio = totalSize > 0 ? (double) uniqueSize / totalSize : 1.0;

            return new VersionStatistics(totalVersions, totalSize, maxVersionNumber, 
                                       firstVersionTime, lastVersionTime, averageSize, compressionRatio);

        } catch (Exception e) {
            logger.error("获取版本统计信息失败: 文件ID={}", file != null ? file.getId() : "null", e);
            return new VersionStatistics(0, 0, 0, null, null, 0, 0.0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countVersions(FileEntity file) {
        if (file == null) {
            return 0;
        }
        return fileVersionRepository.countByFile(file);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalVersionsSize(FileEntity file) {
        if (file == null) {
            return 0;
        }
        Long totalSize = fileVersionRepository.sumSizeBytesByFile(file);
        return totalSize != null ? totalSize : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getNextVersionNumber(FileEntity file) {
        if (file == null) {
            return 1;
        }
        
        Optional<FileVersion> latestVersion = getLatestVersion(file);
        if (latestVersion.isPresent()) {
            return latestVersion.get().getVersionNumber() + 1;
        }
        
        return 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateVersion(FileEntity file) {
        // 基本检查，可以根据业务需求扩展
        return file != null && file.getId() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean versionExists(FileEntity file, Integer versionNumber) {
        if (file == null || versionNumber == null) {
            return false;
        }
        return fileVersionRepository.existsByFileAndVersionNumber(file, versionNumber);
    }

    // ==================== 批量操作 ====================

    @Override
    public int batchDeleteVersions(List<Long> versionIds, Long deleterId) {
        int deletedCount = 0;
        for (Long versionId : versionIds) {
            if (deleteVersion(versionId, deleterId)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> batchVerifyVersionIntegrity(List<Long> versionIds) {
        Map<Long, Boolean> results = new HashMap<>();
        for (Long versionId : versionIds) {
            results.put(versionId, verifyVersionIntegrity(versionId));
        }
        return results;
    }

    // ==================== 搜索和过滤 ====================

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> searchVersionsByComment(FileEntity file, String keyword) {
        if (file == null || keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findByFileAndChangeCommentContaining(file, keyword.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getVersionsWithComment(FileEntity file) {
        if (file == null) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findVersionsWithCommentByFile(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getVersionsWithoutComment(FileEntity file) {
        if (file == null) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findVersionsWithoutCommentByFile(file);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getLargeVersions(FileEntity file, Long sizeThreshold) {
        if (file == null || sizeThreshold == null) {
            return new ArrayList<>();
        }
        return fileVersionRepository.findLargeVersionsByFile(file, sizeThreshold);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<FileVersion>> getDuplicateVersions(FileEntity file) {
        if (file == null) {
            return new HashMap<>();
        }
        
        List<FileVersion> allVersions = getAllVersions(file, false);
        return allVersions.stream()
            .filter(version -> version.getFileHash() != null)
            .collect(Collectors.groupingBy(FileVersion::getFileHash))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据保留数量选择要删除的版本
     */
    private List<FileVersion> selectVersionsByCount(List<FileVersion> allVersions, CleanupConfig config) {
        if (config.getKeepCount() == null || config.getKeepCount() <= 0) {
            return new ArrayList<>();
        }

        List<FileVersion> versionsToDelete = new ArrayList<>();
        
        // 排除需要保护的版本
        List<FileVersion> candidateVersions = allVersions.stream()
            .filter(version -> !shouldPreserveVersion(version, allVersions, config))
            .collect(Collectors.toList());

        // 如果候选版本数量超过保留数量，则删除多余的
        if (candidateVersions.size() > config.getKeepCount()) {
            versionsToDelete.addAll(candidateVersions.subList(config.getKeepCount(), candidateVersions.size()));
        }

        return versionsToDelete;
    }

    /**
     * 根据保留天数选择要删除的版本
     */
    private List<FileVersion> selectVersionsByDays(List<FileVersion> allVersions, CleanupConfig config) {
        if (config.getKeepDays() == null || config.getKeepDays() <= 0) {
            return new ArrayList<>();
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minus(config.getKeepDays(), ChronoUnit.DAYS);
        
        return allVersions.stream()
            .filter(version -> version.getCreatedAt() != null && version.getCreatedAt().isBefore(cutoffTime))
            .filter(version -> !shouldPreserveVersion(version, allVersions, config))
            .collect(Collectors.toList());
    }

    /**
     * 根据大小限制选择要删除的版本
     */
    private List<FileVersion> selectVersionsBySize(List<FileVersion> allVersions, CleanupConfig config) {
        if (config.getKeepSizeBytes() == null || config.getKeepSizeBytes() <= 0) {
            return new ArrayList<>();
        }

        List<FileVersion> versionsToDelete = new ArrayList<>();
        long currentSize = 0;
        
        // 按版本号倒序，优先保留新版本
        List<FileVersion> sortedVersions = allVersions.stream()
            .sorted(Comparator.comparing(FileVersion::getVersionNumber).reversed())
            .collect(Collectors.toList());

        for (FileVersion version : sortedVersions) {
            if (shouldPreserveVersion(version, allVersions, config)) {
                currentSize += version.getSizeBytes();
                continue;
            }

            if (currentSize + version.getSizeBytes() <= config.getKeepSizeBytes()) {
                currentSize += version.getSizeBytes();
            } else {
                versionsToDelete.add(version);
            }
        }

        return versionsToDelete;
    }

    /**
     * 根据混合策略选择要删除的版本
     */
    private List<FileVersion> selectVersionsByMixed(List<FileVersion> allVersions, CleanupConfig config) {
        Set<FileVersion> versionsToDelete = new HashSet<>();
        
        // 应用所有策略
        if (config.getKeepCount() != null) {
            versionsToDelete.addAll(selectVersionsByCount(allVersions, config));
        }
        if (config.getKeepDays() != null) {
            versionsToDelete.addAll(selectVersionsByDays(allVersions, config));
        }
        if (config.getKeepSizeBytes() != null) {
            versionsToDelete.addAll(selectVersionsBySize(allVersions, config));
        }

        return new ArrayList<>(versionsToDelete);
    }

    /**
     * 判断版本是否应该被保护
     */
    private boolean shouldPreserveVersion(FileVersion version, List<FileVersion> allVersions, CleanupConfig config) {
        // 保护第一个版本
        if (config.isPreserveFirstVersion() && version.isFirstVersion()) {
            return true;
        }

        // 保护最后一个版本
        if (config.isPreserveLastVersion()) {
            Optional<FileVersion> latestVersion = allVersions.stream()
                .max(Comparator.comparing(FileVersion::getVersionNumber));
            if (latestVersion.isPresent() && latestVersion.get().equals(version)) {
                return true;
            }
        }

        return false;
    }
} 