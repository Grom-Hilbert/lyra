package tslc.beihaiyun.lyra.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import tslc.beihaiyun.lyra.service.CacheService;

import java.lang.reflect.Method;

/**
 * 缓存监控切面
 * 监控缓存操作的性能和命中率
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Aspect
@Component
public class CacheMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheMonitoringAspect.class);

    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private CacheManager cacheManager;

    /**
     * 监控缓存操作
     */
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheableOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            // 获取@Cacheable注解信息
            Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            Cacheable cacheable = method.getAnnotation(Cacheable.class);
            
            if (cacheable != null && cacheable.value().length > 0) {
                String cacheName = cacheable.value()[0];
                
                // 检查缓存是否命中
                boolean cacheHit = checkCacheHit(cacheName, joinPoint.getArgs(), cacheable.key());
                
                if (cacheHit) {
                    cacheService.recordCacheHit(cacheName);
                    log.debug("缓存命中: {}.{} - 缓存: {}", className, methodName, cacheName);
                } else {
                    cacheService.recordCacheMiss(cacheName);
                    log.debug("缓存未命中: {}.{} - 缓存: {}", className, methodName, cacheName);
                }
            }
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > 1000) { // 记录超过1秒的操作
                log.warn("缓存操作耗时较长: {}.{} - {}ms", className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("缓存操作异常: {}.{} - {}ms, 错误: {}", 
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 监控缓存清理操作
     */
    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object monitorCacheEvictOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录缓存清理操作
            Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            org.springframework.cache.annotation.CacheEvict cacheEvict = 
                method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            
            if (cacheEvict != null && cacheEvict.value().length > 0) {
                String cacheName = cacheEvict.value()[0];
                cacheService.recordCacheEviction(cacheName);
                
                if (cacheEvict.allEntries()) {
                    log.debug("缓存全部清理: {}.{} - 缓存: {}", className, methodName, cacheName);
                } else {
                    log.debug("缓存部分清理: {}.{} - 缓存: {}", className, methodName, cacheName);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            if (executionTime > 500) { // 记录超过500ms的清理操作
                log.warn("缓存清理耗时较长: {}.{} - {}ms", className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("缓存清理异常: {}.{} - {}ms, 错误: {}", 
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 检查缓存是否命中
     */
    private boolean checkCacheHit(String cacheName, Object[] args, String keyExpression) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return false;
            }
            
            // 简化的键生成逻辑，实际应该根据SpEL表达式生成
            String key = generateCacheKey(args, keyExpression);
            Cache.ValueWrapper valueWrapper = cache.get(key);
            
            return valueWrapper != null;
            
        } catch (Exception e) {
            log.debug("检查缓存命中失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(Object[] args, String keyExpression) {
        // 简化的键生成逻辑
        if (keyExpression != null && !keyExpression.isEmpty()) {
            // 这里应该实现完整的SpEL表达式解析
            // 为了简化，直接使用参数的字符串表示
            if (args != null && args.length > 0) {
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) keyBuilder.append(":");
                    keyBuilder.append(args[i] != null ? args[i].toString() : "null");
                }
                return keyBuilder.toString();
            }
        }
        
        return "default";
    }
}
