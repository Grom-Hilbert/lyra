package tslc.beihaiyun.lyra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Lyra 应用配置属性
 * 支持多层配置体系：默认配置 -> 环境变量 -> 外部配置文件 -> 动态配置
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Component
@Validated
@ConfigurationProperties(prefix = "lyra")
public class LyraProperties {

    @Valid
    @NestedConfigurationProperty
    private JwtConfig jwt = new JwtConfig();
    
    @Valid
    @NestedConfigurationProperty
    private StorageConfig storage = new StorageConfig();
    
    @Valid
    @NestedConfigurationProperty
    private SystemConfig system = new SystemConfig();

    @Valid
    @NestedConfigurationProperty
    private CacheConfig cache = new CacheConfig();
    
    // Getters and Setters
    public JwtConfig getJwt() {
        return jwt;
    }

    public void setJwt(JwtConfig jwt) {
        this.jwt = jwt;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    public SystemConfig getSystem() {
        return system;
    }

    public void setSystem(SystemConfig system) {
        this.system = system;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    /**
     * JWT 配置
     */
    public static class JwtConfig {
        /**
         * JWT签名密钥，生产环境必须修改
         */
        @NotBlank(message = "JWT密钥不能为空")
        @Size(min = 32, message = "JWT密钥长度至少32个字符")
        private String secret = "DefaultSecretKey_Please_Change_In_Production_Environment_32Characters";
        
        /**
         * JWT令牌过期时间（毫秒）
         */
        @NotNull(message = "JWT过期时间不能为空")
        @Min(value = 300000, message = "JWT过期时间不能少于5分钟")
        @Max(value = 604800000, message = "JWT过期时间不能超过7天")
        private Long expiration = 86400000L; // 24小时
        
        /**
         * 刷新令牌过期时间（毫秒）
         */
        @NotNull(message = "刷新令牌过期时间不能为空")
        @Min(value = 3600000, message = "刷新令牌过期时间不能少于1小时")
        @Max(value = 2592000000L, message = "刷新令牌过期时间不能超过30天")
        private Long refreshExpiration = 604800000L; // 7天

        // Getters and Setters
        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Long getExpiration() {
            return expiration;
        }

        public void setExpiration(Long expiration) {
            this.expiration = expiration;
        }

        public Long getRefreshExpiration() {
            return refreshExpiration;
        }

        public void setRefreshExpiration(Long refreshExpiration) {
            this.refreshExpiration = refreshExpiration;
        }
        
        /**
         * 验证JWT配置是否有效
         */
        public boolean isValid() {
            return secret != null && !secret.trim().isEmpty() && 
                   expiration != null && expiration > 0 &&
                   refreshExpiration != null && refreshExpiration > expiration;
        }
    }

    /**
     * 存储配置
     */
    public static class StorageConfig {
        /**
         * 文件存储基础路径
         */
        @NotBlank(message = "文件存储路径不能为空")
        private String basePath = "./data/files";
        
        /**
         * 临时文件存储路径
         */
        @NotBlank(message = "临时文件路径不能为空")
        private String tempPath = "./data/temp";
        
        /**
         * 单个文件最大大小
         */
        @NotBlank(message = "最大文件大小配置不能为空")
        @Pattern(regexp = "^\\d+[KMGT]?B?$", message = "文件大小格式无效，例如：100MB、1GB")
        private String maxFileSize = "100MB";
        
        /**
         * 允许的文件类型，*表示允许所有类型
         */
        @NotBlank(message = "允许的文件类型配置不能为空")
        private String allowedTypes = "*";
        
        /**
         * 是否启用文件去重
         */
        private Boolean enableDeduplication = true;
        
        /**
         * 存储后端类型：local, nfs, s3
         */
        @NotBlank(message = "存储后端类型不能为空")
        @Pattern(regexp = "^(local|nfs|s3)$", message = "存储后端类型必须是：local、nfs、s3")
        private String backend = "local";

        // Getters and Setters
        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getTempPath() {
            return tempPath;
        }

        public void setTempPath(String tempPath) {
            this.tempPath = tempPath;
        }

        public String getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public String getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(String allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
        
        public Boolean getEnableDeduplication() {
            return enableDeduplication;
        }

        public void setEnableDeduplication(Boolean enableDeduplication) {
            this.enableDeduplication = enableDeduplication;
        }
        
        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }
        
        /**
         * 将文件大小字符串转换为字节数
         */
        public long getMaxFileSizeInBytes() {
            if (maxFileSize == null || maxFileSize.trim().isEmpty()) {
                return 104857600L; // 默认100MB
            }
            
            String size = maxFileSize.toUpperCase().trim();
            long multiplier = 1L;
            
            if (size.endsWith("KB") || size.endsWith("K")) {
                multiplier = 1024L;
                size = size.substring(0, size.length() - (size.endsWith("KB") ? 2 : 1));
            } else if (size.endsWith("MB") || size.endsWith("M")) {
                multiplier = 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("MB") ? 2 : 1));
            } else if (size.endsWith("GB") || size.endsWith("G")) {
                multiplier = 1024L * 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("GB") ? 2 : 1));
            } else if (size.endsWith("TB") || size.endsWith("T")) {
                multiplier = 1024L * 1024L * 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("TB") ? 2 : 1));
            } else if (size.endsWith("B")) {
                size = size.substring(0, size.length() - 1);
            }
            
            try {
                return Long.parseLong(size) * multiplier;
            } catch (NumberFormatException e) {
                return 104857600L; // 默认100MB
            }
        }
    }

    /**
     * 系统配置
     */
    public static class SystemConfig {
        /**
         * 系统最大用户数
         */
        @NotNull(message = "最大用户数不能为空")
        @Min(value = 1, message = "最大用户数至少为1")
        @Max(value = 10000, message = "最大用户数不能超过10000")
        private Integer maxUsers = 100;
        
        /**
         * 默认用户空间配额
         */
        @NotBlank(message = "默认空间配额不能为空")
        @Pattern(regexp = "^\\d+[KMGT]?B?$", message = "空间配额格式无效，例如：10GB、100MB")
        private String defaultSpaceQuota = "10GB";
        
        /**
         * 是否启用版本控制
         */
        @NotNull(message = "版本控制启用状态不能为空")
        private Boolean enableVersionControl = true;
        
        /**
         * 系统名称
         */
        @NotBlank(message = "系统名称不能为空")
        @Size(max = 50, message = "系统名称不能超过50个字符")
        private String name = "Lyra Document Management System";
        
        /**
         * 系统描述
         */
        @Size(max = 200, message = "系统描述不能超过200个字符")
        private String description = "企业级云原生文档管理系统";
        
        /**
         * 是否允许用户注册
         */
        @NotNull(message = "用户注册开关不能为空")
        private Boolean allowUserRegistration = false;
        
        /**
         * 系统维护模式
         */
        @NotNull(message = "维护模式状态不能为空")
        private Boolean maintenanceMode = false;

        // Getters and Setters
        public Integer getMaxUsers() {
            return maxUsers;
        }

        public void setMaxUsers(Integer maxUsers) {
            this.maxUsers = maxUsers;
        }

        public String getDefaultSpaceQuota() {
            return defaultSpaceQuota;
        }

        public void setDefaultSpaceQuota(String defaultSpaceQuota) {
            this.defaultSpaceQuota = defaultSpaceQuota;
        }

        public Boolean getEnableVersionControl() {
            return enableVersionControl;
        }

        public void setEnableVersionControl(Boolean enableVersionControl) {
            this.enableVersionControl = enableVersionControl;
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
        
        public Boolean getAllowUserRegistration() {
            return allowUserRegistration;
        }

        public void setAllowUserRegistration(Boolean allowUserRegistration) {
            this.allowUserRegistration = allowUserRegistration;
        }
        
        public Boolean getMaintenanceMode() {
            return maintenanceMode;
        }

        public void setMaintenanceMode(Boolean maintenanceMode) {
            this.maintenanceMode = maintenanceMode;
        }
        
        /**
         * 将空间配额字符串转换为字节数
         */
        public long getDefaultSpaceQuotaInBytes() {
            return parseSize(defaultSpaceQuota, 10737418240L); // 默认10GB
        }
        
        /**
         * 解析大小字符串为字节数
         */
        private long parseSize(String sizeStr, long defaultValue) {
            if (sizeStr == null || sizeStr.trim().isEmpty()) {
                return defaultValue;
            }
            
            String size = sizeStr.toUpperCase().trim();
            long multiplier = 1L;
            
            if (size.endsWith("KB") || size.endsWith("K")) {
                multiplier = 1024L;
                size = size.substring(0, size.length() - (size.endsWith("KB") ? 2 : 1));
            } else if (size.endsWith("MB") || size.endsWith("M")) {
                multiplier = 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("MB") ? 2 : 1));
            } else if (size.endsWith("GB") || size.endsWith("G")) {
                multiplier = 1024L * 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("GB") ? 2 : 1));
            } else if (size.endsWith("TB") || size.endsWith("T")) {
                multiplier = 1024L * 1024L * 1024L * 1024L;
                size = size.substring(0, size.length() - (size.endsWith("TB") ? 2 : 1));
            } else if (size.endsWith("B")) {
                size = size.substring(0, size.length() - 1);
            }
            
            try {
                return Long.parseLong(size) * multiplier;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    /**
     * 缓存配置
     */
    public static class CacheConfig {
        /**
         * 缓存类型：memory(内存缓存) 或 redis(Redis缓存)
         */
        @NotBlank(message = "缓存类型不能为空")
        @Pattern(regexp = "^(memory|redis)$", message = "缓存类型必须是 memory 或 redis")
        private String type = "memory";

        /**
         * 缓存TTL（生存时间），单位：秒
         */
        @Min(value = 60, message = "缓存TTL不能小于60秒")
        @Max(value = 86400, message = "缓存TTL不能大于86400秒(24小时)")
        private Integer ttl = 3600; // 默认1小时

        /**
         * 最大缓存条目数
         */
        @Min(value = 100, message = "最大缓存条目数不能小于100")
        @Max(value = 100000, message = "最大缓存条目数不能大于100000")
        private Integer maxSize = 10000;

        /**
         * 是否启用缓存预热
         */
        private Boolean enableWarmup = true;

        /**
         * 缓存统计信息收集
         */
        private Boolean enableStats = true;

        @Valid
        @NestedConfigurationProperty
        private RedisConfig redis = new RedisConfig();

        @Valid
        @NestedConfigurationProperty
        private MemoryConfig memory = new MemoryConfig();

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getTtl() {
            return ttl;
        }

        public void setTtl(Integer ttl) {
            this.ttl = ttl;
        }

        public Integer getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(Integer maxSize) {
            this.maxSize = maxSize;
        }

        public Boolean getEnableWarmup() {
            return enableWarmup;
        }

        public void setEnableWarmup(Boolean enableWarmup) {
            this.enableWarmup = enableWarmup;
        }

        public Boolean getEnableStats() {
            return enableStats;
        }

        public void setEnableStats(Boolean enableStats) {
            this.enableStats = enableStats;
        }

        public RedisConfig getRedis() {
            return redis;
        }

        public void setRedis(RedisConfig redis) {
            this.redis = redis;
        }

        public MemoryConfig getMemory() {
            return memory;
        }

        public void setMemory(MemoryConfig memory) {
            this.memory = memory;
        }
    }

    /**
     * Redis缓存配置
     */
    public static class RedisConfig {
        /**
         * Redis主机地址
         */
        @NotBlank(message = "Redis主机地址不能为空")
        private String host = "localhost";

        /**
         * Redis端口
         */
        @Min(value = 1, message = "Redis端口必须大于0")
        @Max(value = 65535, message = "Redis端口不能大于65535")
        private Integer port = 6379;

        /**
         * Redis密码
         */
        private String password;

        /**
         * Redis数据库索引
         */
        @Min(value = 0, message = "Redis数据库索引不能小于0")
        @Max(value = 15, message = "Redis数据库索引不能大于15")
        private Integer database = 0;

        /**
         * 连接超时时间（毫秒）
         */
        @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
        private Integer timeout = 5000;

        /**
         * 连接池最大连接数
         */
        @Min(value = 1, message = "连接池最大连接数不能小于1")
        private Integer maxActive = 20;

        /**
         * 连接池最大空闲连接数
         */
        @Min(value = 1, message = "连接池最大空闲连接数不能小于1")
        private Integer maxIdle = 10;

        /**
         * 连接池最小空闲连接数
         */
        @Min(value = 0, message = "连接池最小空闲连接数不能小于0")
        private Integer minIdle = 2;

        /**
         * Redis集群配置
         */
        @Valid
        @NestedConfigurationProperty
        private ClusterConfig cluster = new ClusterConfig();

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getDatabase() {
            return database;
        }

        public void setDatabase(Integer database) {
            this.database = database;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Integer getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(Integer maxActive) {
            this.maxActive = maxActive;
        }

        public Integer getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(Integer maxIdle) {
            this.maxIdle = maxIdle;
        }

        public Integer getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(Integer minIdle) {
            this.minIdle = minIdle;
        }

        public ClusterConfig getCluster() {
            return cluster;
        }

        public void setCluster(ClusterConfig cluster) {
            this.cluster = cluster;
        }
    }

    /**
     * 内存缓存配置
     */
    public static class MemoryConfig {
        /**
         * 初始容量
         */
        @Min(value = 16, message = "初始容量不能小于16")
        private Integer initialCapacity = 100;

        /**
         * 最大权重
         */
        @Min(value = 1000, message = "最大权重不能小于1000")
        private Long maximumWeight = 100000L;

        /**
         * 写入后过期时间（秒）
         */
        @Min(value = 60, message = "写入后过期时间不能小于60秒")
        private Integer expireAfterWrite = 3600;

        /**
         * 访问后过期时间（秒）
         */
        @Min(value = 60, message = "访问后过期时间不能小于60秒")
        private Integer expireAfterAccess = 1800;

        /**
         * 刷新后写入时间（秒）
         */
        @Min(value = 30, message = "刷新后写入时间不能小于30秒")
        private Integer refreshAfterWrite = 300;

        // Getters and Setters
        public Integer getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(Integer initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public Long getMaximumWeight() {
            return maximumWeight;
        }

        public void setMaximumWeight(Long maximumWeight) {
            this.maximumWeight = maximumWeight;
        }

        public Integer getExpireAfterWrite() {
            return expireAfterWrite;
        }

        public void setExpireAfterWrite(Integer expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }

        public Integer getExpireAfterAccess() {
            return expireAfterAccess;
        }

        public void setExpireAfterAccess(Integer expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }

        public Integer getRefreshAfterWrite() {
            return refreshAfterWrite;
        }

        public void setRefreshAfterWrite(Integer refreshAfterWrite) {
            this.refreshAfterWrite = refreshAfterWrite;
        }
    }

    /**
     * Redis集群配置
     */
    public static class ClusterConfig {
        /**
         * 集群节点列表
         */
        private java.util.List<String> nodes = new java.util.ArrayList<>();

        /**
         * 最大重定向次数
         */
        @Min(value = 1, message = "最大重定向次数不能小于1")
        private Integer maxRedirects = 3;

        // Getters and Setters
        public java.util.List<String> getNodes() {
            return nodes;
        }

        public void setNodes(java.util.List<String> nodes) {
            this.nodes = nodes;
        }

        public Integer getMaxRedirects() {
            return maxRedirects;
        }

        public void setMaxRedirects(Integer maxRedirects) {
            this.maxRedirects = maxRedirects;
        }
    }
}