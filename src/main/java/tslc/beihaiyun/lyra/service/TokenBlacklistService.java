package tslc.beihaiyun.lyra.service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    private final LyraProperties lyraProperties;

    // 使用线程安全的Map存储黑名单令牌及其过期时间
    private final ConcurrentMap<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    @Autowired
    public TokenBlacklistService(LyraProperties lyraProperties) {
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
            // 直接从令牌中提取过期时间，避免依赖JwtService
            Date expirationDate = extractExpiration(token);
            blacklistedTokens.put(token, expirationDate);
            
            logger.debug("令牌已添加到黑名单，数量: {}", blacklistedTokens.size());
        } catch (Exception e) {
            logger.warn("无法解析令牌过期时间: {}", e.getMessage());
            // 如果无法解析过期时间，使用最大刷新令牌过期时间作为默认值
            Date defaultExpiration = new Date(System.currentTimeMillis() + lyraProperties.getJwt().getRefreshExpiration());
            blacklistedTokens.put(token, defaultExpiration);
        }
    }

    /**
     * 检查令牌是否在黑名单中
     * 
     * @param token JWT令牌
     * @return 如果令牌在黑名单中返回true
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        return blacklistedTokens.containsKey(token);
    }

    /**
     * 从黑名单中移除令牌
     * 
     * @param token JWT令牌
     */
    public void removeTokenFromBlacklist(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.remove(token);
            logger.debug("令牌已从黑名单中移除");
        }
    }

    /**
     * 清理过期的黑名单令牌
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int removedCount = 0;
        
        // 使用迭代器安全地移除过期令牌
        blacklistedTokens.entrySet().removeIf(entry -> {
            Date expirationDate = entry.getValue();
            return expirationDate.before(now);
        });
        
        if (removedCount > 0) {
            logger.info("清理了 {} 个过期的黑名单令牌，当前数量: {}", removedCount, blacklistedTokens.size());
        }
    }

    /**
     * 获取当前黑名单中的令牌数量
     * 
     * @return 黑名单令牌数量
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * 清空所有黑名单令牌
     */
    public void clearAllBlacklistedTokens() {
        int size = blacklistedTokens.size();
        blacklistedTokens.clear();
        logger.info("已清空所有黑名单令牌，数量: {}", size);
    }

    /**
     * 检查令牌是否即将过期（用于清理优化）
     * 
     * @param token JWT令牌
     * @param thresholdMinutes 阈值分钟数
     * @return 如果令牌将在阈值时间内过期返回true
     */
    public boolean isTokenExpiringSoon(String token, int thresholdMinutes) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            Date expirationDate = extractExpiration(token);
            Date threshold = new Date(System.currentTimeMillis() + (thresholdMinutes * 60 * 1000L));
            return expirationDate.before(threshold);
        } catch (Exception e) {
            logger.debug("无法检查令牌过期时间: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 直接从JWT令牌中提取过期时间
     * 避免对JwtService的依赖
     */
    private Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration();
        } catch (Exception e) {
            logger.debug("令牌解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("无法解析令牌", e);
        }
    }

    /**
     * 获取签名密钥（复制自JwtService以避免依赖）
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(
                lyraProperties.getJwt().getSecret().getBytes()
            )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 