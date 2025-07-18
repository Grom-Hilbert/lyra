package tslc.beihaiyun.lyra.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tslc.beihaiyun.lyra.storage.impl.NFSStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * NFS存储健康检查指示器
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "lyra.storage.primary", havingValue = "nfs")
public class NFSHealthIndicator implements HealthIndicator {
    
    private final StorageServiceFactory storageServiceFactory;
    
    @Autowired
    public NFSHealthIndicator(StorageServiceFactory storageServiceFactory) {
        this.storageServiceFactory = storageServiceFactory;
    }
    
    @Override
    public Health health() {
        try {
            Optional<StorageService> nfsService = storageServiceFactory.getStorageService(StorageType.NFS);
            
            if (nfsService.isEmpty()) {
                return Health.down()
                    .withDetail("error", "NFS存储服务不可用")
                    .build();
            }
            
            StorageStats stats = nfsService.get().getStats();
            
            if (!stats.isHealthy()) {
                return Health.down()
                    .withDetail("error", stats.getHealthMessage())
                    .withDetail("storage-type", "NFS")
                    .build();
            }
            
            // 执行基本的读写测试
            String testKey = ".health-check-" + System.currentTimeMillis();
            try {
                // 测试写入
                StorageResult storeResult = nfsService.get().store(
                    testKey, 
                    new java.io.ByteArrayInputStream("health-check".getBytes()),
                    "health-check".length(),
                    "text/plain"
                );
                
                if (!storeResult.isSuccess()) {
                    return Health.down()
                        .withDetail("error", "NFS写入测试失败")
                        .build();
                }
                
                // 测试读取
                var retrieveResult = nfsService.get().retrieve(testKey);
                if (retrieveResult.isEmpty()) {
                    return Health.down()
                        .withDetail("error", "NFS读取测试失败")
                        .build();
                }
                
                // 清理测试文件
                nfsService.get().delete(testKey);
                
                return Health.up()
                    .withDetail("storage-type", "NFS")
                    .withDetail("total-space", formatBytes(stats.getTotalSpace()))
                    .withDetail("used-space", formatBytes(stats.getUsedSpace()))
                    .withDetail("available-space", formatBytes(stats.getAvailableSpace()))
                    .withDetail("file-count", stats.getFileCount())
                    .withDetail("usage-percentage", String.format("%.1f%%", stats.getUsagePercentage()))
                    .build();
                    
            } catch (Exception e) {
                log.error("NFS健康检查测试失败", e);
                return Health.down()
                    .withDetail("error", "NFS健康检查测试失败: " + e.getMessage())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("NFS健康检查失败", e);
            return Health.down()
                .withDetail("error", "NFS健康检查失败: " + e.getMessage())
                .build();
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}