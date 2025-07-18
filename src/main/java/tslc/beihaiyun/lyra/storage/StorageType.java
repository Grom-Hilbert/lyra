package tslc.beihaiyun.lyra.storage;

/**
 * 存储类型枚举
 */
public enum StorageType {
    
    /**
     * 本地文件系统存储
     */
    LOCAL_FILESYSTEM,
    
    /**
     * S3兼容对象存储
     */
    S3_COMPATIBLE,
    
    /**
     * 网络文件系统（NFS）
     */
    NFS,
    
    /**
     * SMB/CIFS网络存储
     */
    SMB_CIFS,
    
    /**
     * WebDAV存储
     */
    WEBDAV,
    
    /**
     * 内存存储（测试用）
     */
    IN_MEMORY
}