package tslc.beihaiyun.lyra.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BindingResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import tslc.beihaiyun.lyra.dto.FileRequest;
import tslc.beihaiyun.lyra.dto.FileResponse;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.FolderService;

/**
 * FileController基础测试类
 * 直接测试控制器方法，不依赖Spring Context
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
class FileControllerBasicTest {

    @Mock
    private FileService fileService;

    @Mock
    private FolderService folderService;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private FileController fileController;

    private Space testSpace;
    private FileEntity testFile;
    private LyraUserPrincipal mockPrincipal;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // 创建mock用户主体
        mockPrincipal = LyraUserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .enabled(true)
                .accountNonLocked(true)
                .authorities(java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // 创建测试数据
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("测试空间");

        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setOriginalName("test.txt");
        testFile.setPath("/test.txt");
        testFile.setSpace(testSpace);
        testFile.setSizeBytes(1024L);
        testFile.setMimeType("text/plain");
        testFile.setFileHash("abc123");
        testFile.setStoragePath("/storage/abc123.txt");
        testFile.setVersion(1);
        testFile.setStatus(FileEntity.FileStatus.ACTIVE);
        testFile.setIsPublic(false);
        testFile.setDownloadCount(0);
        testFile.setLastModifiedAt(LocalDateTime.now());
    }

    @Test
    void should_uploadFile_successfully() throws Exception {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes());
        
        FileRequest.FileUploadRequest request = new FileRequest.FileUploadRequest();
        request.setSpaceId(1L);

        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        when(fileService.uploadFile(any(), any(), any(), any()))
                .thenReturn(new FileService.FileOperationResult(true, "上传成功", testFile));
        when(bindingResult.hasErrors()).thenReturn(false);

        // 执行测试
        ResponseEntity<FileResponse.FileUploadResponse> response = 
                fileController.uploadFile(file, request, mockPrincipal, bindingResult);

        // 验证结果
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("上传成功", response.getBody().getMessage());
        assertNotNull(response.getBody().getFileInfo());
        assertEquals("test.txt", response.getBody().getFileInfo().getFilename());
    }

    @Test
    void should_return_badRequest_when_spaceNotFound() throws Exception {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes());
        
        FileRequest.FileUploadRequest request = new FileRequest.FileUploadRequest();
        request.setSpaceId(999L);

        when(spaceRepository.findById(999L)).thenReturn(Optional.empty());
        when(bindingResult.hasErrors()).thenReturn(false);

        // 执行测试
        ResponseEntity<FileResponse.FileUploadResponse> response = 
                fileController.uploadFile(file, request, mockPrincipal, bindingResult);

        // 验证结果
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("指定的空间不存在", response.getBody().getMessage());
    }

    @Test
    void should_getFileInfo_successfully() throws Exception {
        when(fileService.getFileById(1L)).thenReturn(Optional.of(testFile));

        // 测试获取文件信息
        ResponseEntity<Map<String, Object>> response = fileController.getFileInfo(1L, mockPrincipal);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        
        // 修复类型转换问题 - data字段是FileInfoResponse对象
        FileResponse.FileInfoResponse data = (FileResponse.FileInfoResponse) response.getBody().get("data");
        assertThat(data.getId()).isEqualTo(1L);
        assertThat(data.getFilename()).isEqualTo("test.txt");
    }

    @Test
    @DisplayName("获取不存在的文件应该返回404")
    void should_returnNotFound_when_fileNotExists() {
        // 准备测试数据
        when(fileService.getFileById(999L)).thenReturn(Optional.empty());

        // 执行测试
        ResponseEntity<Map<String, Object>> response = fileController.getFileInfo(999L, mockPrincipal);
        
        // 验证结果
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
    }

    @Test
    @DisplayName("删除文件应该成功")
    void should_deleteFile_successfully() {
        // 准备测试数据
        when(fileService.deleteFile(1L, 1L)).thenReturn(true);

        // 执行测试
        ResponseEntity<Map<String, Object>> response = fileController.deleteFile(1L, mockPrincipal);
        
        // 验证结果
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void should_initChunkedUpload_successfully() throws Exception {
        // 准备测试数据
        FileRequest.ChunkedUploadInitRequest request = new FileRequest.ChunkedUploadInitRequest();
        request.setFilename("large_file.zip");
        request.setFileSize(10485760L); // 10MB
        request.setFileHash("abc123def456");
        request.setSpaceId(1L);
        request.setChunkSize(1048576); // 1MB

        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));

        // 执行测试
        ResponseEntity<FileResponse.ChunkedUploadResponse> response = 
                fileController.initChunkedUpload(request, mockPrincipal);

        // 验证结果
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getUploadId());
        assertEquals(10, response.getBody().getTotalChunks());
        assertEquals(1048576, response.getBody().getChunkSize());
        assertFalse(response.getBody().isUploadCompleted());
    }

    @Test
    @DisplayName("重命名文件应该成功")
    void should_renameFile_successfully() {
        // 准备测试数据
        FileEntity resultFile = createTestFile();
        resultFile.setName("new-name.txt");
        
        FileService.FileOperationResult operationResult = 
            new FileService.FileOperationResult(true, "重命名成功", resultFile);
        
        when(fileService.renameFile(eq(1L), eq("new-name.txt"), eq(1L))).thenReturn(operationResult);

        // 准备请求
        FileRequest.FileRenameRequest request = new FileRequest.FileRenameRequest();
        request.setNewFilename("new-name.txt");

        // 执行测试
        ResponseEntity<Map<String, Object>> response = fileController.renameFile(1L, request, mockPrincipal);
        
        // 验证结果
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("移动文件应该成功")
    void should_moveFile_successfully() {
        // 准备测试数据
        FileEntity resultFile = createTestFile();
        
        // 创建目标文件夹
        Folder targetFolder = new Folder();
        targetFolder.setId(1L);
        targetFolder.setName("目标文件夹");
        
        FileService.FileOperationResult operationResult = 
            new FileService.FileOperationResult(true, "移动成功", resultFile);
        
        // Mock必要的依赖项
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(targetFolder));
        when(fileService.moveFile(eq(1L), any(Space.class), any(Folder.class), eq(1L))).thenReturn(operationResult);

        // 准备请求
        FileRequest.FileMoveRequest request = new FileRequest.FileMoveRequest();
        request.setTargetSpaceId(1L);
        request.setTargetFolderId(1L);
        request.setKeepOriginal(false);

        // 执行测试
        ResponseEntity<Map<String, Object>> response = fileController.moveFile(1L, request, mockPrincipal);
        
        // 验证结果
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("复制文件应该成功")
    void should_copyFile_successfully() {
        // 准备测试数据
        FileEntity resultFile = createTestFile();
        
        // 创建目标文件夹
        Folder targetFolder = new Folder();
        targetFolder.setId(1L);
        targetFolder.setName("目标文件夹");
        
        FileService.FileOperationResult operationResult = 
            new FileService.FileOperationResult(true, "复制成功", resultFile);
        
        // Mock必要的依赖项
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(targetFolder));
        when(fileService.copyFile(eq(1L), any(Space.class), any(Folder.class), eq(1L))).thenReturn(operationResult);

        // 准备请求
        FileRequest.FileCopyRequest request = new FileRequest.FileCopyRequest();
        request.setTargetSpaceId(1L);
        request.setTargetFolderId(1L);

        // 执行测试
        ResponseEntity<Map<String, Object>> response = fileController.copyFile(1L, request, mockPrincipal);
        
        // 验证结果
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void should_getFileStatistics_successfully() throws Exception {
        // 准备测试数据
        FileService.FileStatistics stats = new FileService.FileStatistics(
                10, 10240, 8, 1, 1, LocalDateTime.now());

        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        when(fileService.getFileStatistics(testSpace)).thenReturn(stats);

        // 执行测试
        ResponseEntity<FileResponse.FileStatisticsResponse> response = 
                fileController.getFileStatistics(1L, mockPrincipal);

        // 验证结果
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalFiles());
        assertEquals(10240, response.getBody().getTotalSize());
        assertEquals(8, response.getBody().getActiveFiles());
        assertEquals(1, response.getBody().getDeletedFiles());
        assertNotNull(response.getBody().getFormattedSize());
    }

    private FileEntity createTestFile() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setName("test.txt");
        file.setOriginalName("test.txt");
        file.setPath("/test.txt");
        file.setSpace(testSpace);
        file.setSizeBytes(1024L);
        file.setMimeType("text/plain");
        file.setFileHash("abc123");
        file.setStoragePath("/storage/abc123.txt");
        file.setVersion(1);
        file.setStatus(FileEntity.FileStatus.ACTIVE);
        file.setIsPublic(false);
        file.setDownloadCount(0);
        file.setLastModifiedAt(LocalDateTime.now());
        return file;
    }
} 