package tslc.beihaiyun.lyra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * 数据库配置类
 * 支持多种数据库：SQLite（默认）、MySQL、PostgreSQL
 */
@Configuration
@EnableJpaRepositories(basePackages = "tslc.beihaiyun.lyra.repository")
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * SQLite数据源配置（默认）
     */
    @Bean
    @Profile({"default", "sqlite"})
    @ConfigurationProperties(prefix = "spring.datasource.sqlite")
    public DataSource sqliteDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:./data/lyra.db")
                .build();
    }

    /**
     * MySQL数据源配置
     */
    @Bean
    @Profile("mysql")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    /**
     * PostgreSQL数据源配置
     */
    @Bean
    @Profile("postgresql")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    public DataSource postgresqlDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}