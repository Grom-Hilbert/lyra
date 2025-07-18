package tslc.beihaiyun.lyra.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据库连接管理器测试类
 */
@ExtendWith(MockitoExtension.class)
class DatabaseConnectionManagerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    private DatabaseConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new DatabaseConnectionManager(dataSource);
    }

    @Test
    void testGetConnection_Success() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);

        // When
        Connection result = connectionManager.getConnection();

        // Then
        assertNotNull(result);
        assertInstanceOf(ManagedConnection.class, result);
        verify(dataSource).getConnection();
    }

    @Test
    void testGetConnection_SQLException() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When & Then
        DatabaseConnectionException exception = assertThrows(
                DatabaseConnectionException.class,
                () -> connectionManager.getConnection()
        );
        
        assertEquals("无法获取数据库连接", exception.getMessage());
        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void testTestConnection_Success() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        // When
        boolean result = connectionManager.testConnection();

        // Then
        assertTrue(result);
        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void testTestConnection_Failed() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        boolean result = connectionManager.testConnection();

        // Then
        assertFalse(result);
    }

    @Test
    void testGetDatabaseInfo_Success() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");
        when(metaData.getDatabaseProductVersion()).thenReturn("1.4.200");
        when(metaData.getDriverName()).thenReturn("H2 JDBC Driver");
        when(metaData.getDriverVersion()).thenReturn("1.4.200");
        when(metaData.getURL()).thenReturn("jdbc:h2:mem:testdb");
        when(metaData.getUserName()).thenReturn("sa");
        when(metaData.supportsTransactions()).thenReturn(true);

        // When
        DatabaseInfo result = connectionManager.getDatabaseInfo();

        // Then
        assertNotNull(result);
        assertEquals("H2", result.getDatabaseProductName());
        assertEquals("1.4.200", result.getDatabaseProductVersion());
        assertEquals("H2 JDBC Driver", result.getDriverName());
        assertEquals("1.4.200", result.getDriverVersion());
        assertEquals("jdbc:h2:mem:testdb", result.getUrl());
        assertEquals("sa", result.getUserName());
        assertTrue(result.isSupportsTransactions());
        verify(connection).close();
    }

    @Test
    void testGetConnectionPoolStats() {
        // When
        ConnectionPoolStats stats = connectionManager.getConnectionPoolStats();

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.getActiveConnections());
        assertEquals(0, stats.getTotalConnections());
    }

    @Test
    void testCheckHealth_Healthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        
        // Set maxPoolSize using reflection to avoid the high usage condition
        java.lang.reflect.Field maxPoolSizeField = DatabaseConnectionManager.class.getDeclaredField("maxPoolSize");
        maxPoolSizeField.setAccessible(true);
        maxPoolSizeField.set(connectionManager, 10);

        // When
        HealthStatus health = connectionManager.checkHealth();

        // Then
        assertNotNull(health);
        assertTrue(health.isHealthy());
        assertEquals("数据库连接正常", health.getMessage());
    }

    @Test
    void testCheckHealth_Unhealthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        // When
        HealthStatus health = connectionManager.checkHealth();

        // Then
        assertNotNull(health);
        assertFalse(health.isHealthy());
        assertEquals("数据库连接测试失败", health.getMessage());
    }

    @Test
    void testReleaseConnection_Success() throws Exception {
        // Given
        when(connection.isClosed()).thenReturn(false);

        // When
        connectionManager.releaseConnection(connection);

        // Then
        verify(connection).close();
    }

    @Test
    void testReleaseConnection_AlreadyClosed() throws Exception {
        // Given
        when(connection.isClosed()).thenReturn(true);

        // When
        connectionManager.releaseConnection(connection);

        // Then
        verify(connection, never()).close();
    }

    @Test
    void testReleaseConnection_Null() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> connectionManager.releaseConnection(null));
    }
}