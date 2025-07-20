package tslc.beihaiyun.lyra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.FileEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单实体测试
 * 不依赖Spring Context，只测试实体类基本功能
 */
@DisplayName("简单实体测试")
class SimpleEntityTest {

    @Test
    @DisplayName("User实体基本功能测试")
    void testUserEntity() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
    }

    @Test
    @DisplayName("FileEntity实体基本功能测试")
    void testFileEntity() {
        FileEntity file = new FileEntity();
        file.setName("test.txt");
        file.setOriginalName("test.txt");
        file.setPath("/test.txt");
        file.setSizeBytes(1024L);
        
        assertNotNull(file);
        assertEquals("test.txt", file.getName());
        assertEquals("/test.txt", file.getPath());
        assertEquals(1024L, file.getSizeBytes());
    }
} 