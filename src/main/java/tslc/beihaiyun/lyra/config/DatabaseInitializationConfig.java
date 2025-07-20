package tslc.beihaiyun.lyra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库初始化配置类
 * 负责数据库脚本执行、健康检查和初始化验证
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Slf4j
@Configuration
public class DatabaseInitializationConfig {

    @Value("${lyra.database.init.enabled:false}")
    private boolean initEnabled;

    /**
     * 数据库初始化器
     * 在应用启动时执行数据库初始化脚本
     * 
     * @param dataSource 数据源
     * @param jdbcTemplate JDBC模板
     * @return 命令行执行器
     */
    @Bean
    @ConditionalOnProperty(value = "lyra.database.init.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner databaseInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("开始数据库初始化检查...");
            
            try {
                // 检查数据库连接
                if (isDatabaseHealthy(dataSource)) {
                    log.info("数据库连接正常");
                    
                    // 检查是否需要初始化数据
                    if (shouldInitializeData(jdbcTemplate)) {
                        log.info("开始执行数据库初始化脚本...");
                        initializeDatabase(dataSource);
                        log.info("数据库初始化完成");
                    } else {
                        log.info("数据库已存在数据，跳过初始化");
                    }
                    
                    // 验证初始化结果
                    validateInitialization(jdbcTemplate);
                    
                } else {
                    log.error("数据库连接失败，无法进行初始化");
                    throw new RuntimeException("数据库连接失败");
                }
                
            } catch (Exception e) {
                log.error("数据库初始化过程中发生错误", e);
                log.warn("继续启动应用，但数据库可能未正确初始化");
            }
        };
    }

    /**
     * 检查数据库连接是否健康
     * 
     * @param dataSource 数据源
     * @return 是否健康
     */
    private boolean isDatabaseHealthy(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(30); // 30秒超时
        } catch (SQLException e) {
            log.error("数据库连接检查失败", e);
            return false;
        }
    }

    /**
     * 检查是否需要初始化数据
     * 通过检查核心表是否存在数据来判断
     * 
     * @param jdbcTemplate JDBC模板
     * @return 是否需要初始化
     */
    private boolean shouldInitializeData(JdbcTemplate jdbcTemplate) {
        try {
            // 检查roles表是否存在数据
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE is_system = TRUE", Integer.class);
            
            if (roleCount != null && roleCount > 0) {
                log.info("发现系统角色数据 {} 条，数据库已初始化", roleCount);
                return false;
            }
            
            log.info("未发现系统角色数据，需要执行初始化");
            return true;
            
        } catch (Exception e) {
            log.warn("检查初始化状态时发生错误，将执行初始化: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 执行数据库初始化脚本
     * 
     * @param dataSource 数据源
     */
    @Transactional
    private void initializeDatabase(DataSource dataSource) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setContinueOnError(false);
            populator.setIgnoreFailedDrops(true);
            
            // 执行数据库模式脚本（如果需要）
            log.info("执行数据库模式脚本");
            populator.addScript(new ClassPathResource("db/schema.sql"));
            
            // 总是执行数据初始化脚本
            log.info("执行数据初始化脚本");
            populator.addScript(new ClassPathResource("db/data.sql"));
            
            populator.execute(dataSource);
            log.info("数据库初始化脚本执行成功");
            
        } catch (Exception e) {
            log.error("执行数据库初始化脚本失败", e);
            throw new RuntimeException("数据库初始化脚本执行失败", e);
        }
    }

    /**
     * 验证数据库初始化结果
     * 
     * @param jdbcTemplate JDBC模板
     */
    private void validateInitialization(JdbcTemplate jdbcTemplate) {
        try {
            log.info("验证数据库初始化结果...");
            
            // 验证基础角色
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE is_system = TRUE", Integer.class);
            log.info("系统角色数量: {}", roleCount);
            
            // 验证基础权限
            Integer permissionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions", Integer.class);
            log.info("权限数量: {}", permissionCount);
            
            // 验证管理员用户
            Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'admin'", Integer.class);
            log.info("管理员用户: {}", adminCount > 0 ? "存在" : "不存在");
            
            // 验证系统配置
            Integer configCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_config", Integer.class);
            log.info("系统配置项数量: {}", configCount);
            
            // 验证关键数据完整性
            if (roleCount < 3 || permissionCount < 10 || adminCount < 1) {
                throw new RuntimeException("数据库初始化验证失败：关键数据缺失");
            }
            
            log.info("数据库初始化验证通过");
            
        } catch (Exception e) {
            log.error("数据库初始化验证失败", e);
            throw new RuntimeException("数据库初始化验证失败", e);
        }
    }

    /**
     * 数据库健康检查Bean
     * 提供应用运行时的数据库健康状态检查
     * 
     * @param dataSource 数据源
     * @return 健康检查器
     */
    @Bean
    public DatabaseHealthChecker databaseHealthChecker(DataSource dataSource) {
        return new DatabaseHealthChecker(dataSource);
    }

    /**
     * 数据库健康检查器
     */
    public static class DatabaseHealthChecker {
        private final DataSource dataSource;

        public DatabaseHealthChecker(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * 检查数据库是否可用
         * 
         * @return 是否可用
         */
        public boolean isHealthy() {
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(10);
            } catch (SQLException e) {
                log.warn("数据库健康检查失败", e);
                return false;
            }
        }

        /**
         * 获取数据库连接信息
         * 
         * @return 连接信息
         */
        public String getConnectionInfo() {
            try (Connection connection = dataSource.getConnection()) {
                return String.format("数据库: %s, 驱动: %s, 版本: %s",
                    connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDriverName(),
                    connection.getMetaData().getDatabaseProductVersion());
            } catch (SQLException e) {
                return "无法获取连接信息: " + e.getMessage();
            }
        }
    }
} 