package tslc.beihaiyun.lyra.dto;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.service.FileService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件操作相关响应DTO集合
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class FileResponse {

    /**
     * 文件信息响应DTO
     */
    public static class FileInfoResponse {
        
        private Long id;
        private String filename;
        private String originalName;
        private String description;
        private String mimeType;
        private Long sizeBytes;
        private String fileHash;
        private String storagePath;
        private Long spaceId;
        private Long folderId;
        private String folderPath;
        private Long uploaderId;
        private String uploaderName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastAccessedAt;
        private FileEntity.FileStatus status;
        private boolean versionControlEnabled;
        private Integer currentVersion;
        private Long downloadCount;
        private boolean isPreviewSupported;
        
        public FileInfoResponse() {}
        
        public FileInfoResponse(FileEntity fileEntity) {
            this.id = fileEntity.getId();
            this.filename = fileEntity.getName();
            this.originalName = fileEntity.getOriginalName();
            this.description = null; // FileEntity暂无description字段
            this.mimeType = fileEntity.getMimeType();
            this.sizeBytes = fileEntity.getSizeBytes();
            this.fileHash = fileEntity.getFileHash();
            this.storagePath = fileEntity.getStoragePath();
            this.spaceId = fileEntity.getSpace() != null ? fileEntity.getSpace().getId() : null;
            this.folderId = fileEntity.getFolder() != null ? fileEntity.getFolder().getId() : null;
            this.folderPath = fileEntity.getFolder() != null ? fileEntity.getFolder().getPath() : "/";
            this.uploaderId = null; // FileEntity暂无uploader字段，需从BaseEntity获取创建者
            this.uploaderName = null; 
            this.createdAt = fileEntity.getCreatedAt();
            this.updatedAt = fileEntity.getUpdatedAt();
            this.lastAccessedAt = fileEntity.getLastModifiedAt(); // 使用lastModifiedAt作为替代
            this.status = fileEntity.getStatus();
            this.versionControlEnabled = false; // FileEntity暂无版本控制字段
            this.currentVersion = fileEntity.getVersion();
            this.downloadCount = fileEntity.getDownloadCount().longValue();
            this.isPreviewSupported = isPreviewable(fileEntity.getMimeType());
        }
        
        private boolean isPreviewable(String mimeType) {
            if (mimeType == null) return false;
            return mimeType.startsWith("text/") || 
                   mimeType.startsWith("image/") ||
                   mimeType.equals("application/pdf") ||
                   mimeType.equals("application/json") ||
                   mimeType.equals("application/xml");
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
        
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getFolderPath() { return folderPath; }
        public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
        
        public Long getUploaderId() { return uploaderId; }
        public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
        
        public String getUploaderName() { return uploaderName; }
        public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
        public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
        
        public FileEntity.FileStatus getStatus() { return status; }
        public void setStatus(FileEntity.FileStatus status) { this.status = status; }
        
        public boolean isVersionControlEnabled() { return versionControlEnabled; }
        public void setVersionControlEnabled(boolean versionControlEnabled) { this.versionControlEnabled = versionControlEnabled; }
        
        public Integer getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
        
        public Long getDownloadCount() { return downloadCount; }
        public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }
        
        public boolean isPreviewSupported() { return isPreviewSupported; }
        public void setPreviewSupported(boolean previewSupported) { isPreviewSupported = previewSupported; }
    }

    /**
     * 文件上传响应DTO
     */
    public static class FileUploadResponse {
        
        private boolean success;
        private String message;
        private FileInfoResponse fileInfo;
        private boolean isDuplicate;
        private String uploadId; // 分块上传时使用
        
        public FileUploadResponse() {}
        
        public FileUploadResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public FileUploadResponse(boolean success, String message, FileInfoResponse fileInfo) {
            this.success = success;
            this.message = message;
            this.fileInfo = fileInfo;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public FileInfoResponse getFileInfo() { return fileInfo; }
        public void setFileInfo(FileInfoResponse fileInfo) { this.fileInfo = fileInfo; }
        
        public boolean isDuplicate() { return isDuplicate; }
        public void setDuplicate(boolean duplicate) { isDuplicate = duplicate; }
        
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    }

    /**
     * 分块上传初始化响应DTO
     */
    public static class ChunkedUploadResponse {
        
        private String uploadId;
        private int totalChunks;
        private int chunkSize;
        private List<Integer> completedChunks;
        private boolean uploadCompleted;
        private FileInfoResponse fileInfo;
        
        // Getters and Setters
        public String getUploadId() { return uploadId; }
        public void setUploadId(String uploadId) { this.uploadId = uploadId; }
        
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        
        public List<Integer> getCompletedChunks() { return completedChunks; }
        public void setCompletedChunks(List<Integer> completedChunks) { this.completedChunks = completedChunks; }
        
        public boolean isUploadCompleted() { return uploadCompleted; }
        public void setUploadCompleted(boolean uploadCompleted) { this.uploadCompleted = uploadCompleted; }
        
        public FileInfoResponse getFileInfo() { return fileInfo; }
        public void setFileInfo(FileInfoResponse fileInfo) { this.fileInfo = fileInfo; }
    }

    /**
     * 批量操作响应DTO
     */
    public static class BatchOperationResponse {
        
        private int totalCount;
        private int successCount;
        private int failureCount;
        private List<String> errorMessages;
        private List<FileInfoResponse> successFiles;
        private double successRate;
        
        public BatchOperationResponse() {}
        
        public BatchOperationResponse(FileService.BatchOperationResult result) {
            this.totalCount = result.getTotalCount();
            this.successCount = result.getSuccessCount();
            this.failureCount = result.getFailureCount();
            this.errorMessages = result.getErrorMessages();
            this.successRate = result.getSuccessRate();
        }
        
        // Getters and Setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public List<FileInfoResponse> getSuccessFiles() { return successFiles; }
        public void setSuccessFiles(List<FileInfoResponse> successFiles) { this.successFiles = successFiles; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }

    /**
     * 文件列表响应DTO
     */
    public static class FileListResponse {
        
        private List<FileInfoResponse> files;
        private int totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        
        // Getters and Setters
        public List<FileInfoResponse> getFiles() { return files; }
        public void setFiles(List<FileInfoResponse> files) { this.files = files; }
        
        public int getTotalElements() { return totalElements; }
        public void setTotalElements(int totalElements) { this.totalElements = totalElements; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    /**
     * 文件统计响应DTO
     */
    public static class FileStatisticsResponse {
        
        private long totalFiles;
        private long totalSize;
        private long activeFiles;
        private long deletedFiles;
        private long archivedFiles;
        private LocalDateTime lastModified;
        private String formattedSize;
        
        public FileStatisticsResponse() {}
        
        public FileStatisticsResponse(FileService.FileStatistics stats) {
            this.totalFiles = stats.getTotalFiles();
            this.totalSize = stats.getTotalSize();
            this.activeFiles = stats.getActiveFiles();
            this.deletedFiles = stats.getDeletedFiles();
            this.archivedFiles = stats.getArchivedFiles();
            this.lastModified = stats.getLastModified();
            this.formattedSize = formatFileSize(stats.getTotalSize());
        }
        
        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp-1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }
        
        // Getters and Setters
        public long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }
        
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        
        public long getActiveFiles() { return activeFiles; }
        public void setActiveFiles(long activeFiles) { this.activeFiles = activeFiles; }
        
        public long getDeletedFiles() { return deletedFiles; }
        public void setDeletedFiles(long deletedFiles) { this.deletedFiles = deletedFiles; }
        
        public long getArchivedFiles() { return archivedFiles; }
        public void setArchivedFiles(long archivedFiles) { this.archivedFiles = archivedFiles; }
        
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
        
        public String getFormattedSize() { return formattedSize; }
        public void setFormattedSize(String formattedSize) { this.formattedSize = formattedSize; }
    }

    /**
     * 通用操作响应DTO
     */
    public static class OperationResponse {
        
        private boolean success;
        private String message;
        private Object data;
        
        public OperationResponse() {}
        
        public OperationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public OperationResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
} 