package tslc.beihaiyun.lyra.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据库连接管理器
 * 负责管理数据库连接的生命周期、监控连接状态和提供连接统计信息
 */
@Slf4j
@Component
public class DatabaseConnectionManager {

    private final DataSource dataSource;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Autowired
    public DatabaseConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        log.info("数据库连接管理器初始化完成");
    }

    /**
     * 获取数据库连接
     * 
     * @return 数据库连接
     * @throws DatabaseConnectionException 连接获取失败时抛出
     */
    public Connection getConnection() throws DatabaseConnectionException {
        try {
            Connection connection = dataSource.getConnection();
            activeConnections.incrementAndGet();
            totalConnections.incrementAndGet();
            
            log.debug("获取数据库连接成功，当前活跃连接数: {}", activeConnections.get());
            return new ManagedConnection(connection, this);
            
        } catch (SQLException e) {
            log.error("获取数据库连接失败", e);
            throw new DatabaseConnectionException("无法获取数据库连接", e);
        }
    }

    /**
     * 释放数据库连接
     * 
     * @param connection 要释放的连接
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    activeConnections.decrementAndGet();
                    log.debug("释放数据库连接成功，当前活跃连接数: {}", activeConnections.get());
                }
            } catch (SQLException e) {
                log.warn("释放数据库连接时发生异常", e);
            }
        }
    }

    /**
     * 测试数据库连接
     * 
     * @return 连接是否正常
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return false;
        }
    }

    /**
     * 获取数据库元数据信息
     * 
     * @return 数据库信息
     * @throws DatabaseConnectionException 获取失败时抛出
     */
    public DatabaseInfo getDatabaseInfo() throws DatabaseConnectionException {
        try (Connection connection = getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            return DatabaseInfo.builder()
                    .databaseProductName(metaData.getDatabaseProductName())
                    .databaseProductVersion(metaData.getDatabaseProductVersion())
                    .driverName(metaData.getDriverName())
                    .driverVersion(metaData.getDriverVersion())
                    .url(metaData.getURL())
                    .userName(metaData.getUserName())
                    .supportsTransactions(metaData.supportsTransactions())
                    .build();
                    
        } catch (SQLException e) {
            log.error("获取数据库元数据失败", e);
            throw new DatabaseConnectionException("无法获取数据库元数据", e);
        }
    }

    /**
     * 获取连接池统计信息
     * 
     * @return 连接池统计信息
     */
    public ConnectionPoolStats getConnectionPoolStats() {
        return ConnectionPoolStats.builder()
                .activeConnections(activeConnections.get())
                .totalConnections(totalConnections.get())
                .maxPoolSize(maxPoolSize)
                .connectionTimeout(connectionTimeout)
                .build();
    }

    /**
     * 检查连接池健康状态
     * 
     * @return 健康状态
     */
    public HealthStatus checkHealth() {
        try {
            boolean connectionValid = testConnection();
            int activeCount = activeConnections.get();
            
            if (!connectionValid) {
                return HealthStatus.builder()
                        .healthy(false)
                        .message("数据库连接测试失败")
                        .build();
            }
            
            if (activeCount >= maxPoolSize * 0.9) {
                return HealthStatus.builder()
                        .healthy(false)
                        .message("连接池使用率过高: " + activeCount + "/" + maxPoolSize)
                        .build();
            }
            
            return HealthStatus.builder()
                    .healthy(true)
                    .message("数据库连接正常")
                    .activeConnections(activeCount)
                    .maxConnections(maxPoolSize)
                    .build();
                    
        } catch (Exception e) {
            log.error("检查数据库健康状态失败", e);
            return HealthStatus.builder()
                    .healthy(false)
                    .message("健康检查异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 强制关闭所有连接（仅用于应用关闭时）
     */
    public void shutdown() {
        log.info("开始关闭数据库连接管理器，当前活跃连接数: {}", activeConnections.get());
        // 这里可以添加更多的清理逻辑
        log.info("数据库连接管理器关闭完成");
    }
}