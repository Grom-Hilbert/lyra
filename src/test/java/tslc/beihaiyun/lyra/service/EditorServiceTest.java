package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.service.impl.EditorServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EditorService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@ExtendWith(MockitoExtension.class)
class EditorServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private VersionService versionService;

    @InjectMocks
    private EditorServiceImpl editorService;

    private FileEntity testFile;
    private Space testSpace;
    private Long testUserId;
    private String testContent;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testContent = "Hello, World!\nThis is a test file.";
        
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("Test Space");
        
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setMimeType("text/plain");
        testFile.setSizeBytes((long)testContent.length());
        testFile.setSpace(testSpace);
        testFile.setDeleted(false);
        testFile.setCreatedAt(LocalDateTime.now());
        testFile.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void should_StartEditSessionSuccessfully() throws IOException {
        // Arrange
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.write")).thenReturn(true);
        when(fileService.getFileContent(testFile.getId())).thenReturn(Optional.of(new ByteArrayInputStream(testContent.getBytes())));

        // Act
        EditorService.EditResult result = editorService.startEditSession(testFile.getId(), testUserId);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String sessionId = (String) data.get("sessionId");
        assertNotNull(sessionId);
        assertEquals(testContent, data.get("content"));
        assertEquals("text", data.get("language"));
        assertEquals("test.txt", data.get("filename"));
        
        verify(fileService).getFileById(testFile.getId());
        verify(permissionService).hasPermission(testUserId, "file.write");
        verify(fileService).getFileContent(testFile.getId());
    }

    @Test
    void should_FailToStartEditSession_WhenFileNotFound() {
        // Arrange
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.empty());

        // Act
        EditorService.EditResult result = editorService.startEditSession(testFile.getId(), testUserId);

        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("文件不存在", result.getMessage());
        
        verify(fileService).getFileById(testFile.getId());
        verify(permissionService, never()).hasPermission(anyLong(), anyString());
    }

    @Test
    void should_FailToStartEditSession_WhenNoPermission() throws IOException {
        // Arrange
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.write")).thenReturn(false);

        // Act
        EditorService.EditResult result = editorService.startEditSession(testFile.getId(), testUserId);

        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("没有编辑权限", result.getMessage());
        
        verify(fileService).getFileById(testFile.getId());
        verify(permissionService).hasPermission(testUserId, "file.write");
        verify(fileService, never()).getFileContent(anyLong());
    }

    @Test
    void should_FailToStartEditSession_WhenFileNotSupported() throws IOException {
        // Arrange
        testFile.setName("test.exe");
        testFile.setMimeType("application/octet-stream");
        
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.write")).thenReturn(true);

        // Act
        EditorService.EditResult result = editorService.startEditSession(testFile.getId(), testUserId);

        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("文件类型不支持编辑", result.getMessage());
        
        verify(fileService).getFileById(testFile.getId());
        verify(permissionService).hasPermission(testUserId, "file.write");
        verify(fileService, never()).getFileContent(anyLong());
    }

    @Test
    void should_UpdateContentSuccessfully() throws IOException {
        // Arrange
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.write")).thenReturn(true);
        when(fileService.getFileContent(testFile.getId())).thenReturn(Optional.of(new ByteArrayInputStream(testContent.getBytes())));
        
        EditorService.EditResult startResult = editorService.startEditSession(testFile.getId(), testUserId);
        @SuppressWarnings("unchecked")
        Map<String, Object> startData = (Map<String, Object>) startResult.getData();
        String sessionId = (String) startData.get("sessionId");
        String newContent = "Updated content\nWith new lines";

        // Act
        EditorService.EditResult result = editorService.updateContent(sessionId, newContent, true);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("内容已更新", result.getMessage());
    }

    @Test
    void should_FailToUpdateContent_WhenSessionNotFound() {
        // Act
        EditorService.EditResult result = editorService.updateContent("non-existent-session", "content", false);

        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("编辑会话不存在或已过期", result.getMessage());
    }

    @Test
    void should_DetectLanguageCorrectly() {
        // Test various file extensions
        assertEquals(Optional.of("java"), editorService.detectLanguage("Test.java", "public class Test {}"));
        assertEquals(Optional.of("javascript"), editorService.detectLanguage("script.js", "function test() {}"));
        assertEquals(Optional.of("python"), editorService.detectLanguage("script.py", "def test():"));
        assertEquals(Optional.of("markdown"), editorService.detectLanguage("README.md", "# Title"));
        assertEquals(Optional.of("json"), editorService.detectLanguage("config.json", "{\"key\": \"value\"}"));
        
        // Test content-based detection
        assertEquals(Optional.of("xml"), editorService.detectLanguage("file.txt", "<?xml version=\"1.0\"?>"));
        assertEquals(Optional.of("text"), editorService.detectLanguage("file.txt", "regular text"));
        
        // Test unsupported/unknown files
        assertEquals(Optional.of("text"), editorService.detectLanguage("unknown.xyz", "unknown content"));
    }

    @Test
    void should_CheckEditSupportCorrectly() {
        // Supported text files
        assertTrue(editorService.isEditSupported("test.txt", "text/plain"));
        assertTrue(editorService.isEditSupported("script.js", "application/javascript"));
        assertTrue(editorService.isEditSupported("config.json", "application/json"));
        assertTrue(editorService.isEditSupported("README.md", "text/markdown"));
        
        // Unsupported binary files
        assertFalse(editorService.isEditSupported("image.jpg", "image/jpeg"));
        assertFalse(editorService.isEditSupported("doc.pdf", "application/pdf"));
        assertFalse(editorService.isEditSupported("app.exe", "application/octet-stream"));
    }

    @Test
    void should_ManageFileLocksCorrectly() throws IOException {
        // Arrange
        when(fileService.getFileById(testFile.getId())).thenReturn(Optional.of(testFile));
        when(permissionService.hasPermission(testUserId, "file.write")).thenReturn(true);
        when(fileService.getFileContent(testFile.getId())).thenReturn(Optional.of(new ByteArrayInputStream(testContent.getBytes())));
        
        EditorService.EditResult startResult = editorService.startEditSession(testFile.getId(), testUserId);
        @SuppressWarnings("unchecked")
        Map<String, Object> startData = (Map<String, Object>) startResult.getData();
        String sessionId = (String) startData.get("sessionId");

        // Test locking
        EditorService.EditResult lockResult = editorService.lockFile(testFile.getId(), testUserId, sessionId);
        assertTrue(lockResult.isSuccess());
        
        // Test lock info
        Optional<EditorService.FileLockInfo> lockInfoOpt = editorService.getFileLockInfo(testFile.getId());
        assertTrue(lockInfoOpt.isPresent());
        EditorService.FileLockInfo lockInfo = lockInfoOpt.get();
        assertEquals(testFile.getId(), lockInfo.getFileId());
        assertEquals(testUserId, lockInfo.getUserId());
        assertEquals(sessionId, lockInfo.getSessionId());
        
        // Test unlocking
        EditorService.EditResult unlockResult = editorService.unlockFile(testFile.getId(), testUserId, sessionId);
        assertTrue(unlockResult.isSuccess());
        
        // Lock info should be empty after unlock
        Optional<EditorService.FileLockInfo> emptyLockInfo = editorService.getFileLockInfo(testFile.getId());
        assertTrue(emptyLockInfo.isEmpty());
    }

    @Test
    void should_GetSupportedLanguages() {
        // Act
        Set<String> supportedLanguages = editorService.getSupportedLanguages();

        // Assert
        assertNotNull(supportedLanguages);
        assertFalse(supportedLanguages.isEmpty());
        assertTrue(supportedLanguages.contains("java"));
        assertTrue(supportedLanguages.contains("javascript"));
        assertTrue(supportedLanguages.contains("python"));
        assertTrue(supportedLanguages.contains("markdown"));
        assertTrue(supportedLanguages.contains("json"));
        assertTrue(supportedLanguages.contains("text"));
    }
} 