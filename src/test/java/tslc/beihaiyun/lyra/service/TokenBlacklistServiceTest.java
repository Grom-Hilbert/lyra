package tslc.beihaiyun.lyra.service;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * TokenBlacklistService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@DisplayName("令牌黑名单服务测试")
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private LyraProperties lyraProperties;

    private TokenBlacklistService tokenBlacklistService;
    private UserDetails testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(jwtService, lyraProperties);
        
        // 创建测试用户
        testUser = User.builder()
            .username("testuser")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
        
        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        
        // 清空黑名单（确保测试独立性）
        tokenBlacklistService.clearBlacklist();
    }

    @Test
    @DisplayName("成功将有效令牌添加到黑名单")
    void testBlacklistValidToken() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000); // 1小时后
        when(jwtService.extractExpiration(validToken)).thenReturn(futureDate);
        
        // Act
        tokenBlacklistService.blacklistToken(validToken);
        
        // Assert
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken), 
                  "有效令牌应该被成功添加到黑名单");
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount(),
                    "黑名单中应该有1个令牌");
        
        verify(jwtService).extractExpiration(validToken);
    }

    @Test
    @DisplayName("过期令牌不会被添加到黑名单")
    void testBlacklistExpiredToken() {
        // Arrange
        Date pastDate = new Date(System.currentTimeMillis() - 3600000); // 1小时前
        when(jwtService.extractExpiration(validToken)).thenReturn(pastDate);
        
        // Act
        tokenBlacklistService.blacklistToken(validToken);
        
        // Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken), 
                   "过期令牌不应该在黑名单中");
        assertEquals(0, tokenBlacklistService.getBlacklistedTokenCount(),
                    "黑名单应该为空");
    }

    @Test
    @DisplayName("添加空令牌到黑名单应该抛出异常")
    void testBlacklistNullToken() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenBlacklistService.blacklistToken(null),
                    "添加null令牌应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenBlacklistService.blacklistToken(""),
                    "添加空字符串令牌应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenBlacklistService.blacklistToken("   "),
                    "添加空白令牌应该抛出异常");
    }

    @Test
    @DisplayName("添加无效令牌到黑名单应该抛出异常")
    void testBlacklistInvalidToken() {
        // Arrange
        when(jwtService.extractExpiration(validToken))
            .thenThrow(new IllegalArgumentException("无效的JWT令牌"));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenBlacklistService.blacklistToken(validToken),
                    "添加无效令牌应该抛出异常");
    }

    @Test
    @DisplayName("检查令牌是否在黑名单中")
    void testIsTokenBlacklisted() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtService.extractExpiration(validToken)).thenReturn(futureDate);
        
        // Act & Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                   "令牌初始时不应该在黑名单中");
        
        tokenBlacklistService.blacklistToken(validToken);
        
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken),
                  "添加后令牌应该在黑名单中");
    }

    @Test
    @DisplayName("检查null或空令牌不在黑名单中")
    void testIsNullTokenBlacklisted() {
        // Act & Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null),
                   "null令牌不应该在黑名单中");
        
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""),
                   "空字符串令牌不应该在黑名单中");
        
        assertFalse(tokenBlacklistService.isTokenBlacklisted("   "),
                   "空白令牌不应该在黑名单中");
    }

    @Test
    @DisplayName("从黑名单中移除令牌")
    void testRemoveTokenFromBlacklist() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtService.extractExpiration(validToken)).thenReturn(futureDate);
        
        tokenBlacklistService.blacklistToken(validToken);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        
        // Act
        boolean removed = tokenBlacklistService.removeTokenFromBlacklist(validToken);
        
        // Assert
        assertTrue(removed, "应该成功移除令牌");
        assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                   "移除后令牌不应该在黑名单中");
        assertEquals(0, tokenBlacklistService.getBlacklistedTokenCount(),
                    "黑名单应该为空");
    }

    @Test
    @DisplayName("移除不存在的令牌返回false")
    void testRemoveNonExistentToken() {
        // Act
        boolean removed = tokenBlacklistService.removeTokenFromBlacklist(validToken);
        
        // Assert
        assertFalse(removed, "移除不存在的令牌应该返回false");
    }

    @Test
    @DisplayName("移除null或空令牌返回false")
    void testRemoveNullToken() {
        // Act & Assert
        assertFalse(tokenBlacklistService.removeTokenFromBlacklist(null),
                   "移除null令牌应该返回false");
        
        assertFalse(tokenBlacklistService.removeTokenFromBlacklist(""),
                   "移除空字符串令牌应该返回false");
        
        assertFalse(tokenBlacklistService.removeTokenFromBlacklist("   "),
                   "移除空白令牌应该返回false");
    }

    @Test
    @DisplayName("获取黑名单令牌数量")
    void testGetBlacklistedTokenCount() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtService.extractExpiration(anyString())).thenReturn(futureDate);
        
        // Act & Assert
        assertEquals(0, tokenBlacklistService.getBlacklistedTokenCount(),
                    "初始黑名单应该为空");
        
        tokenBlacklistService.blacklistToken("token1");
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount(),
                    "添加1个令牌后数量应该为1");
        
        tokenBlacklistService.blacklistToken("token2");
        assertEquals(2, tokenBlacklistService.getBlacklistedTokenCount(),
                    "添加2个令牌后数量应该为2");
        
        tokenBlacklistService.removeTokenFromBlacklist("token1");
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount(),
                    "移除1个令牌后数量应该为1");
    }

    @Test
    @DisplayName("清空黑名单")
    void testClearBlacklist() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtService.extractExpiration(anyString())).thenReturn(futureDate);
        
        tokenBlacklistService.blacklistToken("token1");
        tokenBlacklistService.blacklistToken("token2");
        assertEquals(2, tokenBlacklistService.getBlacklistedTokenCount());
        
        // Act
        tokenBlacklistService.clearBlacklist();
        
        // Assert
        assertEquals(0, tokenBlacklistService.getBlacklistedTokenCount(),
                    "清空后黑名单应该为空");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("token1"),
                   "清空后令牌不应该在黑名单中");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("token2"),
                   "清空后令牌不应该在黑名单中");
    }

    @Test
    @DisplayName("清理过期的黑名单令牌")
    void testCleanupExpiredTokens() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000); // 1小时后
        Date pastDate = new Date(System.currentTimeMillis() - 3600000);   // 1小时前（已过期）
        
        when(jwtService.extractExpiration("validToken")).thenReturn(futureDate);
        when(jwtService.extractExpiration("expiredToken")).thenReturn(futureDate); // 先设为未过期以添加到黑名单
        
        // 添加两个令牌到黑名单
        tokenBlacklistService.blacklistToken("validToken");
        tokenBlacklistService.blacklistToken("expiredToken");
        
        assertEquals(2, tokenBlacklistService.getBlacklistedTokenCount(),
                    "添加2个令牌后数量应该为2");
        
        // 使用反射手动修改黑名单中的过期时间来模拟令牌过期
        try {
            Field blacklistedTokensField = TokenBlacklistService.class.getDeclaredField("blacklistedTokens");
            blacklistedTokensField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, Date> blacklistedTokens = (ConcurrentMap<String, Date>) blacklistedTokensField.get(tokenBlacklistService);
            
            // 将expiredToken的过期时间设置为过去时间
            blacklistedTokens.put("expiredToken", pastDate);
        } catch (Exception e) {
            throw new RuntimeException("无法通过反射修改黑名单状态", e);
        }
        
        // Act - 强制清理过期令牌
        tokenBlacklistService.forceCleanupExpiredTokens();
        
        // Assert
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount(),
                    "清理后应该剩余1个未过期的令牌");
        assertTrue(tokenBlacklistService.isTokenBlacklisted("validToken"),
                  "未过期的令牌应该仍在黑名单中");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("expiredToken"),
                   "过期的令牌应该已被清理");
    }

    @Test
    @DisplayName("重复添加相同令牌到黑名单")
    void testBlacklistSameTokenMultipleTimes() {
        // Arrange
        Date futureDate = new Date(System.currentTimeMillis() + 3600000);
        when(jwtService.extractExpiration(validToken)).thenReturn(futureDate);
        
        // Act
        tokenBlacklistService.blacklistToken(validToken);
        tokenBlacklistService.blacklistToken(validToken);
        tokenBlacklistService.blacklistToken(validToken);
        
        // Assert
        assertEquals(1, tokenBlacklistService.getBlacklistedTokenCount(),
                    "重复添加相同令牌，黑名单中应该只有1个");
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken),
                  "令牌应该在黑名单中");
    }
} 