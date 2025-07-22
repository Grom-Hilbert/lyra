package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.service.impl.PreviewServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PreviewService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@ExtendWith(MockitoExtension.class)
class PreviewServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PreviewServiceImpl previewService;

    private FileEntity testFile;
    private Space testSpace;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("test-space");
        
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setMimeType("text/plain");
        testFile.setSizeBytes(1024L);
        testFile.setStatus(FileEntity.FileStatus.ACTIVE);
        testFile.setSpace(testSpace);
        testFile.setPath("/test.txt");
        testFile.setLastModifiedAt(LocalDateTime.now());
    }

    // ==================== 基础功能测试 ====================

    @Test
    void should_ReturnSupportedTextExtensions() {
        Set<String> extensions = previewService.getSupportedTextExtensions();
        
        assertNotNull(extensions);
        assertFalse(extensions.isEmpty());
        assertTrue(extensions.contains("txt"));
        assertTrue(extensions.contains("md"));
        assertTrue(extensions.contains("json"));
        assertTrue(extensions.contains("java"));
    }

    @Test
    void should_ReturnSupportedImageExtensions() {
        Set<String> extensions = previewService.getSupportedImageExtensions();
        
        assertNotNull(extensions);
        assertFalse(extensions.isEmpty());
        assertTrue(extensions.contains("jpg"));
        assertTrue(extensions.contains("png"));
        assertTrue(extensions.contains("gif"));
        assertTrue(extensions.contains("svg"));
    }

    @Test
    void should_ReturnSupportedMediaExtensions() {
        Set<String> extensions = previewService.getSupportedMediaExtensions();
        
        assertNotNull(extensions);
        assertFalse(extensions.isEmpty());
        assertTrue(extensions.contains("mp3"));
        assertTrue(extensions.contains("mp4"));
        assertTrue(extensions.contains("avi"));
        assertTrue(extensions.contains("wav"));
    }

    // ==================== 预览类型检测测试 ====================

    @Test
    void should_DetectTextPreviewType() {
        assertEquals(PreviewService.PreviewType.TEXT, 
                    previewService.getPreviewType("test.txt", "text/plain"));
        assertEquals(PreviewService.PreviewType.TEXT, 
                    previewService.getPreviewType("test.md", null));
        assertEquals(PreviewService.PreviewType.TEXT, 
                    previewService.getPreviewType("test.json", "application/json"));
    }

    @Test
    void should_DetectImagePreviewType() {
        assertEquals(PreviewService.PreviewType.IMAGE, 
                    previewService.getPreviewType("test.jpg", "image/jpeg"));
        assertEquals(PreviewService.PreviewType.IMAGE, 
                    previewService.getPreviewType("test.png", null));
        assertEquals(PreviewService.PreviewType.IMAGE, 
                    previewService.getPreviewType("unknown.file", "image/png"));
    }

    @Test
    void should_DetectPdfPreviewType() {
        assertEquals(PreviewService.PreviewType.PDF, 
                    previewService.getPreviewType("test.pdf", "application/pdf"));
        assertEquals(PreviewService.PreviewType.PDF, 
                    previewService.getPreviewType("document.pdf", null));
    }

    @Test
    void should_DetectMediaPreviewType() {
        assertEquals(PreviewService.PreviewType.MEDIA, 
                    previewService.getPreviewType("test.mp3", "audio/mpeg"));
        assertEquals(PreviewService.PreviewType.MEDIA, 
                    previewService.getPreviewType("video.mp4", null));
        assertEquals(PreviewService.PreviewType.MEDIA, 
                    previewService.getPreviewType("unknown.file", "video/mp4"));
    }

    @Test
    void should_DetectUnsupportedType() {
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, 
                    previewService.getPreviewType("test.exe", "application/x-executable"));
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, 
                    previewService.getPreviewType("test.unknown", null));
    }

    // ==================== 预览支持检测测试 ====================

    @Test
    void should_SupportTextFiles() {
        assertTrue(previewService.isPreviewSupported("test.txt", "text/plain"));
        assertTrue(previewService.isPreviewSupported("config.json", null));
        assertTrue(previewService.isPreviewSupported("README.md", null));
    }

    @Test
    void should_SupportImageFiles() {
        assertTrue(previewService.isPreviewSupported("photo.jpg", "image/jpeg"));
        assertTrue(previewService.isPreviewSupported("icon.png", null));
        assertTrue(previewService.isPreviewSupported("logo.svg", null));
    }

    @Test
    void should_NotSupportUnsupportedFiles() {
        assertFalse(previewService.isPreviewSupported("program.exe", null));
        assertFalse(previewService.isPreviewSupported("archive.zip", null));
        assertFalse(previewService.isPreviewSupported("unknown.xyz", null));
    }

    // ==================== URL生成测试 ====================

    @Test
    void should_GenerateFileUrl() {
        String url = previewService.generateFileUrl(testFile);
        
        assertNotNull(url);
        assertTrue(url.contains(testFile.getId().toString()));
        assertTrue(url.contains("/api/files/"));
        assertTrue(url.contains("/content"));
    }

    @Test
    void should_GenerateThumbnailUrlForImages() {
        testFile.setName("test.jpg");
        
        Optional<String> thumbnailUrl = previewService.generateThumbnailUrl(testFile);
        
        assertTrue(thumbnailUrl.isPresent());
        assertTrue(thumbnailUrl.get().contains(testFile.getId().toString()));
        assertTrue(thumbnailUrl.get().contains("/thumbnail"));
    }

    @Test
    void should_NotGenerateThumbnailUrlForNonImages() {
        testFile.setName("test.txt");
        
        Optional<String> thumbnailUrl = previewService.generateThumbnailUrl(testFile);
        
        assertFalse(thumbnailUrl.isPresent());
    }

    // ==================== 文本预览测试 ====================

    @Test
    void should_PreviewTextFile() throws IOException {
        String textContent = "Hello, World!\nThis is a test file.\n第三行中文内容";
        InputStream inputStream = new ByteArrayInputStream(textContent.getBytes());
        
        Optional<PreviewService.TextPreviewResult> result = 
            previewService.getTextPreview(inputStream, "test.txt", 1000);
        
        assertTrue(result.isPresent());
        assertEquals(textContent + "\n", result.get().getContent());
        assertEquals(3, result.get().getLineCount());
        assertTrue(result.get().getCharacterCount() > 0);
        assertNotNull(result.get().getEncoding());
    }

    @Test
    void should_LimitTextPreviewLines() throws IOException {
        StringBuilder longContent = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            longContent.append("Line ").append(i).append("\n");
        }
        
        InputStream inputStream = new ByteArrayInputStream(longContent.toString().getBytes());
        
        Optional<PreviewService.TextPreviewResult> result = 
            previewService.getTextPreview(inputStream, "long.txt", 10);
        
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getLineCount());
    }

    // ==================== 文件预览集成测试 ====================

    @Test
    void should_GetFilePreviewSuccessfully() throws IOException {
        // 准备数据
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.read")).thenReturn(true);
        
        String textContent = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(textContent.getBytes());
        when(fileService.getFileContent(testFile.getId())).thenReturn(Optional.of(inputStream));
        
        // 执行测试
        PreviewService.PreviewResult result = previewService.getFilePreview(testFile.getId(), testUserId);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(PreviewService.PreviewType.TEXT, result.getType());
        assertNotNull(result.getData());
        assertNotNull(result.getMetadata());
        assertTrue(result.getData().containsKey("content"));
    }

    @Test
    void should_FailFilePreviewWhenFileNotFound() {
        when(fileService.getFileById(anyLong())).thenReturn(Optional.empty());
        
        PreviewService.PreviewResult result = previewService.getFilePreview(999L, testUserId);
        
        assertFalse(result.isSuccess());
        assertEquals("文件不存在", result.getMessage());
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, result.getType());
    }

    @Test
    void should_FailFilePreviewWhenNoPermission() {
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.read")).thenReturn(false);
        
        PreviewService.PreviewResult result = previewService.getFilePreview(testFile.getId(), testUserId);
        
        assertFalse(result.isSuccess());
        assertEquals("没有预览权限", result.getMessage());
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, result.getType());
    }

    @Test
    void should_FailFilePreviewWhenFileDeleted() {
        testFile.setStatus(FileEntity.FileStatus.DELETED);
        
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.read")).thenReturn(true);
        
        PreviewService.PreviewResult result = previewService.getFilePreview(testFile.getId(), testUserId);
        
        assertFalse(result.isSuccess());
        assertEquals("文件已被删除", result.getMessage());
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, result.getType());
    }

    @Test
    void should_FailFilePreviewForUnsupportedType() {
        testFile.setName("test.exe");
        testFile.setMimeType("application/x-executable");
        
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.read")).thenReturn(true);
        
        PreviewService.PreviewResult result = previewService.getFilePreview(testFile.getId(), testUserId);
        
        assertFalse(result.isSuccess());
        assertEquals("不支持的文件类型", result.getMessage());
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, result.getType());
    }

    // ==================== 图片预览测试 ====================

    @Test
    void should_GetImagePreviewSuccessfully() throws IOException {
        testFile.setName("test.jpg");
        testFile.setMimeType("image/jpeg");
        
        Optional<PreviewService.ImagePreviewResult> result = 
            previewService.getImagePreview(testFile);
        
        assertTrue(result.isPresent());
        assertNotNull(result.get().getContentUrl());
        assertNotNull(result.get().getThumbnailUrl());
        assertEquals("JPG", result.get().getFormat());
        assertEquals(testFile.getSizeBytes(), result.get().getFileSize());
    }

    // ==================== PDF预览测试 ====================

    @Test
    void should_GetPdfPreviewUrl() {
        testFile.setName("document.pdf");
        testFile.setMimeType("application/pdf");
        
        Optional<String> result = previewService.getPdfPreviewUrl(testFile);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().contains(testFile.getId().toString()));
    }

    // ==================== 媒体预览测试 ====================

    @Test
    void should_GetMediaPreviewForAudio() throws IOException {
        testFile.setName("music.mp3");
        testFile.setMimeType("audio/mpeg");
        
        Optional<PreviewService.MediaPreviewResult> result = 
            previewService.getMediaPreview(testFile);
        
        assertTrue(result.isPresent());
        assertEquals("audio", result.get().getMediaType());
        assertNotNull(result.get().getContentUrl());
        assertNotNull(result.get().getMetadata());
    }

    @Test
    void should_GetMediaPreviewForVideo() throws IOException {
        testFile.setName("video.mp4");
        testFile.setMimeType("video/mp4");
        
        Optional<PreviewService.MediaPreviewResult> result = 
            previewService.getMediaPreview(testFile);
        
        assertTrue(result.isPresent());
        assertEquals("video", result.get().getMediaType());
        assertNotNull(result.get().getContentUrl());
        assertNotNull(result.get().getMetadata());
    }

    // ==================== 流预览测试 ====================

    @Test
    void should_PreviewFromStream() {
        String textContent = "Hello from stream!";
        InputStream inputStream = new ByteArrayInputStream(textContent.getBytes());
        
        PreviewService.PreviewResult result = 
            previewService.getPreviewFromStream(inputStream, "test.txt", "text/plain");
        
        assertTrue(result.isSuccess());
        assertEquals(PreviewService.PreviewType.TEXT, result.getType());
        assertTrue(result.getData().containsKey("content"));
    }

    @Test
    void should_FailPreviewFromStreamForUnsupportedType() {
        InputStream inputStream = new ByteArrayInputStream("binary data".getBytes());
        
        PreviewService.PreviewResult result = 
            previewService.getPreviewFromStream(inputStream, "test.exe", "application/x-executable");
        
        assertFalse(result.isSuccess());
        assertEquals(PreviewService.PreviewType.UNSUPPORTED, result.getType());
    }
} 