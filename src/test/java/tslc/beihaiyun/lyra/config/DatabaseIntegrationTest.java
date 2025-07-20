package tslc.beihaiyun.lyra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据库集成测试
 * 验证数据库配置、连接池和初始化脚本的正确性
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("数据库集成测试")
class DatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseInitializationConfig.DatabaseHealthChecker healthChecker;

    @Test
    @DisplayName("数据库连接测试")
    void testDatabaseConnection() throws SQLException {
        // 测试数据源不为空
        assertThat(dataSource).isNotNull();

        // 测试能够获取连接
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(10)).isTrue();

            // 获取数据库元信息
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.getDatabaseProductName()).isNotEmpty();
            
            System.out.println("数据库产品: " + metaData.getDatabaseProductName());
            System.out.println("数据库版本: " + metaData.getDatabaseProductVersion());
            System.out.println("驱动名称: " + metaData.getDriverName());
            System.out.println("驱动版本: " + metaData.getDriverVersion());
        }
    }

    @Test
    @DisplayName("数据库健康检查测试")
    void testDatabaseHealthCheck() {
        // 测试健康检查器
        assertThat(healthChecker).isNotNull();
        assertThat(healthChecker.isHealthy()).isTrue();

        // 测试连接信息获取
        String connectionInfo = healthChecker.getConnectionInfo();
        assertThat(connectionInfo).isNotEmpty();
        assertThat(connectionInfo).contains("数据库:");
        
        System.out.println("连接信息: " + connectionInfo);
    }

    @Test
    @DisplayName("JPA配置测试")
    @Transactional
    void testJpaConfiguration() {
        // 测试JdbcTemplate
        assertThat(jdbcTemplate).isNotNull();

        // 测试基本查询
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);

        // 测试日期时间函数（H2兼容）
        String currentTime = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", String.class);
        assertThat(currentTime).isNotEmpty();
        
        System.out.println("当前数据库时间: " + currentTime);
    }

    @Test
    @DisplayName("数据库基础功能验证")
    void testDatabaseBasicFunction() {
        // 在测试环境中，我们创建一个简单的表来验证数据库功能
        assertThatCode(() -> {
            // 创建测试表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS test_table (id BIGINT PRIMARY KEY, name VARCHAR(50))");
            
            // 插入测试数据
            jdbcTemplate.update("INSERT INTO test_table (id, name) VALUES (?, ?)", 1, "test");
            
            // 查询测试数据
            String name = jdbcTemplate.queryForObject(
                "SELECT name FROM test_table WHERE id = ?", String.class, 1);
            assertThat(name).isEqualTo("test");
            
            // 删除测试表
            jdbcTemplate.execute("DROP TABLE test_table");
            
        }).doesNotThrowAnyException();
        
        System.out.println("数据库基础功能验证通过");
    }

    @Test
    @DisplayName("数据库CRUD操作验证")
    @Transactional
    void testCrudOperations() {
        // 创建测试表
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS test_users (id BIGINT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100))");
        
        // 测试插入
        int insertCount = jdbcTemplate.update(
            "INSERT INTO test_users (id, username, email) VALUES (?, ?, ?)", 
            1, "testuser", "test@example.com");
        assertThat(insertCount).isEqualTo(1);

        // 测试查询
        Map<String, Object> user = jdbcTemplate.queryForMap(
            "SELECT * FROM test_users WHERE id = ?", 1);
        assertThat(user.get("username")).isEqualTo("testuser");
        assertThat(user.get("email")).isEqualTo("test@example.com");

        // 测试更新
        int updateCount = jdbcTemplate.update(
            "UPDATE test_users SET email = ? WHERE id = ?", 
            "updated@example.com", 1);
        assertThat(updateCount).isEqualTo(1);

        // 测试删除
        int deleteCount = jdbcTemplate.update("DELETE FROM test_users WHERE id = ?", 1);
        assertThat(deleteCount).isEqualTo(1);

        System.out.println("数据库CRUD操作验证通过");
    }

    @Test
    @DisplayName("数据库连接池配置验证")
    void testConnectionPoolConfiguration() {
        // 测试能够同时获取多个连接
        assertThatCode(() -> {
            for (int i = 0; i < 5; i++) {
                try (var connection = dataSource.getConnection()) {
                    assertThat(connection.isValid(5)).isTrue();
                }
            }
        }).doesNotThrowAnyException();

        // 测试连接池基本信息
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikariDs) {
            assertThat(hikariDs.getMaximumPoolSize()).isGreaterThan(0);
            System.out.println("连接池类型: HikariCP");
            System.out.println("最大连接数: " + hikariDs.getMaximumPoolSize());
        }

        System.out.println("数据库连接池配置验证通过");
    }

    @Test
    @DisplayName("数据库查询性能验证")
    void testQueryPerformance() {
        // 创建测试表用于性能测试
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS perf_test (id BIGINT PRIMARY KEY, data VARCHAR(100))");

        // 插入一些测试数据
        for (int i = 1; i <= 100; i++) {
            jdbcTemplate.update(
                "INSERT INTO perf_test (id, data) VALUES (?, ?)", 
                i, "test_data_" + i);
        }

        long startTime = System.currentTimeMillis();

        // 执行一些查询
        for (int i = 0; i < 10; i++) {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perf_test", Integer.class);
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perf_test WHERE id < 50", Integer.class);
        }

        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;

        // 查询时间应该很短
        assertThat(queryTime).isLessThan(5000);

        // 清理测试数据
        jdbcTemplate.execute("DROP TABLE perf_test");

        System.out.println("数据库查询性能验证通过，查询耗时: " + queryTime + "ms");
    }

    @Test
    @DisplayName("事务处理验证")
    @Transactional
    void testTransactionHandling() {
        // 创建临时表用于事务测试
        jdbcTemplate.execute(
            "CREATE TEMPORARY TABLE temp_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), email VARCHAR(100), password_hash VARCHAR(255), display_name VARCHAR(100))");
        
        // 获取初始记录数量
        Integer initialCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM temp_users", Integer.class);

        // 插入测试用户
        jdbcTemplate.update(
            "INSERT INTO temp_users (username, email, password_hash, display_name) " +
            "VALUES (?, ?, ?, ?)",
            "testuser", "test@example.com", "hashed_password", "测试用户");

        // 验证插入成功
        Integer newCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM temp_users", Integer.class);
        assertThat(newCount).isEqualTo(initialCount + 1);

        // 事务将在测试结束时回滚，不会影响其他测试
        System.out.println("事务处理验证通过");
    }
} 