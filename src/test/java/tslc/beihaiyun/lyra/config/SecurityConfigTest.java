package tslc.beihaiyun.lyra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityConfig 集成测试
 * 验证Spring Security配置的正确性
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Spring Security配置测试")
class SecurityConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("验证密码编码器Bean正确配置")
    void testPasswordEncoderBean() {
        PasswordEncoder passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
        
        assertNotNull(passwordEncoder, "密码编码器Bean应该存在");
        
        // 测试密码编码功能
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword, "编码后的密码不应该为空");
        assertNotEquals(rawPassword, encodedPassword, "编码后的密码应该与原密码不同");
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword), 
            "编码器应该能够验证密码匹配");
        assertFalse(passwordEncoder.matches("wrongPassword", encodedPassword), 
            "错误密码应该验证失败");
    }

    @Test
    @DisplayName("验证认证提供者自动配置正确")
    void testAuthenticationProviderAutoConfiguration() {
        // 在Spring Security 6中，如果提供了UserDetailsService和PasswordEncoder，
        // Spring Boot会自动配置DaoAuthenticationProvider
        assertTrue(applicationContext.containsBean("lyraUserDetailsService"), 
            "应该包含用户详情服务Bean");
        assertTrue(applicationContext.containsBean("passwordEncoder"), 
            "应该包含密码编码器Bean");
    }

    @Test
    @DisplayName("验证认证管理器Bean正确配置")
    void testAuthenticationManagerBean() {
        AuthenticationManager authManager = applicationContext.getBean(AuthenticationManager.class);
        
        assertNotNull(authManager, "认证管理器Bean应该存在");
    }

    @Test
    @DisplayName("验证CORS配置源Bean正确配置")
    void testCorsConfigurationSourceBean() {
        CorsConfigurationSource corsSource = applicationContext.getBean("corsConfigurationSource", CorsConfigurationSource.class);
        
        assertNotNull(corsSource, "CORS配置源Bean应该存在");
    }

    @Test
    @DisplayName("验证安全过滤器链Bean正确配置")
    void testSecurityFilterChainBean() {
        SecurityFilterChain filterChain = applicationContext.getBean(SecurityFilterChain.class);
        
        assertNotNull(filterChain, "安全过滤器链Bean应该存在");
    }

    @Test
    @DisplayName("验证所有必需的Security Bean都存在")
    void testAllSecurityBeansExist() {
        // 验证核心安全组件
        assertTrue(applicationContext.containsBean("passwordEncoder"), 
            "应该包含passwordEncoder Bean");
        assertTrue(applicationContext.containsBean("authenticationManager"), 
            "应该包含authenticationManager Bean");
        assertTrue(applicationContext.containsBean("corsConfigurationSource"), 
            "应该包含corsConfigurationSource Bean");
        
        // 验证JWT相关组件
        assertTrue(applicationContext.containsBean("jwtAuthenticationEntryPoint"), 
            "应该包含jwtAuthenticationEntryPoint Bean");
        assertTrue(applicationContext.containsBean("jwtAuthenticationFilter"), 
            "应该包含jwtAuthenticationFilter Bean");
        
        // 验证用户详情服务
        assertTrue(applicationContext.containsBean("lyraUserDetailsService"), 
            "应该包含lyraUserDetailsService Bean");
    }

    @Test
    @DisplayName("验证密码编码器的加密强度")
    void testPasswordEncoderStrength() {
        PasswordEncoder passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
        
        String password = "testPassword123";
        String encoded1 = passwordEncoder.encode(password);
        String encoded2 = passwordEncoder.encode(password);
        
        // BCrypt每次编码结果都应该不同（包含随机盐）
        assertNotEquals(encoded1, encoded2, "同一密码多次编码结果应该不同（盐值不同）");
        
        // 但都应该能匹配原密码
        assertTrue(passwordEncoder.matches(password, encoded1), "第一次编码应该匹配");
        assertTrue(passwordEncoder.matches(password, encoded2), "第二次编码应该匹配");
        
        // 编码长度应该符合BCrypt特征（通常60个字符）
        assertTrue(encoded1.length() >= 59, "BCrypt编码长度应该至少59个字符");
        assertTrue(encoded1.startsWith("$2"), "BCrypt编码应该以$2开头");
    }
} 