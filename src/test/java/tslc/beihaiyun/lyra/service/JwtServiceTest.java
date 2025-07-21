package tslc.beihaiyun.lyra.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.mockito.Mockito;

import tslc.beihaiyun.lyra.config.LyraProperties;


/**
 * JwtService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("JWT服务测试")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        LyraProperties lyraProperties = new LyraProperties();
        lyraProperties.getJwt().setSecret("testSecretKeyForJWTTokenGeneration");
        lyraProperties.getJwt().setExpiration(3600000L); // 1小时
        lyraProperties.getJwt().setRefreshExpiration(86400000L); // 24小时

        // 创建模拟的ApplicationContext
        ApplicationContext mockApplicationContext = Mockito.mock(ApplicationContext.class);
        
        // 创建TokenBlacklistService的模拟对象
        tokenBlacklistService = Mockito.mock(TokenBlacklistService.class);
        
        // 配置ApplicationContext返回TokenBlacklistService
        when(mockApplicationContext.getBean(TokenBlacklistService.class))
                .thenReturn(tokenBlacklistService);

        jwtService = new JwtService(lyraProperties, mockApplicationContext);

        // 创建测试用户详情
        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("成功生成JWT令牌")
    void testGenerateToken() {
        String token = jwtService.generateToken(testUserDetails);
        
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
        String token = jwtService.generateToken(testUserDetails);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(testUserDetails.getUsername(), extractedUsername, "提取的用户名应该匹配");
    }

    @Test
    @DisplayName("验证有效令牌")
    void testIsTokenValid() {
        String token = jwtService.generateToken(testUserDetails);
        
        assertTrue(jwtService.isTokenValid(token, testUserDetails), "有效令牌应该通过验证");
        assertTrue(jwtService.isTokenValid(token), "有效令牌应该通过基础验证");
    }

    @Test
    @DisplayName("验证无效令牌")
    void testIsTokenInvalid() {
        String invalidToken = "invalid.token.here";
        
        assertFalse(jwtService.isTokenValid(invalidToken, testUserDetails), "无效令牌应该验证失败");
        assertFalse(jwtService.isTokenValid(invalidToken), "无效令牌应该基础验证失败");
    }

    @Test
    @DisplayName("生成刷新令牌")
    void testGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);
        
        assertNotNull(refreshToken, "刷新令牌不应该为空");
        assertFalse(refreshToken.isEmpty(), "刷新令牌不应该为空字符串");
        
        // 验证刷新令牌有效性
        assertTrue(jwtService.isTokenValid(refreshToken, testUserDetails), "刷新令牌应该有效");
    }

    @Test
    @DisplayName("生成带额外声明的令牌")
    void testGenerateTokenWithExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("department", "IT");
        
        String token = jwtService.generateToken(extraClaims, testUserDetails);
        
        assertNotNull(token, "带额外声明的令牌不应该为空");
        assertTrue(jwtService.isTokenValid(token, testUserDetails), "带额外声明的令牌应该有效");
    }

    @Test
    @DisplayName("生成带用户ID的令牌")
    void testGenerateTokenWithUserId() {
        Long userId = 123L;
        String token = jwtService.generateTokenWithUserId(testUserDetails, userId);
        
        assertNotNull(token, "带用户ID的令牌不应该为空");
        assertTrue(jwtService.isTokenValid(token, testUserDetails), "带用户ID的令牌应该有效");
        
        Long extractedUserId = jwtService.extractUserId(token);
        assertEquals(userId, extractedUserId, "提取的用户ID应该匹配");
    }

    @Test
    @DisplayName("获取令牌剩余有效时间")
    void testGetTokenRemainingTime() {
        String token = jwtService.generateToken(testUserDetails);
        long remainingTime = jwtService.getTokenRemainingTime(token);
        
        assertTrue(remainingTime > 0, "剩余时间应该大于0");
        assertTrue(remainingTime <= 3600000L, "剩余时间应该不超过1小时");
    }

    @Test
    @DisplayName("刷新有效令牌")
    void testRefreshToken() {
        String originalToken = jwtService.generateToken(testUserDetails);
        
        // 等待一秒确保时间戳不同
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String refreshedToken = jwtService.refreshToken(originalToken, testUserDetails);
        
        assertNotNull(refreshedToken, "刷新后的令牌不应该为空");
        assertNotEquals(originalToken, refreshedToken, "刷新后的令牌应该与原令牌不同");
        assertTrue(jwtService.isTokenValid(refreshedToken, testUserDetails), "刷新后的令牌应该有效");
    }

    @Test
    @DisplayName("刷新无效令牌应该抛出异常")
    void testRefreshInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.refreshToken(invalidToken, testUserDetails),
            "刷新无效令牌应该抛出异常");
    }

    @Test
    @DisplayName("提取过期时间")
    void testExtractExpiration() {
        String token = jwtService.generateToken(testUserDetails);
        Date expiration = jwtService.extractExpiration(token);
        
        assertNotNull(expiration, "过期时间不应该为空");
        assertTrue(expiration.after(new Date()), "过期时间应该在当前时间之后");
    }

    @Test
    @DisplayName("提取不存在的用户ID返回null")
    void testExtractNonExistentUserId() {
        String token = jwtService.generateToken(testUserDetails);
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
        
        ApplicationContext mockApplicationContext = Mockito.mock(ApplicationContext.class);
        TokenBlacklistService mockTokenBlacklistService = Mockito.mock(TokenBlacklistService.class);
        when(mockApplicationContext.getBean(TokenBlacklistService.class)).thenReturn(mockTokenBlacklistService);

        JwtService expiredJwtService = new JwtService(expiredProperties, mockApplicationContext);
        String token = expiredJwtService.generateToken(testUserDetails);
        
        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertFalse(expiredJwtService.isTokenValid(token, testUserDetails), "过期令牌应该验证失败");
    }

    @Test
    @DisplayName("验证不同用户的令牌")
    void testTokenForDifferentUser() {
        UserDetails anotherUser = User.builder()
            .username("anotheruser")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        
        String token = jwtService.generateToken(testUserDetails);
        
        assertFalse(jwtService.isTokenValid(token, anotherUser), 
            "其他用户的令牌应该验证失败");
    }

    @Test
    @DisplayName("令牌注销功能测试")
    void testLogoutToken() {
        String token = jwtService.generateToken(testUserDetails);
        
        // 验证令牌初始有效
        assertTrue(jwtService.isTokenValid(token, testUserDetails), "令牌初始应该有效");
        assertFalse(jwtService.isTokenLoggedOut(token), "令牌初始不应该被注销");
        
        // 注销令牌
        jwtService.logoutToken(token);
        
        // 设置Mock，模拟令牌已在黑名单中
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);
        
        // 验证令牌已被注销
        assertTrue(jwtService.isTokenLoggedOut(token), "令牌应该已被注销");
        assertFalse(jwtService.isTokenValid(token, testUserDetails), "已注销的令牌应该验证失败");
        assertFalse(jwtService.isTokenValid(token), "已注销的令牌应该基础验证失败");
    }

    @Test
    @DisplayName("注销空令牌应该抛出异常")
    void testLogoutNullToken() {
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.logoutToken(null),
            "注销null令牌应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.logoutToken(""),
            "注销空字符串令牌应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.logoutToken("   "),
            "注销空白令牌应该抛出异常");
    }

    @Test
    @DisplayName("检查未注销令牌的状态")
    void testCheckNonLoggedOutToken() {
        String token = jwtService.generateToken(testUserDetails);
        
        assertFalse(jwtService.isTokenLoggedOut(token), 
            "未注销的令牌不应该显示为已注销");
        assertFalse(jwtService.isTokenLoggedOut(null), 
            "null令牌不应该显示为已注销");
        assertFalse(jwtService.isTokenLoggedOut(""), 
            "空字符串令牌不应该显示为已注销");
    }

    @Test
    @DisplayName("批量注销功能提醒")
    void testLogoutAllUserTokens() {
        // 当前只是记录警告，验证方法可以调用而不报错
        assertDoesNotThrow(() -> jwtService.logoutAllUserTokens("testuser"),
            "批量注销方法应该可以调用而不报错");
    }

    @Test
    @DisplayName("黑名单集成测试 - 令牌验证与黑名单交互")
    void testTokenValidationWithBlacklist() {
        String token = jwtService.generateToken(testUserDetails);
        
        // 令牌初始有效
        assertTrue(jwtService.isTokenValid(token, testUserDetails), "令牌初始应该有效");
        
        // 注销令牌
        jwtService.logoutToken(token);
        
        // 设置Mock，模拟令牌已在黑名单中
        when(tokenBlacklistService.isTokenBlacklisted(token)).thenReturn(true);
        
        // 验证令牌验证方法正确处理黑名单
        assertFalse(jwtService.isTokenValid(token, testUserDetails), 
            "黑名单中的令牌应该验证失败（带用户验证）");
        assertFalse(jwtService.isTokenValid(token), 
            "黑名单中的令牌应该验证失败（基础验证）");
        
        // 验证刷新已注销的令牌应该失败
        assertThrows(IllegalArgumentException.class, 
            () -> jwtService.refreshToken(token, testUserDetails),
            "刷新已注销的令牌应该抛出异常");
    }
} 