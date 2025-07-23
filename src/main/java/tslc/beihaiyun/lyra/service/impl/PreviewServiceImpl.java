package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.PermissionService;
import tslc.beihaiyun.lyra.service.PreviewService;
import tslc.beihaiyun.lyra.util.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 文件预览服务实现
 * 提供多种文件格式的预览功能，包括文本、图片、PDF、音视频等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@Service
@Transactional
public class PreviewServiceImpl implements PreviewService {

    private static final Logger logger = LoggerFactory.getLogger(PreviewServiceImpl.class);

    // 预览限制配置
    private static final int MAX_TEXT_LINES = 1000;
    private static final int MAX_TEXT_SIZE = 1024 * 1024; // 1MB
    private static final int BUFFER_SIZE = 8192;

    // 支持的文件类型
    private static final Set<String> SUPPORTED_TEXT_EXTENSIONS = Set.of(
        "txt", "md", "json", "xml", "csv", "log", "properties", "yml", "yaml", 
        "java", "js", "ts", "html", "css", "sql", "sh", "bat", "py", "php", "rb", "go"
    );

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico"
    );

    private static final Set<String> SUPPORTED_MEDIA_EXTENSIONS = Set.of(
        "mp3", "mp4", "avi", "mov", "wmv", "flv", "webm", "ogg", "wav", "m4a"
    );

    private static final Set<String> SUPPORTED_PDF_EXTENSIONS = Set.of("pdf");

    private final FileService fileService;
    private final PermissionService permissionService;

    @Autowired
    public PreviewServiceImpl(FileService fileService, PermissionService permissionService) {
        this.fileService = fileService;
        this.permissionService = permissionService;
    }

    // ==================== 核心预览方法 ====================

    @Override
    @Transactional(readOnly = true)
    public PreviewResult getFilePreview(Long fileId, Long userId) {
        try {
            Optional<FileEntity> fileOptional = fileService.getFileById(fileId);
            if (!fileOptional.isPresent()) {
                return createErrorResult(PreviewType.UNSUPPORTED, "文件不存在");
            }

            return getFilePreview(fileOptional.get(), userId);
        } catch (Exception e) {
            logger.error("获取文件预览失败: fileId={}", fileId, e);
            return createErrorResult(PreviewType.UNSUPPORTED, "获取文件预览失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PreviewResult getFilePreview(FileEntity file, Long userId) {
        try {
            // 检查权限
            if (!checkPreviewPermission(file, userId)) {
                return createErrorResult(PreviewType.UNSUPPORTED, "没有预览权限");
            }

            // 检查文件状态
            if (file.getStatus() == FileEntity.FileStatus.DELETED) {
                return createErrorResult(PreviewType.UNSUPPORTED, "文件已被删除");
            }

            // 获取预览类型
            PreviewType type = getPreviewType(file.getName(), file.getMimeType());
            if (type == PreviewType.UNSUPPORTED) {
                return createErrorResult(PreviewType.UNSUPPORTED, "不支持的文件类型");
            }

            // 根据类型生成预览
            return generatePreview(file, type);

        } catch (Exception e) {
            logger.error("获取文件预览失败: fileId={}, fileName={}", file.getId(), file.getName(), e);
            return createErrorResult(PreviewType.UNSUPPORTED, "获取文件预览失败: " + e.getMessage());
        }
    }

    @Override
    public PreviewResult getPreviewFromStream(InputStream inputStream, String filename, String mimeType) {
        try {
            PreviewType type = getPreviewType(filename, mimeType);
            if (type == PreviewType.UNSUPPORTED) {
                return createErrorResult(PreviewType.UNSUPPORTED, "不支持的文件类型");
            }

            if (type == PreviewType.TEXT) {
                Optional<TextPreviewResult> textResult = getTextPreview(inputStream, filename, MAX_TEXT_LINES);
                if (textResult.isPresent()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("content", textResult.get().getContent());
                    data.put("encoding", textResult.get().getEncoding());
                    data.put("lineCount", textResult.get().getLineCount());
                    data.put("characterCount", textResult.get().getCharacterCount());
                    
                    return new PreviewResult(true, PreviewType.TEXT, "文本预览成功", data, new HashMap<>());
                }
            }

            return createErrorResult(PreviewType.UNSUPPORTED, "暂不支持从流预览此类型文件");

        } catch (Exception e) {
            logger.error("从流获取预览失败: filename={}", filename, e);
            return createErrorResult(PreviewType.UNSUPPORTED, "预览失败: " + e.getMessage());
        }
    }

    // ==================== 特定类型预览方法 ====================

    @Override
    public Optional<TextPreviewResult> getTextPreview(InputStream inputStream, String filename, int maxLines) throws IOException {
        try {
            // 检测文件编码
            Charset encoding = detectEncoding(inputStream);
            
            // 重新创建输入流进行读取
            inputStream.reset();
            
            StringBuilder content = new StringBuilder();
            long lineCount = 0;
            long characterCount = 0;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
                String line;
                while ((line = reader.readLine()) != null && lineCount < maxLines) {
                    if (content.length() + line.length() > MAX_TEXT_SIZE) {
                        content.append("...\n[文件内容过大，已截断]");
                        break;
                    }
                    
                    content.append(line).append("\n");
                    lineCount++;
                    characterCount += line.length() + 1;
                }
            }

            return Optional.of(new TextPreviewResult(
                content.toString(), 
                encoding.displayName(), 
                lineCount, 
                characterCount
            ));

        } catch (IOException e) {
            logger.error("读取文本文件失败: {}", filename, e);
            throw e;
        }
    }

    @Override
    public Optional<ImagePreviewResult> getImagePreview(FileEntity file) throws IOException {
        try {
            String contentUrl = generateFileUrl(file);
            Optional<String> thumbnailUrl = generateThumbnailUrl(file);
            
            // 获取图片元数据（这里简化实现，实际可以使用图片库获取真实尺寸）
            String format = FileUtils.getFileExtension(file.getName()).toUpperCase();
            
            return Optional.of(new ImagePreviewResult(
                contentUrl,
                thumbnailUrl.orElse(contentUrl),
                0, // width - 需要图片处理库来获取
                0, // height - 需要图片处理库来获取
                format,
                file.getSizeBytes()
            ));

        } catch (Exception e) {
            logger.error("获取图片预览失败: fileId={}, fileName={}", file.getId(), file.getName(), e);
            throw new IOException("获取图片预览失败", e);
        }
    }

    @Override
    public Optional<String> getPdfPreviewUrl(FileEntity file) {
        try {
            // PDF可以直接通过URL在浏览器中预览
            String url = generateFileUrl(file);
            return Optional.of(url);
        } catch (Exception e) {
            logger.error("获取PDF预览URL失败: fileId={}, fileName={}", file.getId(), file.getName(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MediaPreviewResult> getMediaPreview(FileEntity file) throws IOException {
        try {
            String contentUrl = generateFileUrl(file);
            String extension = FileUtils.getFileExtension(file.getName()).toLowerCase();
            
            // 判断媒体类型
            String mediaType = "unknown";
            if (Set.of("mp3", "wav", "ogg", "m4a").contains(extension)) {
                mediaType = "audio";
            } else if (Set.of("mp4", "avi", "mov", "wmv", "flv", "webm").contains(extension)) {
                mediaType = "video";
            }

            // 创建媒体元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileSize", file.getSizeBytes());
            metadata.put("format", extension.toUpperCase());
            metadata.put("mimeType", file.getMimeType());

            return Optional.of(new MediaPreviewResult(
                contentUrl,
                null, // previewImageUrl - 需要视频处理库来生成预览图
                mediaType,
                0, // duration - 需要媒体处理库来获取
                metadata
            ));

        } catch (Exception e) {
            logger.error("获取媒体预览失败: fileId={}, fileName={}", file.getId(), file.getName(), e);
            throw new IOException("获取媒体预览失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    @Override
    public boolean isPreviewSupported(String filename, String mimeType) {
        return getPreviewType(filename, mimeType) != PreviewType.UNSUPPORTED;
    }

    @Override
    public PreviewType getPreviewType(String filename, String mimeType) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        
        if (SUPPORTED_TEXT_EXTENSIONS.contains(extension)) {
            return PreviewType.TEXT;
        } else if (SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            return PreviewType.IMAGE;
        } else if (SUPPORTED_PDF_EXTENSIONS.contains(extension)) {
            return PreviewType.PDF;
        } else if (SUPPORTED_MEDIA_EXTENSIONS.contains(extension)) {
            return PreviewType.MEDIA;
        }

        // 根据MIME类型进行二次判断
        if (mimeType != null) {
            if (mimeType.startsWith("text/")) {
                return PreviewType.TEXT;
            } else if (mimeType.startsWith("image/")) {
                return PreviewType.IMAGE;
            } else if (mimeType.equals("application/pdf")) {
                return PreviewType.PDF;
            } else if (mimeType.startsWith("audio/") || mimeType.startsWith("video/")) {
                return PreviewType.MEDIA;
            }
        }

        return PreviewType.UNSUPPORTED;
    }

    @Override
    public Set<String> getSupportedTextExtensions() {
        return Collections.unmodifiableSet(SUPPORTED_TEXT_EXTENSIONS);
    }

    @Override
    public Set<String> getSupportedImageExtensions() {
        return Collections.unmodifiableSet(SUPPORTED_IMAGE_EXTENSIONS);
    }

    @Override
    public Set<String> getSupportedMediaExtensions() {
        return Collections.unmodifiableSet(SUPPORTED_MEDIA_EXTENSIONS);
    }

    @Override
    public String generateFileUrl(FileEntity file) {
        // 生成文件访问URL - 这里需要根据实际的文件控制器路径来构建
        return String.format("/api/files/%d/content", file.getId());
    }

    @Override
    public Optional<String> generateThumbnailUrl(FileEntity file) {
        // 对于图片文件，可以生成缩略图URL
        String extension = FileUtils.getFileExtension(file.getName()).toLowerCase();
        if (SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            return Optional.of(String.format("/api/files/%d/thumbnail", file.getId()));
        }
        return Optional.empty();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成预览结果
     */
    private PreviewResult generatePreview(FileEntity file, PreviewType type) {
        try {
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> metadata = createFileMetadata(file);

            switch (type) {
                case TEXT:
                    return generateTextPreview(file, data, metadata);
                case IMAGE:
                    return generateImagePreview(file, data, metadata);
                case PDF:
                    return generatePdfPreview(file, data, metadata);
                case MEDIA:
                    return generateMediaPreview(file, data, metadata);
                default:
                    return createErrorResult(PreviewType.UNSUPPORTED, "不支持的预览类型");
            }
        } catch (Exception e) {
            logger.error("生成预览失败: fileId={}, type={}", file.getId(), type, e);
            return createErrorResult(type, "生成预览失败: " + e.getMessage());
        }
    }

    /**
     * 生成文本预览
     */
    private PreviewResult generateTextPreview(FileEntity file, Map<String, Object> data, Map<String, Object> metadata) throws IOException {
        Optional<InputStream> inputStreamOpt = fileService.getFileContent(file.getId());
        if (!inputStreamOpt.isPresent()) {
            return createErrorResult(PreviewType.TEXT, "无法读取文件内容");
        }

        try (InputStream inputStream = inputStreamOpt.get()) {
            Optional<TextPreviewResult> textResult = getTextPreview(inputStream, file.getName(), MAX_TEXT_LINES);
            if (textResult.isPresent()) {
                TextPreviewResult result = textResult.get();
                data.put("content", result.getContent());
                data.put("encoding", result.getEncoding());
                data.put("lineCount", result.getLineCount());
                data.put("characterCount", result.getCharacterCount());
                
                return new PreviewResult(true, PreviewType.TEXT, "文本预览成功", data, metadata);
            }
        }

        return createErrorResult(PreviewType.TEXT, "文本预览失败");
    }

    /**
     * 生成图片预览
     */
    private PreviewResult generateImagePreview(FileEntity file, Map<String, Object> data, Map<String, Object> metadata) throws IOException {
        Optional<ImagePreviewResult> imageResult = getImagePreview(file);
        if (imageResult.isPresent()) {
            ImagePreviewResult result = imageResult.get();
            data.put("contentUrl", result.getContentUrl());
            data.put("thumbnailUrl", result.getThumbnailUrl());
            data.put("width", result.getWidth());
            data.put("height", result.getHeight());
            data.put("format", result.getFormat());
            
            return new PreviewResult(true, PreviewType.IMAGE, "图片预览成功", data, metadata);
        }

        return createErrorResult(PreviewType.IMAGE, "图片预览失败");
    }

    /**
     * 生成PDF预览
     */
    private PreviewResult generatePdfPreview(FileEntity file, Map<String, Object> data, Map<String, Object> metadata) {
        Optional<String> pdfUrl = getPdfPreviewUrl(file);
        if (pdfUrl.isPresent()) {
            data.put("contentUrl", pdfUrl.get());
            data.put("downloadUrl", generateFileUrl(file));
            
            return new PreviewResult(true, PreviewType.PDF, "PDF预览成功", data, metadata);
        }

        return createErrorResult(PreviewType.PDF, "PDF预览失败");
    }

    /**
     * 生成媒体预览
     */
    private PreviewResult generateMediaPreview(FileEntity file, Map<String, Object> data, Map<String, Object> metadata) throws IOException {
        Optional<MediaPreviewResult> mediaResult = getMediaPreview(file);
        if (mediaResult.isPresent()) {
            MediaPreviewResult result = mediaResult.get();
            data.put("contentUrl", result.getContentUrl());
            data.put("mediaType", result.getMediaType());
            data.put("duration", result.getDuration());
            data.putAll(result.getMetadata());
            
            return new PreviewResult(true, PreviewType.MEDIA, "媒体预览成功", data, metadata);
        }

        return createErrorResult(PreviewType.MEDIA, "媒体预览失败");
    }

    /**
     * 创建文件元数据
     */
    private Map<String, Object> createFileMetadata(FileEntity file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileId", file.getId());
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.getSizeBytes());
        metadata.put("mimeType", file.getMimeType());
        metadata.put("lastModified", file.getLastModifiedAt());
        metadata.put("extension", FileUtils.getFileExtension(file.getName()));
        return metadata;
    }

    /**
     * 创建错误结果
     */
    private PreviewResult createErrorResult(PreviewType type, String message) {
        return new PreviewResult(false, type, message, new HashMap<>(), new HashMap<>());
    }

    /**
     * 检查预览权限
     */
    private boolean checkPreviewPermission(FileEntity file, Long userId) {
        try {
            // 使用权限服务检查用户是否有文件读取权限
            return permissionService.hasPermission(userId, "file.read");
        } catch (Exception e) {
            logger.error("检查预览权限失败: fileId={}, userId={}", file.getId(), userId, e);
            return false;
        }
    }

    /**
     * 检测文件编码
     */
    private Charset detectEncoding(InputStream inputStream) throws IOException {
        // 简单的编码检测，实际可以使用更复杂的库如chardet
        inputStream.mark(1024);
        
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        
        inputStream.reset();
        
        if (bytesRead > 0) {
            // 检查BOM
            if (bytesRead >= 3 && 
                (buffer[0] & 0xFF) == 0xEF && 
                (buffer[1] & 0xFF) == 0xBB && 
                (buffer[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8;
            }
            
            // 简单的UTF-8检测
            try {
                new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                return StandardCharsets.UTF_8;
            } catch (Exception e) {
                // 如果UTF-8解码失败，使用系统默认编码
            }
        }
        
        return Charset.defaultCharset();
    }
} 