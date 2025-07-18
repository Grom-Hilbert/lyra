package tslc.beihaiyun.lyra.storage;

/**
 * 存储错误类型
 */
public enum StorageErrorType {
    
    /**
     * 文件不存在
     */
    FILE_NOT_FOUND,
    
    /**
     * 文件已存在
     */
    FILE_ALREADY_EXISTS,
    
    /**
     * 存储空间不足
     */
    INSUFFICIENT_STORAGE,
    
    /**
     * 权限不足
     */
    ACCESS_DENIED,
    
    /**
     * 网络错误
     */
    NETWORK_ERROR,
    
    /**
     * 配置错误
     */
    CONFIGURATION_ERROR,
    
    /**
     * IO错误
     */
    IO_ERROR,
    
    /**
     * 校验和不匹配
     */
    CHECKSUM_MISMATCH,
    
    /**
     * 文件大小超限
     */
    FILE_SIZE_EXCEEDED,
    
    /**
     * 未知错误
     */
    UNKNOWN
}