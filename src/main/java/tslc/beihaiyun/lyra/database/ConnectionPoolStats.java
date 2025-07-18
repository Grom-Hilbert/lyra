package tslc.beihaiyun.lyra.database;

import lombok.Builder;
import lombok.Data;

/**
 * 连接池统计信息
 * 提供连接池的运行状态和性能指标
 */
@Data
@Builder
public class ConnectionPoolStats {
    
    /**
     * 当前活跃连接数
     */
    private int activeConnections;
    
    /**
     * 总连接数（累计创建的连接数）
     */
    private int totalConnections;
    
    /**
     * 最大连接池大小
     */
    private int maxPoolSize;
    
    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout;
    
    /**
     * 获取连接池使用率
     */
    public double getUsageRate() {
        if (maxPoolSize == 0) return 0.0;
        return (double) activeConnections / maxPoolSize;
    }
    
    /**
     * 获取使用率百分比
     */
    public String getUsagePercentage() {
        return String.format("%.1f%%", getUsageRate() * 100);
    }
    
    /**
     * 判断连接池是否接近满载
     */
    public boolean isNearCapacity() {
        return getUsageRate() > 0.8; // 超过80%认为接近满载
    }
    
    /**
     * 判断连接池是否已满
     */
    public boolean isFull() {
        return activeConnections >= maxPoolSize;
    }
    
    /**
     * 获取可用连接数
     */
    public int getAvailableConnections() {
        return Math.max(0, maxPoolSize - activeConnections);
    }
    
    /**
     * 获取格式化的统计信息
     */
    public String getFormattedStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("连接池统计信息:\n");
        sb.append("  活跃连接: ").append(activeConnections).append("/").append(maxPoolSize).append("\n");
        sb.append("  使用率: ").append(getUsagePercentage()).append("\n");
        sb.append("  可用连接: ").append(getAvailableConnections()).append("\n");
        sb.append("  总连接数: ").append(totalConnections).append("\n");
        sb.append("  连接超时: ").append(connectionTimeout).append("ms\n");
        sb.append("  状态: ").append(getStatusDescription());
        return sb.toString();
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (isFull()) {
            return "连接池已满";
        } else if (isNearCapacity()) {
            return "连接池接近满载";
        } else if (activeConnections == 0) {
            return "连接池空闲";
        } else {
            return "连接池正常";
        }
    }
    
    /**
     * 获取健康评分（0-100）
     */
    public int getHealthScore() {
        if (isFull()) {
            return 0; // 连接池满载，健康度最低
        } else if (isNearCapacity()) {
            return 30; // 接近满载，健康度较低
        } else if (getUsageRate() > 0.5) {
            return 60; // 使用率适中，健康度中等
        } else {
            return 100; // 使用率较低，健康度最高
        }
    }
}