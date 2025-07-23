package tslc.beihaiyun.lyra.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheConfig 集成测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testCacheManagerIsConfigured() {
        assertNotNull(cacheManager, "CacheManager should be configured");
    }

    @Test
    void testAllRequiredCachesAreAvailable() {
        // 验证所有必需的缓存都已配置
        assertNotNull(cacheManager.getCache(CacheConfig.USER_PERMISSIONS_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.USER_ROLES_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.PERMISSION_CHECK_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.INHERITED_PERMISSIONS_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.ROLE_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.FILE_METADATA_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.USER_SESSION_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.SPACE_INFO_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.FOLDER_TREE_CACHE));
        assertNotNull(cacheManager.getCache(CacheConfig.SYSTEM_CONFIG_CACHE));
    }

    @Test
    void testCacheBasicOperations() {
        Cache cache = cacheManager.getCache(CacheConfig.USER_PERMISSIONS_CACHE);
        assertNotNull(cache);

        String key = "test-key";
        String value = "test-value";

        // 测试缓存存储
        cache.put(key, value);
        
        // 测试缓存获取
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper);
        assertEquals(value, wrapper.get());

        // 测试缓存清理
        cache.evict(key);
        wrapper = cache.get(key);
        assertNull(wrapper);
    }

    @Test
    void testCacheClear() {
        Cache cache = cacheManager.getCache(CacheConfig.USER_ROLES_CACHE);
        assertNotNull(cache);

        // 添加一些测试数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // 验证数据存在
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        // 清理缓存
        cache.clear();

        // 验证数据被清理
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testCacheNames() {
        // 验证缓存名称集合包含所有必需的缓存
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.USER_PERMISSIONS_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.USER_ROLES_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.PERMISSION_CHECK_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.INHERITED_PERMISSIONS_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.ROLE_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.FILE_METADATA_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.USER_SESSION_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.SPACE_INFO_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.FOLDER_TREE_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.SYSTEM_CONFIG_CACHE));
    }

    @Test
    void testCacheWithNullValues() {
        Cache cache = cacheManager.getCache(CacheConfig.PERMISSION_CHECK_CACHE);
        assertNotNull(cache);

        String key = "null-test-key";
        
        // 测试存储null值
        cache.put(key, null);
        
        // 验证可以获取null值
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper);
        assertNull(wrapper.get());
    }

    @Test
    void testCacheWithComplexObjects() {
        Cache cache = cacheManager.getCache(CacheConfig.FILE_METADATA_CACHE);
        assertNotNull(cache);

        String key = "complex-object-key";
        TestObject testObject = new TestObject("test", 123);
        
        // 测试存储复杂对象
        cache.put(key, testObject);
        
        // 验证可以获取复杂对象
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper);
        TestObject retrieved = (TestObject) wrapper.get();
        assertNotNull(retrieved);
        assertEquals("test", retrieved.getName());
        assertEquals(123, retrieved.getValue());
    }

    // 测试用的简单对象
    private static class TestObject {
        private String name;
        private int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
