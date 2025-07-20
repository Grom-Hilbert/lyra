package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import tslc.beihaiyun.lyra.config.LyraProperties;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * JwtService 单元测试
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@DisplayName("JWT服务测试")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        // 创建真实的配置对象而不是Mock
        LyraProperties lyraProperties = new LyraProperties();
        LyraProperties.JwtConfig jwtConfig = lyraProperties.getJwt();
        jwtConfig.setSecret("ThisIsATestSecretKeyForJWTThatIsLongEnoughForHS256Algorithm");
        jwtConfig.setExpiration(3600000L); // 1小时
        jwtConfig.setRefreshExpiration(86400000L); // 24小时

        jwtService = new JwtService(lyraProperties);

        // 创建测试用户
        testUser = User.builder()
            .username("testuser")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
    }

    @Test
    @DisplayName("成功生成JWT令牌")
    void testGenerateToken() {
        String token = jwtService.generateToken(testUser);
        
        assertNotNull(token, "令牌不应该为空");
        assertFalse(token.isEmpty(), "令牌不应该为空字符串");
        assertTrue(token.contains("."), "JWT令牌应该包含点分隔符");
        
        // JWT令牌应该包含三个部分（header.payload.signature）
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT令牌应该包含三个部分");
    }

    @Test
    @DisplayName("从令牌中提取用户名")
    void testExtractUsername() {
        String token = jwtService.generateToken(testUser);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(testUser.getUsername(), extractedUsername, "提取的用户名应该匹配");
    }

    @Test
    @DisplayName("验证有效令牌")
    void testIsTokenValid() {
        String token = jwtService.generateToken(testUser);
        
        assertTrue(jwtService.isTokenValid(token, testUser), "有效令牌应该通过验证");
        assertTrue(jwtService.isTokenValid(token), "有效令牌应该通过基础验证");
    }

    @Test
    @DisplayName("验证无效令牌")
    void testIsTokenInvalid() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtService.isTokenValid(invalidToken, testUser), "无效令牌应该验证失败");
        assertFalse(jwtService.isTokenValid(invalidToken), "无效令牌应该基础验证失败");
    }

    @Test
    @DisplayName("生成刷新令牌")
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);
        
        assertNotNull(refreshToken, "刷新令牌不应该为空");
        assertFalse(refreshToken.isEmpty(), "刷新令牌不应该为空字符串");
        
        // 验证刷新令牌有效性
        assertTrue(jwtService.isTokenValid(refreshToken, testUser), "刷新令牌应该有效");
    }

    @Test
    @DisplayName("生成带额外声明的令牌")
    void testGenerateTokenWithExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("department", "IT");
        
        String token = jwtService.generateToken(extraClaims, testUser);
        
        assertNotNull(token, "带额外声明的令牌不应该为空");
        assertTrue(jwtService.isTokenValid(token, testUser), "带额外声明的令牌应该有效");
    }

    @Test
    @DisplayName("生成带用户ID的令牌")
    void testGenerateTokenWithUserId() {
        Long userId = 123L;
        String token = jwtService.generateTokenWithUserId(testUser, userId);
        
        assertNotNull(token, "带用户ID的令牌不应该为空");
        assertTrue(jwtService.isTokenValid(token, testUser), "带用户ID的令牌应该有效");
        
        Long extractedUserId = jwtService.extractUserId(token);
        assertEquals(userId, extractedUserId, "提取的用户ID应该匹配");
    }

    @Test
    @DisplayName("获取令牌剩余有效时间")
    void testGetTokenRemainingTime() {
        String token = jwtService.generateToken(testUser);
        long remainingTime = jwtService.getTokenRemainingTime(token);
        
        assertTrue(remainingTime > 0, "剩余时间应该大于0");
        assertTrue(remainingTime <= 3600000L, "剩余时间应该不超过1小时");
    }

    @Test
    @DisplayName("刷新有效令牌")
    void testRefreshToken() {
        String originalToken = jwtService.generateToken(testUser);
        
        // 等待一秒确保时间戳不同
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String refreshedToken = jwtService.refreshToken(originalToken, testUser);
        
        assertNotNull(refreshedToken, "刷新后的令牌不应该为空");
        assertNotEquals(originalToken, refreshedToken, "刷新后的令牌应该与原令牌不同");
        assertTrue(jwtService.isTokenValid(refreshedToken, testUser), "刷新后的令牌应该有效");
    }

    @Test
    @DisplayName("刷新无效令牌应该抛出异常")
    void testRefreshInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.refreshToken(invalidToken, testUser),
            "刷新无效令牌应该抛出异常");
    }

    @Test
    @DisplayName("提取过期时间")
    void testExtractExpiration() {
        String token = jwtService.generateToken(testUser);
        Date expiration = jwtService.extractExpiration(token);
        
        assertNotNull(expiration, "过期时间不应该为空");
        assertTrue(expiration.after(new Date()), "过期时间应该在当前时间之后");
    }

    @Test
    @DisplayName("提取不存在的用户ID返回null")
    void testExtractNonExistentUserId() {
        String token = jwtService.generateToken(testUser);
        Long userId = jwtService.extractUserId(token);
        
        assertNull(userId, "不存在的用户ID应该返回null");
    }

    @Test
    @DisplayName("处理过期令牌")
    void testExpiredToken() {
        // 创建一个过期时间很短的配置
        LyraProperties expiredProperties = new LyraProperties();
        expiredProperties.getJwt().setSecret("ThisIsATestSecretKeyForJWTThatIsLongEnoughForHS256Algorithm");
        expiredProperties.getJwt().setExpiration(1L); // 1毫秒
        expiredProperties.getJwt().setRefreshExpiration(86400000L);
        
        JwtService expiredJwtService = new JwtService(expiredProperties);
        String token = expiredJwtService.generateToken(testUser);
        
        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertFalse(expiredJwtService.isTokenValid(token, testUser), "过期令牌应该验证失败");
    }

    @Test
    @DisplayName("验证不同用户的令牌")
    void testTokenForDifferentUser() {
        UserDetails anotherUser = User.builder()
            .username("anotheruser")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        
        String token = jwtService.generateToken(testUser);
        
        assertFalse(jwtService.isTokenValid(token, anotherUser), 
            "其他用户的令牌应该验证失败");
    }
} 