package tslc.beihaiyun.lyra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tslc.beihaiyun.lyra.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileDTO extends BaseDTO {

    private String name;
    private String path;
    private String mimeType;
    private Long size;
    private String checksum;
    private FileEntity.SpaceType spaceType;
    private FileEntity.VersionControlType versionControlType;
    private Long folderId;
    private UserDTO owner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime accessedAt;

    private List<FileVersionDTO> versions;
    private List<FilePermissionDTO> permissions;

    /**
     * 文件信息简化DTO
     */
    @Data
    public static class FileInfo {
        private Long id;
        private String name;
        private String path;
        private String type; // FILE or FOLDER
        private String mimeType;
        private Long size;
        private FileEntity.SpaceType spaceType;
        private FileEntity.VersionControlType versionControlType;
        private String owner;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime accessedAt;

        private List<String> permissions;
        private Boolean isShared;
        private Boolean hasVersions;
    }

    /**
     * 文件上传响应DTO
     */
    @Data
    public static class UploadResponse {
        private Long fileId;
        private String name;
        private String path;
        private Long size;
        private String checksum;
        private Integer versionNumber;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime uploadTime;
    }

    /**
     * 文件移动请求DTO
     */
    @Data
    public static class MoveRequest {
        private String targetPath;
        private Long targetFolderId;
        private String newName;
    }

    /**
     * 文件复制请求DTO
     */
    @Data
    public static class CopyRequest {
        private String targetPath;
        private Long targetFolderId;
        private String newName;
        private Boolean copyVersions = false;
    }

    /**
     * 文件重命名请求DTO
     */
    @Data
    public static class RenameRequest {
        private String newName;
    }

    /**
     * 文件分享请求DTO
     */
    @Data
    public static class ShareRequest {
        private Long fileId;
        private String shareType; // PUBLIC or PRIVATE
        private LocalDateTime expiresAt;
        private String password;
        private Boolean allowDownload = true;
        private Boolean allowPreview = true;
    }

    /**
     * 文件分享响应DTO
     */
    @Data
    public static class ShareResponse {
        private String shareId;
        private String shareUrl;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime expiresAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }

    /**
     * 文件搜索请求DTO
     */
    @Data
    public static class SearchRequest {
        private String keyword;
        private String path;
        private FileEntity.SpaceType spaceType;
        private String mimeType;
        private String owner;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime dateFrom;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime dateTo;

        private Long sizeMin;
        private Long sizeMax;
    }

    /**
     * 文件预览响应DTO
     */
    @Data
    public static class PreviewResponse {
        private String previewType; // TEXT, IMAGE, PDF, OFFICE, UNSUPPORTED
        private String content;
        private String imageUrl;
        private Integer pageCount;
        private Integer currentPage;
    }

    /**
     * 文件编辑请求DTO
     */
    @Data
    public static class EditRequest {
        private Long fileId;
        private String content;
        private String versionDescription;
    }

    /**
     * 批量文件操作请求DTO
     */
    @Data
    public static class BatchOperationRequest {
        private List<Long> fileIds;
        private String operation; // DELETE, MOVE, COPY, SHARE
        private String targetPath;
        private Long targetFolderId;
    }

    /**
     * 批量操作响应DTO
     */
    @Data
    public static class BatchOperationResponse {
        private Integer successCount;
        private Integer failureCount;
        private List<OperationResult> results;

        @Data
        public static class OperationResult {
            private Long fileId;
            private Boolean success;
            private String message;
        }
    }
}