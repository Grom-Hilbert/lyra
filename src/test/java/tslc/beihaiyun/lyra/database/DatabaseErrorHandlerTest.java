package tslc.beihaiyun.lyra.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库错误处理器测试类
 */
class DatabaseErrorHandlerTest {

    private DatabaseErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new DatabaseErrorHandler();
    }

    @Test
    void testHandleSQLException_ConnectionFailed() {
        // Given
        SQLException sqlException = new SQLException("Connection refused", "08001", 1);

        // When
        DatabaseConnectionException result = errorHandler.handleSQLException(sqlException, "测试连接");

        // Then
        assertNotNull(result);
        assertEquals(DatabaseErrorType.CONNECTION_FAILED, result.getErrorType());
        assertTrue(result.getMessage().contains("测试连接"));
        assertEquals(sqlException, result.getCause());
    }

    @Test
    void testHandleSQLException_DuplicateKey() {
        // Given
        SQLException sqlException = new SQLException("Duplicate entry", "23000", 1062);

        // When
        DatabaseConnectionException result = errorHandler.handleSQLException(sqlException, "插入数据");

        // Then
        assertNotNull(result);
        assertEquals(DatabaseErrorType.DUPLICATE_KEY, result.getErrorType());
        assertTrue(result.getMessage().contains("插入数据"));
    }

    @Test
    void testHandleSQLException_Timeout() {
        // Given
        SQLException sqlException = new SQLException("Connection timeout", null, 0);

        // When
        DatabaseConnectionException result = errorHandler.handleSQLException(sqlException, "查询数据");

        // Then
        assertNotNull(result);
        assertEquals(DatabaseErrorType.CONNECTION_TIMEOUT, result.getErrorType());
    }

    @Test
    void testIsRetryable_ConnectionTimeout() {
        // Given
        SQLException sqlException = new SQLException("Connection timeout", null, 0);
        DatabaseConnectionException exception = errorHandler.handleSQLException(sqlException, "测试");

        // When
        boolean result = errorHandler.isRetryable(exception);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRetryable_SyntaxError() {
        // Given
        SQLException sqlException = new SQLException("Syntax error", "42000", 1064);
        DatabaseConnectionException exception = errorHandler.handleSQLException(sqlException, "测试");

        // When
        boolean result = errorHandler.isRetryable(exception);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetRetryAdvice_ConnectionTimeout() {
        // Given
        SQLException sqlException = new SQLException("Connection timeout", null, 0);
        DatabaseConnectionException exception = errorHandler.handleSQLException(sqlException, "测试");

        // When
        DatabaseErrorHandler.RetryAdvice advice = errorHandler.getRetryAdvice(exception);

        // Then
        assertNotNull(advice);
        assertTrue(advice.isShouldRetry());
        assertEquals(3, advice.getMaxRetries());
        assertEquals(1000, advice.getDelayMillis());
        assertEquals(2.0, advice.getBackoffMultiplier());
        assertNotNull(advice.getAdvice());
    }

    @Test
    void testGetRetryAdvice_SyntaxError() {
        // Given
        SQLException sqlException = new SQLException("Syntax error", "42000", 1064);
        DatabaseConnectionException exception = errorHandler.handleSQLException(sqlException, "测试");

        // When
        DatabaseErrorHandler.RetryAdvice advice = errorHandler.getRetryAdvice(exception);

        // Then
        assertNotNull(advice);
        assertFalse(advice.isShouldRetry());
        assertEquals(0, advice.getMaxRetries());
    }

    @Test
    void testRecordError() {
        // Given
        SQLException sqlException = new SQLException("Test error", "08001", 1);
        DatabaseConnectionException exception = errorHandler.handleSQLException(sqlException, "测试");

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> errorHandler.recordError(exception));
    }

    @Test
    void testDatabaseConnectionException_DetailedMessage() {
        // Given
        SQLException cause = new SQLException("Original error");
        DatabaseConnectionException exception = new DatabaseConnectionException(
                "Test message", cause, "TEST_001", DatabaseErrorType.CONNECTION_FAILED);

        // When
        String detailedMessage = exception.getDetailedMessage();

        // Then
        assertNotNull(detailedMessage);
        assertTrue(detailedMessage.contains("无法建立数据库连接")); // Use the actual description
        assertTrue(detailedMessage.contains("TEST_001"));
        assertTrue(detailedMessage.contains("Test message"));
        assertTrue(detailedMessage.contains("Original error"));
    }
}