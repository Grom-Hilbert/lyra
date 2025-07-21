package tslc.beihaiyun.lyra.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件工具类测试
 * 测试FileUtils的所有实用方法
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("文件工具类测试")
class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("计算SHA-256哈希值应正确")
    void should_CalculateCorrectSHA256_When_ValidInputStream() throws IOException {
        // Given
        String content = "测试哈希值计算";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());

        // When
        String hash = FileUtils.calculateSHA256(inputStream);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[a-f0-9]+"));
    }

    @Test
    @DisplayName("计算文件SHA-256哈希值应正确")
    void should_CalculateCorrectSHA256_When_ValidFile() throws IOException {
        // Given
        String content = "文件哈希值测试内容";
        Path testFile = tempDir.resolve("hash-test.txt");
        Files.write(testFile, content.getBytes());

        // When
        String hash = FileUtils.calculateSHA256(testFile);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[a-f0-9]+"));
    }

    @Test
    @DisplayName("获取MIME类型应正确")
    void should_GetCorrectMimeType_When_ValidFilename() {
        // When & Then
        assertEquals("text/plain", FileUtils.getMimeType("test.txt"));
        assertEquals("image/jpeg", FileUtils.getMimeType("photo.jpg"));
        assertEquals("image/jpeg", FileUtils.getMimeType("photo.jpeg"));
        assertEquals("image/png", FileUtils.getMimeType("image.png"));
        assertEquals("application/pdf", FileUtils.getMimeType("document.pdf"));
        assertEquals("application/json", FileUtils.getMimeType("data.json"));
        assertEquals("application/octet-stream", FileUtils.getMimeType("unknown.xyz"));
        assertEquals("application/octet-stream", FileUtils.getMimeType(""));
        assertEquals("application/octet-stream", FileUtils.getMimeType(null));
    }

    @Test
    @DisplayName("获取文件扩展名应正确")
    void should_GetCorrectExtension_When_ValidFilename() {
        // When & Then
        assertEquals("txt", FileUtils.getFileExtension("test.txt"));
        assertEquals("pdf", FileUtils.getFileExtension("document.pdf"));
        assertEquals("", FileUtils.getFileExtension("noextension"));
        assertEquals("", FileUtils.getFileExtension(".hiddenfile"));
        assertEquals("", FileUtils.getFileExtension(""));
        assertEquals("", FileUtils.getFileExtension(null));
        assertEquals("gz", FileUtils.getFileExtension("archive.tar.gz"));
    }

    @Test
    @DisplayName("获取不带扩展名的文件名应正确")
    void should_GetNameWithoutExtension_When_ValidFilename() {
        // When & Then
        assertEquals("test", FileUtils.getNameWithoutExtension("test.txt"));
        assertEquals("document", FileUtils.getNameWithoutExtension("document.pdf"));
        assertEquals("noextension", FileUtils.getNameWithoutExtension("noextension"));
        assertEquals("", FileUtils.getNameWithoutExtension(".hiddenfile"));
        assertEquals("", FileUtils.getNameWithoutExtension(""));
        assertEquals("", FileUtils.getNameWithoutExtension(null));
        assertEquals("archive.tar", FileUtils.getNameWithoutExtension("archive.tar.gz"));
    }

    @Test
    @DisplayName("清理文件名应移除非法字符")
    void should_SanitizeFilename_When_ContainsInvalidChars() {
        // When & Then
        assertEquals("test_file.txt", FileUtils.sanitizeFilename("test:file.txt"));
        assertEquals("test_file.txt", FileUtils.sanitizeFilename("test*file.txt"));
        assertEquals("test_file.txt", FileUtils.sanitizeFilename("test?file.txt"));
        assertEquals("test_file.txt", FileUtils.sanitizeFilename("test<file>.txt"));
        assertEquals("test_file.txt", FileUtils.sanitizeFilename("test|file.txt"));
        assertEquals("test_file", FileUtils.sanitizeFilename("test/file\\"));
        assertEquals("unnamed", FileUtils.sanitizeFilename(""));
        assertEquals("unnamed", FileUtils.sanitizeFilename(null));
        assertEquals("unnamed", FileUtils.sanitizeFilename("   ...   "));
    }

    @Test
    @DisplayName("解析文件大小应正确")
    void should_ParseFileSize_When_ValidSizeString() {
        // When & Then
        assertEquals(100, FileUtils.parseFileSize("100"));
        assertEquals(100, FileUtils.parseFileSize("100B"));
        assertEquals(1024, FileUtils.parseFileSize("1KB"));
        assertEquals(1024 * 1024, FileUtils.parseFileSize("1MB"));
        assertEquals(1024L * 1024 * 1024, FileUtils.parseFileSize("1GB"));
        assertEquals(1024L * 1024 * 1024 * 1024, FileUtils.parseFileSize("1TB"));
        assertEquals(512 * 1024, FileUtils.parseFileSize("512KB"));
        assertEquals((long)(1.5 * 1024 * 1024), FileUtils.parseFileSize("1.5MB"));
    }

    @Test
    @DisplayName("解析无效文件大小应抛出异常")
    void should_ThrowException_When_InvalidSizeString() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> FileUtils.parseFileSize(""));
        assertThrows(IllegalArgumentException.class, () -> FileUtils.parseFileSize(null));
        assertThrows(IllegalArgumentException.class, () -> FileUtils.parseFileSize("invalid"));
        assertThrows(IllegalArgumentException.class, () -> FileUtils.parseFileSize("100XB"));
        assertThrows(IllegalArgumentException.class, () -> FileUtils.parseFileSize("abc"));
    }

    @Test
    @DisplayName("格式化文件大小应正确")
    void should_FormatFileSize_When_ValidSize() {
        // When & Then
        assertEquals("0 B", FileUtils.formatFileSize(0));
        assertEquals("100 B", FileUtils.formatFileSize(100));
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024));
        assertEquals("1.0 MB", FileUtils.formatFileSize(1024 * 1024));
        assertEquals("1.0 GB", FileUtils.formatFileSize(1024L * 1024 * 1024));
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536));
        assertEquals("0 B", FileUtils.formatFileSize(-100));
    }

    @Test
    @DisplayName("生成存储路径应包含正确格式")
    void should_GenerateStoragePath_When_ValidInput() {
        // Given
        String hash = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456";
        String filename = "test.txt";

        // When
        String storagePath = FileUtils.generateStoragePath(hash, filename);

        // Then
        assertNotNull(storagePath);
        assertTrue(storagePath.contains("a1"));
        assertTrue(storagePath.contains("b2"));
        assertTrue(storagePath.endsWith(".txt"));
        assertTrue(storagePath.matches("\\d{4}/\\d{2}/\\d{2}/[a-f0-9]{2}/[a-f0-9]{2}/[a-f0-9]+\\.txt"));
    }

    @Test
    @DisplayName("生成存储路径在哈希为空时应使用降级方案")
    void should_GenerateStoragePath_When_NullHash() {
        // Given
        String filename = "test.txt";

        // When
        String storagePath = FileUtils.generateStoragePath(null, filename);

        // Then
        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".txt"));
        assertTrue(storagePath.matches("\\d{4}/\\d{2}/\\d{2}/\\d{6}_[a-f0-9]+\\.txt"));
    }

    @Test
    @DisplayName("生成唯一文件名应正确工作")
    void should_GenerateUniqueFilename_When_ConflictExists() {
        // Given
        String originalFilename = "test.txt";
        Set<String> existingNames = Set.of("test.txt", "test(1).txt", "test(2).txt");

        // When
        String uniqueFilename = FileUtils.generateUniqueFilename(originalFilename, existingNames);

        // Then
        assertEquals("test(3).txt", uniqueFilename);
    }

    @Test
    @DisplayName("生成唯一文件名在无冲突时应返回原名")
    void should_ReturnOriginalFilename_When_NoConflict() {
        // Given
        String originalFilename = "unique.txt";
        Set<String> existingNames = Set.of("other.txt", "another.txt");

        // When
        String uniqueFilename = FileUtils.generateUniqueFilename(originalFilename, existingNames);

        // Then
        assertEquals("unique.txt", uniqueFilename);
    }

    @Test
    @DisplayName("检查文件类型是否允许应正确")
    void should_CheckFileTypeAllowed_When_ValidConfiguration() {
        // When & Then
        assertTrue(FileUtils.isFileTypeAllowed("test.txt", "*"));
        assertTrue(FileUtils.isFileTypeAllowed("test.txt", "txt,pdf,doc"));
        assertTrue(FileUtils.isFileTypeAllowed("test.pdf", "txt,pdf,doc"));
        assertFalse(FileUtils.isFileTypeAllowed("test.exe", "txt,pdf,doc"));
        assertTrue(FileUtils.isFileTypeAllowed("test.TXT", "txt,pdf,doc")); // 大小写不敏感
        assertTrue(FileUtils.isFileTypeAllowed("test.txt", ".txt,.pdf,.doc")); // 支持带点号的格式
        assertTrue(FileUtils.isFileTypeAllowed("test.txt", null)); // null表示允许所有
        assertTrue(FileUtils.isFileTypeAllowed("test.txt", "")); // 空字符串表示允许所有
    }

    @Test
    @DisplayName("创建目录应成功")
    void should_CreateDirectory_When_NotExists() throws IOException {
        // Given
        Path newDir = tempDir.resolve("new/nested/directory");
        assertFalse(Files.exists(newDir));

        // When
        FileUtils.ensureDirectoryExists(newDir);

        // Then
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));
    }

    @Test
    @DisplayName("创建已存在的目录应不报错")
    void should_NotThrow_When_DirectoryAlreadyExists() throws IOException {
        // Given
        Path existingDir = tempDir.resolve("existing");
        Files.createDirectories(existingDir);
        assertTrue(Files.exists(existingDir));

        // When & Then
        assertDoesNotThrow(() -> FileUtils.ensureDirectoryExists(existingDir));
    }

    @Test
    @DisplayName("安全删除文件应成功")
    void should_SafeDeleteFile_When_FileExists() throws IOException {
        // Given
        Path testFile = tempDir.resolve("delete-test.txt");
        Files.write(testFile, "要删除的内容".getBytes());
        assertTrue(Files.exists(testFile));

        // When
        boolean deleted = FileUtils.safeDelete(testFile);

        // Then
        assertTrue(deleted);
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("安全删除不存在的文件应返回false")
    void should_ReturnFalse_When_DeleteNonExistentFile() {
        // Given
        Path nonExistentFile = tempDir.resolve("non-existent.txt");
        assertFalse(Files.exists(nonExistentFile));

        // When
        boolean deleted = FileUtils.safeDelete(nonExistentFile);

        // Then
        assertFalse(deleted);
    }

    @Test
    @DisplayName("检查文件类型应正确")
    void should_CheckFileType_Correctly() {
        // When & Then
        assertTrue(FileUtils.isTextFile("text/plain"));
        assertTrue(FileUtils.isTextFile("application/json"));
        assertTrue(FileUtils.isTextFile("application/xml"));
        assertTrue(FileUtils.isTextFile("application/javascript"));
        assertFalse(FileUtils.isTextFile("image/jpeg"));
        assertFalse(FileUtils.isTextFile(null));

        assertTrue(FileUtils.isImageFile("image/jpeg"));
        assertTrue(FileUtils.isImageFile("image/png"));
        assertFalse(FileUtils.isImageFile("text/plain"));
        assertFalse(FileUtils.isImageFile(null));

        assertTrue(FileUtils.isVideoFile("video/mp4"));
        assertTrue(FileUtils.isVideoFile("video/avi"));
        assertFalse(FileUtils.isVideoFile("audio/mp3"));
        assertFalse(FileUtils.isVideoFile(null));

        assertTrue(FileUtils.isAudioFile("audio/mp3"));
        assertTrue(FileUtils.isAudioFile("audio/wav"));
        assertFalse(FileUtils.isAudioFile("video/mp4"));
        assertFalse(FileUtils.isAudioFile(null));
    }
} 