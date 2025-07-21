package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import tslc.beihaiyun.lyra.dto.FileRequest;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController集成测试类
 * 测试完整的文件操作流程
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
    }

    // ==================== 文件上传集成测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
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

        // 下载文件并验证内容
        mockMvc.perform(get("/api/files/" + fileId + "/download")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(content().string(testContent));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_previewTextFile_successfully() throws Exception {
        // 上传文本文件
        String testContent = "# 测试文档\n\n这是一个测试用的Markdown文档。";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.md", "text/markdown", testContent.getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long fileId = objectMapper.readTree(uploadResponse)
                .path("fileInfo")
                .path("id")
                .asLong();

        // 预览文件
        mockMvc.perform(get("/api/files/" + fileId + "/preview")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown"))
                .andExpect(header().string("Cache-Control", "max-age=3600"))
                .andExpect(content().string(testContent));
    }

    // ==================== 文件操作集成测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_performFileOperations_successfully() throws Exception {
        // 1. 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "file", "original.txt", "text/plain", "Original content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long fileId = objectMapper.readTree(uploadResponse)
                .path("fileInfo")
                .path("id")
                .asLong();

        // 2. 重命名文件
        FileRequest.FileRenameRequest renameRequest = new FileRequest.FileRenameRequest();
        renameRequest.setNewFilename("renamed.txt");

        mockMvc.perform(post("/api/files/" + fileId + "/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. 获取文件信息验证重命名
        mockMvc.perform(get("/api/files/" + fileId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("renamed.txt"));

        // 4. 复制文件
        FileRequest.FileCopyRequest copyRequest = new FileRequest.FileCopyRequest();
        copyRequest.setTargetSpaceId(testSpace.getId());
        copyRequest.setNewFilename("copied.txt");

        mockMvc.perform(post("/api/files/" + fileId + "/copy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(copyRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. 删除原文件
        mockMvc.perform(delete("/api/files/" + fileId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== 文件搜索集成测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_searchFiles_successfully() throws Exception {
        // 上传多个测试文件
        String[] filenames = {"document1.txt", "image.jpg", "document2.md", "script.js"};
        String[] contents = {"Text document 1", "fake image data", "Markdown document", "JavaScript code"};
        String[] mimeTypes = {"text/plain", "image/jpeg", "text/markdown", "application/javascript"};

        for (int i = 0; i < filenames.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filenames[i], mimeTypes[i], contents[i].getBytes());

            mockMvc.perform(multipart("/api/files/upload")
                    .file(file)
                    .param("spaceId", testSpace.getId().toString())
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        // 搜索包含"document"的文件
        FileRequest.FileSearchRequest searchRequest = new FileRequest.FileSearchRequest();
        searchRequest.setKeyword("document");
        searchRequest.setSpaceId(testSpace.getId());
        searchRequest.setPage(0);
        searchRequest.setSize(10);

        mockMvc.perform(post("/api/files/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(2)) // document1.txt 和 document2.md
                .andExpect(jsonPath("$.totalElements").value(2));

        // 搜索特定MIME类型的文件
        searchRequest.setKeyword("");
        searchRequest.setMimeType("text/plain");

        mockMvc.perform(post("/api/files/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(1)) // 只有document1.txt
                .andExpect(jsonPath("$.files[0].mimeType").value("text/plain"));
    }

    // ==================== 文件列表和统计集成测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_getFileListAndStatistics_successfully() throws Exception {
        // 上传几个文件
        for (int i = 1; i <= 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "file" + i + ".txt", "text/plain", 
                    ("Content of file " + i).getBytes());

            mockMvc.perform(multipart("/api/files/upload")
                    .file(file)
                    .param("spaceId", testSpace.getId().toString())
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        // 获取文件列表
        mockMvc.perform(get("/api/files/space/" + testSpace.getId())
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt")
                .param("direction", "desc")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));

        // 获取文件统计信息
        mockMvc.perform(get("/api/files/space/" + testSpace.getId() + "/statistics")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(5))
                .andExpect(jsonPath("$.activeFiles").value(5))
                .andExpect(jsonPath("$.deletedFiles").value(0))
                .andExpect(jsonPath("$.formattedSize").exists());
    }

    // ==================== 边界条件和错误场景测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_handleFileUploadErrors_gracefully() throws Exception {
        // 测试上传到不存在的空间
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("spaceId", "99999")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("指定的空间不存在"));

        // 测试无效的文件操作
        mockMvc.perform(get("/api/files/99999/download")
                .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/files/99999")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_validateRequestParameters_properly() throws Exception {
        // 测试无效的搜索请求
        FileRequest.FileSearchRequest invalidSearchRequest = new FileRequest.FileSearchRequest();
        // 缺少必需的keyword和spaceId

        mockMvc.perform(post("/api/files/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSearchRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        // 测试无效的重命名请求
        FileRequest.FileRenameRequest invalidRenameRequest = new FileRequest.FileRenameRequest();
        invalidRenameRequest.setNewFilename(""); // 空文件名

        mockMvc.perform(post("/api/files/1/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRenameRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ==================== 性能和大文件测试 ====================

    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    void should_handleLargeFileUpload_successfully() throws Exception {
        // 创建较大的测试文件（1MB）
        byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large_file.bin", "application/octet-stream", largeContent);

        // 上传大文件
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(largeFile)
                .param("spaceId", testSpace.getId().toString())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileInfo.sizeBytes").value(largeContent.length))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long fileId = objectMapper.readTree(uploadResponse)
                .path("fileInfo")
                .path("id")
                .asLong();

        // 下载大文件并验证大小
        mockMvc.perform(get("/api/files/" + fileId + "/download")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Length", String.valueOf(largeContent.length)));
    }
} 