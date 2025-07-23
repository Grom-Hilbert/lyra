package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 文件预览请求DTO
 * 用于接收文件预览相关的请求参数
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public class PreviewRequest {

    /**
     * 文件ID
     */
    @NotNull(message = "文件ID不能为空")
    @Min(value = 1, message = "文件ID必须大于0")
    private Long fileId;

    /**
     * 预览选项 - 文本文件最大行数
     */
    private Integer maxLines = 1000;

    /**
     * 预览选项 - 是否生成缩略图
     */
    private Boolean generateThumbnail = true;

    /**
     * 预览选项 - 缩略图尺寸
     */
    private String thumbnailSize = "medium"; // small, medium, large

    /**
     * 预览选项 - 是否包含元数据
     */
    private Boolean includeMetadata = true;

    // 默认构造函数
    public PreviewRequest() {}

    // 构造函数
    public PreviewRequest(Long fileId) {
        this.fileId = fileId;
    }

    public PreviewRequest(Long fileId, Integer maxLines, Boolean generateThumbnail, 
                         String thumbnailSize, Boolean includeMetadata) {
        this.fileId = fileId;
        this.maxLines = maxLines;
        this.generateThumbnail = generateThumbnail;
        this.thumbnailSize = thumbnailSize;
        this.includeMetadata = includeMetadata;
    }

    // Getter和Setter方法
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getMaxLines() {
        return maxLines != null ? maxLines : 1000;
    }

    public void setMaxLines(Integer maxLines) {
        this.maxLines = maxLines;
    }

    public Boolean getGenerateThumbnail() {
        return generateThumbnail != null ? generateThumbnail : true;
    }

    public void setGenerateThumbnail(Boolean generateThumbnail) {
        this.generateThumbnail = generateThumbnail;
    }

    public String getThumbnailSize() {
        return thumbnailSize != null ? thumbnailSize : "medium";
    }

    public void setThumbnailSize(String thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public Boolean getIncludeMetadata() {
        return includeMetadata != null ? includeMetadata : true;
    }

    public void setIncludeMetadata(Boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    @Override
    public String toString() {
        return "PreviewRequest{" +
                "fileId=" + fileId +
                ", maxLines=" + maxLines +
                ", generateThumbnail=" + generateThumbnail +
                ", thumbnailSize='" + thumbnailSize + '\'' +
                ", includeMetadata=" + includeMetadata +
                '}';
    }
} 