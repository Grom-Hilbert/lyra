package tslc.beihaiyun.lyra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.FileEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 文件缓存服务
 * 专门处理文件内容和元数据的缓存
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class FileCacheService {

    private static final Logger logger = LoggerFactory.getLogger(FileCacheService.class);
    
    private final LyraProperties lyraProperties;
    private final StorageService storageService;
    
    // 文件大小限制：小于1MB的文件才缓存内容
    private static final long CACHE_CONTENT_SIZE_LIMIT = 1024 * 1024; // 1MB

    public FileCacheService(LyraProperties lyraProperties, StorageService storageService) {
        this.lyraProperties = lyraProperties;
        this.storageService = storageService;
    }

    /**
     * 缓存文件元数据
     */
    @Cacheable(value = CacheConfig.FILE_METADATA_CACHE, 
               key = "'metadata:' + #fileId",
               condition = "#fileId != null",
               unless = "#result == null")
    public Optional<FileEntity> getFileMetadata(Long fileId, FileEntity file) {
        logger.debug("缓存文件元数据: fileId={}", fileId);
        return Optional.ofNullable(file);
    }

    /**
     * 更新文件元数据缓存
     */
    @CachePut(value = CacheConfig.FILE_METADATA_CACHE, 
              key = "'metadata:' + #fileId",
              condition = "#fileId != null and #file != null")
    public Optional<FileEntity> updateFileMetadata(Long fileId, FileEntity file) {
        logger.debug("更新文件元数据缓存: fileId={}", fileId);
        return Optional.of(file);
    }

    /**
     * 清除文件元数据缓存
     */
    @CacheEvict(value = CacheConfig.FILE_METADATA_CACHE, 
                key = "'metadata:' + #fileId",
                condition = "#fileId != null")
    public void evictFileMetadata(Long fileId) {
        logger.debug("清除文件元数据缓存: fileId={}", fileId);
    }

    /**
     * 缓存小文件内容
     * 只缓存小于1MB的文件内容
     */
    @Cacheable(value = CacheConfig.FILE_METADATA_CACHE, 
               key = "'content:' + #fileId",
               condition = "#fileId != null and #file != null and #file.sizeBytes <= " + CACHE_CONTENT_SIZE_LIMIT,
               unless = "#result == null")
    public Optional<byte[]> getFileContent(Long fileId, FileEntity file) {
        if (file == null || file.getSizeBytes() > CACHE_CONTENT_SIZE_LIMIT) {
            return Optional.empty();
        }

        try {
            logger.debug("缓存文件内容: fileId={}, size={}", fileId, file.getSizeBytes());
            Optional<InputStream> inputStreamOpt = storageService.load(file.getStoragePath());
            
            if (inputStreamOpt.isPresent()) {
                try (InputStream inputStream = inputStreamOpt.get()) {
                    byte[] content = inputStream.readAllBytes();
                    return Optional.of(content);
                }
            }
            return Optional.empty();
            
        } catch (IOException e) {
            logger.error("读取文件内容失败: fileId={}", fileId, e);
            return Optional.empty();
        }
    }

    /**
     * 更新文件内容缓存
     */
    @CachePut(value = CacheConfig.FILE_METADATA_CACHE, 
              key = "'content:' + #fileId",
              condition = "#fileId != null and #content != null and #content.length <= " + CACHE_CONTENT_SIZE_LIMIT)
    public Optional<byte[]> updateFileContent(Long fileId, byte[] content) {
        logger.debug("更新文件内容缓存: fileId={}, size={}", fileId, content != null ? content.length : 0);
        return Optional.ofNullable(content);
    }

    /**
     * 清除文件内容缓存
     */
    @CacheEvict(value = CacheConfig.FILE_METADATA_CACHE, 
                key = "'content:' + #fileId",
                condition = "#fileId != null")
    public void evictFileContent(Long fileId) {
        logger.debug("清除文件内容缓存: fileId={}", fileId);
    }

    /**
     * 清除文件的所有缓存（元数据和内容）
     */
    @CacheEvict(value = CacheConfig.FILE_METADATA_CACHE, 
                key = "'metadata:' + #fileId",
                condition = "#fileId != null")
    public void evictAllFileCache(Long fileId) {
        logger.debug("清除文件所有缓存: fileId={}", fileId);
        evictFileContent(fileId);
    }

    /**
     * 获取缓存的文件输入流
     * 优先从缓存获取，缓存未命中则从存储服务获取
     */
    public Optional<InputStream> getCachedFileStream(Long fileId, FileEntity file) {
        if (file == null) {
            return Optional.empty();
        }

        // 对于小文件，尝试从缓存获取
        if (file.getSizeBytes() <= CACHE_CONTENT_SIZE_LIMIT) {
            Optional<byte[]> cachedContent = getFileContent(fileId, file);
            if (cachedContent.isPresent()) {
                logger.debug("从缓存获取文件流: fileId={}", fileId);
                return Optional.of(new ByteArrayInputStream(cachedContent.get()));
            }
        }

        // 大文件或缓存未命中，直接从存储服务获取
        try {
            logger.debug("从存储服务获取文件流: fileId={}", fileId);
            return storageService.load(file.getStoragePath());
        } catch (IOException e) {
            logger.error("从存储服务获取文件流失败: fileId={}", fileId, e);
            return Optional.empty();
        }
    }

    /**
     * 预热文件缓存
     * 预加载热点文件的元数据和小文件内容
     */
    public void warmUpFileCache(java.util.List<FileEntity> hotFiles) {
        if (hotFiles == null || hotFiles.isEmpty()) {
            return;
        }

        logger.info("开始预热文件缓存，文件数量: {}", hotFiles.size());
        
        for (FileEntity file : hotFiles) {
            try {
                // 预热文件元数据
                getFileMetadata(file.getId(), file);
                
                // 预热小文件内容
                if (file.getSizeBytes() <= CACHE_CONTENT_SIZE_LIMIT) {
                    getFileContent(file.getId(), file);
                }
                
            } catch (Exception e) {
                logger.warn("预热文件缓存失败: fileId={}", file.getId(), e);
            }
        }
        
        logger.info("文件缓存预热完成");
    }

    /**
     * 获取文件缓存统计信息
     */
    public FileCacheStats getFileCacheStats() {
        // 这里可以实现缓存统计逻辑
        // 暂时返回基础统计信息
        return new FileCacheStats();
    }

    /**
     * 文件缓存统计信息
     */
    public static class FileCacheStats {
        private long metadataCacheHits = 0;
        private long metadataCacheMisses = 0;
        private long contentCacheHits = 0;
        private long contentCacheMisses = 0;
        private long cachedFileCount = 0;
        private long totalCacheSize = 0;

        // Getters and Setters
        public long getMetadataCacheHits() {
            return metadataCacheHits;
        }

        public void setMetadataCacheHits(long metadataCacheHits) {
            this.metadataCacheHits = metadataCacheHits;
        }

        public long getMetadataCacheMisses() {
            return metadataCacheMisses;
        }

        public void setMetadataCacheMisses(long metadataCacheMisses) {
            this.metadataCacheMisses = metadataCacheMisses;
        }

        public long getContentCacheHits() {
            return contentCacheHits;
        }

        public void setContentCacheHits(long contentCacheHits) {
            this.contentCacheHits = contentCacheHits;
        }

        public long getContentCacheMisses() {
            return contentCacheMisses;
        }

        public void setContentCacheMisses(long contentCacheMisses) {
            this.contentCacheMisses = contentCacheMisses;
        }

        public long getCachedFileCount() {
            return cachedFileCount;
        }

        public void setCachedFileCount(long cachedFileCount) {
            this.cachedFileCount = cachedFileCount;
        }

        public long getTotalCacheSize() {
            return totalCacheSize;
        }

        public void setTotalCacheSize(long totalCacheSize) {
            this.totalCacheSize = totalCacheSize;
        }
    }
}
