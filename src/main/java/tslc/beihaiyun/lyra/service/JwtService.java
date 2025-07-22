package tslc.beihaiyun.lyra.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * JWT服务
 * 提供JWT令牌的生成、验证、解析等功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final LyraProperties lyraProperties;
    private final ApplicationContext applicationContext;
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    public JwtService(LyraProperties lyraProperties, ApplicationContext applicationContext) {
        this.lyraProperties = lyraProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 延迟获取令牌黑名单服务（避免循环依赖）
     */
    private TokenBlacklistService getTokenBlacklistService() {
        if (tokenBlacklistService == null) {
            try {
                tokenBlacklistService = applicationContext.getBean(TokenBlacklistService.class);
            } catch (Exception e) {
                logger.debug("TokenBlacklistService not available: {}", e.getMessage());
                return null;
            }
        }
        return tokenBlacklistService;
    }

    /**
     * 从JWT令牌中提取用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从JWT令牌中提取过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从JWT令牌中提取指定的声明
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 生成JWT令牌
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 生成JWT令牌（带额外声明）
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, lyraProperties.getJwt().getExpiration());
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, lyraProperties.getJwt().getRefreshExpiration());
    }

    /**
     * 构建JWT令牌
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * 验证JWT令牌是否有效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // 首先检查令牌是否在黑名单中
            TokenBlacklistService blacklistService = getTokenBlacklistService();
            if (blacklistService != null && blacklistService.isTokenBlacklisted(token)) {
                logger.debug("令牌在黑名单中，验证失败");
                return false;
            }
            
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("令牌验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证JWT令牌是否有效（不验证用户信息）
     */
    public boolean isTokenValid(String token) {
        try {
            // 首先检查令牌是否在黑名单中
            TokenBlacklistService blacklistService = getTokenBlacklistService();
            if (blacklistService != null && blacklistService.isTokenBlacklisted(token)) {
                logger.debug("令牌在黑名单中，验证失败");
                return false;
            }
            
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("令牌验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查JWT令牌是否过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 从JWT令牌中提取所有声明
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException e) {
            logger.error("无效的JWT令牌：{}", e.getMessage());
            throw new IllegalArgumentException("无效的JWT令牌", e);
        } catch (ExpiredJwtException e) {
            logger.error("JWT令牌已过期：{}", e.getMessage());
            throw new IllegalArgumentException("JWT令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            logger.error("不支持的JWT令牌：{}", e.getMessage());
            throw new IllegalArgumentException("不支持的JWT令牌", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT声明字符串为空：{}", e.getMessage());
            throw new IllegalArgumentException("JWT声明字符串为空", e);
        } catch (Exception e) {
            logger.error("JWT令牌解析失败：{}", e.getMessage());
            throw new IllegalArgumentException("JWT令牌解析失败", e);
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(
                lyraProperties.getJwt().getSecret().getBytes()
            )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     */
    public long getTokenRemainingTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 刷新JWT令牌
     */
    public String refreshToken(String token, UserDetails userDetails) {
        if (isTokenValid(token, userDetails)) {
            return generateToken(userDetails);
        }
        throw new IllegalArgumentException("无法刷新无效的令牌");
    }

    /**
     * 从令牌中获取用户ID（如果存在）
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 生成包含用户ID的令牌
     */
    public String generateTokenWithUserId(UserDetails userDetails, Long userId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        return generateToken(extraClaims, userDetails);
    }

    /**
     * 注销令牌（将令牌添加到黑名单）
     * 
     * @param token 要注销的JWT令牌
     * @throws IllegalArgumentException 如果令牌无效
     */
    public void logoutToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("令牌不能为空");
        }
        
        TokenBlacklistService blacklistService = getTokenBlacklistService();
        if (blacklistService != null) {
            blacklistService.blacklistToken(token);
            logger.debug("令牌已成功注销");
        } else {
            logger.warn("令牌黑名单服务未初始化，无法注销令牌");
            throw new IllegalStateException("令牌黑名单服务未可用");
        }
    }

    /**
     * 批量注销用户的所有令牌
     * 注意：此方法需要扩展实现，当前只支持单个令牌注销
     * 
     * @param username 用户名
     */
    public void logoutAllUserTokens(String username) {
        // TODO: 实现批量注销功能，需要记录用户的活跃令牌
        logger.warn("批量注销功能尚未实现，用户: {}", username);
    }

    /**
     * 检查令牌是否已被注销（在黑名单中）
     * 
     * @param token JWT令牌
     * @return 如果令牌已被注销返回true，否则返回false
     */
    public boolean isTokenLoggedOut(String token) {
        TokenBlacklistService blacklistService = getTokenBlacklistService();
        if (blacklistService != null) {
            return blacklistService.isTokenBlacklisted(token);
        }
        return false;
    }
} 