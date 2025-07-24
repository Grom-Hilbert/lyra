package tslc.beihaiyun.lyra.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.micrometer.core.instrument.MeterRegistry;
import tslc.beihaiyun.lyra.service.MonitoringService;

/**
 * 监控功能集成测试
 * 测试健康检查器和指标收集功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@SpringJUnitConfig
@TestPropertySource(properties = {
    "lyra.cache.type=memory",
    "management.endpoints.web.exposure.include=health,metrics,info,prometheus",
    "management.metrics.export.prometheus.enabled=true"
})
public class MonitoringIntegrationTest {

    @Autowired
    private StorageHealthIndicator storageHealthIndicator;

    @Autowired
    private BusinessMetricsHealthIndicator businessMetricsHealthIndicator;

    @Autowired
    private CacheHealthIndicator cacheHealthIndicator;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void testStorageHealthIndicator() {
        // 测试存储健康检查器
        Health health = storageHealthIndicator.health();
        
        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isIn("UP", "DOWN");
        assertThat(health.getDetails()).isNotNull();
        assertThat(health.getDetails()).containsKey("basePath.status");
        assertThat(health.getDetails()).containsKey("tempPath.status");
        assertThat(health.getDetails()).containsKey("diskSpace.total");
    }

    @Test
    public void testBusinessMetricsHealthIndicator() {
        // 测试业务指标健康检查器
        Health health = businessMetricsHealthIndicator.health();
        
        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isIn("UP", "DOWN");
        assertThat(health.getDetails()).isNotNull();
        
        // 检查是否有业务指标或错误信息
        boolean hasMetrics = health.getDetails().containsKey("users") && 
                           health.getDetails().containsKey("files") && 
                           health.getDetails().containsKey("spaces") && 
                           health.getDetails().containsKey("storage") && 
                           health.getDetails().containsKey("configuration");
        
        boolean hasError = health.getDetails().containsKey("collectionError");
        
        // 如果没有业务指标，至少应该有错误信息
        assertThat(hasMetrics || hasError).isTrue();
    }

    @Test
    public void testCacheHealthIndicator() {
        // 测试缓存健康检查器
        Health health = cacheHealthIndicator.health();
        
        assertThat(health).isNotNull();
        assertThat(health.getStatus().getCode()).isIn("UP", "DOWN");
        assertThat(health.getDetails()).isNotNull();
        assertThat(health.getDetails()).containsKey("cacheType");
        assertThat(health.getDetails()).containsKey("ttl");
        assertThat(health.getDetails()).containsKey("maxSize");
        assertThat(health.getDetails()).containsKey("cacheNames");
        assertThat(health.getDetails()).containsKey("cacheCount");
    }

    @Test
    public void testMonitoringServiceInitialization() {
        // 测试监控服务初始化
        assertThat(monitoringService).isNotNull();
        
        // 验证指标注册表不为空
        assertThat(meterRegistry).isNotNull();
        
        // 验证有注册的指标
        assertThat(meterRegistry.getMeters()).isNotEmpty();
        
        // 验证存在业务相关的指标
        boolean hasLyraMetrics = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().startsWith("lyra."));
        assertThat(hasLyraMetrics).isTrue();
    }

    @Test
    public void testMetricsAvailability() {
        // 测试指标可用性
        assertThat(meterRegistry.find("lyra.users.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.users.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.files.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.files.size.bytes").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.spaces.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.storage.total.bytes").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.storage.used.bytes").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.storage.free.bytes").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.storage.usage.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.config.max.users").gauge()).isNotNull();
        assertThat(meterRegistry.find("lyra.config.maintenance.mode").gauge()).isNotNull();
    }

    @Test
    public void testHealthIndicatorsNotNull() {
        // 测试所有健康检查器都已正确注入
        assertThat(storageHealthIndicator).isNotNull();
        assertThat(businessMetricsHealthIndicator).isNotNull();
        assertThat(cacheHealthIndicator).isNotNull();
    }

    @Test
    public void testHealthIndicatorsReturnValidStatus() {
        // 测试所有健康检查器返回有效状态
        Health storageHealth = storageHealthIndicator.health();
        Health businessHealth = businessMetricsHealthIndicator.health();
        Health cacheHealth = cacheHealthIndicator.health();
        
        // 验证状态不为空且为有效值
        assertThat(storageHealth.getStatus()).isNotNull();
        assertThat(businessHealth.getStatus()).isNotNull();
        assertThat(cacheHealth.getStatus()).isNotNull();
        
        assertThat(storageHealth.getStatus().getCode()).isIn("UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN");
        assertThat(businessHealth.getStatus().getCode()).isIn("UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN");
        assertThat(cacheHealth.getStatus().getCode()).isIn("UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN");
    }

    @Test
    public void testMetricValues() {
        // 测试指标值的合理性
        Double totalUsers = meterRegistry.find("lyra.users.total").gauge().value();
        Double totalFiles = meterRegistry.find("lyra.files.total").gauge().value();
        Double totalSpaces = meterRegistry.find("lyra.spaces.total").gauge().value();
        Double maxUsers = meterRegistry.find("lyra.config.max.users").gauge().value();
        Double maintenanceMode = meterRegistry.find("lyra.config.maintenance.mode").gauge().value();
        
        // 验证指标值不为空且在合理范围内
        assertThat(totalUsers).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(totalFiles).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(totalSpaces).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(maxUsers).isNotNull().isGreaterThan(0);
        assertThat(maintenanceMode).isNotNull().isIn(0.0, 1.0);
    }
} 