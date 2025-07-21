package tslc.beihaiyun.lyra.service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * 令牌黑名单服务
 * 管理已注销或无效的JWT令牌，防止被重复使用
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final JwtService jwtService;
    private final LyraProperties lyraProperties;

    // 使用线程安全的Map存储黑名单令牌及其过期时间
    private final ConcurrentMap<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    @Autowired
    public TokenBlacklistService(@Lazy JwtService jwtService, LyraProperties lyraProperties) {
        this.jwtService = jwtService;
        this.lyraProperties = lyraProperties;
    }

    /**
     * 将令牌添加到黑名单
     * 
     * @param token JWT令牌
     * @throws IllegalArgumentException 如果令牌无效
     */
    public void blacklistToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("令牌不能为空");
        }

        try {
            // 提取令牌的过期时间
            Date expirationDate = jwtService.extractExpiration(token);
            
            // 只有未过期的令牌才需要加入黑名单
            if (expirationDate.after(new Date())) {
                blacklistedTokens.put(token, expirationDate);
                logger.debug("令牌已添加到黑名单，过期时间: {}", expirationDate);
            } else {
                logger.debug("令牌已过期，无需添加到黑名单");
            }
        } catch (Exception e) {
            logger.error("处理令牌黑名单时发生错误: {}", e.getMessage());
            throw new IllegalArgumentException("无效的令牌", e);
        }
    }

    /**
     * 检查令牌是否在黑名单中
     * 
     * @param token JWT令牌
     * @return 如果令牌在黑名单中返回true，否则返回false
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        boolean isBlacklisted = blacklistedTokens.containsKey(token);
        if (isBlacklisted) {
            logger.debug("令牌在黑名单中");
        }
        return isBlacklisted;
    }

    /**
     * 从黑名单中移除令牌（通常用于测试或特殊情况）
     * 
     * @param token JWT令牌
     * @return 如果令牌被成功移除返回true，否则返回false
     */
    public boolean removeTokenFromBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        Date removedExpiration = blacklistedTokens.remove(token);
        if (removedExpiration != null) {
            logger.debug("令牌已从黑名单中移除");
            return true;
        }
        return false;
    }

    /**
     * 获取黑名单中的令牌数量
     * 
     * @return 黑名单中的令牌数量
     */
    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }

    /**
     * 清空所有黑名单令牌（主要用于测试）
     */
    public void clearBlacklist() {
        blacklistedTokens.clear();
        logger.debug("已清空所有黑名单令牌");
    }

    /**
     * 定期清理过期的黑名单令牌
     * 每小时执行一次清理任务
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int initialSize = blacklistedTokens.size();
        
        // 移除所有已过期的令牌
        blacklistedTokens.entrySet().removeIf(entry -> 
            entry.getValue().before(now));
        
        int finalSize = blacklistedTokens.size();
        if (initialSize > finalSize) {
            logger.info("清理了 {} 个过期的黑名单令牌，当前黑名单大小: {}", 
                       initialSize - finalSize, finalSize);
        }
    }

    /**
     * 强制清理过期令牌（用于测试或手动触发）
     */
    public void forceCleanupExpiredTokens() {
        cleanupExpiredTokens();
    }
} 