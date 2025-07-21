package tslc.beihaiyun.lyra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.service.FolderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件夹操作响应DTO
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class FolderResponse {

    /**
     * 文件夹详细信息响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderDetailResponse {
        
        private Long id;
        private String name;
        private String path;
        private Long parentFolderId;
        private Long spaceId;
        private Integer level;
        private Boolean isRoot;
        private Long sizeBytes;
        private Integer fileCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;

        /**
         * 从Folder实体转换为响应DTO
         */
        public static FolderDetailResponse fromEntity(Folder folder) {
            if (folder == null) {
                return null;
            }
            return new FolderDetailResponse(
                folder.getId(),
                folder.getName(),
                folder.getPath(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getSpace() != null ? folder.getSpace().getId() : null,
                folder.getLevel(),
                folder.getIsRoot(),
                folder.getSizeBytes(),
                folder.getFileCount(),
                folder.getCreatedAt(),
                folder.getUpdatedAt(),
                folder.getCreatedBy(),
                folder.getUpdatedBy()
            );
        }
    }

    /**
     * 文件夹简要信息响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderSummaryResponse {
        
        private Long id;
        private String name;
        private String path;
        private Integer level;
        private Boolean isRoot;
        private Long sizeBytes;
        private Integer fileCount;

        /**
         * 从Folder实体转换为简要响应DTO
         */
        public static FolderSummaryResponse fromEntity(Folder folder) {
            if (folder == null) {
                return null;
            }
            return new FolderSummaryResponse(
                folder.getId(),
                folder.getName(),
                folder.getPath(),
                folder.getLevel(),
                folder.getIsRoot(),
                folder.getSizeBytes(),
                folder.getFileCount()
            );
        }
    }

    /**
     * 文件夹树节点响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderTreeResponse {
        
        private FolderSummaryResponse folder;
        private List<FolderTreeResponse> children;
        private Long totalSize;
        private Integer totalFileCount;

        /**
         * 从FolderTreeNode转换为响应DTO
         */
        public static FolderTreeResponse fromTreeNode(FolderService.FolderTreeNode node) {
            if (node == null) {
                return null;
            }
            
            List<FolderTreeResponse> children = node.getChildren().stream()
                .map(FolderTreeResponse::fromTreeNode)
                .collect(Collectors.toList());
                
            return new FolderTreeResponse(
                FolderSummaryResponse.fromEntity(node.getFolder()),
                children,
                node.getTotalSize(),
                node.getTotalFileCount()
            );
        }
    }

    /**
     * 文件夹操作结果响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderOperationResponse {
        
        private Boolean success;
        private String message;
        private FolderDetailResponse folder;

        /**
         * 从FolderOperationResult转换为响应DTO
         */
        public static FolderOperationResponse fromResult(FolderService.FolderOperationResult result) {
            if (result == null) {
                return null;
            }
            return new FolderOperationResponse(
                result.isSuccess(),
                result.getMessage(),
                FolderDetailResponse.fromEntity(result.getFolder())
            );
        }
    }

    /**
     * 批量操作结果响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchOperationResponse {
        
        private Integer totalCount;
        private Integer successCount;
        private Integer failureCount;
        private List<String> errorMessages;
        private Double successRate;

        /**
         * 从BatchFolderOperationResult转换为响应DTO
         */
        public static BatchOperationResponse fromResult(FolderService.BatchFolderOperationResult result) {
            if (result == null) {
                return null;
            }
            return new BatchOperationResponse(
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailureCount(),
                result.getErrorMessages(),
                result.getSuccessRate()
            );
        }
    }

    /**
     * 文件夹统计信息响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderStatisticsResponse {
        
        private Long totalFolders;
        private Long totalSize;
        private Integer maxDepth;
        private Long emptyFolders;
        private FolderSummaryResponse largestFolder;

        /**
         * 从FolderStatistics转换为响应DTO
         */
        public static FolderStatisticsResponse fromStatistics(FolderService.FolderStatistics statistics) {
            if (statistics == null) {
                return null;
            }
            return new FolderStatisticsResponse(
                statistics.getTotalFolders(),
                statistics.getTotalSize(),
                statistics.getMaxDepth(),
                statistics.getEmptyFolders(),
                FolderSummaryResponse.fromEntity(statistics.getLargestFolder())
            );
        }
    }

    /**
     * 配额检查响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaCheckResponse {
        
        private Long currentUsedSize;
        private Long totalQuota;
        private Long availableSize;
        private Boolean canAllocate;
        private String message;
    }

    /**
     * API统一响应格式
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        
        private Boolean success;
        private String message;
        private T data;
        private Long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "操作成功", data, System.currentTimeMillis());
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data, System.currentTimeMillis());
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null, System.currentTimeMillis());
        }

        public static <T> ApiResponse<T> error(String message, T data) {
            return new ApiResponse<>(false, message, data, System.currentTimeMillis());
        }
    }
} 