package tslc.beihaiyun.lyra.webdav;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.FolderService;
import tslc.beihaiyun.lyra.service.UserService;
import tslc.beihaiyun.lyra.service.VersionService;

/**
 * WebDAV 版本控制集成测试
 * 
 * 测试WebDAV协议与版本控制系统的完整集成，包括：
 * - 文件上传时的自动版本创建
 * - PROPFIND响应中的版本属性
 * - 版本历史访问
 * - 特定版本内容获取
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WebDavVersionControlIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LyraWebDavResourceService webDavResourceService;

    @Autowired
    private WebDavVersionControlService versionControlService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    private User testUser;
    private Space testSpace;
    private String testFilePath;

    @BeforeEach
    void setUp() {
        // 初始化MockMvc，包含Spring Security配置
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("webdav-test-user");
        testUser.setEmail("webdav-test@example.com");
        testUser.setPassword("encrypted-password");
        testUser = userRepository.save(testUser);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setName("test-space");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);
        testSpace = spaceRepository.save(testSpace);

        testFilePath = "/webdav/personal/test-space/test-document.txt";
        
        // 设置Spring Security认证上下文，使用LyraUserPrincipal
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testWebDavFileUploadCreatesVersion() throws Exception {
        String fileName = "test-document.txt";
        String initialContent = "初始文档内容";
        byte[] contentBytes = initialContent.getBytes(StandardCharsets.UTF_8);
        String testFilePath = "/webdav/personal/" + testSpace.getName() + "/" + fileName;
        
        // 使用 WebDAV 资源服务上传文件 (模拟 WebDAV PUT 请求)
        boolean uploadResult = webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName, 
            new ByteArrayInputStream(contentBytes), 
            contentBytes.length
        );
        
        assertTrue(uploadResult, "首次文件上传应该成功");
        
        // 验证文件是否创建并有初始版本
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertNotNull(resource, "上传的文件应该可以通过WebDAV访问");
        assertTrue(resource.isResource(), "资源应该是文件类型");

        // 验证版本是否创建
        List<FileVersion> versions = versionService.getAllVersions(resource.getFileEntity(), true);
        assertFalse(versions.isEmpty(), "文件应该有版本历史");
        assertEquals(1, versions.size(), "初次上传应该创建一个版本");
        assertEquals(1, versions.get(0).getVersionNumber(), "初始版本号应该是1");
        assertEquals((long)contentBytes.length, (long)versions.get(0).getSizeBytes(), "版本大小应该匹配");
    }

    @Test
    void testWebDavFileUpdateCreatesNewVersion() throws Exception {
        String fileName = "test-document.txt";
        String initialContent = "初始文档内容";
        String updatedContent = "更新后的文档内容，包含更多信息";
        byte[] initialBytes = initialContent.getBytes(StandardCharsets.UTF_8);
        byte[] updatedBytes = updatedContent.getBytes(StandardCharsets.UTF_8);
        String testFilePath = "/webdav/personal/" + testSpace.getName() + "/" + fileName;
        
        // 首次上传
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(initialBytes), 
            initialBytes.length
        );
        
        // 获取资源
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertNotNull(resource);
        
        // 二次上传（更新文件）
        boolean updateResult = webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(updatedBytes),
            updatedBytes.length
        );
        
        assertTrue(updateResult, "文件更新应该成功");
        
        // 验证版本历史
        List<FileVersion> versions = versionService.getAllVersions(resource.getFileEntity(), true);
        assertEquals(2, versions.size(), "更新后应该有2个版本");
        
        // 验证版本号递增
        versions.sort((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()));
        assertEquals(1, versions.get(0).getVersionNumber(), "第一个版本号应该是1");
        assertEquals(2, versions.get(1).getVersionNumber(), "第二个版本号应该是2");
        
        // 验证内容大小变化
        assertEquals((long)initialBytes.length, (long)versions.get(0).getSizeBytes(), "第一版本大小应该匹配初始内容");
        assertEquals((long)updatedBytes.length, (long)versions.get(1).getSizeBytes(), "第二版本大小应该匹配更新内容");
    }
    
    @Test
    void testPropfindIncludesVersionProperties() throws Exception {
        String fileName = "test-document.txt";
        String content = "测试文档内容";
        String testFilePath = "/webdav/personal/" + testSpace.getName() + "/" + fileName;
        
        // 准备认证信息
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // 设置认证上下文并创建测试文件
        SecurityContextHolder.getContext().setAuthentication(auth);
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
            content.length()
        );

        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf("PROPFIND"), testFilePath)
                .header("Depth", "0")
                .contentType(MediaType.APPLICATION_XML)
                .with(authentication(auth)))
                .andExpect(status().is(207)) // WebDAV PROPFIND 返回 207 Multi-Status
                .andReturn();
        
        // 验证响应包含版本信息
        String responseXml = result.getResponse().getContentAsString();
        assertTrue(responseXml.contains("version-number"), "PROPFIND 响应应该包含版本号属性");
        assertTrue(responseXml.contains("version-count"), "PROPFIND 响应应该包含版本数量属性");
    }

    @Test
    void testVersionHistoryAccess() throws Exception {
        String fileName = "test-document.txt";
        String content1 = "第一版内容";
        String content2 = "第二版内容";
        
        // 创建并更新文件
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(content1.getBytes(StandardCharsets.UTF_8)), 
            content1.length()
        );
        
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(content2.getBytes(StandardCharsets.UTF_8)),
            content2.length()
        );
        
        // 获取文件
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertNotNull(resource);
        
        // 验证版本历史访问
        List<FileVersion> versions = versionService.getAllVersions(resource.getFileEntity(), true);
        assertEquals(2, versions.size(), "应该有2个版本");
        
        // 验证可以访问特定版本的内容
        for (FileVersion version : versions) {
            Optional<InputStream> versionContent = versionService.getVersionContent(version.getId());
            assertTrue(versionContent.isPresent(), "版本内容应该可以访问");
        }
    }

    @Test 
    void testSpecificVersionContentAccess() throws Exception {
        String fileName = "version-test.txt";
        String content1 = "原始内容";
        String content2 = "修改后内容";
        byte[] content1Bytes = content1.getBytes(StandardCharsets.UTF_8);
        byte[] content2Bytes = content2.getBytes(StandardCharsets.UTF_8);
        
        // 创建文件
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(content1Bytes), 
            content1Bytes.length
        );
        
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        List<FileVersion> versions = versionService.getAllVersions(resource.getFileEntity(), true);
        Long firstVersionId = versions.get(0).getId();
        
        // 更新文件
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(content2Bytes),
            content2Bytes.length
        );
        
        // 验证可以访问第一个版本的内容
        Optional<InputStream> version1Content = versionService.getVersionContent(firstVersionId);
        assertTrue(version1Content.isPresent(), "第一版本内容应该可以访问");
        String actualContent1 = new String(version1Content.get().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(content1, actualContent1, "第一版本内容应该保持不变");
        
        // 验证当前文件内容是最新的
        Optional<InputStream> currentContentStream = fileService.getFileContent(resource.getFileEntity().getId());
        assertTrue(currentContentStream.isPresent(), "当前文件内容应该可以访问");
        String currentContent = new String(currentContentStream.get().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(content2, currentContent, "当前文件内容应该是最新版本");
    }

    @Test
    void testVersionControlResourceWithVersionInfo() {
        String fileName = "resource-test.txt";
        String content = "资源测试内容";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        
        // 创建文件
        webDavResourceService.uploadFile(
            "personal/" + testSpace.getName() + "/" + fileName,
            new ByteArrayInputStream(contentBytes), 
            contentBytes.length
        );
        
        // 获取资源
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertNotNull(resource, "资源应该存在");
        
        // 验证版本控制相关属性
        assertEquals((Integer)1, resource.getCurrentVersionNumber(), "新文件版本号应该是1");
        assertEquals((Long)1L, resource.getTotalVersionCount(), "新文件版本数应该是1");
        assertNotNull(resource.getLastModified(), "最后修改时间应该存在");
        
        // 验证资源实现了版本控制接口
        assertTrue(resource instanceof tslc.beihaiyun.lyra.webdav.LyraResource, 
                  "资源应该是LyraResource类型，支持版本控制");
    }

    @Test
    void testVersionControlIntegrationEnd2End() throws Exception {
        String fileName = "integration-test.txt";
        String testFilePath = "/webdav/personal/" + testSpace.getName() + "/" + fileName;

        // 准备认证信息
        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // 1. 通过WebDAV上传文件
        String content1 = "集成测试第一版";
        mockMvc.perform(put(testFilePath)
                .content(content1)
                .contentType(MediaType.TEXT_PLAIN)
                .with(authentication(auth)))
                .andExpect(status().isOk());
        
        // 2. 验证文件创建和版本记录
        // 设置认证上下文
        SecurityContextHolder.getContext().setAuthentication(auth);
        LyraResource resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertNotNull(resource, "文件应该创建成功");
        assertEquals((Integer)1, resource.getCurrentVersionNumber(), "初始版本号应该是1");
        
        // 3. 通过WebDAV更新文件
        String content2 = "集成测试第二版，内容更丰富";
        mockMvc.perform(put(testFilePath)
                .content(content2)
                .contentType(MediaType.TEXT_PLAIN)
                .with(authentication(auth)))
                .andExpect(status().isOk());
        
        // 4. 验证版本递增
        // 设置认证上下文
        SecurityContextHolder.getContext().setAuthentication(auth);
        resource = webDavResourceService.getResource("personal/" + testSpace.getName() + "/" + fileName);
        assertEquals((Integer)2, resource.getCurrentVersionNumber(), "更新后版本号应该是2");
        assertEquals((Long)2L, resource.getTotalVersionCount(), "应该有2个版本");
        
        // 5. 通过PROPFIND获取版本信息
        mockMvc.perform(request(HttpMethod.valueOf("PROPFIND"), testFilePath)
                .header("Depth", "0")
                .with(authentication(auth)))
                .andExpect(status().is(207)) // WebDAV PROPFIND 返回 207 Multi-Status
                .andExpect(content().string(containsString("version-number")))
                .andExpect(content().string(containsString("version-count")));
    }
} 