package tslc.beihaiyun.lyra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * 测试环境专用的安全配置
 * 只在test profile下生效，不影响生产环境
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Configuration
@Profile("test")
public class TestSecurityConfig {

    /**
     * 测试环境的密码编码器
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 测试环境的UserDetailsService
     * 提供简单的内存用户，避免循环依赖
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService(PasswordEncoder passwordEncoder) {
        return username -> {
            switch (username) {
                case "admin":
                    return User.builder()
                            .username("admin")
                            .password(passwordEncoder.encode("admin"))
                            .authorities(List.of(
                                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                                    new SimpleGrantedAuthority("ROLE_USER")
                            ))
                            .build();
                            
                case "testuser":
                    return User.builder()
                            .username("testuser")
                            .password(passwordEncoder.encode("password"))
                            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                            .build();
                            
                case "user":
                    return User.builder()
                            .username("user")
                            .password(passwordEncoder.encode("password"))
                            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                            .build();
                            
                default:
                    throw new UsernameNotFoundException("User not found: " + username);
            }
        };
    }
} 