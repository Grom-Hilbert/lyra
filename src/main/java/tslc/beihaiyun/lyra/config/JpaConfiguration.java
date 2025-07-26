package tslc.beihaiyun.lyra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA自动配置类
 * 根据数据库类型自动配置Hibernate方言
 * 
 * @author Lyra Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class JpaConfiguration {

    @Autowired
    private DatabaseProperties databaseProperties;

    @Autowired
    private JpaProperties jpaProperties;

    /**
     * 根据数据库类型配置JPA属性
     */
    @PostConstruct
    public void configureJpaProperties() {
        DatabaseType dbType = databaseProperties.getType();
        log.info("配置JPA属性 - 数据库类型: {}", dbType);
        
        Map<String, String> properties = jpaProperties.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            jpaProperties.setProperties(properties);
        }
        
        // 根据数据库类型设置方言
        String dialect = getDialectForDatabaseType(dbType);
        if (dialect != null) {
            properties.put("hibernate.dialect", dialect);
            log.info("设置Hibernate方言: {}", dialect);
        }
        
        // 数据库特定配置
        configureForDatabaseType(properties, dbType);
    }
    
    /**
     * 获取数据库类型对应的Hibernate方言
     */
    private String getDialectForDatabaseType(DatabaseType type) {
        switch (type) {
            case SQLITE:
                return "org.hibernate.community.dialect.SQLiteDialect";
            case MYSQL:
                return "org.hibernate.dialect.MySQL8Dialect";
            case POSTGRESQL:
                return "org.hibernate.dialect.PostgreSQLDialect";
            default:
                log.warn("未知的数据库类型: {}, 使用默认方言", type);
                return null;
        }
    }
    
    /**
     * 为不同数据库类型配置特定的JPA属性
     */
    private void configureForDatabaseType(Map<String, String> properties, DatabaseType type) {
        switch (type) {
            case SQLITE:
                // SQLite特定配置
                properties.put("hibernate.connection.provider_disables_autocommit", "true");
                properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
                break;
                
            case MYSQL:
                // MySQL特定配置
                properties.put("hibernate.connection.characterEncoding", "utf8");
                properties.put("hibernate.connection.CharSet", "utf8");
                properties.put("hibernate.connection.useUnicode", "true");
                break;
                
            case POSTGRESQL:
                // PostgreSQL特定配置
                properties.put("hibernate.jdbc.lob.non_contextual_creation", "true");
                break;
                
            default:
                // 默认配置
                break;
        }
    }
}
