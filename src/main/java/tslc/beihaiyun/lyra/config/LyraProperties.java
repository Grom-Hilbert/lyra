package tslc.beihaiyun.lyra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lyra 应用配置属性
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Component
@ConfigurationProperties(prefix = "lyra")
public class LyraProperties {

    private JwtConfig jwt = new JwtConfig();
    private StorageConfig storage = new StorageConfig();
    private SystemConfig system = new SystemConfig();

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

    /**
     * JWT 配置
     */
    public static class JwtConfig {
        private String secret = "DefaultSecretKey";
        private Long expiration = 86400000L; // 24小时
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
    }

    /**
     * 存储配置
     */
    public static class StorageConfig {
        private String basePath = "./data/files";
        private String tempPath = "./data/temp";
        private String maxFileSize = "100MB";
        private String allowedTypes = "*";

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
    }

    /**
     * 系统配置
     */
    public static class SystemConfig {
        private Integer maxUsers = 100;
        private String defaultSpaceQuota = "10GB";
        private Boolean enableVersionControl = true;

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
    }
} 