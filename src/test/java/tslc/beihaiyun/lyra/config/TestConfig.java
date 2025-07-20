package tslc.beihaiyun.lyra.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 测试配置类
 * 提供测试环境专用的配置和Bean
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * 空的CommandLineRunner，避免数据库初始化在测试中执行
     */
    @Bean
    @Primary
    public CommandLineRunner testDatabaseInitializer() {
        return args -> {
            // 测试环境不执行数据库初始化
        };
    }
} 