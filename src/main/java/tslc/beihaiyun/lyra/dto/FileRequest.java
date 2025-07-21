package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;

/**
 * 文件操作相关请求DTO集合
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class FileRequest {

    /**
     * 文件上传请求DTO
     */
    public static class FileUploadRequest {
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
        
        private Long folderId; // 可选，为null表示根目录
        
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        private String filename; // 可选，使用原文件名
        
        @Size(max = 500, message = "描述长度不能超过500个字符")
        private String description;
        
        private boolean overwrite = false; // 是否覆盖同名文件
        
        private boolean enableVersionControl = true; // 是否启用版本控制
        
        // Getters and Setters
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isOverwrite() { return overwrite; }
        public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
        
        public boolean isEnableVersionControl() { return enableVersionControl; }
        public void setEnableVersionControl(boolean enableVersionControl) { this.enableVersionControl = enableVersionControl; }
    }

    /**
     * 分块上传初始化请求DTO
     */
    public static class ChunkedUploadInitRequest {
        
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        private String filename;
        
        @NotNull(message = "文件大小不能为空")
        @Min(value = 1, message = "文件大小必须大于0")
        private Long fileSize;
        
        @NotBlank(message = "文件哈希值不能为空")
        private String fileHash;
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
        
        private Long folderId;
        
        @Size(max = 500, message = "描述长度不能超过500个字符")
        private String description;
        
        @Min(value = 1024, message = "分块大小至少为1KB")
        @Max(value = 104857600, message = "分块大小不能超过100MB")
        private Integer chunkSize = 1048576; // 默认1MB
        
        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getChunkSize() { return chunkSize; }
        public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    }

    /**
     * 文件移动请求DTO
     */
    public static class FileMoveRequest {
        
        @NotNull(message = "目标空间ID不能为空")
        private Long targetSpaceId;
        
        private Long targetFolderId; // null表示移动到根目录
        
        private boolean keepOriginal = false; // 是否保留原文件（复制模式）
        
        // Getters and Setters
        public Long getTargetSpaceId() { return targetSpaceId; }
        public void setTargetSpaceId(Long targetSpaceId) { this.targetSpaceId = targetSpaceId; }
        
        public Long getTargetFolderId() { return targetFolderId; }
        public void setTargetFolderId(Long targetFolderId) { this.targetFolderId = targetFolderId; }
        
        public boolean isKeepOriginal() { return keepOriginal; }
        public void setKeepOriginal(boolean keepOriginal) { this.keepOriginal = keepOriginal; }
    }

    /**
     * 文件复制请求DTO
     */
    public static class FileCopyRequest {
        
        @NotNull(message = "目标空间ID不能为空")
        private Long targetSpaceId;
        
        private Long targetFolderId; // null表示复制到根目录
        
        @Size(max = 255, message = "新文件名长度不能超过255个字符")
        private String newFilename; // 可选，使用原文件名
        
        // Getters and Setters
        public Long getTargetSpaceId() { return targetSpaceId; }
        public void setTargetSpaceId(Long targetSpaceId) { this.targetSpaceId = targetSpaceId; }
        
        public Long getTargetFolderId() { return targetFolderId; }
        public void setTargetFolderId(Long targetFolderId) { this.targetFolderId = targetFolderId; }
        
        public String getNewFilename() { return newFilename; }
        public void setNewFilename(String newFilename) { this.newFilename = newFilename; }
    }

    /**
     * 文件重命名请求DTO
     */
    public static class FileRenameRequest {
        
        @NotBlank(message = "新文件名不能为空")
        @Size(min = 1, max = 255, message = "文件名长度必须在1-255个字符之间")
        private String newFilename;
        
        // Getters and Setters
        public String getNewFilename() { return newFilename; }
        public void setNewFilename(String newFilename) { this.newFilename = newFilename; }
    }

    /**
     * 文件批量操作请求DTO
     */
    public static class FileBatchOperationRequest {
        
        @NotNull(message = "文件ID列表不能为空")
        @Size(min = 1, message = "至少需要选择一个文件")
        private List<Long> fileIds;
        
        @NotNull(message = "目标空间ID不能为空")
        private Long targetSpaceId;
        
        private Long targetFolderId; // null表示操作到根目录
        
        // Getters and Setters
        public List<Long> getFileIds() { return fileIds; }
        public void setFileIds(List<Long> fileIds) { this.fileIds = fileIds; }
        
        public Long getTargetSpaceId() { return targetSpaceId; }
        public void setTargetSpaceId(Long targetSpaceId) { this.targetSpaceId = targetSpaceId; }
        
        public Long getTargetFolderId() { return targetFolderId; }
        public void setTargetFolderId(Long targetFolderId) { this.targetFolderId = targetFolderId; }
    }

    /**
     * 文件更新请求DTO
     */
    public static class FileUpdateRequest {
        
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        private String filename;
        
        @Size(max = 500, message = "描述长度不能超过500个字符")
        private String description;
        
        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 文件搜索请求DTO
     */
    public static class FileSearchRequest {
        
        @NotBlank(message = "搜索关键字不能为空")
        @Size(min = 1, max = 100, message = "搜索关键字长度必须在1-100个字符之间")
        private String keyword;
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
        
        private String mimeType; // 可选的MIME类型过滤
        
        private boolean includeDeleted = false; // 是否包含已删除文件
        
        private int page = 0; // 页码，从0开始
        
        private int size = 20; // 每页数量
        
        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        
        public boolean isIncludeDeleted() { return includeDeleted; }
        public void setIncludeDeleted(boolean includeDeleted) { this.includeDeleted = includeDeleted; }
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }
} 