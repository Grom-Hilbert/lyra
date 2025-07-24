package tslc.beihaiyun.lyra.health;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

/**
 * 业务指标健康检查器
 * 收集和报告系统业务指标
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Component
public class BusinessMetricsHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(BusinessMetricsHealthIndicator.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileEntityRepository fileEntityRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private LyraProperties lyraProperties;

    @Override
    public Health health() {
        try {
            Map<String, Object> businessMetrics = collectBusinessMetrics();
            
            // 检查关键业务指标
            Health.Builder healthBuilder = Health.up();
            
            // 添加业务指标详情
            healthBuilder.withDetails(businessMetrics);
            
            // 检查业务健康状况
            checkBusinessHealth(businessMetrics, healthBuilder);
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            log.error("业务指标收集失败", e);
            return Health.down()
                    .withDetail("error", "业务指标收集失败")
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }

    /**
     * 收集业务指标
     */
    private Map<String, Object> collectBusinessMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // 用户相关指标
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByEnabledTrue();
            long adminUsers = userRepository.countAdminUsers();
            
            Map<String, Object> userMetrics = new HashMap<>();
            userMetrics.put("total", totalUsers);
            userMetrics.put("active", activeUsers);
            userMetrics.put("admins", adminUsers);
            userMetrics.put("disabled", totalUsers - activeUsers);
            metrics.put("users", userMetrics);

            // 文件相关指标
            long totalFiles = fileEntityRepository.count();
            long totalSize = fileEntityRepository.getTotalFileSize();
            
            Map<String, Object> fileMetrics = new HashMap<>();
            fileMetrics.put("totalCount", totalFiles);
            fileMetrics.put("totalSize", formatBytes(totalSize));
            fileMetrics.put("totalSizeBytes", totalSize);
            fileMetrics.put("averageFileSize", totalFiles > 0 ? formatBytes(totalSize / totalFiles) : "0 B");
            metrics.put("files", fileMetrics);

            // 空间相关指标
            long totalSpaces = spaceRepository.count();
            long personalSpaces = spaceRepository.countByType(Space.SpaceType.PERSONAL);
            long enterpriseSpaces = spaceRepository.countByType(Space.SpaceType.ENTERPRISE);
            
            Map<String, Object> spaceMetrics = new HashMap<>();
            spaceMetrics.put("total", totalSpaces);
            spaceMetrics.put("personal", personalSpaces);
            spaceMetrics.put("enterprise", enterpriseSpaces);
            metrics.put("spaces", spaceMetrics);

            // 存储相关指标
            String basePath = lyraProperties.getStorage().getBasePath();
            File storageDir = new File(basePath);
            long totalSpace = storageDir.getTotalSpace();
            long freeSpace = storageDir.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            Map<String, Object> storageMetrics = new HashMap<>();
            storageMetrics.put("totalSpace", formatBytes(totalSpace));
            storageMetrics.put("usedSpace", formatBytes(usedSpace));
            storageMetrics.put("freeSpace", formatBytes(freeSpace));
            storageMetrics.put("usagePercentage", String.format("%.2f%%", 
                totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0));
            metrics.put("storage", storageMetrics);

            // 系统配置指标
            Map<String, Object> configMetrics = new HashMap<>();
            configMetrics.put("maxUsers", lyraProperties.getSystem().getMaxUsers());
            configMetrics.put("defaultSpaceQuota", lyraProperties.getSystem().getDefaultSpaceQuota());
                                         configMetrics.put("versionControlEnabled", lyraProperties.getSystem().getEnableVersionControl());
                             configMetrics.put("maintenanceMode", lyraProperties.getSystem().getMaintenanceMode());
            metrics.put("configuration", configMetrics);

            log.debug("业务指标收集完成: 用户{}个, 文件{}个, 空间{}个", totalUsers, totalFiles, totalSpaces);

        } catch (Exception e) {
            log.error("收集业务指标时发生错误", e);
            metrics.put("collectionError", e.getMessage());
        }

        return metrics;
    }

    /**
     * 检查业务健康状况
     */
    private void checkBusinessHealth(Map<String, Object> metrics, Health.Builder healthBuilder) {
        try {
            // 检查用户数量是否达到限制
            @SuppressWarnings("unchecked")
            Map<String, Object> userMetrics = (Map<String, Object>) metrics.get("users");
            if (userMetrics != null) {
                Long totalUsers = (Long) userMetrics.get("total");
                Integer maxUsers = lyraProperties.getSystem().getMaxUsers();
                
                if (totalUsers != null && maxUsers != null && totalUsers >= maxUsers) {
                    healthBuilder.down();
                    healthBuilder.withDetail("userLimit", "用户数量已达到系统限制");
                } else if (totalUsers != null && maxUsers != null && totalUsers >= maxUsers * 0.9) {
                    healthBuilder.withDetail("userWarning", "用户数量接近系统限制");
                }
            }

            // 检查存储使用情况
            @SuppressWarnings("unchecked")
            Map<String, Object> storageMetrics = (Map<String, Object>) metrics.get("storage");
            if (storageMetrics != null) {
                String usagePercentageStr = (String) storageMetrics.get("usagePercentage");
                if (usagePercentageStr != null) {
                    double usagePercentage = Double.parseDouble(usagePercentageStr.replace("%", ""));
                    if (usagePercentage > 95) {
                        healthBuilder.down();
                        healthBuilder.withDetail("storageWarning", "存储空间严重不足");
                    } else if (usagePercentage > 85) {
                        healthBuilder.withDetail("storageWarning", "存储空间使用率较高");
                    }
                }
            }

            // 检查是否处于维护模式
            @SuppressWarnings("unchecked")
            Map<String, Object> configMetrics = (Map<String, Object>) metrics.get("configuration");
            if (configMetrics != null) {
                Boolean maintenanceMode = (Boolean) configMetrics.get("maintenanceMode");
                if (maintenanceMode != null && maintenanceMode) {
                    healthBuilder.withDetail("maintenanceMode", "系统当前处于维护模式");
                }
            }

        } catch (Exception e) {
            log.warn("检查业务健康状况时发生错误", e);
            healthBuilder.withDetail("healthCheckWarning", "业务健康检查部分失败");
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