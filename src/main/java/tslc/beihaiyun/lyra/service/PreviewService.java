package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 文件预览服务接口
 * 提供多种文件格式的预览功能，包括文本、图片、PDF、音视频等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public interface PreviewService {

    /**
     * 预览类型枚举
     */
    enum PreviewType {
        /** 文本预览 - 直接返回文件内容 */
        TEXT,
        /** 图片预览 - 返回图片URL和元数据 */
        IMAGE,
        /** PDF预览 - 返回PDF URL用于浏览器内置查看器 */
        PDF,
        /** 音视频预览 - 返回媒体URL和元数据 */
        MEDIA,
        /** 不支持的文件类型 */
        UNSUPPORTED
    }

    /**
     * 预览结果
     */
    class PreviewResult {
        private final boolean success;
        private final PreviewType type;
        private final String message;
        private final Map<String, Object> data;
        private final Map<String, Object> metadata;

        public PreviewResult(boolean success, PreviewType type, String message, 
                           Map<String, Object> data, Map<String, Object> metadata) {
            this.success = success;
            this.type = type;
            this.message = message;
            this.data = data;
            this.metadata = metadata;
        }

        public boolean isSuccess() { return success; }
        public PreviewType getType() { return type; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * 文本预览结果
     */
    class TextPreviewResult {
        private final String content;
        private final String encoding;
        private final long lineCount;
        private final long characterCount;

        public TextPreviewResult(String content, String encoding, long lineCount, long characterCount) {
            this.content = content;
            this.encoding = encoding;
            this.lineCount = lineCount;
            this.characterCount = characterCount;
        }

        public String getContent() { return content; }
        public String getEncoding() { return encoding; }
        public long getLineCount() { return lineCount; }
        public long getCharacterCount() { return characterCount; }
    }

    /**
     * 图片预览结果
     */
    class ImagePreviewResult {
        private final String contentUrl;
        private final String thumbnailUrl;
        private final int width;
        private final int height;
        private final String format;
        private final long fileSize;

        public ImagePreviewResult(String contentUrl, String thumbnailUrl, int width, int height, 
                                String format, long fileSize) {
            this.contentUrl = contentUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.width = width;
            this.height = height;
            this.format = format;
            this.fileSize = fileSize;
        }

        public String getContentUrl() { return contentUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getFormat() { return format; }
        public long getFileSize() { return fileSize; }
    }

    /**
     * 媒体预览结果
     */
    class MediaPreviewResult {
        private final String contentUrl;
        private final String previewImageUrl;
        private final String mediaType;
        private final long duration;
        private final Map<String, Object> metadata;

        public MediaPreviewResult(String contentUrl, String previewImageUrl, String mediaType, 
                                long duration, Map<String, Object> metadata) {
            this.contentUrl = contentUrl;
            this.previewImageUrl = previewImageUrl;
            this.mediaType = mediaType;
            this.duration = duration;
            this.metadata = metadata;
        }

        public String getContentUrl() { return contentUrl; }
        public String getPreviewImageUrl() { return previewImageUrl; }
        public String getMediaType() { return mediaType; }
        public long getDuration() { return duration; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    // ==================== 核心预览方法 ====================

    /**
     * 获取文件预览
     * 
     * @param fileId 文件ID
     * @param userId 用户ID（用于权限检查）
     * @return 预览结果
     */
    PreviewResult getFilePreview(Long fileId, Long userId);

    /**
     * 从文件实体获取预览
     * 
     * @param file 文件实体
     * @param userId 用户ID（用于权限检查）
     * @return 预览结果
     */
    PreviewResult getFilePreview(FileEntity file, Long userId);

    /**
     * 从输入流获取预览
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param mimeType MIME类型
     * @return 预览结果
     */
    PreviewResult getPreviewFromStream(InputStream inputStream, String filename, String mimeType);

    // ==================== 特定类型预览方法 ====================

    /**
     * 获取文本文件预览
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @param maxLines 最大行数限制（防止大文件影响性能）
     * @return 文本预览结果
     * @throws IOException 读取异常
     */
    Optional<TextPreviewResult> getTextPreview(InputStream inputStream, String filename, int maxLines) throws IOException;

    /**
     * 获取图片文件预览
     * 
     * @param file 文件实体
     * @return 图片预览结果
     * @throws IOException 读取异常
     */
    Optional<ImagePreviewResult> getImagePreview(FileEntity file) throws IOException;

    /**
     * 获取PDF文件预览
     * 
     * @param file 文件实体
     * @return PDF预览URL
     */
    Optional<String> getPdfPreviewUrl(FileEntity file);

    /**
     * 获取媒体文件预览
     * 
     * @param file 文件实体
     * @return 媒体预览结果
     * @throws IOException 读取异常
     */
    Optional<MediaPreviewResult> getMediaPreview(FileEntity file) throws IOException;

    // ==================== 辅助方法 ====================

    /**
     * 检查文件是否支持预览
     * 
     * @param filename 文件名
     * @param mimeType MIME类型
     * @return 是否支持预览
     */
    boolean isPreviewSupported(String filename, String mimeType);

    /**
     * 获取文件的预览类型
     * 
     * @param filename 文件名
     * @param mimeType MIME类型
     * @return 预览类型
     */
    PreviewType getPreviewType(String filename, String mimeType);

    /**
     * 获取支持的文本文件扩展名
     * 
     * @return 支持的扩展名集合
     */
    Set<String> getSupportedTextExtensions();

    /**
     * 获取支持的图片文件扩展名
     * 
     * @return 支持的扩展名集合
     */
    Set<String> getSupportedImageExtensions();

    /**
     * 获取支持的媒体文件扩展名
     * 
     * @return 支持的扩展名集合
     */
    Set<String> getSupportedMediaExtensions();

    /**
     * 生成文件的直接访问URL
     * 
     * @param file 文件实体
     * @return 访问URL
     */
    String generateFileUrl(FileEntity file);

    /**
     * 生成文件的缩略图URL（如果适用）
     * 
     * @param file 文件实体
     * @return 缩略图URL
     */
    Optional<String> generateThumbnailUrl(FileEntity file);
} 