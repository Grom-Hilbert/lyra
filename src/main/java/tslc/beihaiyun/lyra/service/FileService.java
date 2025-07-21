package tslc.beihaiyun.lyra.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件管理服务接口
 * 提供文件的完整生命周期管理，包括CRUD操作、文件移动复制、回收站管理等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public interface FileService {

    /**
     * 文件操作结果
     */
    class FileOperationResult {
        private final boolean success;
        private final String message;
        private final FileEntity fileEntity;
        private final Exception exception;

        public FileOperationResult(boolean success, String message, FileEntity fileEntity) {
            this.success = success;
            this.message = message;
            this.fileEntity = fileEntity;
            this.exception = null;
        }

        public FileOperationResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.fileEntity = null;
            this.exception = exception;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public FileEntity getFileEntity() { return fileEntity; }
        public Exception getException() { return exception; }
    }

    /**
     * 批量操作结果
     */
    class BatchOperationResult {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final List<String> errorMessages;

        public BatchOperationResult(int totalCount, int successCount, int failureCount, List<String> errorMessages) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessages = errorMessages;
        }

        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getErrorMessages() { return errorMessages; }
        public boolean isAllSuccess() { return failureCount == 0; }
        public double getSuccessRate() { return totalCount > 0 ? (double) successCount / totalCount : 0; }
    }

    // ==================== 基础CRUD操作 ====================

    /**
     * 上传文件
     * 
     * @param file 上传的文件
     * @param space 目标空间
     * @param folder 目标文件夹（可为null表示根目录）
     * @param uploaderId 上传者ID
     * @return 文件操作结果
     */
    FileOperationResult uploadFile(MultipartFile file, Space space, Folder folder, Long uploaderId);

    /**
     * 从输入流创建文件
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param contentType 内容类型
     * @param space 目标空间
     * @param folder 目标文件夹
     * @param creatorId 创建者ID
     * @return 文件操作结果
     */
    FileOperationResult createFile(InputStream inputStream, String filename, String contentType, 
                                 Space space, Folder folder, Long creatorId);

    /**
     * 根据ID获取文件
     * 
     * @param fileId 文件ID
     * @return 文件实体（可选）
     */
    Optional<FileEntity> getFileById(Long fileId);

    /**
     * 根据空间和路径获取文件
     * 
     * @param space 所属空间
     * @param path 文件路径
     * @return 文件实体（可选）
     */
    Optional<FileEntity> getFileByPath(Space space, String path);

    /**
     * 获取文件内容流
     * 
     * @param fileId 文件ID
     * @return 文件输入流（可选）
     * @throws IOException 读取异常
     */
    Optional<InputStream> getFileContent(Long fileId) throws IOException;

    /**
     * 更新文件内容
     * 
     * @param fileId 文件ID
     * @param inputStream 新的文件内容流
     * @param updaterId 更新者ID
     * @return 文件操作结果
     */
    FileOperationResult updateFileContent(Long fileId, InputStream inputStream, Long updaterId);

    /**
     * 更新文件信息
     * 
     * @param fileId 文件ID
     * @param newName 新文件名（可选）
     * @param newDescription 新描述（可选）
     * @param updaterId 更新者ID
     * @return 文件操作结果
     */
    FileOperationResult updateFileInfo(Long fileId, String newName, String newDescription, Long updaterId);

    /**
     * 删除文件（移动到回收站）
     * 
     * @param fileId 文件ID
     * @param deleterId 删除者ID
     * @return 操作是否成功
     */
    boolean deleteFile(Long fileId, Long deleterId);

    /**
     * 彻底删除文件（物理删除）
     * 
     * @param fileId 文件ID
     * @param deleterId 删除者ID
     * @return 操作是否成功
     */
    boolean permanentDeleteFile(Long fileId, Long deleterId);

    // ==================== 文件操作 ====================

    /**
     * 移动文件
     * 
     * @param fileId 文件ID
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹（可为null）
     * @param operatorId 操作者ID
     * @return 文件操作结果
     */
    FileOperationResult moveFile(Long fileId, Space targetSpace, Folder targetFolder, Long operatorId);

    /**
     * 复制文件
     * 
     * @param fileId 源文件ID
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹（可为null）
     * @param operatorId 操作者ID
     * @return 文件操作结果
     */
    FileOperationResult copyFile(Long fileId, Space targetSpace, Folder targetFolder, Long operatorId);

    /**
     * 重命名文件
     * 
     * @param fileId 文件ID
     * @param newName 新文件名
     * @param operatorId 操作者ID
     * @return 文件操作结果
     */
    FileOperationResult renameFile(Long fileId, String newName, Long operatorId);

    // ==================== 批量操作 ====================

    /**
     * 批量删除文件
     * 
     * @param fileIds 文件ID列表
     * @param deleterId 删除者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchDeleteFiles(List<Long> fileIds, Long deleterId);

    /**
     * 批量移动文件
     * 
     * @param fileIds 文件ID列表
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchMoveFiles(List<Long> fileIds, Space targetSpace, Folder targetFolder, Long operatorId);

    /**
     * 批量复制文件
     * 
     * @param fileIds 文件ID列表
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchCopyFiles(List<Long> fileIds, Space targetSpace, Folder targetFolder, Long operatorId);

    // ==================== 查询和搜索 ====================

    /**
     * 获取空间下的所有文件
     * 
     * @param space 所属空间
     * @param includeDeleted 是否包含已删除文件
     * @return 文件列表
     */
    List<FileEntity> getFilesBySpace(Space space, boolean includeDeleted);

    /**
     * 获取文件夹下的文件
     * 
     * @param folder 所属文件夹
     * @param includeDeleted 是否包含已删除文件
     * @return 文件列表
     */
    List<FileEntity> getFilesByFolder(Folder folder, boolean includeDeleted);

    /**
     * 获取空间根目录下的文件
     * 
     * @param space 所属空间
     * @param includeDeleted 是否包含已删除文件
     * @return 文件列表
     */
    List<FileEntity> getRootFilesBySpace(Space space, boolean includeDeleted);

    /**
     * 分页查询文件
     * 
     * @param space 所属空间
     * @param folder 所属文件夹（可为null）
     * @param includeDeleted 是否包含已删除文件
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<FileEntity> getFilesPaged(Space space, Folder folder, boolean includeDeleted, Pageable pageable);

    /**
     * 搜索文件
     * 
     * @param space 搜索空间
     * @param keyword 关键字
     * @param mimeType MIME类型过滤（可选）
     * @param includeDeleted 是否包含已删除文件
     * @return 文件列表
     */
    List<FileEntity> searchFiles(Space space, String keyword, String mimeType, boolean includeDeleted);

    // ==================== 回收站管理 ====================

    /**
     * 获取回收站中的文件
     * 
     * @param space 所属空间
     * @param pageable 分页参数
     * @return 回收站文件分页结果
     */
    Page<FileEntity> getRecycleBinFiles(Space space, Pageable pageable);

    /**
     * 从回收站恢复文件
     * 
     * @param fileId 文件ID
     * @param restorerId 恢复者ID
     * @return 操作是否成功
     */
    boolean restoreFileFromRecycleBin(Long fileId, Long restorerId);

    /**
     * 批量从回收站恢复文件
     * 
     * @param fileIds 文件ID列表
     * @param restorerId 恢复者ID
     * @return 批量操作结果
     */
    BatchOperationResult batchRestoreFilesFromRecycleBin(List<Long> fileIds, Long restorerId);

    /**
     * 清空回收站
     * 
     * @param space 所属空间
     * @param operatorId 操作者ID
     * @return 清理的文件数量
     */
    int emptyRecycleBin(Space space, Long operatorId);

    /**
     * 自动清理过期的回收站文件
     * 
     * @param retentionDays 保留天数
     * @return 清理的文件数量
     */
    int cleanupExpiredRecycleBinFiles(int retentionDays);

    // ==================== 统计和信息 ====================

    /**
     * 获取空间文件统计信息
     * 
     * @param space 所属空间
     * @return 文件统计信息
     */
    FileStatistics getFileStatistics(Space space);

    /**
     * 获取用户文件统计信息
     * 
     * @param userId 用户ID
     * @param space 所属空间
     * @return 文件统计信息
     */
    FileStatistics getUserFileStatistics(Long userId, Space space);

    /**
     * 检查文件名是否在空间中已存在
     * 
     * @param space 所属空间
     * @param folder 所属文件夹
     * @param filename 文件名
     * @param excludeFileId 排除的文件ID（用于重命名时检查）
     * @return 是否已存在
     */
    boolean isFilenameExists(Space space, Folder folder, String filename, Long excludeFileId);

    /**
     * 验证文件完整性
     * 
     * @param fileId 文件ID
     * @return 是否完整
     */
    boolean verifyFileIntegrity(Long fileId);

    /**
     * 文件统计信息
     */
    class FileStatistics {
        private final long totalFiles;
        private final long totalSize;
        private final long activeFiles;
        private final long deletedFiles;
        private final long archivedFiles;
        private final LocalDateTime lastModified;

        public FileStatistics(long totalFiles, long totalSize, long activeFiles, 
                            long deletedFiles, long archivedFiles, LocalDateTime lastModified) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.activeFiles = activeFiles;
            this.deletedFiles = deletedFiles;
            this.archivedFiles = archivedFiles;
            this.lastModified = lastModified;
        }

        public long getTotalFiles() { return totalFiles; }
        public long getTotalSize() { return totalSize; }
        public long getActiveFiles() { return activeFiles; }
        public long getDeletedFiles() { return deletedFiles; }
        public long getArchivedFiles() { return archivedFiles; }
        public LocalDateTime getLastModified() { return lastModified; }
    }
} 