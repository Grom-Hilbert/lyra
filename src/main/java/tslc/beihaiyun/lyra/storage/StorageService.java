package tslc.beihaiyun.lyra.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 文件存储服务抽象接口
 * 提供统一的文件存储操作接口，支持本地存储和对象存储
 */
public interface StorageService {
    
    /**
     * 存储文件
     * 
     * @param key 存储键值，用于唯一标识文件
     * @param inputStream 文件输入流
     * @param contentLength 文件大小
     * @param contentType 文件类型
     * @return 存储结果信息
     * @throws StorageException 存储异常
     */
    StorageResult store(String key, InputStream inputStream, long contentLength, String contentType) throws StorageException;
    
    /**
     * 获取文件
     * 
     * @param key 存储键值
     * @return 文件输入流，如果文件不存在返回空
     * @throws StorageException 存储异常
     */
    Optional<InputStream> retrieve(String key) throws StorageException;
    
    /**
     * 删除文件
     * 
     * @param key 存储键值
     * @return 是否删除成功
     * @throws StorageException 存储异常
     */
    boolean delete(String key) throws StorageException;
    
    /**
     * 检查文件是否存在
     * 
     * @param key 存储键值
     * @return 文件是否存在
     * @throws StorageException 存储异常
     */
    boolean exists(String key) throws StorageException;
    
    /**
     * 获取文件元数据
     * 
     * @param key 存储键值
     * @return 文件元数据，如果文件不存在返回空
     * @throws StorageException 存储异常
     */
    Optional<StorageMetadata> getMetadata(String key) throws StorageException;
    
    /**
     * 复制文件
     * 
     * @param sourceKey 源文件键值
     * @param targetKey 目标文件键值
     * @return 复制结果信息
     * @throws StorageException 存储异常
     */
    StorageResult copy(String sourceKey, String targetKey) throws StorageException;
    
    /**
     * 移动文件
     * 
     * @param sourceKey 源文件键值
     * @param targetKey 目标文件键值
     * @return 移动结果信息
     * @throws StorageException 存储异常
     */
    StorageResult move(String sourceKey, String targetKey) throws StorageException;
    
    /**
     * 获取存储类型
     * 
     * @return 存储类型
     */
    StorageType getStorageType();
    
    /**
     * 获取存储统计信息
     * 
     * @return 存储统计信息
     */
    StorageStats getStats();
}