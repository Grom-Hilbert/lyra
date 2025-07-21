package tslc.beihaiyun.lyra.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.ApplicationContext;
import org.mockito.Mockito;

import tslc.beihaiyun.lyra.config.LyraProperties;

/**
 * TokenBlacklistService测试类
 * 测试令牌黑名单服务的所有功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@DisplayName("TokenBlacklistService测试")
class TokenBlacklistServiceTest {

    private LyraProperties lyraProperties;
    private JwtService jwtService;
    private TokenBlacklistService tokenBlacklistService;
    private UserDetails testUserDetails;
    private String validToken;

    @BeforeEach
    void setUp() {
        // 创建真实的配置对象
        lyraProperties = new LyraProperties();
        lyraProperties.getJwt().setSecret("ThisIsATestSecretKeyForJWTThatIsLongEnoughForHS256Algorithm");
        lyraProperties.getJwt().setExpiration(3600000L); // 1小时
        lyraProperties.getJwt().setRefreshExpiration(86400000L); // 24小时

        tokenBlacklistService = new TokenBlacklistService(lyraProperties);

        // 创建模拟的JwtService用于生成测试令牌
        ApplicationContext mockApplicationContext = Mockito.mock(ApplicationContext.class);
        jwtService = new JwtService(lyraProperties, mockApplicationContext);

        // 清空黑名单
        tokenBlacklistService.clearAllBlacklistedTokens();

        // 创建测试用户详情
        testUserDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
                
        // 生成有效的测试令牌
        validToken = jwtService.generateToken(testUserDetails);
    }

    @Test
    @DisplayName("将有效令牌添加到黑名单")
    void testBlacklistValidToken() {
        // Act
        tokenBlacklistService.blacklistToken(validToken);
        
        // Assert
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken), 
                "有效令牌应该被成功添加到黑名单");
        assertEquals(1, tokenBlacklistService.getBlacklistSize(),
                "黑名单中应该有1个令牌");
    }

    @Test
    @DisplayName("清空黑名单")
    void testClearBlacklist() {
        // Arrange - 添加一些令牌到黑名单
        tokenBlacklistService.blacklistToken("token1");
        tokenBlacklistService.blacklistToken("token2");
        
        // Act
        tokenBlacklistService.clearAllBlacklistedTokens();
        
        // Assert
        assertEquals(0, tokenBlacklistService.getBlacklistSize(),
                "清空后黑名单应该为空");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("token1"),
                "token1应该不在黑名单中");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("token2"),
                "token2应该不在黑名单中");
    }

    @Test
    @DisplayName("检查非黑名单令牌")
    void testIsTokenBlacklistedForNonBlacklistedToken() {
        // Act & Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                "非黑名单令牌应该返回false");
    }

    @Test
    @DisplayName("检查黑名单令牌")
    void testIsTokenBlacklistedForBlacklistedToken() {
        // Arrange
        tokenBlacklistService.blacklistToken(validToken);
        
        // Act & Assert
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken),
                "黑名单令牌应该返回true");
    }

    @Test
    @DisplayName("空或无效令牌不应该在黑名单中")
    void testIsTokenBlacklistedForInvalidTokens() {
        // Act & Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null),
                "null令牌应该返回false");
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""),
                "空令牌应该返回false");
        assertFalse(tokenBlacklistService.isTokenBlacklisted("   "),
                "空白令牌应该返回false");
    }

    @Test
    @DisplayName("处理空令牌添加到黑名单")
    void testBlacklistNullToken() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> tokenBlacklistService.blacklistToken(null),
                "添加空令牌到黑名单应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
                () -> tokenBlacklistService.blacklistToken(""),
                "添加空字符串令牌到黑名单应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, 
                () -> tokenBlacklistService.blacklistToken("   "),
                "添加空白令牌到黑名单应该抛出异常");
    }

    @Test
    @DisplayName("从黑名单移除令牌")
    void testRemoveTokenFromBlacklist() {
        // Arrange
        tokenBlacklistService.blacklistToken(validToken);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken), 
                "令牌应该在黑名单中");
        
        // Act
        tokenBlacklistService.removeTokenFromBlacklist(validToken);
        
        // Assert
        assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken), "令牌应该不再在黑名单中");
        assertEquals(0, tokenBlacklistService.getBlacklistSize(),
                "黑名单应该为空");
    }

    @Test
    @DisplayName("移除不存在的令牌不应该抛出异常")
    void testRemoveNonExistentToken() {
        // Act & Assert - 移除不存在的令牌不应该抛出异常
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(validToken),
                "移除不存在的令牌不应该抛出异常");
    }

    @Test
    @DisplayName("处理空值移除操作")
    void testRemoveInvalidTokens() {
        // Act & Assert - 这些操作不应该抛出异常
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(null),
                "空令牌不应该抛出异常");
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist(""),
                "空字符串令牌不应该抛出异常");
        assertDoesNotThrow(() -> tokenBlacklistService.removeTokenFromBlacklist("   "),
                "空白令牌不应该抛出异常");
    }

    @Test
    @DisplayName("获取黑名单大小")
    void testGetBlacklistSize() {
        // 初始状态
        assertEquals(0, tokenBlacklistService.getBlacklistSize(),
                "初始黑名单应该为空");
        
        // 添加令牌
        tokenBlacklistService.blacklistToken("token1");
        assertEquals(1, tokenBlacklistService.getBlacklistSize(),
                "添加1个令牌后应该为1");
        
        tokenBlacklistService.blacklistToken("token2");
        assertEquals(2, tokenBlacklistService.getBlacklistSize(),
                "添加2个令牌后应该为2");
        
        // 移除令牌
        tokenBlacklistService.removeTokenFromBlacklist("token1");
        assertEquals(1, tokenBlacklistService.getBlacklistSize(),
                "移除1个令牌后应该为1");
        
        // 清空黑名单
        tokenBlacklistService.clearAllBlacklistedTokens();
        assertEquals(0, tokenBlacklistService.getBlacklistSize(),
                "清空后应该为0");
    }

    @Test
    @DisplayName("批量操作测试")
    void testBatchOperations() {
        // 批量添加
        String[] tokens = {"token1", "token2", "token3"};
        for (String token : tokens) {
            tokenBlacklistService.blacklistToken(token);
        }
        
        assertEquals(3, tokenBlacklistService.getBlacklistSize(),
                "批量添加后应该有3个令牌");
        
        // 验证所有令牌都在黑名单中
        for (String token : tokens) {
            assertTrue(tokenBlacklistService.isTokenBlacklisted(token),
                    "令牌 " + token + " 应该在黑名单中");
        }
    }

    @Test
    @DisplayName("即将过期令牌检查")
    void testIsTokenExpiringSoon() {
        // 使用生成的有效令牌测试
        
        // 检查令牌是否即将在1小时内过期
        boolean expiringSoon = tokenBlacklistService.isTokenExpiringSoon(validToken, 120); // 2小时
        assertTrue(expiringSoon, "1小时有效期的令牌在2小时阈值内应该被视为即将过期");
        
        // 检查令牌是否即将在30分钟内过期
        boolean expiringSoonShort = tokenBlacklistService.isTokenExpiringSoon(validToken, 30); // 30分钟
        assertFalse(expiringSoonShort, "1小时有效期的令牌在30分钟阈值内不应该被视为即将过期");
        
        // 测试无效令牌
        assertFalse(tokenBlacklistService.isTokenExpiringSoon("invalid.token", 60),
                "无效令牌应该返回false");
        assertFalse(tokenBlacklistService.isTokenExpiringSoon(null, 60),
                "null令牌应该返回false");
    }
} 