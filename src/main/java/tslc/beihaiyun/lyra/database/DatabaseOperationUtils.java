package tslc.beihaiyun.lyra.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 数据库操作工具类
 * 提供常用的数据库操作方法，集成连接管理和错误处理
 */
@Slf4j
@Component
public class DatabaseOperationUtils {

    private final DatabaseConnectionManager connectionManager;
    private final DatabaseErrorHandler errorHandler;

    @Autowired
    public DatabaseOperationUtils(DatabaseConnectionManager connectionManager, 
                                DatabaseErrorHandler errorHandler) {
        this.connectionManager = connectionManager;
        this.errorHandler = errorHandler;
    }

    /**
     * 执行查询操作
     * 
     * @param sql SQL查询语句
     * @param parameters 查询参数
     * @param rowMapper 行映射器
     * @return 查询结果列表
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public <T> List<T> executeQuery(String sql, Object[] parameters, RowMapper<T> rowMapper) 
            throws DatabaseConnectionException {
        return executeWithRetry("查询操作", () -> {
            List<T> results = new ArrayList<>();
            
            try (Connection connection = connectionManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                setParameters(statement, parameters);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(rowMapper.mapRow(resultSet));
                    }
                }
                
                log.debug("查询操作完成，返回 {} 条记录", results.size());
                return results;
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "执行查询: " + sql);
            }
        });
    }

    /**
     * 执行更新操作（INSERT, UPDATE, DELETE）
     * 
     * @param sql SQL更新语句
     * @param parameters 更新参数
     * @return 受影响的行数
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public int executeUpdate(String sql, Object[] parameters) throws DatabaseConnectionException {
        return executeWithRetry("更新操作", () -> {
            try (Connection connection = connectionManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                setParameters(statement, parameters);
                int affectedRows = statement.executeUpdate();
                
                log.debug("更新操作完成，影响 {} 行记录", affectedRows);
                return affectedRows;
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "执行更新: " + sql);
            }
        });
    }

    /**
     * 执行批量更新操作
     * 
     * @param sql SQL更新语句
     * @param parametersList 批量参数列表
     * @return 每个操作受影响的行数数组
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public int[] executeBatch(String sql, List<Object[]> parametersList) throws DatabaseConnectionException {
        return executeWithRetry("批量更新操作", () -> {
            try (Connection connection = connectionManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                connection.setAutoCommit(false);
                
                for (Object[] parameters : parametersList) {
                    setParameters(statement, parameters);
                    statement.addBatch();
                }
                
                int[] results = statement.executeBatch();
                connection.commit();
                
                log.debug("批量更新操作完成，处理 {} 个批次", results.length);
                return results;
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "执行批量更新: " + sql);
            }
        });
    }

    /**
     * 执行事务操作
     * 
     * @param operations 事务操作列表
     * @return 事务执行结果
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public <T> T executeTransaction(TransactionCallback<T> operations) throws DatabaseConnectionException {
        return executeWithRetry("事务操作", () -> {
            try (Connection connection = connectionManager.getConnection()) {
                connection.setAutoCommit(false);
                
                try {
                    T result = operations.doInTransaction(connection);
                    connection.commit();
                    
                    log.debug("事务操作成功提交");
                    return result;
                    
                } catch (Exception e) {
                    connection.rollback();
                    log.warn("事务操作回滚");
                    
                    if (e instanceof SQLException) {
                        throw errorHandler.handleSQLException((SQLException) e, "执行事务操作");
                    } else {
                        throw new DatabaseConnectionException("事务执行失败", e);
                    }
                }
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "事务操作");
            }
        });
    }

    /**
     * 检查表是否存在
     * 
     * @param tableName 表名
     * @return 表是否存在
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public boolean tableExists(String tableName) throws DatabaseConnectionException {
        return executeWithRetry("检查表存在性", () -> {
            try (Connection connection = connectionManager.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                    boolean exists = resultSet.next();
                    log.debug("表 {} {}", tableName, exists ? "存在" : "不存在");
                    return exists;
                }
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "检查表存在性: " + tableName);
            }
        });
    }

    /**
     * 获取表的列信息
     * 
     * @param tableName 表名
     * @return 列信息列表
     * @throws DatabaseConnectionException 数据库操作异常
     */
    public List<ColumnInfo> getTableColumns(String tableName) throws DatabaseConnectionException {
        return executeWithRetry("获取表列信息", () -> {
            List<ColumnInfo> columns = new ArrayList<>();
            
            try (Connection connection = connectionManager.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                
                try (ResultSet resultSet = metaData.getColumns(null, null, tableName, null)) {
                    while (resultSet.next()) {
                        ColumnInfo column = ColumnInfo.builder()
                                .columnName(resultSet.getString("COLUMN_NAME"))
                                .dataType(resultSet.getInt("DATA_TYPE"))
                                .typeName(resultSet.getString("TYPE_NAME"))
                                .columnSize(resultSet.getInt("COLUMN_SIZE"))
                                .nullable(resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                                .defaultValue(resultSet.getString("COLUMN_DEF"))
                                .build();
                        columns.add(column);
                    }
                }
                
                log.debug("获取表 {} 的列信息，共 {} 列", tableName, columns.size());
                return columns;
                
            } catch (SQLException e) {
                throw errorHandler.handleSQLException(e, "获取表列信息: " + tableName);
            }
        });
    }

    /**
     * 执行带重试机制的操作
     */
    private <T> T executeWithRetry(String operationName, DatabaseOperation<T> operation) 
            throws DatabaseConnectionException {
        DatabaseConnectionException lastException = null;
        
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return operation.execute();
                
            } catch (DatabaseConnectionException e) {
                lastException = e;
                errorHandler.recordError(e);
                
                if (attempt < 3 && errorHandler.isRetryable(e)) {
                    DatabaseErrorHandler.RetryAdvice advice = errorHandler.getRetryAdvice(e);
                    long delay = (long) (advice.getDelayMillis() * Math.pow(advice.getBackoffMultiplier(), attempt - 1));
                    
                    log.warn("{}失败，第{}次重试，延迟{}ms。错误: {}", 
                            operationName, attempt, delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DatabaseConnectionException("操作被中断", ie);
                    }
                } else {
                    break;
                }
            }
        }
        
        log.error("{}最终失败，已重试3次", operationName);
        throw lastException;
    }

    /**
     * 设置PreparedStatement参数
     */
    private void setParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
        }
    }

    /**
     * 行映射器接口
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet resultSet) throws SQLException;
    }

    /**
     * 事务回调接口
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection connection) throws Exception;
    }

    /**
     * 数据库操作接口
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute() throws DatabaseConnectionException;
    }

    /**
     * 列信息实体类
     */
    public static class ColumnInfo {
        private String columnName;
        private int dataType;
        private String typeName;
        private int columnSize;
        private boolean nullable;
        private String defaultValue;

        public static ColumnInfoBuilder builder() {
            return new ColumnInfoBuilder();
        }

        public static class ColumnInfoBuilder {
            private String columnName;
            private int dataType;
            private String typeName;
            private int columnSize;
            private boolean nullable;
            private String defaultValue;

            public ColumnInfoBuilder columnName(String columnName) {
                this.columnName = columnName;
                return this;
            }

            public ColumnInfoBuilder dataType(int dataType) {
                this.dataType = dataType;
                return this;
            }

            public ColumnInfoBuilder typeName(String typeName) {
                this.typeName = typeName;
                return this;
            }

            public ColumnInfoBuilder columnSize(int columnSize) {
                this.columnSize = columnSize;
                return this;
            }

            public ColumnInfoBuilder nullable(boolean nullable) {
                this.nullable = nullable;
                return this;
            }

            public ColumnInfoBuilder defaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public ColumnInfo build() {
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.columnName = this.columnName;
                columnInfo.dataType = this.dataType;
                columnInfo.typeName = this.typeName;
                columnInfo.columnSize = this.columnSize;
                columnInfo.nullable = this.nullable;
                columnInfo.defaultValue = this.defaultValue;
                return columnInfo;
            }
        }

        // Getters
        public String getColumnName() { return columnName; }
        public int getDataType() { return dataType; }
        public String getTypeName() { return typeName; }
        public int getColumnSize() { return columnSize; }
        public boolean isNullable() { return nullable; }
        public String getDefaultValue() { return defaultValue; }
    }
}