package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件夹操作请求DTO
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class FolderRequest {

    /**
     * 文件夹创建请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFolderRequest {
        
        @NotBlank(message = "文件夹名称不能为空")
        @Size(max = 255, message = "文件夹名称长度不能超过255个字符")
        private String name;
        
        private Long parentFolderId;
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
    }

    /**
     * 文件夹更新请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFolderRequest {
        
        @NotBlank(message = "文件夹名称不能为空")
        @Size(max = 255, message = "文件夹名称长度不能超过255个字符")
        private String name;
    }

    /**
     * 文件夹移动请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveFolderRequest {
        
        @NotNull(message = "文件夹ID不能为空")
        private Long folderId;
        
        private Long targetParentFolderId;
    }

    /**
     * 批量创建文件夹请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchCreateFolderRequest {
        
        @NotNull(message = "文件夹名称列表不能为空")
        @Size(min = 1, max = 100, message = "批量创建文件夹数量必须在1-100之间")
        private List<@NotBlank @Size(max = 255) String> folderNames;
        
        private Long parentFolderId;
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
    }

    /**
     * 批量删除文件夹请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDeleteFolderRequest {
        
        @NotNull(message = "文件夹ID列表不能为空")
        @Size(min = 1, max = 100, message = "批量删除文件夹数量必须在1-100之间")
        private List<@NotNull Long> folderIds;
        
        private boolean force = false;
    }

    /**
     * 批量移动文件夹请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchMoveFolderRequest {
        
        @NotNull(message = "文件夹ID列表不能为空")
        @Size(min = 1, max = 100, message = "批量移动文件夹数量必须在1-100之间")
        private List<@NotNull Long> folderIds;
        
        private Long targetParentFolderId;
    }

    /**
     * 文件夹搜索请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFolderRequest {
        
        @NotBlank(message = "搜索关键词不能为空")
        @Size(max = 100, message = "搜索关键词长度不能超过100个字符")
        private String keyword;
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
    }

    /**
     * 版本控制设置请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionControlRequest {
        
        @NotNull(message = "版本控制启用状态不能为空")
        private Boolean versionControlEnabled;
        
        private Boolean inheritFromParent = false;
    }

    /**
     * 配额检查请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaCheckRequest {
        
        @NotNull(message = "空间ID不能为空")
        private Long spaceId;
        
        private Long additionalSize = 0L;
    }
} 