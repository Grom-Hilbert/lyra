package tslc.beihaiyun.lyra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tslc.beihaiyun.lyra.dto.FolderRequest;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FolderController集成测试
 * 基于成功的AuthController和FileController集成测试模式
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class FolderControllerIntegrationTest {

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
        testUser.setPassword("hashedpassword");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setName("Test Space");
        testSpace.setDescription("Test space for integration testing");
        testSpace.setOwner(testUser);
        testSpace.setQuotaLimit(1000000L);
        testSpace.setQuotaUsed(0L);
        testSpace = spaceRepository.save(testSpace);

        // 设置SecurityContext for all tests
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("应该成功创建文件夹")
    void should_createFolder_when_validRequest() throws Exception {
        // Arrange
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        request.setName("新建文件夹");
        request.setSpaceId(testSpace.getId());

        // Act & Assert
        String responseContent = mockMvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(result -> {
                    System.out.println("=== API Response Content ===");
                    System.out.println(result.getResponse().getContentAsString());
                    System.out.println("============================");
                })
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.folder.name").value("新建文件夹"))
                .andExpect(jsonPath("$.data.folder.spaceId").value(testSpace.getId()))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("应该成功获取文件夹详情")
    void should_getFolderDetails_when_folderExists() throws Exception {
        // Arrange
        // 手动设置SecurityContext
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Folder testFolder = new Folder();
        testFolder.setName("测试文件夹");
        testFolder.setPath("/测试文件夹");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(0L);
        testFolder.setFileCount(0);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        testFolder = folderRepository.save(testFolder);

        // Act & Assert
        mockMvc.perform(get("/api/folders/{id}", testFolder.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("测试文件夹"))
                .andExpect(jsonPath("$.data.path").value("/测试文件夹"));
    }

    @Test
    @DisplayName("应该成功更新文件夹信息")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_updateFolder_when_validRequest() throws Exception {
        // Arrange
        Folder testFolder = new Folder();
        testFolder.setName("原始文件夹");
        testFolder.setPath("/原始文件夹");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(0L);
        testFolder.setFileCount(0);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        testFolder = folderRepository.save(testFolder);

        FolderRequest.UpdateFolderRequest request = new FolderRequest.UpdateFolderRequest();
        request.setName("更新后的文件夹");

        // Act & Assert
        mockMvc.perform(put("/api/folders/{id}", testFolder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.folder.name").value("更新后的文件夹"));
    }

    @Test
    @DisplayName("应该成功删除文件夹")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_deleteFolder_when_folderExists() throws Exception {
        // Arrange
        Folder testFolder = new Folder();
        testFolder.setName("待删除文件夹");
        testFolder.setPath("/待删除文件夹");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(0L);
        testFolder.setFileCount(0);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        testFolder = folderRepository.save(testFolder);

        // Act & Assert
        mockMvc.perform(delete("/api/folders/{id}", testFolder.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("应该成功获取文件夹树形结构")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_getFolderTree_when_spaceExists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/folders/tree")
                        .param("spaceId", testSpace.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("应该成功搜索文件夹")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_searchFolders_when_validKeyword() throws Exception {
        // Arrange
        Folder testFolder = new Folder();
        testFolder.setName("搜索测试文件夹");
        testFolder.setPath("/搜索测试文件夹");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(0L);
        testFolder.setFileCount(0);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        folderRepository.save(testFolder);

        // Act & Assert
        mockMvc.perform(get("/api/folders/search")
                        .param("keyword", "搜索测试")
                        .param("spaceId", testSpace.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("应该成功获取文件夹统计信息")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_getFolderStatistics_when_folderExists() throws Exception {
        // Arrange
        Folder testFolder = new Folder();
        testFolder.setName("统计测试文件夹");
        testFolder.setPath("/统计测试文件夹");
        testFolder.setSpace(testSpace);
        testFolder.setLevel(1);
        testFolder.setIsRoot(false);
        testFolder.setSizeBytes(1024L);
        testFolder.setFileCount(5);
        testFolder.setCreatedBy(testUser.getUsername());
        testFolder.setUpdatedBy(testUser.getUsername());
        testFolder = folderRepository.save(testFolder);

        // Act & Assert
        mockMvc.perform(get("/api/folders/{id}/statistics", testFolder.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSize").value(1024))
                .andExpect(jsonPath("$.data.fileCount").value(5));
    }

    @Test
    @DisplayName("创建文件夹时应该验证必填字段")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_returnValidationError_when_requiredFieldMissing() throws Exception {
        // Arrange
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        // 不设置name字段，应该触发验证错误

        // Act & Assert
        mockMvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("访问不存在的文件夹应该返回404")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void should_returnNotFound_when_folderNotExists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/folders/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
} 