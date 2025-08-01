package tslc.beihaiyun.lyra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA 配置类
 * 负责JPA审计和Repository扫描配置
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "tslc.beihaiyun.lyra.repository")
public class JpaConfig {

    /**
     * 审计信息提供者
     * 获取当前操作用户信息用于JPA审计
     * 
     * @return 审计信息提供者
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            
            String name = authentication.getName();
            if (name == null || name.trim().isEmpty()) {
                return Optional.of("system");
            }
            
            return Optional.of(name);
        };
    }
} 