package tslc.beihaiyun.lyra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 缓存配置类
 * 支持内存缓存，为后续扩展Redis做准备
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String USER_PERMISSIONS_CACHE = "userPermissions";
    public static final String USER_ROLES_CACHE = "userRoles";
    public static final String PERMISSION_CHECK_CACHE = "permissionCheck";
    public static final String INHERITED_PERMISSIONS_CACHE = "inheritedPermissions";
    public static final String ROLE_CACHE = "roleCache";

    // 新增缓存类别
    public static final String FILE_METADATA_CACHE = "fileMetadata";
    public static final String USER_SESSION_CACHE = "userSession";
    public static final String SPACE_INFO_CACHE = "spaceInfo";
    public static final String FOLDER_TREE_CACHE = "folderTree";
    public static final String SYSTEM_CONFIG_CACHE = "systemConfig";

    /**
     * 内存缓存管理器
     * 使用ConcurrentMapCacheManager作为默认实现
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            USER_PERMISSIONS_CACHE,
            USER_ROLES_CACHE,
            PERMISSION_CHECK_CACHE,
            INHERITED_PERMISSIONS_CACHE,
            ROLE_CACHE,
            FILE_METADATA_CACHE,
            USER_SESSION_CACHE,
            SPACE_INFO_CACHE,
            FOLDER_TREE_CACHE,
            SYSTEM_CONFIG_CACHE
        );
    }

}