package tslc.beihaiyun.lyra.database;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库健康状态信息
 * 用于监控和报告数据库连接的健康状况
 */
@Data
@Builder
public class HealthStatus {
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 状态消息
     */
    private String message;
    
    /**
     * 当前活跃连接数
     */
    @Builder.Default
    private int activeConnections = 0;
    
    /**
     * 最大连接数
     */
    @Builder.Default
    private int maxConnections = 0;
    
    /**
     * 检查时间
     */
    @Builder.Default
    private LocalDateTime checkTime = LocalDateTime.now();
    
    /**
     * 响应时间（毫秒）
     */
    @Builder.Default
    private long responseTime = 0;
    
    /**
     * 错误详情
     */
    private String errorDetails;
    
    /**
     * 获取健康状态级别
     */
    public HealthLevel getHealthLevel() {
        if (!healthy) {
            return HealthLevel.CRITICAL;
        }
        
        if (maxConnections > 0) {
            double usageRate = (double) activeConnections / maxConnections;
            if (usageRate > 0.9) {
                return HealthLevel.WARNING;
            } else if (usageRate > 0.7) {
                return HealthLevel.CAUTION;
            }
        }
        
        return HealthLevel.HEALTHY;
    }
    
    /**
     * 获取状态图标
     */
    public String getStatusIcon() {
        switch (getHealthLevel()) {
            case HEALTHY:
                return "✅";
            case CAUTION:
                return "⚠️";
            case WARNING:
                return "🟡";
            case CRITICAL:
                return "❌";
            default:
                return "❓";
        }
    }
    
    /**
     * 获取格式化的健康报告
     */
    public String getFormattedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(getStatusIcon()).append(" 数据库健康状态报告\n");
        sb.append("状态: ").append(healthy ? "健康" : "异常").append("\n");
        sb.append("级别: ").append(getHealthLevel().getDisplayName()).append("\n");
        sb.append("消息: ").append(message).append("\n");
        
        if (maxConnections > 0) {
            sb.append("连接: ").append(activeConnections).append("/").append(maxConnections);
            sb.append(" (").append(String.format("%.1f%%", (double) activeConnections / maxConnections * 100)).append(")\n");
        }
        
        sb.append("检查时间: ").append(checkTime).append("\n");
        
        if (responseTime > 0) {
            sb.append("响应时间: ").append(responseTime).append("ms\n");
        }
        
        if (errorDetails != null && !errorDetails.isEmpty()) {
            sb.append("错误详情: ").append(errorDetails);
        }
        
        return sb.toString();
    }
    
    /**
     * 获取简短的状态描述
     */
    public String getShortStatus() {
        return getStatusIcon() + " " + getHealthLevel().getDisplayName() + ": " + message;
    }
    
    /**
     * 健康状态级别枚举
     */
    public enum HealthLevel {
        HEALTHY("健康", "系统运行正常"),
        CAUTION("注意", "系统运行正常但需要关注"),
        WARNING("警告", "系统存在潜在问题"),
        CRITICAL("严重", "系统存在严重问题");
        
        private final String displayName;
        private final String description;
        
        HealthLevel(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
}