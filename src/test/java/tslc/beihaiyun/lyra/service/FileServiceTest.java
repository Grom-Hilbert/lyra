package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件管理服务集成测试
 * 测试FileService的所有核心功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文件管理服务集成测试")
class FileServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileEntityRepository fileEntityRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Space testSpace;
    private Folder testFolder;
    private FileEntity testFile;

    @BeforeEach
    void setUp() {
        // 清理数据
        fileEntityRepository.deleteAll();
        folderRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setName("Test Space");
        testSpace.setDescription("Test space for file operations");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);
        testSpace.setCreatedBy(testUser.getId().toString());
        testSpace.setUpdatedBy(testUser.getId().toString());
        testSpace = spaceRepository.save(testSpace);

        // 创建测试文件夹
        testFolder = new Folder();
        testFolder.setName("Test Folder");
        testFolder.setPath("/testfolder");
        testFolder.setSpace(testSpace);
        testFolder.setCreatedBy(testUser.getId().toString());
        testFolder.setUpdatedBy(testUser.getId().toString());
        testFolder = folderRepository.save(testFolder);
    }

    // ==================== 基础CRUD操作测试 ====================

    @Test
    @DisplayName("上传文件应成功")
    void should_UploadFile_When_ValidFile() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "test.txt", "test.txt", "text/plain", "Hello World".getBytes());

        // When
        FileService.FileOperationResult result = fileService.uploadFile(
            file, testSpace, testFolder, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFileEntity());
        assertEquals("test.txt", result.getFileEntity().getName());
        assertEquals(testSpace.getId(), result.getFileEntity().getSpace().getId());
        assertEquals(testFolder.getId(), result.getFileEntity().getFolder().getId());
        assertEquals(11L, result.getFileEntity().getSizeBytes());
    }

    @Test
    @DisplayName("上传空文件应失败")
    void should_FailUploadFile_When_EmptyFile() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "empty.txt", "empty.txt", "text/plain", new byte[0]);

        // When
        FileService.FileOperationResult result = fileService.uploadFile(
            emptyFile, testSpace, testFolder, testUser.getId());

        // Then
        assertFalse(result.isSuccess());
        assertNull(result.getFileEntity());
        assertTrue(result.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("创建文件应成功")
    void should_CreateFile_When_ValidInputStream() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream("Test content".getBytes());
        String filename = "created.txt";
        String contentType = "text/plain";

        // When
        FileService.FileOperationResult result = fileService.createFile(
            inputStream, filename, contentType, testSpace, testFolder, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFileEntity());
        assertEquals(filename, result.getFileEntity().getName());
        assertEquals(12L, result.getFileEntity().getSizeBytes());
    }

    @Test
    @DisplayName("根据ID获取文件应成功")
    void should_GetFileById_When_FileExists() {
        // Given
        createTestFile();

        // When
        Optional<FileEntity> result = fileService.getFileById(testFile.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testFile.getId(), result.get().getId());
        assertEquals(testFile.getName(), result.get().getName());
    }

    @Test
    @DisplayName("根据不存在的ID获取文件应返回空")
    void should_ReturnEmpty_When_FileNotExists() {
        // When
        Optional<FileEntity> result = fileService.getFileById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取文件内容应成功")
    void should_GetFileContent_When_FileExists() throws IOException {
        // Given
        createTestFile();

        // When
        Optional<InputStream> result = fileService.getFileContent(testFile.getId());

        // Then
        assertTrue(result.isPresent());
        // 验证内容（这里简化，实际可以读取流内容验证）
    }

    @Test
    @DisplayName("更新文件内容应成功")
    void should_UpdateFileContent_When_ValidContent() throws IOException {
        // Given
        createTestFile();
        InputStream newContent = new ByteArrayInputStream("Updated content".getBytes());

        // When
        FileService.FileOperationResult result = fileService.updateFileContent(
            testFile.getId(), newContent, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFileEntity());
        assertEquals(15L, result.getFileEntity().getSizeBytes()); // "Updated content" 长度
        assertEquals(2, result.getFileEntity().getVersion()); // 版本应该增加
    }

    @Test
    @DisplayName("更新文件信息应成功")
    void should_UpdateFileInfo_When_ValidName() {
        // Given
        createTestFile();
        String newName = "updated_test.txt";

        // When
        FileService.FileOperationResult result = fileService.updateFileInfo(
            testFile.getId(), newName, null, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertEquals(newName, result.getFileEntity().getName());
    }

    @Test
    @DisplayName("删除文件应移动到回收站")
    void should_MoveToRecycleBin_When_DeleteFile() {
        // Given
        createTestFile();

        // When
        boolean result = fileService.deleteFile(testFile.getId(), testUser.getId());

        // Then
        assertTrue(result);
        
        // 验证文件状态变为DELETED
        Optional<FileEntity> deletedFile = fileService.getFileById(testFile.getId());
        assertTrue(deletedFile.isPresent());
        assertEquals(FileEntity.FileStatus.DELETED, deletedFile.get().getStatus());
    }

    @Test
    @DisplayName("永久删除文件应彻底删除")
    void should_PermanentlyDelete_When_PermanentDeleteFile() {
        // Given
        createTestFile();

        // When
        boolean result = fileService.permanentDeleteFile(testFile.getId(), testUser.getId());

        // Then
        assertTrue(result);
        
        // 验证文件从数据库中删除
        Optional<FileEntity> deletedFile = fileService.getFileById(testFile.getId());
        assertFalse(deletedFile.isPresent());
    }

    // ==================== 文件操作测试 ====================

    @Test
    @DisplayName("移动文件应成功")
    void should_MoveFile_When_ValidTarget() {
        // Given
        createTestFile();
        Folder targetFolder = createAnotherFolder();

        // When
        FileService.FileOperationResult result = fileService.moveFile(
            testFile.getId(), testSpace, targetFolder, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertEquals(targetFolder.getId(), result.getFileEntity().getFolder().getId());
        assertTrue(result.getFileEntity().getPath().contains(targetFolder.getName()));
    }

    @Test
    @DisplayName("复制文件应成功")
    void should_CopyFile_When_ValidTarget() {
        // Given
        createTestFile();
        Folder targetFolder = createAnotherFolder();

        // When
        FileService.FileOperationResult result = fileService.copyFile(
            testFile.getId(), testSpace, targetFolder, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotEquals(testFile.getId(), result.getFileEntity().getId()); // 新文件ID不同
        assertEquals(targetFolder.getId(), result.getFileEntity().getFolder().getId());
        assertEquals(testFile.getSizeBytes(), result.getFileEntity().getSizeBytes());
    }

    @Test
    @DisplayName("重命名文件应成功")
    void should_RenameFile_When_ValidName() {
        // Given
        createTestFile();
        String newName = "renamed_file.txt";

        // When
        FileService.FileOperationResult result = fileService.renameFile(
            testFile.getId(), newName, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertEquals(newName, result.getFileEntity().getName());
    }

    @Test
    @DisplayName("重命名为已存在文件名应失败")
    void should_FailRename_When_NameExists() {
        // Given
        createTestFile();
        createAnotherFile("existing.txt");

        // When
        FileService.FileOperationResult result = fileService.renameFile(
            testFile.getId(), "existing.txt", testUser.getId());

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("已存在"));
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量删除文件应成功")
    void should_BatchDeleteFiles_When_ValidFileIds() {
        // Given
        createTestFile();
        FileEntity file2 = createAnotherFile("file2.txt");
        List<Long> fileIds = Arrays.asList(testFile.getId(), file2.getId());

        // When
        FileService.BatchOperationResult result = fileService.batchDeleteFiles(
            fileIds, testUser.getId());

        // Then
        assertTrue(result.isAllSuccess());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("批量移动文件应成功")
    void should_BatchMoveFiles_When_ValidTarget() {
        // Given
        createTestFile();
        FileEntity file2 = createAnotherFile("file2.txt");
        Folder targetFolder = createAnotherFolder();
        List<Long> fileIds = Arrays.asList(testFile.getId(), file2.getId());

        // When
        FileService.BatchOperationResult result = fileService.batchMoveFiles(
            fileIds, testSpace, targetFolder, testUser.getId());

        // Then
        assertTrue(result.isAllSuccess());
        assertEquals(2, result.getSuccessCount());
    }

    // ==================== 查询和搜索测试 ====================

    @Test
    @DisplayName("获取空间文件应成功")
    void should_GetFilesBySpace_When_FilesExist() {
        // Given
        createTestFile();
        createAnotherFile("file2.txt");

        // When
        List<FileEntity> result = fileService.getFilesBySpace(testSpace, false);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(file -> 
            file.getStatus() == FileEntity.FileStatus.ACTIVE));
    }

    @Test
    @DisplayName("搜索文件应返回匹配结果")
    void should_SearchFiles_When_KeywordMatches() {
        // Given
        createTestFile(); // test.txt
        createAnotherFile("example.txt");
        createAnotherFile("document.pdf");

        // When
        List<FileEntity> result = fileService.searchFiles(
            testSpace, "test", null, false);

        // Then
        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getName());
    }

    @Test
    @DisplayName("分页查询文件应正确分页")
    void should_GetFilesPaged_When_MultipleFiles() {
        // Given
        createTestFile();
        createAnotherFile("file2.txt");
        createAnotherFile("file3.txt");
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<FileEntity> result = fileService.getFilesPaged(
            testSpace, null, false, pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertTrue(result.hasNext());
    }

    // ==================== 回收站管理测试 ====================

    @Test
    @DisplayName("获取回收站文件应成功")
    void should_GetRecycleBinFiles_When_DeletedFilesExist() {
        // Given
        createTestFile();
        fileService.deleteFile(testFile.getId(), testUser.getId());
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<FileEntity> result = fileService.getRecycleBinFiles(testSpace, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(FileEntity.FileStatus.DELETED, 
            result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("从回收站恢复文件应成功")
    void should_RestoreFile_When_FileInRecycleBin() {
        // Given
        createTestFile();
        fileService.deleteFile(testFile.getId(), testUser.getId());

        // When
        boolean result = fileService.restoreFileFromRecycleBin(
            testFile.getId(), testUser.getId());

        // Then
        assertTrue(result);
        
        // 验证文件状态恢复为ACTIVE
        Optional<FileEntity> restoredFile = fileService.getFileById(testFile.getId());
        assertTrue(restoredFile.isPresent());
        assertEquals(FileEntity.FileStatus.ACTIVE, restoredFile.get().getStatus());
    }

    @Test
    @DisplayName("清空回收站应删除所有已删除文件")
    void should_EmptyRecycleBin_When_DeletedFilesExist() {
        // Given
        createTestFile();
        FileEntity file2 = createAnotherFile("file2.txt");
        fileService.deleteFile(testFile.getId(), testUser.getId());
        fileService.deleteFile(file2.getId(), testUser.getId());

        // When
        int result = fileService.emptyRecycleBin(testSpace, testUser.getId());

        // Then
        assertEquals(2, result);
        
        // 验证文件已被永久删除
        assertFalse(fileService.getFileById(testFile.getId()).isPresent());
        assertFalse(fileService.getFileById(file2.getId()).isPresent());
    }

    // ==================== 统计和信息测试 ====================

    @Test
    @DisplayName("获取文件统计应正确")
    void should_GetFileStatistics_When_FilesExist() {
        // Given
        createTestFile();
        createAnotherFile("file2.txt");
        FileEntity file3 = createAnotherFile("file3.txt");
        fileService.deleteFile(file3.getId(), testUser.getId());

        // When
        FileService.FileStatistics result = fileService.getFileStatistics(testSpace);

        // Then
        assertEquals(3, result.getTotalFiles());
        assertEquals(2, result.getActiveFiles());
        assertEquals(1, result.getDeletedFiles());
        assertEquals(0, result.getArchivedFiles());
        assertTrue(result.getTotalSize() > 0);
    }

    @Test
    @DisplayName("检查文件名存在应正确")
    void should_CheckFilenameExists_When_FileWithSameNameExists() {
        // Given
        createTestFile(); // test.txt

        // When & Then
        assertTrue(fileService.isFilenameExists(
            testSpace, testFolder, "test.txt", null));
        assertFalse(fileService.isFilenameExists(
            testSpace, testFolder, "nonexistent.txt", null));
    }

    @Test
    @DisplayName("验证文件完整性应正确")
    void should_VerifyFileIntegrity_When_FileExists() {
        // Given
        createTestFile();

        // When
        boolean result = fileService.verifyFileIntegrity(testFile.getId());

        // Then
        assertTrue(result);
    }

    // ==================== 辅助方法 ====================

    private void createTestFile() {
        try {
            MockMultipartFile file = new MockMultipartFile(
                "test.txt", "test.txt", "text/plain", "Test content".getBytes());
            
            FileService.FileOperationResult result = fileService.uploadFile(
                file, testSpace, testFolder, testUser.getId());
            
            assertTrue(result.isSuccess());
            testFile = result.getFileEntity();
        } catch (Exception e) {
            fail("Failed to create test file: " + e.getMessage());
        }
    }

    private FileEntity createAnotherFile(String filename) {
        try {
            MockMultipartFile file = new MockMultipartFile(
                filename, filename, "text/plain", "Another test content".getBytes());
            
            FileService.FileOperationResult result = fileService.uploadFile(
                file, testSpace, testFolder, testUser.getId());
            
            assertTrue(result.isSuccess());
            return result.getFileEntity();
        } catch (Exception e) {
            fail("Failed to create another file: " + e.getMessage());
            return null;
        }
    }

    private Folder createAnotherFolder() {
        Folder folder = new Folder();
        folder.setName("anotherfolder");
        folder.setPath("/anotherfolder");
        folder.setSpace(testSpace);
        folder.setCreatedBy(testUser.getId().toString());
        folder.setUpdatedBy(testUser.getId().toString());
        return folderRepository.save(folder);
    }
} 