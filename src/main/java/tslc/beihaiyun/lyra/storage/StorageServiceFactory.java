package tslc.beihaiyun.lyra.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tslc.beihaiyun.lyra.storage.impl.LocalFileStorageService;
import tslc.beihaiyun.lyra.storage.impl.S3StorageService;

import java.util.List;
import java.util.Optional;

/**
 * 存储服务工厂
 * 根据配置选择合适的存储服务实现
 */
@Slf4j
@Component
public class StorageServiceFactory {
    
    private final List<StorageService> storageServices;
    private final String primaryStorageType;
    
    @Autowired
    public StorageServiceFactory(
            List<StorageService> storageServices,
            @Value("${lyra.storage.primary:local}") String primaryStorageType) {
        this.storageServices = storageServices;
        this.primaryStorageType = primaryStorageType;
        
        log.info("可用存储服务: {}", storageServices.stream()
            .map(service -> service.getStorageType().name())
            .toList());
        log.info("主要存储类型: {}", primaryStorageType);
    }
    
    /**
     * 获取主要存储服务
     * 
     * @return 主要存储服务
     */
    public StorageService getPrimaryStorageService() {
        StorageType targetType = parseStorageType(primaryStorageType);
        
        Optional<StorageService> service = storageServices.stream()
            .filter(s -> s.getStorageType() == targetType)
            .findFirst();
            
        if (service.isPresent()) {
            log.debug("使用存储服务: {}", service.get().getStorageType());
            return service.get();
        }
        
        // 如果找不到指定的存储服务，使用第一个可用的
        if (!storageServices.isEmpty()) {
            StorageService fallbackService = storageServices.get(0);
            log.warn("找不到指定的存储服务类型: {}，使用备用服务: {}", 
                primaryStorageType, fallbackService.getStorageType());
            return fallbackService;
        }
        
        throw new RuntimeException("没有可用的存储服务");
    }
    
    /**
     * 根据类型获取存储服务
     * 
     * @param storageType 存储类型
     * @return 存储服务，如果不存在返回空
     */
    public Optional<StorageService> getStorageService(StorageType storageType) {
        return storageServices.stream()
            .filter(service -> service.getStorageType() == storageType)
            .findFirst();
    }
    
    /**
     * 获取所有可用的存储服务
     * 
     * @return 存储服务列表
     */
    public List<StorageService> getAllStorageServices() {
        return List.copyOf(storageServices);
    }
    
    /**
     * 检查指定类型的存储服务是否可用
     * 
     * @param storageType 存储类型
     * @return 是否可用
     */
    public boolean isStorageServiceAvailable(StorageType storageType) {
        return storageServices.stream()
            .anyMatch(service -> service.getStorageType() == storageType);
    }
    
    private StorageType parseStorageType(String typeString) {
        try {
            return switch (typeString.toLowerCase()) {
                case "local", "filesystem" -> StorageType.LOCAL_FILESYSTEM;
                case "s3", "object" -> StorageType.S3_COMPATIBLE;
                case "nfs" -> StorageType.NFS;
                case "smb", "cifs" -> StorageType.SMB_CIFS;
                case "webdav" -> StorageType.WEBDAV;
                case "memory" -> StorageType.IN_MEMORY;
                default -> {
                    log.warn("未知的存储类型: {}，使用默认的本地存储", typeString);
                    yield StorageType.LOCAL_FILESYSTEM;
                }
            };
        } catch (Exception e) {
            log.warn("解析存储类型失败: {}，使用默认的本地存储", typeString, e);
            return StorageType.LOCAL_FILESYSTEM;
        }
    }
}