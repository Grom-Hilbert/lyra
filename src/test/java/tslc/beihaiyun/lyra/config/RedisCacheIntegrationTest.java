package tslc.beihaiyun.lyra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 缓存集成测试
 * 需要Redis服务器运行才能执行
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "lyra.cache.type=redis",
    "lyra.cache.redis.host=localhost",
    "lyra.cache.redis.port=6379",
    "lyra.cache.redis.database=15", // 使用测试数据库
    "lyra.cache.ttl=60"
})
@EnabledIfEnvironmentVariable(named = "REDIS_ENABLED", matches = "true")
class RedisCacheIntegrationTest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedisConnectionAndConfiguration() {
        // 验证Redis相关Bean已正确配置
        assertNotNull(cacheManager, "CacheManager应该已配置");
        assertNotNull(redisTemplate, "RedisTemplate应该已配置");
        assertNotNull(stringRedisTemplate, "StringRedisTemplate应该已配置");
        
        // 验证CacheManager类型
        assertEquals("RedisCacheManager", cacheManager.getClass().getSimpleName());
    }

    @Test
    void testRedisCacheOperations() {
        if (cacheManager == null) {
            return; // Redis未配置，跳过测试
        }

        Cache cache = cacheManager.getCache(CacheConfig.FILE_METADATA_CACHE);
        assertNotNull(cache, "文件元数据缓存应该存在");

        String key = "test-key";
        String value = "test-value";

        // 测试缓存存储
        cache.put(key, value);
        
        // 测试缓存获取
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper, "缓存值应该存在");
        assertEquals(value, wrapper.get(), "缓存值应该匹配");

        // 测试缓存清理
        cache.evict(key);
        wrapper = cache.get(key);
        assertNull(wrapper, "缓存值应该已被清理");
    }

    @Test
    void testRedisTemplateOperations() {
        if (redisTemplate == null) {
            return; // Redis未配置，跳过测试
        }

        String key = "test:redis:template";
        String value = "redis-template-value";

        // 测试RedisTemplate操作
        redisTemplate.opsForValue().set(key, value);
        Object retrievedValue = redisTemplate.opsForValue().get(key);
        assertEquals(value, retrievedValue, "RedisTemplate值应该匹配");

        // 清理测试数据
        redisTemplate.delete(key);
        assertNull(redisTemplate.opsForValue().get(key), "值应该已被删除");
    }

    @Test
    void testStringRedisTemplateOperations() {
        if (stringRedisTemplate == null) {
            return; // Redis未配置，跳过测试
        }

        String key = "test:string:template";
        String value = "string-template-value";

        // 测试StringRedisTemplate操作
        stringRedisTemplate.opsForValue().set(key, value);
        String retrievedValue = stringRedisTemplate.opsForValue().get(key);
        assertEquals(value, retrievedValue, "StringRedisTemplate值应该匹配");

        // 清理测试数据
        stringRedisTemplate.delete(key);
        assertNull(stringRedisTemplate.opsForValue().get(key), "值应该已被删除");
    }

    @Test
    void testAllRequiredCachesWithRedis() {
        if (cacheManager == null) {
            return; // Redis未配置，跳过测试
        }

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
    void testCacheWithComplexObjects() {
        if (cacheManager == null) {
            return; // Redis未配置，跳过测试
        }

        Cache cache = cacheManager.getCache(CacheConfig.FILE_METADATA_CACHE);
        assertNotNull(cache);

        String key = "complex-object-key";
        TestObject testObject = new TestObject("redis-test", 456);
        
        // 测试存储复杂对象
        cache.put(key, testObject);
        
        // 验证可以获取复杂对象
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper);
        TestObject retrieved = (TestObject) wrapper.get();
        assertNotNull(retrieved);
        assertEquals("redis-test", retrieved.getName());
        assertEquals(456, retrieved.getValue());

        // 清理测试数据
        cache.evict(key);
    }

    @Test
    void testCacheTTL() throws InterruptedException {
        if (cacheManager == null) {
            return; // Redis未配置，跳过测试
        }

        Cache cache = cacheManager.getCache(CacheConfig.FILE_METADATA_CACHE);
        assertNotNull(cache);

        String key = "ttl-test-key";
        String value = "ttl-test-value";

        // 存储值
        cache.put(key, value);
        
        // 立即验证值存在
        Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper);
        assertEquals(value, wrapper.get());

        // 注意：由于TTL设置为60秒，这里不等待过期
        // 在实际生产环境中，可以设置更短的TTL进行测试
    }

    /**
     * 测试对象类
     */
    public static class TestObject {
        private String name;
        private int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
