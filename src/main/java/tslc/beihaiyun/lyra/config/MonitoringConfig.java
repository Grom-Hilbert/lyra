package tslc.beihaiyun.lyra.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 监控配置类
 * 配置监控相关的bean和自定义指标
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
public class MonitoringConfig {

    @Value("${spring.application.name:lyra}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * 自定义MeterRegistry配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", applicationName)
                .commonTags("environment", activeProfile)
                .commonTags("version", getClass().getPackage().getImplementationVersion() != null 
                           ? getClass().getPackage().getImplementationVersion() : "unknown");
    }

    /**
     * 自定义Info端点信息贡献器
     */
    @Bean
    public InfoContributor lyraInfoContributor(Environment environment, LyraProperties lyraProperties) {
        return builder -> {
            Map<String, Object> lyraInfo = new HashMap<>();
            
            // 应用信息
            lyraInfo.put("name", applicationName);
            lyraInfo.put("description", "企业级云原生文档管理系统");
            lyraInfo.put("version", getClass().getPackage().getImplementationVersion() != null 
                        ? getClass().getPackage().getImplementationVersion() : "development");
            lyraInfo.put("buildTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 环境信息
            Map<String, Object> environmentInfo = new HashMap<>();
            environmentInfo.put("activeProfiles", environment.getActiveProfiles());
            environmentInfo.put("javaVersion", System.getProperty("java.version"));
            environmentInfo.put("javaVendor", System.getProperty("java.vendor"));
            environmentInfo.put("osName", System.getProperty("os.name"));
            environmentInfo.put("osVersion", System.getProperty("os.version"));
            environmentInfo.put("osArch", System.getProperty("os.arch"));
            lyraInfo.put("environment", environmentInfo);
            
            // 系统配置信息
            Map<String, Object> systemInfo = new HashMap<>();
            LyraProperties.SystemConfig systemConfig = lyraProperties.getSystem();
            systemInfo.put("maxUsers", systemConfig.getMaxUsers());
            systemInfo.put("defaultSpaceQuota", systemConfig.getDefaultSpaceQuota());
            systemInfo.put("versionControlEnabled", systemConfig.getEnableVersionControl());
            systemInfo.put("allowUserRegistration", systemConfig.getAllowUserRegistration());
            systemInfo.put("maintenanceMode", systemConfig.getMaintenanceMode());
            lyraInfo.put("system", systemInfo);
            
            // 存储配置信息
            Map<String, Object> storageInfo = new HashMap<>();
            LyraProperties.StorageConfig storageConfig = lyraProperties.getStorage();
            storageInfo.put("backend", storageConfig.getBackend());
            storageInfo.put("maxFileSize", storageConfig.getMaxFileSize());
            storageInfo.put("enableDeduplication", storageConfig.getEnableDeduplication());
            lyraInfo.put("storage", storageInfo);
            
            // 缓存配置信息
            Map<String, Object> cacheInfo = new HashMap<>();
            LyraProperties.CacheConfig cacheConfig = lyraProperties.getCache();
            cacheInfo.put("type", cacheConfig.getType());
            cacheInfo.put("ttl", cacheConfig.getTtl() + "s");
            cacheInfo.put("maxSize", cacheConfig.getMaxSize());
            cacheInfo.put("enableStats", cacheConfig.getEnableStats());
            lyraInfo.put("cache", cacheInfo);
            
            builder.withDetail("lyra", lyraInfo);
        };
    }

    /**
     * 运行时指标贡献器
     */
    @Bean
    public InfoContributor runtimeInfoContributor() {
        return builder -> {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> runtimeInfo = new HashMap<>();
            
            // 内存信息
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("total", runtime.totalMemory());
            memoryInfo.put("free", runtime.freeMemory());
            memoryInfo.put("used", runtime.totalMemory() - runtime.freeMemory());
            memoryInfo.put("max", runtime.maxMemory());
            memoryInfo.put("usagePercentage", 
                Math.round(((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100));
            runtimeInfo.put("memory", memoryInfo);
            
            // 处理器信息
            runtimeInfo.put("processors", runtime.availableProcessors());
            
            // 系统时间
            runtimeInfo.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            runtimeInfo.put("uptime", System.currentTimeMillis());
            
            builder.withDetail("runtime", runtimeInfo);
        };
    }
} 