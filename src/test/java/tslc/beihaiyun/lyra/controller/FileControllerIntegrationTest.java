package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tslc.beihaiyun.lyra.dto.FileRequest;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController集成测试
 * 验证文件管理API的完整流程
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@AutoConfigureWebMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class FileControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private FolderRepository folderRepository;

    private MockMvc mockMvc;
    private User testUser;
    private Space testSpace;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$N.zmdr9k7uOCdyj3wWmRsOE9C7gfPx/3RCTfX7c4.FPU8Q9H9l1wy"); // "password"
        testUser.setDisplayName("Test User");
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setName("测试空间");
        testSpace.setDescription("用于文件操作测试的空间");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);
        testSpace.setQuotaLimit(1073741824L); // 1GB
        testSpace.setVersionControlEnabled(true);
        testSpace = spaceRepository.save(testSpace);

        // 手动设置SecurityContext，确保提供LyraUserPrincipal
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ==================== 文件上传集成测试 ====================

    @Test
    @DisplayName("应该成功上传和下载文件")
    void should_uploadAndDownloadFile_successfully() throws Exception {
        // 创建测试文件
        String testContent = "Hello, World! This is a test file content.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", testContent.getBytes());

        // 上传文件
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileInfo.filename").value("test.txt"))
                .andExpect(jsonPath("$.fileInfo.spaceId").value(testSpace.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从响应中获取文件ID
        Long fileId = objectMapper.readTree(uploadResponse)
                .path("fileInfo")
                .path("id")
                .asLong();

        // 下载文件
        mockMvc.perform(get("/api/files/{id}/download", fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().string(testContent));
    }

    @Test
    @DisplayName("应该成功执行文件操作")
    void should_performFileOperations_successfully() throws Exception {
        // 创建测试文件
        String testContent = "This is a test file for operations.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "operations-test.txt", "text/plain", testContent.getBytes());

        // 上传文件
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 获取文件ID
        Long fileId = objectMapper.readTree(uploadResponse)
                .path("fileInfo")
                .path("id")
                .asLong();

        // 获取文件详情
        mockMvc.perform(get("/api/files/{id}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.filename").value("operations-test.txt"));

        // 重命名文件
        FileRequest.FileRenameRequest renameRequest = new FileRequest.FileRenameRequest();
        renameRequest.setNewFilename("renamed-file.txt");

        mockMvc.perform(put("/api/files/{id}/name", fileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 删除文件
        mockMvc.perform(delete("/api/files/{id}", fileId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("应该成功搜索文件")
    void should_searchFiles_successfully() throws Exception {
        // 先上传一个文件用于搜索
        MockMultipartFile file = new MockMultipartFile(
                "file", "searchable-file.txt", "text/plain", "搜索测试内容".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk());

        // 执行搜索
        mockMvc.perform(get("/api/files/search")
                .param("spaceId", testSpace.getId().toString())
                .param("query", "searchable")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("应该成功处理文件夹上传")
    void should_uploadToFolder_successfully() throws Exception {
        // 创建文件夹
        Folder testFolder = new Folder();
        testFolder.setName("test-folder");
        testFolder.setPath("/test-folder");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(0L);
        testFolder.setFileCount(0);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        testFolder = folderRepository.save(testFolder);

        // 上传文件到文件夹
        MockMultipartFile file = new MockMultipartFile(
                "file", "folder-test.txt", "text/plain", "文件夹测试内容".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .param("folderId", testFolder.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileInfo.filename").value("folder-test.txt"));
    }

    @Test
    @DisplayName("应该成功处理多文件上传")
    void should_uploadMultipleFiles_successfully() throws Exception {
        // 创建多个测试文件
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "file1.txt", "text/plain", "第一个文件内容".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "file2.txt", "text/plain", "第二个文件内容".getBytes());

        mockMvc.perform(multipart("/api/files/batch-upload")
                .file(file1)
                .file(file2)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadResults").isArray())
                .andExpect(jsonPath("$.data.uploadResults.length()").value(2));
    }

    @Test
    @DisplayName("应该成功处理大文件上传")
    void should_handleLargeFileUpload_successfully() throws Exception {
        // 创建一个较大的测试文件（1MB）
        byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large-file.bin", "application/octet-stream", largeContent);

        mockMvc.perform(multipart("/api/files/upload")
                .file(largeFile)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileInfo.filename").value("large-file.bin"))
                .andExpect(jsonPath("$.fileInfo.sizeBytes").value(1024 * 1024));
    }

    @Test
    @DisplayName("应该正确处理文件类型验证")
    void should_validateFileType_correctly() throws Exception {
        // 测试允许的文件类型
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "valid.txt", "text/plain", "有效文件内容".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(validFile)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
} 