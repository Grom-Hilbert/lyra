package tslc.beihaiyun.lyra.storage;

import lombok.Builder;
import lombok.Data;

/**
 * 存储统计信息
 */
@Data
@Builder
public class StorageStats {
    
    /**
     * 总存储空间（字节）
     */
    private long totalSpace;
    
    /**
     * 已使用空间（字节）
     */
    private long usedSpace;
    
    /**
     * 可用空间（字节）
     */
    private long availableSpace;
    
    /**
     * 文件总数
     */
    private long fileCount;
    
    /**
     * 存储类型
     */
    private StorageType storageType;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 健康检查消息
     */
    private String healthMessage;
    
    /**
     * 计算使用率百分比
     * 
     * @return 使用率百分比（0-100）
     */
    public double getUsagePercentage() {
        if (totalSpace <= 0) {
            return 0.0;
        }
        return (double) usedSpace / totalSpace * 100.0;
    }
}