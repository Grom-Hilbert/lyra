package tslc.beihaiyun.lyra.health;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * 缓存健康检查器
 * 检查缓存系统的健康状态和性能指标
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Component
public class CacheHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(CacheHealthIndicator.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private LyraProperties lyraProperties;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // 检查缓存管理器状态
            if (cacheManager == null) {
                return Health.down()
                        .withDetail("error", "CacheManager未配置")
                        .build();
            }

            // 获取缓存配置信息
            LyraProperties.CacheConfig cacheConfig = lyraProperties.getCache();
            details.put("cacheType", cacheConfig.getType());
            details.put("ttl", cacheConfig.getTtl() + " seconds");
            details.put("maxSize", cacheConfig.getMaxSize());
            details.put("enableWarmup", cacheConfig.getEnableWarmup());
            details.put("enableStats", cacheConfig.getEnableStats());

            // 检查缓存名称和状态
            Collection<String> cacheNames = cacheManager.getCacheNames();
            details.put("cacheNames", cacheNames);
            details.put("cacheCount", cacheNames.size());

            // 测试缓存基本功能
            boolean allCachesHealthy = true;
            Map<String, Object> cacheStatuses = new HashMap<>();
            
            for (String cacheName : cacheNames) {
                try {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        // 尝试放入和获取测试数据
                        String testKey = "_health_check_" + System.currentTimeMillis();
                        String testValue = "test_value";
                        
                        cache.put(testKey, testValue);
                        Object retrievedValue = cache.get(testKey, String.class);
                        
                        // 清理测试数据
                        cache.evict(testKey);
                        
                        if (testValue.equals(retrievedValue)) {
                            cacheStatuses.put(cacheName, "UP");
                        } else {
                            cacheStatuses.put(cacheName, "DOWN - value mismatch");
                            allCachesHealthy = false;
                        }
                    } else {
                        cacheStatuses.put(cacheName, "DOWN - cache is null");
                        allCachesHealthy = false;
                    }
                } catch (Exception e) {
                    log.warn("缓存 {} 健康检查失败: {}", cacheName, e.getMessage());
                    cacheStatuses.put(cacheName, "DOWN - " + e.getMessage());
                    allCachesHealthy = false;
                }
            }
            
            details.put("cacheStatuses", cacheStatuses);

            // 添加缓存配置细节
            if ("redis".equals(cacheConfig.getType())) {
                LyraProperties.RedisConfig redisConfig = cacheConfig.getRedis();
                Map<String, Object> redisDetails = new HashMap<>();
                redisDetails.put("host", redisConfig.getHost());
                redisDetails.put("port", redisConfig.getPort());
                redisDetails.put("database", redisConfig.getDatabase());
                redisDetails.put("timeout", redisConfig.getTimeout() + "ms");
                redisDetails.put("maxActive", redisConfig.getMaxActive());
                redisDetails.put("maxIdle", redisConfig.getMaxIdle());
                redisDetails.put("minIdle", redisConfig.getMinIdle());
                details.put("redisConfig", redisDetails);
            } else if ("memory".equals(cacheConfig.getType())) {
                LyraProperties.MemoryConfig memoryConfig = cacheConfig.getMemory();
                Map<String, Object> memoryDetails = new HashMap<>();
                memoryDetails.put("initialCapacity", memoryConfig.getInitialCapacity());
                memoryDetails.put("maximumWeight", memoryConfig.getMaximumWeight());
                memoryDetails.put("expireAfterWrite", memoryConfig.getExpireAfterWrite() + "s");
                memoryDetails.put("expireAfterAccess", memoryConfig.getExpireAfterAccess() + "s");
                memoryDetails.put("refreshAfterWrite", memoryConfig.getRefreshAfterWrite() + "s");
                details.put("memoryConfig", memoryDetails);
            }

            if (allCachesHealthy) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "一个或多个缓存不健康")
                        .withDetails(details)
                        .build();
            }

        } catch (Exception e) {
            log.error("缓存健康检查失败", e);
            return Health.down()
                    .withDetail("error", "缓存健康检查异常: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
} 