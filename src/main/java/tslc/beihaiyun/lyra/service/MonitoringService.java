package tslc.beihaiyun.lyra.service;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

/**
 * 监控服务
 * 收集和暴露系统指标给Prometheus
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileEntityRepository fileEntityRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private LyraProperties lyraProperties;

    // 业务指标缓存
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong totalSpaces = new AtomicLong(0);
    private final AtomicLong totalFileSize = new AtomicLong(0);

    // 性能指标
    private Counter fileUploadCounter;
    private Counter fileDownloadCounter;
    private Timer fileOperationTimer;
    private Counter authenticationCounter;
    private Counter authenticationFailureCounter;

    /**
     * 应用启动后初始化监控指标
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMetrics() {
        log.info("初始化监控指标...");

        // 注册业务指标
        registerBusinessMetrics();

        // 注册性能指标
        registerPerformanceMetrics();

        // 初始化指标数据
        updateBusinessMetrics();

        log.info("监控指标初始化完成");
    }

    /**
     * 注册业务指标
     */
    private void registerBusinessMetrics() {
        // 用户相关指标
        Gauge.builder("lyra.users.total", totalUsers, AtomicLong::get)
                .description("系统总用户数")
                .register(meterRegistry);

        Gauge.builder("lyra.users.active", activeUsers, AtomicLong::get)
                .description("活跃用户数")
                .register(meterRegistry);

        // 文件相关指标
        Gauge.builder("lyra.files.total", totalFiles, AtomicLong::get)
                .description("系统总文件数")
                .register(meterRegistry);

        Gauge.builder("lyra.files.size.bytes", totalFileSize, AtomicLong::get)
                .description("文件总大小（字节）")
                .register(meterRegistry);

        // 空间相关指标
        Gauge.builder("lyra.spaces.total", totalSpaces, AtomicLong::get)
                .description("系统总空间数")
                .register(meterRegistry);

        // 存储空间指标
        Gauge.builder("lyra.storage.total.bytes", this, MonitoringService::getStorageTotalSpace)
                .description("存储总容量（字节）")
                .register(meterRegistry);

        Gauge.builder("lyra.storage.used.bytes", this, MonitoringService::getStorageUsedSpace)
                .description("已使用存储空间（字节）")
                .register(meterRegistry);

        Gauge.builder("lyra.storage.free.bytes", this, MonitoringService::getStorageFreeSpace)
                .description("可用存储空间（字节）")
                .register(meterRegistry);

        Gauge.builder("lyra.storage.usage.percentage", this, MonitoringService::getStorageUsagePercentage)
                .description("存储空间使用率")
                .register(meterRegistry);

        // 系统配置指标
        Gauge.builder("lyra.config.max.users", lyraProperties.getSystem(), s -> s.getMaxUsers())
                .description("系统最大用户数限制")
                .register(meterRegistry);

        Gauge.builder("lyra.config.maintenance.mode", lyraProperties.getSystem(), s -> s.getMaintenanceMode() ? 1 : 0)
                .description("维护模式状态（1=开启，0=关闭）")
                .register(meterRegistry);
    }

    /**
     * 注册性能指标
     */
    private void registerPerformanceMetrics() {
        // 文件操作计数器
        fileUploadCounter = Counter.builder("lyra.files.uploads.total")
                .description("文件上传次数")
                .register(meterRegistry);

        fileDownloadCounter = Counter.builder("lyra.files.downloads.total")
                .description("文件下载次数")
                .register(meterRegistry);

        // 文件操作计时器
        fileOperationTimer = Timer.builder("lyra.files.operations.duration")
                .description("文件操作耗时")
                .register(meterRegistry);

        // 认证相关计数器
        authenticationCounter = Counter.builder("lyra.auth.attempts.total")
                .description("认证尝试次数")
                .register(meterRegistry);

        authenticationFailureCounter = Counter.builder("lyra.auth.failures.total")
                .description("认证失败次数")
                .register(meterRegistry);
    }

    /**
     * 定期更新业务指标
     */
    @Scheduled(fixedRate = 60000) // 每分钟更新一次
    public void updateBusinessMetrics() {
        try {
            log.debug("更新业务指标...");

            // 更新用户指标
            totalUsers.set(userRepository.count());
            activeUsers.set(userRepository.countByEnabledTrue());

            // 更新文件指标
            totalFiles.set(fileEntityRepository.count());
            totalFileSize.set(fileEntityRepository.getTotalFileSize());

            // 更新空间指标
            totalSpaces.set(spaceRepository.count());

            log.debug("业务指标更新完成: 用户{}个, 文件{}个, 空间{}个", 
                     totalUsers.get(), totalFiles.get(), totalSpaces.get());

        } catch (Exception e) {
            log.error("更新业务指标失败", e);
        }
    }

    /**
     * 记录文件上传
     */
    public void recordFileUpload() {
        fileUploadCounter.increment();
    }

    /**
     * 记录文件下载
     */
    public void recordFileDownload() {
        fileDownloadCounter.increment();
    }

    /**
     * 记录文件操作时间
     */
    public Timer.Sample startFileOperationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 记录认证尝试
     */
    public void recordAuthenticationAttempt() {
        authenticationCounter.increment();
    }

    /**
     * 记录认证失败
     */
    public void recordAuthenticationFailure() {
        authenticationFailureCounter.increment();
    }

    /**
     * 获取存储总容量
     */
    public double getStorageTotalSpace() {
        try {
            String basePath = lyraProperties.getStorage().getBasePath();
            File storageDir = new File(basePath);
            return storageDir.getTotalSpace();
        } catch (Exception e) {
            log.warn("获取存储总容量失败", e);
            return 0;
        }
    }

    /**
     * 获取已使用存储空间
     */
    public double getStorageUsedSpace() {
        try {
            String basePath = lyraProperties.getStorage().getBasePath();
            File storageDir = new File(basePath);
            return storageDir.getTotalSpace() - storageDir.getFreeSpace();
        } catch (Exception e) {
            log.warn("获取已使用存储空间失败", e);
            return 0;
        }
    }

    /**
     * 获取可用存储空间
     */
    public double getStorageFreeSpace() {
        try {
            String basePath = lyraProperties.getStorage().getBasePath();
            File storageDir = new File(basePath);
            return storageDir.getFreeSpace();
        } catch (Exception e) {
            log.warn("获取可用存储空间失败", e);
            return 0;
        }
    }

    /**
     * 获取存储使用率
     */
    public double getStorageUsagePercentage() {
        try {
            String basePath = lyraProperties.getStorage().getBasePath();
            File storageDir = new File(basePath);
            long totalSpace = storageDir.getTotalSpace();
            long freeSpace = storageDir.getFreeSpace();
            
            if (totalSpace > 0) {
                return ((double) (totalSpace - freeSpace) / totalSpace) * 100;
            }
            return 0;
        } catch (Exception e) {
            log.warn("获取存储使用率失败", e);
            return 0;
        }
    }
} 