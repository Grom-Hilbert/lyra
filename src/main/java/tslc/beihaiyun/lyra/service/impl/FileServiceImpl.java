package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件管理服务实现
 * 提供完整的文件生命周期管理，包括CRUD操作、移动复制、回收站管理等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileEntityRepository fileEntityRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;

    @Autowired
    public FileServiceImpl(FileEntityRepository fileEntityRepository,
                          FolderRepository folderRepository,
                          StorageService storageService) {
        this.fileEntityRepository = fileEntityRepository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
    }

    // ==================== 基础CRUD操作 ====================

    @Override
    public FileOperationResult uploadFile(MultipartFile file, Space space, Folder folder, Long uploaderId) {
        String originalFilename = file != null ? file.getOriginalFilename() : "unknown";
        try {
            // 验证输入参数
            if (file == null || file.isEmpty()) {
                return new FileOperationResult(false, "上传文件不能为空", (FileEntity) null);
            }

            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return new FileOperationResult(false, "文件名不能为空", (FileEntity) null);
            }

            // 清理文件名
            String sanitizedFilename = FileUtils.sanitizeFilename(originalFilename);
            
            // 检查文件名是否已存在
            if (isFilenameExists(space, folder, sanitizedFilename, null)) {
                sanitizedFilename = generateUniqueFilename(space, folder, sanitizedFilename);
            }

            // 存储文件
            StorageService.StorageResult storageResult = storageService.store(file);

            // 创建文件实体
            FileEntity fileEntity = createFileEntity(
                sanitizedFilename,
                originalFilename,
                generateFilePath(folder, sanitizedFilename),
                folder,
                space,
                storageResult.getSizeBytes(),
                file.getContentType(),
                storageResult.getFileHash(),
                storageResult.getStoragePath(),
                uploaderId
            );

            // 保存到数据库
            fileEntity = fileEntityRepository.save(fileEntity);

            logger.info("文件上传成功: {}, 用户: {}, 空间: {}", sanitizedFilename, uploaderId, space.getId());
            return new FileOperationResult(true, "文件上传成功", fileEntity);

        } catch (IOException e) {
            logger.error("文件上传失败: {}", originalFilename, e);
            return new FileOperationResult(false, "文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件上传过程中发生未知错误: {}", originalFilename, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public FileOperationResult createFile(InputStream inputStream, String filename, String contentType,
                                        Space space, Folder folder, Long creatorId) {
        try {
            // 验证输入参数
            if (inputStream == null) {
                return new FileOperationResult(false, "文件内容不能为空", (FileEntity) null);
            }

            if (filename == null || filename.trim().isEmpty()) {
                return new FileOperationResult(false, "文件名不能为空", (FileEntity) null);
            }

            // 清理文件名
            String sanitizedFilename = FileUtils.sanitizeFilename(filename);
            
            // 检查文件名是否已存在
            if (isFilenameExists(space, folder, sanitizedFilename, null)) {
                sanitizedFilename = generateUniqueFilename(space, folder, sanitizedFilename);
            }

            // 存储文件
            StorageService.StorageResult storageResult = storageService.store(inputStream, sanitizedFilename, contentType);

            // 创建文件实体
            FileEntity fileEntity = createFileEntity(
                sanitizedFilename,
                filename,
                generateFilePath(folder, sanitizedFilename),
                folder,
                space,
                storageResult.getSizeBytes(),
                contentType,
                storageResult.getFileHash(),
                storageResult.getStoragePath(),
                creatorId
            );

            // 保存到数据库
            fileEntity = fileEntityRepository.save(fileEntity);

            logger.info("文件创建成功: {}, 用户: {}, 空间: {}", sanitizedFilename, creatorId, space.getId());
            return new FileOperationResult(true, "文件创建成功", fileEntity);

        } catch (IOException e) {
            logger.error("文件创建失败: {}", filename, e);
            return new FileOperationResult(false, "文件创建失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件创建过程中发生未知错误: {}", filename, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileEntity> getFileById(Long fileId) {
        if (fileId == null) {
            return Optional.empty();
        }
        return fileEntityRepository.findById(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileEntity> getFileByPath(Space space, String path) {
        if (space == null || path == null || path.trim().isEmpty()) {
            return Optional.empty();
        }
        return fileEntityRepository.findBySpaceAndPath(space, path.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InputStream> getFileContent(Long fileId) throws IOException {
        Optional<FileEntity> fileOptional = getFileById(fileId);
        if (!fileOptional.isPresent()) {
            return Optional.empty();
        }

        FileEntity file = fileOptional.get();
        if (file.getStatus() == FileEntity.FileStatus.DELETED) {
            return Optional.empty();
        }

        return storageService.load(file.getStoragePath());
    }

    @Override
    public FileOperationResult updateFileContent(Long fileId, InputStream inputStream, Long updaterId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                return new FileOperationResult(false, "文件不存在", (FileEntity) null);
            }

            FileEntity fileEntity = fileOptional.get();
            if (fileEntity.getStatus() == FileEntity.FileStatus.DELETED) {
                return new FileOperationResult(false, "文件已被删除", (FileEntity) null);
            }

            // 存储新内容
            StorageService.StorageResult storageResult = storageService.store(
                inputStream, fileEntity.getName(), fileEntity.getMimeType());

            // 删除旧文件
            storageService.delete(fileEntity.getStoragePath());

            // 更新文件信息
            fileEntity.setSizeBytes(storageResult.getSizeBytes());
            fileEntity.setFileHash(storageResult.getFileHash());
            fileEntity.setStoragePath(storageResult.getStoragePath());
            fileEntity.setVersion(fileEntity.getVersion() + 1);
            fileEntity.setLastModifiedAt(LocalDateTime.now());
            fileEntity.setUpdatedBy(updaterId.toString());

            fileEntity = fileEntityRepository.save(fileEntity);

            logger.info("文件内容更新成功: {}, 用户: {}", fileEntity.getName(), updaterId);
            return new FileOperationResult(true, "文件内容更新成功", fileEntity);

        } catch (IOException e) {
            logger.error("文件内容更新失败: {}", fileId, e);
            return new FileOperationResult(false, "文件内容更新失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("文件内容更新过程中发生未知错误: {}", fileId, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public FileOperationResult updateFileInfo(Long fileId, String newName, String newDescription, Long updaterId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                return new FileOperationResult(false, "文件不存在", (FileEntity) null);
            }

            FileEntity fileEntity = fileOptional.get();
            if (fileEntity.getStatus() == FileEntity.FileStatus.DELETED) {
                return new FileOperationResult(false, "文件已被删除", (FileEntity) null);
            }

            boolean updated = false;

            // 更新文件名
            if (newName != null && !newName.trim().isEmpty()) {
                String sanitizedName = FileUtils.sanitizeFilename(newName.trim());
                if (!sanitizedName.equals(fileEntity.getName())) {
                    // 检查新文件名是否已存在
                    if (isFilenameExists(fileEntity.getSpace(), fileEntity.getFolder(), sanitizedName, fileId)) {
                        return new FileOperationResult(false, "文件名已存在", (FileEntity) null);
                    }

                    fileEntity.setName(sanitizedName);
                    // 更新路径
                    String newPath = generateFilePath(fileEntity.getFolder(), sanitizedName);
                    fileEntity.setPath(newPath);
                    updated = true;
                }
            }

            // 更新描述（如果FileEntity有description字段的话）
            // if (newDescription != null) {
            //     fileEntity.setDescription(newDescription);
            //     updated = true;
            // }

            if (updated) {
                fileEntity.setUpdatedBy(updaterId.toString());
                fileEntity = fileEntityRepository.save(fileEntity);
                logger.info("文件信息更新成功: {}, 用户: {}", fileEntity.getName(), updaterId);
                return new FileOperationResult(true, "文件信息更新成功", fileEntity);
            } else {
                return new FileOperationResult(true, "没有变更", fileEntity);
            }

        } catch (Exception e) {
            logger.error("文件信息更新失败: {}", fileId, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(Long fileId, Long deleterId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                logger.warn("尝试删除不存在的文件: {}", fileId);
                return false;
            }

            FileEntity fileEntity = fileOptional.get();
            if (fileEntity.getStatus() == FileEntity.FileStatus.DELETED) {
                logger.warn("文件已被删除: {}", fileId);
                return true; // 已经删除，返回true
            }

            // 标记为删除状态
            fileEntity.setStatus(FileEntity.FileStatus.DELETED);
            fileEntity.setUpdatedBy(deleterId.toString());
            fileEntityRepository.save(fileEntity);

            logger.info("文件删除成功: {}, 用户: {}", fileEntity.getName(), deleterId);
            return true;

        } catch (Exception e) {
            logger.error("文件删除失败: {}", fileId, e);
            return false;
        }
    }

    @Override
    public boolean permanentDeleteFile(Long fileId, Long deleterId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                logger.warn("尝试永久删除不存在的文件: {}", fileId);
                return false;
            }

            FileEntity fileEntity = fileOptional.get();

            // 删除物理文件
            boolean storageDeleted = storageService.delete(fileEntity.getStoragePath());
            if (!storageDeleted) {
                logger.warn("物理文件删除失败，但继续删除数据库记录: {}", fileEntity.getStoragePath());
            }

            // 删除数据库记录
            fileEntityRepository.delete(fileEntity);

            logger.info("文件永久删除成功: {}, 用户: {}", fileEntity.getName(), deleterId);
            return true;

        } catch (Exception e) {
            logger.error("文件永久删除失败: {}", fileId, e);
            return false;
        }
    }

    // ==================== 文件操作 ====================

    @Override
    public FileOperationResult moveFile(Long fileId, Space targetSpace, Folder targetFolder, Long operatorId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                return new FileOperationResult(false, "文件不存在", (FileEntity) null);
            }

            FileEntity fileEntity = fileOptional.get();
            if (fileEntity.getStatus() == FileEntity.FileStatus.DELETED) {
                return new FileOperationResult(false, "文件已被删除", (FileEntity) null);
            }

            // 检查目标位置是否已存在同名文件
            if (isFilenameExists(targetSpace, targetFolder, fileEntity.getName(), fileId)) {
                return new FileOperationResult(false, "目标位置已存在同名文件", (FileEntity) null);
            }

            // 更新文件信息
            fileEntity.setSpace(targetSpace);
            fileEntity.setFolder(targetFolder);
            fileEntity.setPath(generateFilePath(targetFolder, fileEntity.getName()));
            fileEntity.setUpdatedBy(operatorId.toString());

            fileEntity = fileEntityRepository.save(fileEntity);

            logger.info("文件移动成功: {} -> {}, 用户: {}", 
                fileEntity.getName(), targetSpace.getName(), operatorId);
            return new FileOperationResult(true, "文件移动成功", fileEntity);

        } catch (Exception e) {
            logger.error("文件移动失败: {}", fileId, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public FileOperationResult copyFile(Long fileId, Space targetSpace, Folder targetFolder, Long operatorId) {
        try {
            Optional<FileEntity> sourceFileOptional = getFileById(fileId);
            if (!sourceFileOptional.isPresent()) {
                return new FileOperationResult(false, "源文件不存在", (FileEntity) null);
            }

            FileEntity sourceFile = sourceFileOptional.get();
            if (sourceFile.getStatus() == FileEntity.FileStatus.DELETED) {
                return new FileOperationResult(false, "源文件已被删除", (FileEntity) null);
            }

            // 生成新文件名（如果目标位置已存在同名文件）
            String newFilename = sourceFile.getName();
            if (isFilenameExists(targetSpace, targetFolder, newFilename, null)) {
                newFilename = generateUniqueFilename(targetSpace, targetFolder, newFilename);
            }

            // 复制物理文件
            String newStoragePath = FileUtils.generateStoragePath(sourceFile.getFileHash(), newFilename);
            boolean copySuccess = storageService.copy(sourceFile.getStoragePath(), newStoragePath);
            if (!copySuccess) {
                return new FileOperationResult(false, "物理文件复制失败", (FileEntity) null);
            }

            // 创建新文件实体
            FileEntity newFileEntity = createFileEntity(
                newFilename,
                sourceFile.getOriginalName(),
                generateFilePath(targetFolder, newFilename),
                targetFolder,
                targetSpace,
                sourceFile.getSizeBytes(),
                sourceFile.getMimeType(),
                sourceFile.getFileHash(),
                newStoragePath,
                operatorId
            );

            newFileEntity = fileEntityRepository.save(newFileEntity);

            logger.info("文件复制成功: {} -> {}, 用户: {}", 
                sourceFile.getName(), newFilename, operatorId);
            return new FileOperationResult(true, "文件复制成功", newFileEntity);

        } catch (Exception e) {
            logger.error("文件复制失败: {}", fileId, e);
            return new FileOperationResult(false, "系统错误: " + e.getMessage(), e);
        }
    }

    @Override
    public FileOperationResult renameFile(Long fileId, String newName, Long operatorId) {
        return updateFileInfo(fileId, newName, null, operatorId);
    }

    // ==================== 批量操作 ====================

    @Override
    public BatchOperationResult batchDeleteFiles(List<Long> fileIds, Long deleterId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = fileIds.size();

        for (Long fileId : fileIds) {
            try {
                if (deleteFile(fileId, deleterId)) {
                    successCount++;
                } else {
                    errorMessages.add("文件ID " + fileId + ": 删除失败");
                }
            } catch (Exception e) {
                errorMessages.add("文件ID " + fileId + ": " + e.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        logger.info("批量删除文件完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
        
        return new BatchOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    @Override
    public BatchOperationResult batchMoveFiles(List<Long> fileIds, Space targetSpace, Folder targetFolder, Long operatorId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = fileIds.size();

        for (Long fileId : fileIds) {
            try {
                FileOperationResult result = moveFile(fileId, targetSpace, targetFolder, operatorId);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    errorMessages.add("文件ID " + fileId + ": " + result.getMessage());
                }
            } catch (Exception e) {
                errorMessages.add("文件ID " + fileId + ": " + e.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        logger.info("批量移动文件完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
        
        return new BatchOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    @Override
    public BatchOperationResult batchCopyFiles(List<Long> fileIds, Space targetSpace, Folder targetFolder, Long operatorId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = fileIds.size();

        for (Long fileId : fileIds) {
            try {
                FileOperationResult result = copyFile(fileId, targetSpace, targetFolder, operatorId);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    errorMessages.add("文件ID " + fileId + ": " + result.getMessage());
                }
            } catch (Exception e) {
                errorMessages.add("文件ID " + fileId + ": " + e.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        logger.info("批量复制文件完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
        
        return new BatchOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    // ==================== 查询和搜索 ====================

    @Override
    @Transactional(readOnly = true)
    public List<FileEntity> getFilesBySpace(Space space, boolean includeDeleted) {
        if (includeDeleted) {
            return fileEntityRepository.findBySpace(space);
        } else {
            return fileEntityRepository.findBySpaceAndStatus(space, FileEntity.FileStatus.ACTIVE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntity> getFilesByFolder(Folder folder, boolean includeDeleted) {
        List<FileEntity> files = fileEntityRepository.findByFolder(folder);
        if (!includeDeleted) {
            files = files.stream()
                    .filter(file -> file.getStatus() == FileEntity.FileStatus.ACTIVE)
                    .collect(Collectors.toList());
        }
        return files;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntity> getRootFilesBySpace(Space space, boolean includeDeleted) {
        List<FileEntity> files = fileEntityRepository.findBySpaceAndFolderIsNull(space);
        if (!includeDeleted) {
            files = files.stream()
                    .filter(file -> file.getStatus() == FileEntity.FileStatus.ACTIVE)
                    .collect(Collectors.toList());
        }
        return files;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileEntity> getFilesPaged(Space space, Folder folder, boolean includeDeleted, Pageable pageable) {
        if (folder != null) {
            Page<FileEntity> files = fileEntityRepository.findByFolder(folder, pageable);
            if (!includeDeleted) {
                // Note: 这里应该在repository层面过滤，这只是示例
                // 实际应该添加自定义查询方法
            }
            return files;
        } else {
            if (includeDeleted) {
                return fileEntityRepository.findBySpace(space, pageable);
            } else {
                return fileEntityRepository.findBySpaceAndStatus(space, FileEntity.FileStatus.ACTIVE, pageable);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntity> searchFiles(Space space, String keyword, String mimeType, boolean includeDeleted) {
        List<FileEntity> results = new ArrayList<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            results = fileEntityRepository.findBySpaceAndNameContainingIgnoreCase(space, keyword.trim());
        } else {
            results = fileEntityRepository.findBySpace(space);
        }

        // 过滤MIME类型
        if (mimeType != null && !mimeType.trim().isEmpty()) {
            results = results.stream()
                    .filter(file -> mimeType.equals(file.getMimeType()))
                    .collect(Collectors.toList());
        }

        // 过滤删除状态
        if (!includeDeleted) {
            results = results.stream()
                    .filter(file -> file.getStatus() == FileEntity.FileStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        return results;
    }

    // ==================== 回收站管理 ====================

    @Override
    @Transactional(readOnly = true)
    public Page<FileEntity> getRecycleBinFiles(Space space, Pageable pageable) {
        return fileEntityRepository.findBySpaceAndStatus(space, FileEntity.FileStatus.DELETED, pageable);
    }

    @Override
    public boolean restoreFileFromRecycleBin(Long fileId, Long restorerId) {
        try {
            Optional<FileEntity> fileOptional = getFileById(fileId);
            if (!fileOptional.isPresent()) {
                logger.warn("尝试恢复不存在的文件: {}", fileId);
                return false;
            }

            FileEntity fileEntity = fileOptional.get();
            if (fileEntity.getStatus() != FileEntity.FileStatus.DELETED) {
                logger.warn("文件未在回收站中: {}", fileId);
                return false;
            }

            // 检查恢复位置是否有同名文件
            if (isFilenameExists(fileEntity.getSpace(), fileEntity.getFolder(), fileEntity.getName(), fileId)) {
                // 生成新的文件名
                String newName = generateUniqueFilename(fileEntity.getSpace(), fileEntity.getFolder(), fileEntity.getName());
                fileEntity.setName(newName);
                fileEntity.setPath(generateFilePath(fileEntity.getFolder(), newName));
            }

            fileEntity.setStatus(FileEntity.FileStatus.ACTIVE);
            fileEntity.setUpdatedBy(restorerId.toString());
            fileEntityRepository.save(fileEntity);

            logger.info("文件从回收站恢复成功: {}, 用户: {}", fileEntity.getName(), restorerId);
            return true;

        } catch (Exception e) {
            logger.error("文件从回收站恢复失败: {}", fileId, e);
            return false;
        }
    }

    @Override
    public BatchOperationResult batchRestoreFilesFromRecycleBin(List<Long> fileIds, Long restorerId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = fileIds.size();

        for (Long fileId : fileIds) {
            try {
                if (restoreFileFromRecycleBin(fileId, restorerId)) {
                    successCount++;
                } else {
                    errorMessages.add("文件ID " + fileId + ": 恢复失败");
                }
            } catch (Exception e) {
                errorMessages.add("文件ID " + fileId + ": " + e.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        logger.info("批量恢复文件完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
        
        return new BatchOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    @Override
    public int emptyRecycleBin(Space space, Long operatorId) {
        try {
            List<FileEntity> deletedFiles = fileEntityRepository.findBySpaceAndStatus(space, FileEntity.FileStatus.DELETED);
            int count = 0;

            for (FileEntity file : deletedFiles) {
                if (permanentDeleteFile(file.getId(), operatorId)) {
                    count++;
                }
            }

            logger.info("清空回收站完成: 空间={}, 删除文件数={}, 操作者={}", space.getName(), count, operatorId);
            return count;

        } catch (Exception e) {
            logger.error("清空回收站失败: 空间={}", space.getName(), e);
            return 0;
        }
    }

    @Override
    public int cleanupExpiredRecycleBinFiles(int retentionDays) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
            List<FileEntity> expiredFiles = fileEntityRepository.findByStatus(FileEntity.FileStatus.DELETED)
                    .stream()
                    .filter(file -> file.getUpdatedAt().isBefore(cutoffDate))
                    .collect(Collectors.toList());

            int count = 0;
            for (FileEntity file : expiredFiles) {
                if (permanentDeleteFile(file.getId(), 0L)) { // 系统清理，操作者ID为0
                    count++;
                }
            }

            logger.info("自动清理过期回收站文件完成: 删除文件数={}, 保留天数={}", count, retentionDays);
            return count;

        } catch (Exception e) {
            logger.error("自动清理过期回收站文件失败", e);
            return 0;
        }
    }

    // ==================== 统计和信息 ====================

    @Override
    @Transactional(readOnly = true)
    public FileStatistics getFileStatistics(Space space) {
        List<FileEntity> allFiles = fileEntityRepository.findBySpace(space);
        
        long totalFiles = allFiles.size();
        long totalSize = allFiles.stream().mapToLong(FileEntity::getSizeBytes).sum();
        long activeFiles = allFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.ACTIVE ? 1 : 0).sum();
        long deletedFiles = allFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.DELETED ? 1 : 0).sum();
        long archivedFiles = allFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.ARCHIVED ? 1 : 0).sum();
        
        LocalDateTime lastModified = allFiles.stream()
                .map(FileEntity::getLastModifiedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new FileStatistics(totalFiles, totalSize, activeFiles, deletedFiles, archivedFiles, lastModified);
    }

    @Override
    @Transactional(readOnly = true)
    public FileStatistics getUserFileStatistics(Long userId, Space space) {
        List<FileEntity> userFiles = fileEntityRepository.findBySpace(space)
                .stream()
                .filter(file -> userId.toString().equals(file.getCreatedBy()))
                .collect(Collectors.toList());
        
        long totalFiles = userFiles.size();
        long totalSize = userFiles.stream().mapToLong(FileEntity::getSizeBytes).sum();
        long activeFiles = userFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.ACTIVE ? 1 : 0).sum();
        long deletedFiles = userFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.DELETED ? 1 : 0).sum();
        long archivedFiles = userFiles.stream().mapToLong(file -> 
            file.getStatus() == FileEntity.FileStatus.ARCHIVED ? 1 : 0).sum();
        
        LocalDateTime lastModified = userFiles.stream()
                .map(FileEntity::getLastModifiedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new FileStatistics(totalFiles, totalSize, activeFiles, deletedFiles, archivedFiles, lastModified);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFilenameExists(Space space, Folder folder, String filename, Long excludeFileId) {
        if (excludeFileId != null) {
            return fileEntityRepository.existsBySpaceAndPathAndIdNot(
                space, generateFilePath(folder, filename), excludeFileId);
        } else {
            return fileEntityRepository.existsBySpaceAndPath(
                space, generateFilePath(folder, filename));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyFileIntegrity(Long fileId) {
        Optional<FileEntity> fileOptional = getFileById(fileId);
        if (!fileOptional.isPresent()) {
            return false;
        }

        FileEntity file = fileOptional.get();
        return storageService.verifyIntegrity(file.getStoragePath(), file.getFileHash());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建文件实体
     */
    private FileEntity createFileEntity(String name, String originalName, String path,
                                       Folder folder, Space space, long sizeBytes,
                                       String mimeType, String fileHash, String storagePath,
                                       Long creatorId) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(name);
        fileEntity.setOriginalName(originalName);
        fileEntity.setPath(path);
        fileEntity.setFolder(folder);
        fileEntity.setSpace(space);
        fileEntity.setSizeBytes(sizeBytes);
        fileEntity.setMimeType(mimeType);
        fileEntity.setFileHash(fileHash);
        fileEntity.setStoragePath(storagePath);
        fileEntity.setVersion(1);
        fileEntity.setStatus(FileEntity.FileStatus.ACTIVE);
        fileEntity.setIsPublic(false);
        fileEntity.setDownloadCount(0);
        fileEntity.setLastModifiedAt(LocalDateTime.now());
        fileEntity.setCreatedBy(creatorId.toString());
        fileEntity.setUpdatedBy(creatorId.toString());
        return fileEntity;
    }

    /**
     * 生成文件路径
     */
    private String generateFilePath(Folder folder, String filename) {
        if (folder == null) {
            // 根目录文件直接返回文件名，不加前导斜杠
            return filename;
        }
        
        String folderPath = folder.getPath();
        if (folderPath.endsWith("/")) {
            return folderPath + filename;
        } else {
            return folderPath + "/" + filename;
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFilename(Space space, Folder folder, String originalFilename) {
        String nameWithoutExt = FileUtils.getNameWithoutExtension(originalFilename);
        String extension = FileUtils.getFileExtension(originalFilename);
        
        int counter = 1;
        String newFilename;
        
        do {
            if (extension.isEmpty()) {
                newFilename = nameWithoutExt + "_" + counter;
            } else {
                newFilename = nameWithoutExt + "_" + counter + "." + extension;
            }
            counter++;
        } while (isFilenameExists(space, folder, newFilename, null) && counter < 1000);
        
        return newFilename;
    }
} 