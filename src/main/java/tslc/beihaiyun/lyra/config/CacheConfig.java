package tslc.beihaiyun.lyra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 * 支持内存缓存和Redis缓存，根据配置自动选择
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private final LyraProperties lyraProperties;

    public CacheConfig(LyraProperties lyraProperties) {
        this.lyraProperties = lyraProperties;
    }

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
     * 仅在缓存类型为memory时生效
     */
    @Bean("memoryCacheManager")
    @Primary
    @ConditionalOnProperty(name = "lyra.cache.type", havingValue = "memory", matchIfMissing = true)
    public CacheManager memoryCacheManager() {
        logger.info("配置内存缓存管理器");
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

    // ==================== Redis 缓存配置 ====================

    /**
     * Redis连接工厂
     * 支持单机和集群模式
     */
    @Bean
    @ConditionalOnProperty(name = "lyra.cache.type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            LyraProperties.RedisConfig redisConfig = lyraProperties.getCache().getRedis();

            // 检查是否为集群模式
            if (redisConfig.getCluster() != null && redisConfig.getCluster().getNodes() != null
                && !redisConfig.getCluster().getNodes().isEmpty()) {
                logger.info("配置 Redis 集群模式，节点: {}", redisConfig.getCluster().getNodes());
                return createClusterConnectionFactory(redisConfig);
            } else {
                logger.info("配置 Redis 单机模式，地址: {}:{}", redisConfig.getHost(), redisConfig.getPort());
                return createStandaloneConnectionFactory(redisConfig);
            }
        } catch (Exception e) {
            logger.error("Redis连接配置失败", e);
            throw new RuntimeException("Redis连接配置失败", e);
        }
    }

    /**
     * Redis缓存管理器
     */
    @Bean("redisCacheManager")
    @Primary
    @ConditionalOnProperty(name = "lyra.cache.type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.cache.RedisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            logger.info("配置Redis缓存管理器");

            // 默认缓存配置
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认TTL 1小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

            // 不同缓存的特定配置
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // 用户会话缓存：较短的TTL
            cacheConfigurations.put(USER_SESSION_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));

            // 权限缓存：中等TTL
            cacheConfigurations.put(USER_PERMISSIONS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
            cacheConfigurations.put(USER_ROLES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
            cacheConfigurations.put(PERMISSION_CHECK_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

            // 文件元数据缓存：较长的TTL
            cacheConfigurations.put(FILE_METADATA_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)));

            // 系统配置缓存：最长的TTL
            cacheConfigurations.put(SYSTEM_CONFIG_CACHE, defaultConfig.entryTtl(Duration.ofHours(6)));

            return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
        } catch (Exception e) {
            logger.error("Redis缓存管理器配置失败，使用内存缓存", e);
            return createFallbackCacheManager();
        }
    }

    /**
     * Redis模板
     */
    @Bean
    @ConditionalOnProperty(name = "lyra.cache.type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        logger.info("RedisTemplate配置完成");
        return template;
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建单机模式连接工厂
     */
    private LettuceConnectionFactory createStandaloneConnectionFactory(LyraProperties.RedisConfig redisConfig) {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisConfig.getHost());
        standaloneConfig.setPort(redisConfig.getPort());
        standaloneConfig.setDatabase(redisConfig.getDatabase());

        if (redisConfig.getPassword() != null && !redisConfig.getPassword().trim().isEmpty()) {
            standaloneConfig.setPassword(redisConfig.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig);
        factory.setValidateConnection(true);
        return factory;
    }

    /**
     * 创建集群模式连接工厂
     */
    private LettuceConnectionFactory createClusterConnectionFactory(LyraProperties.RedisConfig redisConfig) {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
            redisConfig.getCluster().getNodes());

        if (redisConfig.getPassword() != null && !redisConfig.getPassword().trim().isEmpty()) {
            clusterConfig.setPassword(redisConfig.getPassword());
        }

        if (redisConfig.getCluster().getMaxRedirects() != null) {
            clusterConfig.setMaxRedirects(redisConfig.getCluster().getMaxRedirects());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig);
        factory.setValidateConnection(true);
        return factory;
    }

    /**
     * 创建备选缓存管理器
     */
    private CacheManager createFallbackCacheManager() {
        logger.warn("使用内存缓存作为备选方案");
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