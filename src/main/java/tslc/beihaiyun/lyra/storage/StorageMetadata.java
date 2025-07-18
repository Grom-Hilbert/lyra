package tslc.beihaiyun.lyra.storage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 存储文件元数据
 */
@Data
@Builder
public class StorageMetadata {
    
    /**
     * 存储键值
     */
    private String key;
    
    /**
     * 文件大小
     */
    private long size;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 文件校验和
     */
    private String checksum;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 存储路径
     */
    private String storagePath;
    
    /**
     * 自定义元数据
     */
    private Map<String, String> customMetadata;
    
    /**
     * 存储类型
     */
    private StorageType storageType;
}