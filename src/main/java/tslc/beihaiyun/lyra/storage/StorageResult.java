package tslc.beihaiyun.lyra.storage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 存储操作结果
 */
@Data
@Builder
public class StorageResult {
    
    /**
     * 存储键值
     */
    private String key;
    
    /**
     * 文件大小
     */
    private long size;
    
    /**
     * 文件校验和（MD5或SHA256）
     */
    private String checksum;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 存储时间
     */
    private LocalDateTime storedAt;
    
    /**
     * 存储路径（本地存储时使用）
     */
    private String storagePath;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}