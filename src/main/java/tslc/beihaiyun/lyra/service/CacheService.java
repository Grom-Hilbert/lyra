package tslc.beihaiyun.lyra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存服务
 * 负责缓存预热、清理、监控和管理
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private LyraProperties lyraProperties;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    

    
    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private FileEntityRepository fileEntityRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private FileCacheService fileCacheService;

    // 缓存统计信息
    private final Map<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private final AtomicLong hitCount = new AtomicLong(0);
        private final AtomicLong missCount = new AtomicLong(0);
        private final AtomicLong evictionCount = new AtomicLong(0);
        private volatile long lastAccessTime = System.currentTimeMillis();
        
        public void recordHit() {
            hitCount.incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }
        
        public void recordMiss() {
            missCount.incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }
        
        public void recordEviction() {
            evictionCount.incrementAndGet();
        }
        
        public long getHitCount() { return hitCount.get(); }
        public long getMissCount() { return missCount.get(); }
        public long getEvictionCount() { return evictionCount.get(); }
        public long getLastAccessTime() { return lastAccessTime; }
        
        public double getHitRate() {
            long total = hitCount.get() + missCount.get();
            return total == 0 ? 0.0 : (double) hitCount.get() / total;
        }
    }

    /**
     * 应用启动完成后执行缓存预热
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("cacheWarmupExecutor")
    public void warmUpCache() {
        if (!lyraProperties.getCache().getEnableWarmup()) {
            log.info("缓存预热已禁用，跳过预热过程");
            return;
        }
        
        log.info("开始缓存预热...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 预热用户权限缓存
            warmUpUserPermissions();
            
            // 预热角色缓存
            warmUpRoles();
            
            // 预热系统配置缓存
            warmUpSystemConfig();
            
            // 预热空间信息缓存
            warmUpSpaceInfo();

            // 预热文件缓存
            warmUpFileCache();

            long endTime = System.currentTimeMillis();
            log.info("缓存预热完成，耗时: {}ms", endTime - startTime);
            
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }

    /**
     * 预热用户权限缓存
     */
    private void warmUpUserPermissions() {
        log.debug("预热用户权限缓存...");
        
        // 获取活跃用户（最近30天登录的用户）
        List<User> activeUsers = userRepository.findActiveUsers(30);
        
        for (User user : activeUsers) {
            try {
                // 预加载用户权限
                permissionService.getUserPermissions(user.getId());

                // 预加载用户角色信息
                roleService.getUserRoleInfo(user.getId());
                
            } catch (Exception e) {
                log.warn("预热用户 {} 的权限缓存失败: {}", user.getId(), e.getMessage());
            }
        }
        
        log.debug("用户权限缓存预热完成，处理了 {} 个用户", activeUsers.size());
    }

    /**
     * 预热角色缓存
     */
    private void warmUpRoles() {
        log.debug("预热角色缓存...");
        
        List<Role> roles = roleRepository.findByEnabledTrue();

        for (Role role : roles) {
            try {
                // 预加载角色权限检查
                roleService.hasPermissionThroughRoles(role.getId(), "READ");
                
            } catch (Exception e) {
                log.warn("预热角色 {} 的缓存失败: {}", role.getId(), e.getMessage());
            }
        }
        
        log.debug("角色缓存预热完成，处理了 {} 个角色", roles.size());
    }

    /**
     * 预热系统配置缓存
     */
    private void warmUpSystemConfig() {
        log.debug("预热系统配置缓存...");
        
        // 预加载常用的系统配置
        Cache systemConfigCache = cacheManager.getCache(CacheConfig.SYSTEM_CONFIG_CACHE);
        if (systemConfigCache != null) {
            // 这里可以预加载一些常用的系统配置
            // 例如：系统设置、默认权限等
            systemConfigCache.put("system.initialized", true);
        }
        
        log.debug("系统配置缓存预热完成");
    }

    /**
     * 预热空间信息缓存
     */
    private void warmUpSpaceInfo() {
        log.debug("预热空间信息缓存...");
        
        List<Space> spaces = spaceRepository.findTop100ByOrderByUpdatedAtDesc();
        
        Cache spaceInfoCache = cacheManager.getCache(CacheConfig.SPACE_INFO_CACHE);
        if (spaceInfoCache != null) {
            for (Space space : spaces) {
                try {
                    spaceInfoCache.put("space:" + space.getId(), space);
                } catch (Exception e) {
                    log.warn("预热空间 {} 的缓存失败: {}", space.getId(), e.getMessage());
                }
            }
        }
        
        log.debug("空间信息缓存预热完成，处理了 {} 个空间", spaces.size());
    }

    /**
     * 预热文件缓存
     */
    private void warmUpFileCache() {
        log.debug("预热文件缓存...");

        // 获取活跃文件（限制数量以避免过多缓存）
        List<FileEntity> activeFiles = fileEntityRepository.findByStatus(FileEntity.FileStatus.ACTIVE);

        // 限制预热文件数量，选择最近修改的前50个文件
        List<FileEntity> hotFiles = activeFiles.stream()
                .sorted((f1, f2) -> f2.getLastModifiedAt().compareTo(f1.getLastModifiedAt()))
                .limit(50)
                .collect(java.util.stream.Collectors.toList());

        if (!hotFiles.isEmpty()) {
            fileCacheService.warmUpFileCache(hotFiles);
        }

        log.debug("文件缓存预热完成，处理了 {} 个文件", hotFiles.size());
    }

    /**
     * 清理指定缓存
     */
    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("已清理缓存: {}", cacheName);
        }
    }

    /**
     * 清理指定缓存的特定键
     */
    public void evictCacheKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("已清理缓存键: {}:{}", cacheName, key);
        }
    }

    /**
     * 清理所有缓存
     */
    public void evictAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            evictCache(cacheName);
        }
        log.info("已清理所有缓存");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new ConcurrentHashMap<>();
                cacheInfo.put("name", cacheName);
                
                // 获取缓存大小（如果支持）
                try {
                    if (cache.getNativeCache() instanceof java.util.concurrent.ConcurrentMap) {
                        java.util.concurrent.ConcurrentMap<?, ?> nativeCache = 
                            (java.util.concurrent.ConcurrentMap<?, ?>) cache.getNativeCache();
                        cacheInfo.put("size", nativeCache.size());
                    }
                } catch (Exception e) {
                    cacheInfo.put("size", "N/A");
                }
                
                // 获取自定义统计信息
                CacheStats cacheStats = cacheStatsMap.get(cacheName);
                if (cacheStats != null) {
                    cacheInfo.put("hitCount", cacheStats.getHitCount());
                    cacheInfo.put("missCount", cacheStats.getMissCount());
                    cacheInfo.put("hitRate", String.format("%.2f%%", cacheStats.getHitRate() * 100));
                    cacheInfo.put("evictionCount", cacheStats.getEvictionCount());
                    cacheInfo.put("lastAccessTime", cacheStats.getLastAccessTime());
                }
                
                stats.put(cacheName, cacheInfo);
            }
        }
        
        return stats;
    }

    /**
     * 定期清理过期缓存统计信息
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredStats() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 24 * 60 * 60 * 1000; // 24小时
        
        cacheStatsMap.entrySet().removeIf(entry -> {
            CacheStats stats = entry.getValue();
            return (currentTime - stats.getLastAccessTime()) > expireTime;
        });
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordHit();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordMiss();
    }

    /**
     * 记录缓存驱逐
     */
    public void recordCacheEviction(String cacheName) {
        cacheStatsMap.computeIfAbsent(cacheName, k -> new CacheStats()).recordEviction();
    }
}
