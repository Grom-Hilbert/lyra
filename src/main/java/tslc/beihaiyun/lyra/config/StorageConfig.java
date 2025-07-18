package tslc.beihaiyun.lyra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tslc.beihaiyun.lyra.storage.StorageService;
import tslc.beihaiyun.lyra.storage.StorageServiceFactory;

/**
 * 存储服务配置
 */
@Slf4j
@Configuration
public class StorageConfig {
    
    /**
     * 存储配置属性
     */
    @ConfigurationProperties(prefix = "lyra.storage")
    public static class StorageProperties {
        private String primary = "local";
        private Local local = new Local();
        private S3 s3 = new S3();
        private Nfs nfs = new Nfs();
        
        public String getPrimary() { return primary; }
        public void setPrimary(String primary) { this.primary = primary; }
        public Local getLocal() { return local; }
        public void setLocal(Local local) { this.local = local; }
        public S3 getS3() { return s3; }
        public void setS3(S3 s3) { this.s3 = s3; }
        public Nfs getNfs() { return nfs; }
        public void setNfs(Nfs nfs) { this.nfs = nfs; }
        
        public static class Local {
            private String root = "./storage";
            private long maxFileSize = 104857600L; // 100MB
            
            public String getRoot() { return root; }
            public void setRoot(String root) { this.root = root; }
            public long getMaxFileSize() { return maxFileSize; }
            public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        }
        
        public static class S3 {
            private boolean enabled = false;
            private String endpoint;
            private String region = "us-east-1";
            private String accessKey;
            private String secretKey;
            private String bucket;
            private long maxFileSize = 104857600L; // 100MB
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getEndpoint() { return endpoint; }
            public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
            public String getRegion() { return region; }
            public void setRegion(String region) { this.region = region; }
            public String getAccessKey() { return accessKey; }
            public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
            public String getSecretKey() { return secretKey; }
            public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
            public String getBucket() { return bucket; }
            public void setBucket(String bucket) { this.bucket = bucket; }
            public long getMaxFileSize() { return maxFileSize; }
            public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        }
        
        public static class Nfs {
            private boolean enabled = false;
            private String server;
            private String exportPath;
            private String mountPoint = "/mnt/nfs-storage";
            private String mountOptions = "rw,sync,hard,intr";
            private long maxFileSize = 104857600L; // 100MB
            private int connectionTimeout = 30000;
            private int readTimeout = 60000;
            private int retryCount = 3;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getServer() { return server; }
            public void setServer(String server) { this.server = server; }
            public String getExportPath() { return exportPath; }
            public void setExportPath(String exportPath) { this.exportPath = exportPath; }
            public String getMountPoint() { return mountPoint; }
            public void setMountPoint(String mountPoint) { this.mountPoint = mountPoint; }
            public String getMountOptions() { return mountOptions; }
            public void setMountOptions(String mountOptions) { this.mountOptions = mountOptions; }
            public long getMaxFileSize() { return maxFileSize; }
            public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
            public int getConnectionTimeout() { return connectionTimeout; }
            public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
            public int getReadTimeout() { return readTimeout; }
            public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
            public int getRetryCount() { return retryCount; }
            public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        }
    }
    
    @Bean
    @ConfigurationProperties(prefix = "lyra.storage")
    public StorageProperties storageProperties() {
        return new StorageProperties();
    }
    
    /**
     * 主要存储服务Bean
     * 通过工厂获取配置的主要存储服务
     */
    @Bean
    @Primary
    public StorageService primaryStorageService(StorageServiceFactory factory) {
        StorageService service = factory.getPrimaryStorageService();
        log.info("配置主要存储服务: {}", service.getStorageType());
        return service;
    }
}