package tslc.beihaiyun.lyra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tslc.beihaiyun.lyra.service.CacheService;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存监控、管理和统计功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@RestController
@RequestMapping("/api/admin/cache")
@PreAuthorize("hasAuthority('system.admin')")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            Map<String, Object> statistics = cacheService.getCacheStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取缓存统计信息失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理指定缓存
     * 
     * @param cacheName 缓存名称
     * @return 操作结果
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, Object>> evictCache(@PathVariable String cacheName) {
        try {
            cacheService.evictCache(cacheName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "缓存清理成功");
            result.put("cacheName", cacheName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "缓存清理失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理指定缓存的特定键
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 操作结果
     */
    @DeleteMapping("/{cacheName}/keys/{key}")
    public ResponseEntity<Map<String, Object>> evictCacheKey(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        try {
            cacheService.evictCacheKey(cacheName, key);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "缓存键清理成功");
            result.put("cacheName", cacheName);
            result.put("key", key);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "缓存键清理失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清理所有缓存
     * 
     * @return 操作结果
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        try {
            cacheService.evictAllCaches();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "所有缓存清理成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "缓存清理失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 手动触发缓存预热
     * 
     * @return 操作结果
     */
    @PostMapping("/warmup")
    public ResponseEntity<Map<String, Object>> warmUpCache() {
        try {
            cacheService.warmUpCache();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "缓存预热已启动");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "缓存预热启动失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取缓存健康状态
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            Map<String, Object> statistics = cacheService.getCacheStatistics();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("cacheCount", statistics.size());
            health.put("details", statistics);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "DOWN");
            error.put("error", "缓存健康检查失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
