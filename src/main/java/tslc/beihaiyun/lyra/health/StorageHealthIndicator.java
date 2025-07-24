package tslc.beihaiyun.lyra.health;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * 文件存储健康检查器
 * 检查文件存储系统的健康状态
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(StorageHealthIndicator.class);

    @Autowired
    private LyraProperties lyraProperties;

    @Override
    public Health health() {
        try {
            return checkStorageHealth();
        } catch (Exception e) {
            log.error("存储健康检查失败", e);
            return Health.down()
                    .withDetail("error", "存储健康检查失败")
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }

    /**
     * 检查存储系统健康状态
     */
    private Health checkStorageHealth() throws IOException {
        String basePath = lyraProperties.getStorage().getBasePath();
        String tempPath = lyraProperties.getStorage().getTempPath();

        Health.Builder healthBuilder = Health.up();

        // 检查主存储路径
        checkPath(basePath, "basePath", healthBuilder);

        // 检查临时存储路径
        checkPath(tempPath, "tempPath", healthBuilder);

        // 检查磁盘空间
        checkDiskSpace(basePath, healthBuilder);

        return healthBuilder.build();
    }

    /**
     * 检查单个路径的健康状态
     */
    private void checkPath(String pathStr, String pathType, Health.Builder healthBuilder) throws IOException {
        Path path = Paths.get(pathStr);
        
        // 检查路径是否存在
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("创建存储目录: {}", path);
            } catch (IOException e) {
                healthBuilder.down();
                healthBuilder.withDetail(pathType + ".error", "无法创建目录: " + e.getMessage());
                return;
            }
        }

        // 检查是否为目录
        if (!Files.isDirectory(path)) {
            healthBuilder.down();
            healthBuilder.withDetail(pathType + ".error", "路径不是目录");
            return;
        }

        // 检查读权限
        if (!Files.isReadable(path)) {
            healthBuilder.down();
            healthBuilder.withDetail(pathType + ".error", "路径不可读");
            return;
        }

        // 检查写权限
        if (!Files.isWritable(path)) {
            healthBuilder.down();
            healthBuilder.withDetail(pathType + ".error", "路径不可写");
            return;
        }

        // 测试写入权限
        try {
            Path testFile = path.resolve(".health-check-" + System.currentTimeMillis());
            Files.createFile(testFile);
            Files.delete(testFile);
        } catch (IOException e) {
            healthBuilder.down();
            healthBuilder.withDetail(pathType + ".error", "无法执行写入测试: " + e.getMessage());
            return;
        }

        healthBuilder.withDetail(pathType + ".status", "OK");
        healthBuilder.withDetail(pathType + ".path", pathStr);
    }

    /**
     * 检查磁盘空间
     */
    private void checkDiskSpace(String pathStr, Health.Builder healthBuilder) {
        try {
            File path = new File(pathStr);
            long totalSpace = path.getTotalSpace();
            long freeSpace = path.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double usagePercentage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;

            healthBuilder.withDetail("diskSpace.total", formatBytes(totalSpace));
            healthBuilder.withDetail("diskSpace.free", formatBytes(freeSpace));
            healthBuilder.withDetail("diskSpace.used", formatBytes(usedSpace));
            healthBuilder.withDetail("diskSpace.usagePercentage", String.format("%.2f%%", usagePercentage));

            // 磁盘使用率超过90%时报警
            if (usagePercentage > 90) {
                healthBuilder.down();
                healthBuilder.withDetail("diskSpace.warning", "磁盘使用率过高");
            } else if (usagePercentage > 80) {
                healthBuilder.withDetail("diskSpace.warning", "磁盘使用率偏高");
            }

        } catch (Exception e) {
            log.warn("获取磁盘空间信息失败", e);
            healthBuilder.withDetail("diskSpace.error", "无法获取磁盘空间信息");
        }
    }

    /**
     * 格式化字节数为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
} 