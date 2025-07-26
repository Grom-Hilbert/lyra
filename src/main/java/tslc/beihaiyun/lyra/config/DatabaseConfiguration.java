package tslc.beihaiyun.lyra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;

/**
 * 数据库自动配置类
 * 
 * @author Lyra Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class DatabaseConfiguration {

    @Autowired
    private DatabaseProperties databaseProperties;

    /**
     * 配置数据源
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        log.info("配置数据库连接 - 类型: {}", databaseProperties.getType());
        
        // 如果是SQLite，确保目录存在
        if (databaseProperties.isFileDatabase()) {
            ensureDirectoryExists(databaseProperties.getFilePath());
        }
        
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        config.setDriverClassName(databaseProperties.getDriverClassName());
        config.setJdbcUrl(databaseProperties.buildJdbcUrl());
        
        // 网络数据库需要用户名密码
        if (databaseProperties.isNetworkDatabase()) {
            config.setUsername(databaseProperties.getEffectiveUsername());
            config.setPassword(databaseProperties.getEffectivePassword());
        }
        
        // 连接池配置
        config.setMaximumPoolSize(databaseProperties.getMaxPoolSize());
        config.setMinimumIdle(databaseProperties.getMinIdle());
        config.setConnectionTimeout(databaseProperties.getConnectionTimeout());
        config.setIdleTimeout(databaseProperties.getIdleTimeout());
        config.setMaxLifetime(databaseProperties.getMaxLifetime());
        
        // 连接池名称
        config.setPoolName("LyraHikariPool");
        
        // 数据库特定配置
        configureForDatabaseType(config, databaseProperties.getType());
        
        log.info("数据库连接配置完成 - URL: {}", databaseProperties.buildJdbcUrl());
        
        return new HikariDataSource(config);
    }
    
    /**
     * 为不同数据库类型配置特定参数
     */
    private void configureForDatabaseType(HikariConfig config, DatabaseType type) {
        switch (type) {
            case SQLITE:
                // SQLite特定配置
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                // SQLite通常不需要太多连接
                config.setMaximumPoolSize(Math.min(config.getMaximumPoolSize(), 5));
                break;
                
            case MYSQL:
                // MySQL特定配置
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
                break;
                
            case POSTGRESQL:
                // PostgreSQL特定配置
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("reWriteBatchedInserts", "true");
                break;
                
            default:
                log.warn("未知的数据库类型: {}", type);
        }
    }
    
    /**
     * 确保SQLite数据库文件的目录存在
     */
    private void ensureDirectoryExists(String filePath) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    log.info("创建数据库目录: {}", parentDir.getAbsolutePath());
                } else {
                    log.warn("无法创建数据库目录: {}", parentDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("创建数据库目录时发生错误", e);
        }
    }
}
