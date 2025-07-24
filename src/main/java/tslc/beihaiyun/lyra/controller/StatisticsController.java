package tslc.beihaiyun.lyra.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.service.UserService;
import tslc.beihaiyun.lyra.service.MonitoringService;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

/**
 * 系统统计控制器
 * 提供系统统计和报告功能的REST API
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final UserService userService;
    private final MonitoringService monitoringService;
    private final UserRepository userRepository;
    private final FileEntityRepository fileEntityRepository;
    private final FolderRepository folderRepository;
    private final SpaceRepository spaceRepository;

    @Autowired
    public StatisticsController(
            UserService userService,
            MonitoringService monitoringService,
            UserRepository userRepository,
            FileEntityRepository fileEntityRepository,
            FolderRepository folderRepository,
            SpaceRepository spaceRepository) {
        this.userService = userService;
        this.monitoringService = monitoringService;
        this.userRepository = userRepository;
        this.fileEntityRepository = fileEntityRepository;
        this.folderRepository = folderRepository;
        this.spaceRepository = spaceRepository;
    }

    /**
     * 获取系统概览统计
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 用户统计
            UserService.UserStatistics userStats = userService.getUserStatistics();
            Map<String, Object> userOverview = new HashMap<>();
            userOverview.put("total", userStats.getTotalUsers());
            userOverview.put("active", userStats.getActiveUsers());
            userOverview.put("pending", userStats.getPendingUsers());
            userOverview.put("disabled", userStats.getDisabledUsers());
            userOverview.put("locked", userStats.getLockedUsers());
            userOverview.put("emailVerified", userStats.getEmailVerifiedUsers());
            overview.put("users", userOverview);
            
            // 文件统计
            Map<String, Object> fileOverview = new HashMap<>();
            fileOverview.put("totalFiles", fileEntityRepository.count());
            fileOverview.put("totalSize", fileEntityRepository.getTotalFileSize());
            fileOverview.put("totalFolders", folderRepository.count());
            fileOverview.put("totalSpaces", spaceRepository.count());
            overview.put("files", fileOverview);
            
            // 存储统计
            Map<String, Object> storageOverview = new HashMap<>();
            storageOverview.put("totalSpace", monitoringService.getStorageTotalSpace());
            storageOverview.put("usedSpace", monitoringService.getStorageUsedSpace());
            storageOverview.put("freeSpace", monitoringService.getStorageFreeSpace());
            storageOverview.put("usagePercentage", monitoringService.getStorageUsagePercentage());
            overview.put("storage", storageOverview);
            
            // 系统信息
            Map<String, Object> systemOverview = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            systemOverview.put("javaVersion", System.getProperty("java.version"));
            systemOverview.put("osName", System.getProperty("os.name"));
            systemOverview.put("processors", runtime.availableProcessors());
            systemOverview.put("maxMemory", runtime.maxMemory());
            systemOverview.put("totalMemory", runtime.totalMemory());
            systemOverview.put("freeMemory", runtime.freeMemory());
            systemOverview.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            systemOverview.put("memoryUsagePercentage", 
                Math.round(((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100));
            overview.put("system", systemOverview);
            
            // 生成时间
            overview.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("生成系统概览统计");
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("获取系统概览统计失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取系统概览统计失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户详细统计
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 基础用户统计
            UserService.UserStatistics userStats = userService.getUserStatistics();
            stats.put("basicStats", Map.of(
                "total", userStats.getTotalUsers(),
                "active", userStats.getActiveUsers(),
                "pending", userStats.getPendingUsers(),
                "disabled", userStats.getDisabledUsers(),
                "locked", userStats.getLockedUsers(),
                "deactivated", userStats.getDeactivatedUsers(),
                "enabled", userStats.getEnabledUsers(),
                "emailVerified", userStats.getEmailVerifiedUsers()
            ));
            
            // 按状态分组统计
            List<Object[]> statusCounts = userRepository.countByStatus();
            Map<String, Long> statusStats = new HashMap<>();
            for (Object[] row : statusCounts) {
                User.UserStatus status = (User.UserStatus) row[0];
                Long count = (Long) row[1];
                statusStats.put(status.name(), count);
            }
            stats.put("statusDistribution", statusStats);
            
            // 存储使用统计
            Map<String, Object> storageStats = new HashMap<>();
            List<User> highUsageUsers = userService.findUsersWithHighStorageUsage(0.8); // 使用率超过80%
            storageStats.put("highUsageUsers", highUsageUsers.size());
            storageStats.put("highUsageUsersList", highUsageUsers.stream()
                .map(user -> Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "usagePercentage", Math.round(userService.calculateStorageUsageRatio(user) * 100),
                    "storageUsed", user.getStorageUsed(),
                    "storageQuota", user.getStorageQuota()
                ))
                .limit(10) // 只返回前10个
                .toList());
            
            // 计算总配额使用情况
            Long totalQuota = userRepository.getTotalStorageQuota();
            Long totalUsed = userRepository.getTotalStorageUsed();
            storageStats.put("totalQuota", totalQuota);
            storageStats.put("totalUsed", totalUsed);
            storageStats.put("totalUsagePercentage", 
                totalQuota > 0 ? Math.round(((double) totalUsed / totalQuota) * 100) : 0);
            
            stats.put("storageUsage", storageStats);
            
            // 生成时间
            stats.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("生成用户详细统计");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取用户统计失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取用户统计失败：" + e.getMessage()));
        }
    }

    /**
     * 获取文件和存储统计
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> getFileStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 基础文件统计
            Map<String, Object> basicStats = new HashMap<>();
            basicStats.put("totalFiles", fileEntityRepository.count());
            basicStats.put("totalFolders", folderRepository.count());
            basicStats.put("totalSpaces", spaceRepository.count());
            basicStats.put("totalFileSize", fileEntityRepository.getTotalFileSize());
            stats.put("basicStats", basicStats);
            
            // 文件类型分布统计
            List<Object[]> fileTypeCounts = fileEntityRepository.countByFileType();
            Map<String, Long> fileTypeStats = new HashMap<>();
            for (Object[] row : fileTypeCounts) {
                String fileType = (String) row[0];
                Long count = (Long) row[1];
                fileTypeStats.put(fileType != null ? fileType : "unknown", count);
            }
            stats.put("fileTypeDistribution", fileTypeStats);
            
            // 文件大小分布统计
            List<Object[]> fileSizeRanges = fileEntityRepository.countByFileSizeRange();
            Map<String, Long> sizeRangeStats = new HashMap<>();
            for (Object[] row : fileSizeRanges) {
                String sizeRange = (String) row[0];
                Long count = (Long) row[1];
                sizeRangeStats.put(sizeRange, count);
            }
            stats.put("fileSizeDistribution", sizeRangeStats);
            
            // 用户文件统计（Top 10）
            List<Object[]> userFileCounts = fileEntityRepository.countFilesByUser();
            List<Map<String, Object>> topUsers = userFileCounts.stream()
                .limit(10)
                .map(row -> Map.of(
                    "userId", row[0],
                    "username", row[1],
                    "fileCount", row[2],
                    "totalSize", row[3]
                ))
                .toList();
            stats.put("topUsersByFiles", topUsers);
            
            // 空间统计
            List<Object[]> spaceCounts = spaceRepository.getSpaceStatistics();
            List<Map<String, Object>> spaceStats = spaceCounts.stream()
                .map(row -> Map.of(
                    "spaceId", row[0],
                    "spaceName", row[1],
                    "spaceType", row[2],
                    "fileCount", row[3],
                    "totalSize", row[4]
                ))
                .toList();
            stats.put("spaceStatistics", spaceStats);
            
            // 生成时间
            stats.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("生成文件和存储统计");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取文件统计失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取文件统计失败：" + e.getMessage()));
        }
    }

    /**
     * 获取系统性能统计
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // JVM性能统计
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmStats = new HashMap<>();
            jvmStats.put("maxMemory", runtime.maxMemory());
            jvmStats.put("totalMemory", runtime.totalMemory());
            jvmStats.put("freeMemory", runtime.freeMemory());
            jvmStats.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmStats.put("memoryUsagePercentage", 
                Math.round(((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100));
            jvmStats.put("availableProcessors", runtime.availableProcessors());
            stats.put("jvm", jvmStats);
            
            // 存储性能统计
            Map<String, Object> storageStats = new HashMap<>();
            storageStats.put("totalSpace", monitoringService.getStorageTotalSpace());
            storageStats.put("usedSpace", monitoringService.getStorageUsedSpace());
            storageStats.put("freeSpace", monitoringService.getStorageFreeSpace());
            storageStats.put("usagePercentage", monitoringService.getStorageUsagePercentage());
            stats.put("storage", storageStats);
            
            // 系统信息
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("javaVendor", System.getProperty("java.vendor"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("osVersion", System.getProperty("os.version"));
            systemInfo.put("osArch", System.getProperty("os.arch"));
            systemInfo.put("uptime", System.currentTimeMillis());
            stats.put("system", systemInfo);
            
            // 生成时间
            stats.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("生成系统性能统计");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取性能统计失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取性能统计失败：" + e.getMessage()));
        }
    }

    /**
     * 生成系统报告
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generateSystemReport(
            @RequestParam(defaultValue = "overview") String type) {
        try {
            Map<String, Object> report = new HashMap<>();
            
            // 报告头信息
            Map<String, Object> header = new HashMap<>();
            header.put("reportType", type);
            header.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            header.put("version", "1.0.0");
            report.put("header", header);
            
            switch (type.toLowerCase()) {
                case "overview":
                    // 综合概览报告
                    report.put("users", userService.getUserStatistics());
                    report.put("files", Map.of(
                        "totalFiles", fileEntityRepository.count(),
                        "totalSize", fileEntityRepository.getTotalFileSize(),
                        "totalFolders", folderRepository.count(),
                        "totalSpaces", spaceRepository.count()
                    ));
                    report.put("storage", Map.of(
                        "totalSpace", monitoringService.getStorageTotalSpace(),
                        "usedSpace", monitoringService.getStorageUsedSpace(),
                        "freeSpace", monitoringService.getStorageFreeSpace(),
                        "usagePercentage", monitoringService.getStorageUsagePercentage()
                    ));
                    break;
                    
                case "users":
                    // 用户详细报告
                    report.put("statistics", userService.getUserStatistics());
                    report.put("statusDistribution", userRepository.countByStatus());
                    report.put("highStorageUsers", userService.findUsersWithHighStorageUsage(0.8));
                    break;
                    
                case "storage":
                    // 存储使用报告
                    report.put("totalQuota", userRepository.getTotalStorageQuota());
                    report.put("totalUsed", userRepository.getTotalStorageUsed());
                    report.put("fileStatistics", Map.of(
                        "totalFiles", fileEntityRepository.count(),
                        "totalSize", fileEntityRepository.getTotalFileSize(),
                        "fileTypes", fileEntityRepository.countByFileType()
                    ));
                    report.put("topUsers", fileEntityRepository.countFilesByUser());
                    break;
                    
                case "performance":
                    // 性能报告
                    Runtime runtime = Runtime.getRuntime();
                    report.put("memory", Map.of(
                        "max", runtime.maxMemory(),
                        "total", runtime.totalMemory(),
                        "free", runtime.freeMemory(),
                        "used", runtime.totalMemory() - runtime.freeMemory()
                    ));
                    report.put("system", Map.of(
                        "processors", runtime.availableProcessors(),
                        "javaVersion", System.getProperty("java.version"),
                        "osName", System.getProperty("os.name")
                    ));
                    break;
                    
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "不支持的报告类型: " + type));
            }
            
            logger.info("生成系统报告: {}", type);
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("生成系统报告失败: {}", type, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "生成系统报告失败：" + e.getMessage()));
        }
    }

    /**
     * 获取存储使用趋势（简化版，实际应该基于历史数据）
     */
    @GetMapping("/trends/storage")
    public ResponseEntity<Map<String, Object>> getStorageTrends() {
        try {
            Map<String, Object> trends = new HashMap<>();
            
            // 当前存储使用情况
            Map<String, Object> current = new HashMap<>();
            current.put("totalSpace", monitoringService.getStorageTotalSpace());
            current.put("usedSpace", monitoringService.getStorageUsedSpace());
            current.put("freeSpace", monitoringService.getStorageFreeSpace());
            current.put("usagePercentage", monitoringService.getStorageUsagePercentage());
            current.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            trends.put("current", current);
            
            // 用户存储分布
            List<User> highUsageUsers = userService.findUsersWithHighStorageUsage(0.7);
            trends.put("highUsageUserCount", highUsageUsers.size());
            trends.put("totalUsers", userRepository.count());
            trends.put("warningThreshold", 70); // 70%预警阈值
            
            // 文件增长趋势（简化）
            Long totalFiles = fileEntityRepository.count();
            Long totalSize = fileEntityRepository.getTotalFileSize();
            trends.put("growth", Map.of(
                "totalFiles", totalFiles,
                "totalSize", totalSize,
                "averageFileSize", totalFiles > 0 ? totalSize / totalFiles : 0
            ));
            
            logger.debug("生成存储使用趋势");
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            logger.error("获取存储趋势失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取存储趋势失败：" + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态摘要
     */
    @GetMapping("/health-summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 整体健康状态
            boolean isHealthy = true;
            List<String> warnings = new java.util.ArrayList<>();
            List<String> errors = new java.util.ArrayList<>();
            
            // 检查存储使用率
            double storageUsage = monitoringService.getStorageUsagePercentage();
            if (storageUsage > 95) {
                isHealthy = false;
                errors.add("存储空间严重不足（使用率: " + Math.round(storageUsage) + "%）");
            } else if (storageUsage > 85) {
                warnings.add("存储空间使用率较高（使用率: " + Math.round(storageUsage) + "%）");
            }
            
            // 检查内存使用率
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100;
            if (memoryUsage > 90) {
                isHealthy = false;
                errors.add("内存使用率过高（使用率: " + Math.round(memoryUsage) + "%）");
            } else if (memoryUsage > 80) {
                warnings.add("内存使用率较高（使用率: " + Math.round(memoryUsage) + "%）");
            }
            
            // 检查用户状态
            UserService.UserStatistics userStats = userService.getUserStatistics();
            if (userStats.getLockedUsers() > 0) {
                warnings.add("系统中有 " + userStats.getLockedUsers() + " 个被锁定的用户账户");
            }
            
            health.put("healthy", isHealthy);
            health.put("warnings", warnings);
            health.put("errors", errors);
            health.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 系统指标摘要
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("storageUsage", Math.round(storageUsage));
            metrics.put("memoryUsage", Math.round(memoryUsage));
            metrics.put("totalUsers", userStats.getTotalUsers());
            metrics.put("activeUsers", userStats.getActiveUsers());
            metrics.put("totalFiles", fileEntityRepository.count());
            health.put("metrics", metrics);
            
            logger.debug("生成系统健康状态摘要");
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("获取系统健康状态失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取系统健康状态失败：" + e.getMessage()));
        }
    }
} 