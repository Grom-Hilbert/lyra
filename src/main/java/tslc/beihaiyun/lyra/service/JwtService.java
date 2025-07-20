package tslc.beihaiyun.lyra.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import tslc.beihaiyun.lyra.config.LyraProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    @Autowired
    public JwtService(LyraProperties lyraProperties) {
        this.lyraProperties = lyraProperties;
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
        if (userIdClaim instanceof Number) {
            return ((Number) userIdClaim).longValue();
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
} 