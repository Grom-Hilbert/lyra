package tslc.beihaiyun.lyra.webdav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.FolderService;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.service.UserService;

/**
 * LyraWebDavResourceService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LyraWebDavResourceService 单元测试")
class LyraWebDavResourceServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private FileService fileService;

    @Mock
    private FolderService folderService;

    @Mock
    private StorageService storageService;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SecurityContext securityContext;

    private LyraWebDavResourceService resourceService;

    private User testUser;
    private Space testSpace;

    @BeforeEach
    void setUp() {
        resourceService = new LyraWebDavResourceService(
                userService, fileService, folderService, storageService, spaceRepository);

        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // 准备测试空间
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("myspace");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);

        // Mock Spring Security Context
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("解析根路径")
    void testParseRootPath() {
        // Given
        String path = "/webdav";

        // When
        LyraWebDavResourceService.WebDavPathInfo pathInfo = resourceService.parsePath(path);

        // Then
        assertNotNull(pathInfo);
        assertEquals(LyraWebDavResourceService.WebDavPathType.ROOT, pathInfo.getType());
        assertNull(pathInfo.getSpacePath());
        assertNull(pathInfo.getFilePath());
        assertEquals("/", pathInfo.getFullPath());
    }

    @Test
    @DisplayName("解析个人空间路径")
    void testParsePersonalSpacePath() {
        // Given
        String path = "/webdav/personal";

        // When
        LyraWebDavResourceService.WebDavPathInfo pathInfo = resourceService.parsePath(path);

        // Then
        assertNotNull(pathInfo);
        assertEquals(LyraWebDavResourceService.WebDavPathType.PERSONAL, pathInfo.getType());
        assertEquals("", pathInfo.getSpacePath());
        assertEquals("", pathInfo.getFilePath());
        assertEquals("/personal", pathInfo.getFullPath());
    }

    @Test
    @DisplayName("解析具体空间路径")
    void testParseSpecificSpacePath() {
        // Given
        String path = "/webdav/personal/myspace";

        // When
        LyraWebDavResourceService.WebDavPathInfo pathInfo = resourceService.parsePath(path);

        // Then
        assertNotNull(pathInfo);
        assertEquals(LyraWebDavResourceService.WebDavPathType.PERSONAL, pathInfo.getType());
        assertEquals("myspace", pathInfo.getSpacePath());
        assertEquals("", pathInfo.getFilePath());
        assertEquals("/personal/myspace", pathInfo.getFullPath());
    }

    @Test
    @DisplayName("解析文件路径")
    void testParseFilePath() {
        // Given
        String path = "/webdav/personal/myspace/documents/test.txt";

        // When
        LyraWebDavResourceService.WebDavPathInfo pathInfo = resourceService.parsePath(path);

        // Then
        assertNotNull(pathInfo);
        assertEquals(LyraWebDavResourceService.WebDavPathType.PERSONAL, pathInfo.getType());
        assertEquals("myspace", pathInfo.getSpacePath());
        assertEquals("documents/test.txt", pathInfo.getFilePath());
        assertEquals("/personal/myspace/documents/test.txt", pathInfo.getFullPath());
    }

    @Test
    @DisplayName("解析企业空间路径")
    void testParseEnterprisePath() {
        // Given
        String path = "/webdav/enterprise/workspace/folder/file.pdf";

        // When
        LyraWebDavResourceService.WebDavPathInfo pathInfo = resourceService.parsePath(path);

        // Then
        assertNotNull(pathInfo);
        assertEquals(LyraWebDavResourceService.WebDavPathType.ENTERPRISE, pathInfo.getType());
        assertEquals("workspace", pathInfo.getSpacePath());
        assertEquals("folder/file.pdf", pathInfo.getFilePath());
        assertEquals("/enterprise/workspace/folder/file.pdf", pathInfo.getFullPath());
    }

    @Test
    @DisplayName("获取当前用户 - 认证用户")
    void testGetCurrentUserAuthenticated() {
        // Given
        LyraUserPrincipal principal = LyraUserPrincipal.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .password("")
                .authorities(List.of())
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User currentUser = resourceService.getCurrentUser();

        // Then
        assertNotNull(currentUser);
        assertEquals(testUser, currentUser);
    }

    @Test
    @DisplayName("获取当前用户 - 未认证")
    void testGetCurrentUserNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        User currentUser = resourceService.getCurrentUser();

        // Then
        assertNull(currentUser);
    }

    @Test
    @DisplayName("获取系统根资源")
    void testGetSystemRootResource() {
        // Given
        String path = "/webdav";

        // When
        LyraResource resource = resourceService.getResource(path);

        // Then
        assertNotNull(resource);
        assertTrue(resource.isSystemRoot());
        assertEquals("/webdav", resource.getPath());
        assertEquals(LyraResource.SpaceType.SYSTEM, resource.getSpaceType());
        assertTrue(resource.isCollection());
        
        // 验证子资源
        List<LyraResource> children = resource.getChildren();
        assertEquals(2, children.size());
        
        LyraResource personalSpace = children.stream()
                .filter(child -> "personal".equals(child.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(personalSpace);
        assertEquals(LyraResource.SpaceType.PERSONAL, personalSpace.getSpaceType());
        
        LyraResource enterpriseSpace = children.stream()
                .filter(child -> "enterprise".equals(child.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(enterpriseSpace);
        assertEquals(LyraResource.SpaceType.ENTERPRISE, enterpriseSpace.getSpaceType());
    }

    @Test
    @DisplayName("检查资源是否存在")
    void testResourceExists() {
        // Given
        String rootPath = "/webdav";
        String nonExistentPath = "/webdav/personal/nonexistent";

        // When & Then
        assertTrue(resourceService.resourceExists(rootPath));
        assertFalse(resourceService.resourceExists(nonExistentPath));
    }

    @Test
    @DisplayName("检查是否为目录")
    void testIsDirectory() {
        // Given
        String rootPath = "/webdav";
        String nonExistentPath = "/webdav/nonexistent";

        // When & Then
        assertTrue(resourceService.isDirectory(rootPath));
        assertFalse(resourceService.isDirectory(nonExistentPath));
    }

    @Test
    @DisplayName("获取文件大小")
    void testGetFileSize() {
        // Given
        String rootPath = "/webdav";
        String nonExistentPath = "/webdav/nonexistent";

        // When & Then
        assertEquals(0L, resourceService.getFileSize(rootPath)); // 目录大小为0
        assertEquals(0L, resourceService.getFileSize(nonExistentPath)); // 不存在的资源大小为0
    }

    @Test
    @DisplayName("列出根目录内容")
    void testListRootDirectory() {
        // Given
        String rootPath = "/webdav";

        // When
        List<LyraWebDavResourceService.WebDavResource> resources = resourceService.listDirectory(rootPath);

        // Then
        assertNotNull(resources);
        assertEquals(2, resources.size());
        
        boolean hasPersonal = resources.stream()
                .anyMatch(resource -> "personal".equals(resource.getName()) && resource.isDirectory());
        boolean hasEnterprise = resources.stream()
                .anyMatch(resource -> "enterprise".equals(resource.getName()) && resource.isDirectory());
        
        assertTrue(hasPersonal);
        assertTrue(hasEnterprise);
    }

    @Test
    @DisplayName("列出非目录内容")
    void testListNonDirectoryContent() {
        // Given
        String nonExistentPath = "/webdav/nonexistent";

        // When
        List<LyraWebDavResourceService.WebDavResource> resources = resourceService.listDirectory(nonExistentPath);

        // Then
        assertNotNull(resources);
        assertTrue(resources.isEmpty());
    }

    @Test
    @DisplayName("获取文件内容 - 文件不存在")
    void testGetFileContentNotExists() {
        // Given
        String nonExistentPath = "/webdav/nonexistent";

        // When & Then
        assertThrows(IOException.class, () -> {
            resourceService.getFileContent(nonExistentPath);
        });
    }

    @Test
    @DisplayName("创建目录 - 用户未认证")
    void testCreateDirectoryNotAuthenticated() {
        // Given
        String path = "/webdav/personal/myspace/newfolder";
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = resourceService.createDirectory(path);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("上传文件 - 用户未认证")
    void testUploadFileNotAuthenticated() throws IOException {
        // Given
        String path = "/webdav/personal/myspace/test.txt";
        ByteArrayInputStream content = new ByteArrayInputStream("test content".getBytes());
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = resourceService.uploadFile(path, content, 12L);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("删除资源 - 用户未认证")
    void testDeleteResourceNotAuthenticated() {
        // Given
        String path = "/webdav/personal/myspace/test.txt";
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = resourceService.deleteResource(path);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("WebDavPathInfo toString方法")
    void testWebDavPathInfoToString() {
        // Given
        LyraWebDavResourceService.WebDavPathInfo pathInfo = 
                new LyraWebDavResourceService.WebDavPathInfo(
                        LyraWebDavResourceService.WebDavPathType.PERSONAL,
                        "myspace",
                        "documents/test.txt",
                        "/personal/myspace/documents/test.txt");

        // When
        String toString = pathInfo.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("PERSONAL"));
        assertTrue(toString.contains("myspace"));
        assertTrue(toString.contains("documents/test.txt"));
        assertTrue(toString.contains("/personal/myspace/documents/test.txt"));
    }

    @Test
    @DisplayName("WebDavResource 基础属性")
    void testWebDavResource() {
        // Given
        String name = "test.txt";
        boolean isDirectory = false;
        long size = 1024L;
        long lastModified = System.currentTimeMillis();

        // When
        LyraWebDavResourceService.WebDavResource resource = 
                new LyraWebDavResourceService.WebDavResource(name, isDirectory, size, lastModified);

        // Then
        assertEquals(name, resource.getName());
        assertEquals(isDirectory, resource.isDirectory());
        assertEquals(size, resource.getSize());
        assertEquals(lastModified, resource.getLastModified());
    }
} 