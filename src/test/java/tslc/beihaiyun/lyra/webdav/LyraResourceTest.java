package tslc.beihaiyun.lyra.webdav;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

/**
 * LyraResource 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@DisplayName("LyraResource 单元测试")
class LyraResourceTest {

    @Test
    @DisplayName("创建文件资源")
    void testCreateFileResource() {
        // Given
        String name = "test.txt";
        String path = "/webdav/personal/space1/test.txt";
        Long size = 1024L;
        String contentType = "text/plain";
        
        // When
        LyraResource resource = LyraResource.file()
                .name(name)
                .path(path)
                .size(size)
                .contentType(contentType)
                .spaceType(LyraResource.SpaceType.PERSONAL)
                .build();
        
        // Then
        assertNotNull(resource);
        assertEquals(name, resource.getName());
        assertEquals(path, resource.getPath());
        assertEquals(size, resource.getSize());
        assertEquals(contentType, resource.getContentType());
        assertEquals(LyraResource.ResourceType.RESOURCE, resource.getResourceType());
        assertEquals(LyraResource.SpaceType.PERSONAL, resource.getSpaceType());
        assertTrue(resource.isResource());
        assertFalse(resource.isCollection());
        assertEquals(1024L, resource.getActualSize());
    }

    @Test
    @DisplayName("创建集合资源")
    void testCreateCollectionResource() {
        // Given
        String name = "documents";
        String path = "/webdav/enterprise/space1/documents";
        
        // When
        LyraResource resource = LyraResource.collection()
                .name(name)
                .path(path)
                .spaceType(LyraResource.SpaceType.ENTERPRISE)
                .build();
        
        // Then
        assertNotNull(resource);
        assertEquals(name, resource.getName());
        assertEquals(path, resource.getPath());
        assertEquals(LyraResource.ResourceType.COLLECTION, resource.getResourceType());
        assertEquals(LyraResource.SpaceType.ENTERPRISE, resource.getSpaceType());
        assertTrue(resource.isCollection());
        assertFalse(resource.isResource());
        assertEquals(0L, resource.getActualSize());
    }

    @Test
    @DisplayName("创建空间根目录资源")
    void testCreateSpaceRootResource() {
        // Given
        String name = "myspace";
        String path = "/webdav/personal/myspace";
        
        // When
        LyraResource resource = LyraResource.spaceRoot()
                .name(name)
                .path(path)
                .spaceType(LyraResource.SpaceType.PERSONAL)
                .build();
        
        // Then
        assertNotNull(resource);
        assertEquals(name, resource.getName());
        assertEquals(path, resource.getPath());
        assertEquals(LyraResource.ResourceType.SPACE_ROOT, resource.getResourceType());
        assertEquals(LyraResource.SpaceType.PERSONAL, resource.getSpaceType());
        assertTrue(resource.isCollection());
        assertTrue(resource.isSpaceRoot());
        assertFalse(resource.isResource());
    }

    @Test
    @DisplayName("创建系统根目录资源")
    void testCreateSystemRootResource() {
        // When
        LyraResource resource = LyraResource.createSystemRoot();
        
        // Then
        assertNotNull(resource);
        assertEquals("", resource.getName());
        assertEquals("/webdav", resource.getPath());
        assertEquals(LyraResource.ResourceType.SYSTEM_ROOT, resource.getResourceType());
        assertEquals(LyraResource.SpaceType.SYSTEM, resource.getSpaceType());
        assertTrue(resource.isCollection());
        assertTrue(resource.isSystemRoot());
        assertFalse(resource.isResource());
        assertEquals("Lyra WebDAV", resource.getDisplayName());
    }

    @Test
    @DisplayName("从FileEntity创建资源")
    void testFromFileEntity() {
        // Given
        FileEntity fileEntity = mock(FileEntity.class);
        when(fileEntity.getName()).thenReturn("test.pdf");
        when(fileEntity.getSizeBytes()).thenReturn(2048L);
        when(fileEntity.getMimeType()).thenReturn("application/pdf");
        when(fileEntity.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(fileEntity.getUpdatedAt()).thenReturn(LocalDateTime.now());
        
        String webdavPath = "/webdav/personal/space1/test.pdf";
        
        // When
        LyraResource resource = LyraResource.fromFileEntity(fileEntity, webdavPath);
        
        // Then
        assertNotNull(resource);
        assertEquals("test.pdf", resource.getName());
        assertEquals(webdavPath, resource.getPath());
        assertEquals(2048L, resource.getActualSize());
        assertEquals("application/pdf", resource.getContentType());
        assertEquals(LyraResource.ResourceType.RESOURCE, resource.getResourceType());
        assertEquals(fileEntity, resource.getFileEntity());
        assertTrue(resource.isResource());
        assertFalse(resource.isCollection());
    }

    @Test
    @DisplayName("从Folder创建资源")
    void testFromFolder() {
        // Given
        Folder folder = mock(Folder.class);
        when(folder.getName()).thenReturn("documents");
        when(folder.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(folder.getUpdatedAt()).thenReturn(LocalDateTime.now());
        
        String webdavPath = "/webdav/enterprise/space1/documents";
        
        // When
        LyraResource resource = LyraResource.fromFolder(folder, webdavPath);
        
        // Then
        assertNotNull(resource);
        assertEquals("documents", resource.getName());
        assertEquals(webdavPath, resource.getPath());
        assertEquals(LyraResource.ResourceType.COLLECTION, resource.getResourceType());
        assertEquals(folder, resource.getFolder());
        assertTrue(resource.isCollection());
        assertFalse(resource.isResource());
        assertEquals(0L, resource.getActualSize());
    }

    @Test
    @DisplayName("从Space创建资源")
    void testFromSpace() {
        // Given
        Space space = mock(Space.class);
        when(space.getName()).thenReturn("myspace");
        when(space.getType()).thenReturn(Space.SpaceType.PERSONAL);
        when(space.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(space.getUpdatedAt()).thenReturn(LocalDateTime.now());
        
        String webdavPath = "/webdav/personal/myspace";
        
        // When
        LyraResource resource = LyraResource.fromSpace(space, webdavPath);
        
        // Then
        assertNotNull(resource);
        assertEquals("myspace", resource.getName());
        assertEquals(webdavPath, resource.getPath());
        assertEquals(LyraResource.ResourceType.SPACE_ROOT, resource.getResourceType());
        assertEquals(LyraResource.SpaceType.PERSONAL, resource.getSpaceType());
        assertEquals(space, resource.getSpace());
        assertTrue(resource.isCollection());
        assertTrue(resource.isSpaceRoot());
        assertFalse(resource.isResource());
    }

    @Test
    @DisplayName("测试资源类型XML生成")
    void testResourceTypeXml() {
        // Given
        LyraResource fileResource = LyraResource.file().name("test.txt").path("/test").build();
        LyraResource collectionResource = LyraResource.collection().name("folder").path("/folder").build();
        
        // When & Then
        assertEquals("", fileResource.getResourceTypeXml());
        assertEquals("<D:collection/>", collectionResource.getResourceTypeXml());
    }

    @Test
    @DisplayName("测试内容类型获取")
    void testActualContentType() {
        // Given
        LyraResource fileResource = LyraResource.file()
                .name("test.txt")
                .path("/test")
                .contentType("text/plain")
                .build();
        
        LyraResource collectionResource = LyraResource.collection()
                .name("folder")
                .path("/folder")
                .build();
        
        LyraResource fileWithoutType = LyraResource.file()
                .name("unknown")
                .path("/unknown")
                .build();
        
        // When & Then
        assertEquals("text/plain", fileResource.getActualContentType());
        assertEquals("httpd/unix-directory", collectionResource.getActualContentType());
        assertEquals("application/octet-stream", fileWithoutType.getActualContentType());
    }

    @Test
    @DisplayName("测试toBuilder方法")
    void testToBuilder() {
        // Given
        LyraResource original = LyraResource.file()
                .name("test.txt")
                .path("/test")
                .size(1024L)
                .contentType("text/plain")
                .build();
        
        // When
        LyraResource modified = original.toBuilder()
                .name("modified.txt")
                .size(2048L)
                .build();
        
        // Then
        assertNotNull(modified);
        assertEquals("modified.txt", modified.getName());
        assertEquals("/test", modified.getPath()); // 保持原有路径
        assertEquals(2048L, modified.getActualSize());
        assertEquals("text/plain", modified.getContentType()); // 保持原有类型
        
        // 原始对象不变
        assertEquals("test.txt", original.getName());
        assertEquals(1024L, original.getActualSize());
    }

    @Test
    @DisplayName("测试equals和hashCode")
    void testEqualsAndHashCode() {
        // Given
        LyraResource resource1 = LyraResource.file().name("test.txt").path("/test").build();
        LyraResource resource2 = LyraResource.file().name("test.txt").path("/test").build();
        LyraResource resource3 = LyraResource.file().name("other.txt").path("/other").build();
        
        // When & Then
        assertEquals(resource1, resource2);
        assertEquals(resource1.hashCode(), resource2.hashCode());
        assertNotEquals(resource1, resource3);
        assertNotEquals(resource1.hashCode(), resource3.hashCode());
    }

    @Test
    @DisplayName("测试toString方法")
    void testToString() {
        // Given
        LyraResource resource = LyraResource.file()
                .name("test.txt")
                .path("/webdav/personal/space1/test.txt")
                .size(1024L)
                .spaceType(LyraResource.SpaceType.PERSONAL)
                .build();
        
        // When
        String toString = resource.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("test.txt"));
        assertTrue(toString.contains("/webdav/personal/space1/test.txt"));
        assertTrue(toString.contains("RESOURCE"));
        assertTrue(toString.contains("PERSONAL"));
        assertTrue(toString.contains("1024"));
    }
} 