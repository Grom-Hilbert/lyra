package tslc.beihaiyun.lyra.entity.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 存储配额值对象
 * 封装存储配额和使用量的业务逻辑
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class StorageQuota {

    /**
     * 存储配额（字节）
     */
    @Column(name = "storage_quota", nullable = false)
    private Long quota = 10737418240L; // 10GB 默认配额

    /**
     * 已使用存储（字节）
     */
    @Column(name = "storage_used", nullable = false)
    private Long used = 0L;

    // 常量定义
    public static final Long DEFAULT_QUOTA = 10737418240L; // 10GB
    public static final Long MIN_QUOTA = 1073741824L; // 1GB
    public static final Long MAX_QUOTA = 1099511627776L; // 1TB

    // 单位转换常量
    public static final Long BYTES_PER_KB = 1024L;
    public static final Long BYTES_PER_MB = 1024L * 1024L;
    public static final Long BYTES_PER_GB = 1024L * 1024L * 1024L;

    /**
     * 创建默认存储配额
     * 
     * @return 默认存储配额对象
     */
    public static StorageQuota createDefault() {
        return new StorageQuota(DEFAULT_QUOTA, 0L);
    }

    /**
     * 创建指定配额的存储配额对象
     * 
     * @param quotaInGB 配额大小（GB）
     * @return 存储配额对象
     */
    public static StorageQuota createWithQuotaInGB(Long quotaInGB) {
        if (quotaInGB == null || quotaInGB <= 0) {
            throw new IllegalArgumentException("配额大小必须大于0");
        }
        
        Long quotaInBytes = quotaInGB * BYTES_PER_GB;
        if (quotaInBytes < MIN_QUOTA || quotaInBytes > MAX_QUOTA) {
            throw new IllegalArgumentException("配额大小超出允许范围: " + quotaInGB + "GB");
        }
        
        return new StorageQuota(quotaInBytes, 0L);
    }

    /**
     * 检查是否有足够的存储空间
     * 
     * @param additionalSize 需要增加的存储大小（字节）
     * @return 是否有足够空间
     */
    public boolean hasEnoughSpace(Long additionalSize) {
        if (additionalSize == null || additionalSize < 0) {
            return false;
        }
        return (used + additionalSize) <= quota;
    }

    /**
     * 计算存储使用率
     * 
     * @return 使用率（0.0-1.0）
     */
    public double getUsageRatio() {
        if (quota == null || quota == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) used / quota);
    }

    /**
     * 计算剩余存储空间
     * 
     * @return 剩余空间（字节）
     */
    public Long getRemainingSpace() {
        return Math.max(0L, quota - used);
    }

    /**
     * 增加已使用存储
     * 
     * @param size 增加的大小（字节）
     * @return 新的存储配额对象
     */
    public StorageQuota addUsage(Long size) {
        if (size == null || size < 0) {
            throw new IllegalArgumentException("增加的存储大小不能为负数");
        }
        
        Long newUsed = used + size;
        if (newUsed > quota) {
            throw new IllegalStateException("存储使用量超出配额限制");
        }
        
        return new StorageQuota(quota, newUsed);
    }

    /**
     * 减少已使用存储
     * 
     * @param size 减少的大小（字节）
     * @return 新的存储配额对象
     */
    public StorageQuota reduceUsage(Long size) {
        if (size == null || size < 0) {
            throw new IllegalArgumentException("减少的存储大小不能为负数");
        }
        
        Long newUsed = Math.max(0L, used - size);
        return new StorageQuota(quota, newUsed);
    }

    /**
     * 更新配额
     * 
     * @param newQuota 新配额（字节）
     * @return 新的存储配额对象
     */
    public StorageQuota updateQuota(Long newQuota) {
        if (newQuota == null || newQuota < MIN_QUOTA || newQuota > MAX_QUOTA) {
            throw new IllegalArgumentException("配额大小超出允许范围");
        }
        
        if (newQuota < used) {
            throw new IllegalStateException("新配额不能小于已使用的存储量");
        }
        
        return new StorageQuota(newQuota, used);
    }

    /**
     * 检查是否接近配额限制
     * 
     * @param threshold 阈值（0.0-1.0）
     * @return 是否接近限制
     */
    public boolean isNearQuotaLimit(double threshold) {
        return getUsageRatio() >= threshold;
    }

    /**
     * 格式化显示配额信息
     * 
     * @return 格式化字符串
     */
    public String formatQuotaInfo() {
        return String.format("%.1f GB / %.1f GB (%.1f%%)", 
            bytesToGB(used), 
            bytesToGB(quota), 
            getUsageRatio() * 100);
    }

    /**
     * 字节转换为GB
     * 
     * @param bytes 字节数
     * @return GB数值
     */
    private double bytesToGB(Long bytes) {
        return (double) bytes / BYTES_PER_GB;
    }

    /**
     * 验证存储配额数据的完整性
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return quota != null && quota > 0 && 
               used != null && used >= 0 && 
               used <= quota;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageQuota that = (StorageQuota) o;
        return Objects.equals(quota, that.quota) && Objects.equals(used, that.used);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quota, used);
    }

    @Override
    public String toString() {
        return "StorageQuota{" +
                "quota=" + quota +
                ", used=" + used +
                ", usageRatio=" + String.format("%.2f%%", getUsageRatio() * 100) +
                '}';
    }
} 