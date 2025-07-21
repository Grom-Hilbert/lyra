package tslc.beihaiyun.lyra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 配置权限缓存管理器和相关缓存策略
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 权限缓存名称
     */
    public static final String USER_PERMISSIONS_CACHE = "userPermissions";
    public static final String USER_ROLES_CACHE = "userRoles";
    public static final String PERMISSION_CHECK_CACHE = "permissionCheck";
    public static final String INHERITED_PERMISSIONS_CACHE = "inheritedPermissions";

    /**
     * 缓存管理器
     * 使用ConcurrentMapCacheManager作为默认实现
     * 
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            USER_PERMISSIONS_CACHE,
            USER_ROLES_CACHE,
            PERMISSION_CHECK_CACHE,
            INHERITED_PERMISSIONS_CACHE
        );
    }
} 