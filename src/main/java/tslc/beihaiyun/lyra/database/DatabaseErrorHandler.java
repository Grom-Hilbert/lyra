package tslc.beihaiyun.lyra.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 数据库错误处理工具
 * 提供统一的数据库异常处理和错误恢复机制
 */
@Slf4j
@Component
public class DatabaseErrorHandler {

    private final Map<DatabaseErrorType, Function<SQLException, String>> errorMessageHandlers;
    private final Map<DatabaseErrorType, Function<SQLException, Boolean>> retryableCheckers;

    public DatabaseErrorHandler() {
        this.errorMessageHandlers = initializeErrorMessageHandlers();
        this.retryableCheckers = initializeRetryableCheckers();
    }

    /**
     * 处理SQL异常并转换为业务异常
     * 
     * @param e SQL异常
     * @param operation 操作描述
     * @return 数据库连接异常
     */
    public DatabaseConnectionException handleSQLException(SQLException e, String operation) {
        DatabaseErrorType errorType = DatabaseErrorType.fromSQLException(e);
        String errorCode = generateErrorCode(errorType, e);
        String message = generateErrorMessage(errorType, e, operation);
        
        log.error("数据库操作异常 - 操作: {}, 错误类型: {}, 错误代码: {}, 原始异常: {}", 
                operation, errorType.getName(), errorCode, e.getMessage(), e);
        
        return new DatabaseConnectionException(message, e, errorCode, errorType);
    }

    /**
     * 判断异常是否可以重试
     * 
     * @param exception 数据库异常
     * @return 是否可以重试
     */
    public boolean isRetryable(DatabaseConnectionException exception) {
        DatabaseErrorType errorType = exception.getErrorType();
        
        if (exception.getCause() instanceof SQLException) {
            SQLException sqlException = (SQLException) exception.getCause();
            Function<SQLException, Boolean> checker = retryableCheckers.get(errorType);
            if (checker != null) {
                return checker.apply(sqlException);
            }
        }
        
        // 默认的重试策略
        switch (errorType) {
            case CONNECTION_TIMEOUT:
            case CONNECTION_POOL_EXHAUSTED:
            case DEADLOCK:
            case TRANSACTION_ROLLBACK:
                return true;
            case CONNECTION_FAILED:
            case AUTHENTICATION_FAILED:
            case DATABASE_NOT_FOUND:
            case TABLE_NOT_FOUND:
            case SYNTAX_ERROR:
            case PERMISSION_DENIED:
                return false;
            default:
                return false;
        }
    }

    /**
     * 获取重试建议
     * 
     * @param exception 数据库异常
     * @return 重试建议
     */
    public RetryAdvice getRetryAdvice(DatabaseConnectionException exception) {
        DatabaseErrorType errorType = exception.getErrorType();
        
        switch (errorType) {
            case CONNECTION_TIMEOUT:
                return RetryAdvice.builder()
                        .shouldRetry(true)
                        .maxRetries(3)
                        .delayMillis(1000)
                        .backoffMultiplier(2.0)
                        .advice("连接超时，建议增加超时时间或检查网络连接")
                        .build();
                        
            case CONNECTION_POOL_EXHAUSTED:
                return RetryAdvice.builder()
                        .shouldRetry(true)
                        .maxRetries(5)
                        .delayMillis(500)
                        .backoffMultiplier(1.5)
                        .advice("连接池耗尽，建议稍后重试或增加连接池大小")
                        .build();
                        
            case DEADLOCK:
                return RetryAdvice.builder()
                        .shouldRetry(true)
                        .maxRetries(3)
                        .delayMillis(100)
                        .backoffMultiplier(2.0)
                        .advice("发生死锁，建议随机延迟后重试")
                        .build();
                        
            case TRANSACTION_ROLLBACK:
                return RetryAdvice.builder()
                        .shouldRetry(true)
                        .maxRetries(2)
                        .delayMillis(200)
                        .backoffMultiplier(1.0)
                        .advice("事务回滚，建议检查数据完整性后重试")
                        .build();
                        
            default:
                return RetryAdvice.builder()
                        .shouldRetry(false)
                        .maxRetries(0)
                        .delayMillis(0)
                        .backoffMultiplier(1.0)
                        .advice("该错误类型不建议重试，请检查代码逻辑或数据")
                        .build();
        }
    }

    /**
     * 生成用户友好的错误消息
     * 
     * @param errorType 错误类型
     * @param e SQL异常
     * @param operation 操作描述
     * @return 错误消息
     */
    private String generateErrorMessage(DatabaseErrorType errorType, SQLException e, String operation) {
        Function<SQLException, String> handler = errorMessageHandlers.get(errorType);
        if (handler != null) {
            return String.format("执行操作 '%s' 时发生错误: %s", operation, handler.apply(e));
        }
        
        return String.format("执行操作 '%s' 时发生数据库错误: %s", operation, errorType.getDescription());
    }

    /**
     * 生成错误代码
     * 
     * @param errorType 错误类型
     * @param e SQL异常
     * @return 错误代码
     */
    private String generateErrorCode(DatabaseErrorType errorType, SQLException e) {
        return String.format("DB_%s_%d", errorType.name(), Math.abs(e.getMessage().hashCode() % 10000));
    }

    /**
     * 初始化错误消息处理器
     */
    private Map<DatabaseErrorType, Function<SQLException, String>> initializeErrorMessageHandlers() {
        Map<DatabaseErrorType, Function<SQLException, String>> handlers = new HashMap<>();
        
        handlers.put(DatabaseErrorType.CONNECTION_FAILED, e -> 
            "无法连接到数据库，请检查数据库服务是否正常运行");
            
        handlers.put(DatabaseErrorType.CONNECTION_TIMEOUT, e -> 
            "数据库连接超时，请检查网络连接或增加超时时间");
            
        handlers.put(DatabaseErrorType.AUTHENTICATION_FAILED, e -> 
            "数据库认证失败，请检查用户名和密码是否正确");
            
        handlers.put(DatabaseErrorType.DATABASE_NOT_FOUND, e -> 
            "指定的数据库不存在，请检查数据库名称是否正确");
            
        handlers.put(DatabaseErrorType.TABLE_NOT_FOUND, e -> 
            "指定的数据表不存在，请检查表名是否正确或执行数据库迁移");
            
        handlers.put(DatabaseErrorType.DUPLICATE_KEY, e -> 
            "数据重复，违反了唯一性约束");
            
        handlers.put(DatabaseErrorType.FOREIGN_KEY_VIOLATION, e -> 
            "违反外键约束，请检查关联数据是否存在");
            
        handlers.put(DatabaseErrorType.CONSTRAINT_VIOLATION, e -> 
            "违反数据库约束条件，请检查数据的完整性");
            
        handlers.put(DatabaseErrorType.DEADLOCK, e -> 
            "数据库操作发生死锁，系统将自动重试");
            
        handlers.put(DatabaseErrorType.SYNTAX_ERROR, e -> 
            "SQL语句语法错误，请检查SQL语句的正确性");
            
        handlers.put(DatabaseErrorType.PERMISSION_DENIED, e -> 
            "权限不足，无法执行该数据库操作");
            
        handlers.put(DatabaseErrorType.DATA_TOO_LONG, e -> 
            "数据长度超过字段限制，请减少数据长度");
            
        return handlers;
    }

    /**
     * 初始化重试检查器
     */
    private Map<DatabaseErrorType, Function<SQLException, Boolean>> initializeRetryableCheckers() {
        Map<DatabaseErrorType, Function<SQLException, Boolean>> checkers = new HashMap<>();
        
        checkers.put(DatabaseErrorType.CONNECTION_TIMEOUT, e -> true);
        checkers.put(DatabaseErrorType.CONNECTION_POOL_EXHAUSTED, e -> true);
        checkers.put(DatabaseErrorType.DEADLOCK, e -> true);
        checkers.put(DatabaseErrorType.TRANSACTION_ROLLBACK, e -> 
            !e.getMessage().toLowerCase().contains("constraint"));
        checkers.put(DatabaseErrorType.CONNECTION_FAILED, e -> 
            e.getMessage().toLowerCase().contains("timeout"));
            
        return checkers;
    }

    /**
     * 记录错误统计信息
     * 
     * @param exception 数据库异常
     */
    public void recordError(DatabaseConnectionException exception) {
        // 这里可以添加错误统计逻辑，比如发送到监控系统
        log.warn("数据库错误统计 - 类型: {}, 代码: {}, 消息: {}", 
                exception.getErrorType().getName(), 
                exception.getErrorCode(), 
                exception.getMessage());
    }

    /**
     * 重试建议信息
     */
    public static class RetryAdvice {
        private boolean shouldRetry;
        private int maxRetries;
        private long delayMillis;
        private double backoffMultiplier;
        private String advice;

        public static RetryAdviceBuilder builder() {
            return new RetryAdviceBuilder();
        }

        public static class RetryAdviceBuilder {
            private boolean shouldRetry;
            private int maxRetries;
            private long delayMillis;
            private double backoffMultiplier;
            private String advice;

            public RetryAdviceBuilder shouldRetry(boolean shouldRetry) {
                this.shouldRetry = shouldRetry;
                return this;
            }

            public RetryAdviceBuilder maxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
                return this;
            }

            public RetryAdviceBuilder delayMillis(long delayMillis) {
                this.delayMillis = delayMillis;
                return this;
            }

            public RetryAdviceBuilder backoffMultiplier(double backoffMultiplier) {
                this.backoffMultiplier = backoffMultiplier;
                return this;
            }

            public RetryAdviceBuilder advice(String advice) {
                this.advice = advice;
                return this;
            }

            public RetryAdvice build() {
                RetryAdvice retryAdvice = new RetryAdvice();
                retryAdvice.shouldRetry = this.shouldRetry;
                retryAdvice.maxRetries = this.maxRetries;
                retryAdvice.delayMillis = this.delayMillis;
                retryAdvice.backoffMultiplier = this.backoffMultiplier;
                retryAdvice.advice = this.advice;
                return retryAdvice;
            }
        }

        // Getters
        public boolean isShouldRetry() { return shouldRetry; }
        public int getMaxRetries() { return maxRetries; }
        public long getDelayMillis() { return delayMillis; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public String getAdvice() { return advice; }
    }
}