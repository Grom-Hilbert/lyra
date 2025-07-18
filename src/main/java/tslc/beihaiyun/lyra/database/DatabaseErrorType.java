package tslc.beihaiyun.lyra.database;

/**
 * 数据库错误类型枚举
 * 定义各种数据库操作可能遇到的错误类型
 */
public enum DatabaseErrorType {
    
    CONNECTION_FAILED("连接失败", "无法建立数据库连接"),
    CONNECTION_TIMEOUT("连接超时", "数据库连接超时"),
    CONNECTION_POOL_EXHAUSTED("连接池耗尽", "连接池中没有可用连接"),
    AUTHENTICATION_FAILED("认证失败", "数据库用户名或密码错误"),
    DATABASE_NOT_FOUND("数据库不存在", "指定的数据库不存在"),
    TABLE_NOT_FOUND("表不存在", "指定的数据表不存在"),
    CONSTRAINT_VIOLATION("约束违反", "违反数据库约束条件"),
    DUPLICATE_KEY("重复键", "违反唯一性约束"),
    FOREIGN_KEY_VIOLATION("外键约束违反", "违反外键约束"),
    TRANSACTION_ROLLBACK("事务回滚", "事务执行失败并回滚"),
    DEADLOCK("死锁", "数据库操作发生死锁"),
    SYNTAX_ERROR("SQL语法错误", "SQL语句语法不正确"),
    PERMISSION_DENIED("权限不足", "没有执行该操作的权限"),
    DATA_TOO_LONG("数据过长", "数据长度超过字段限制"),
    INVALID_DATA_TYPE("数据类型无效", "数据类型不匹配"),
    UNKNOWN_ERROR("未知错误", "未知的数据库错误");

    private final String name;
    private final String description;

    DatabaseErrorType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据SQLException获取对应的错误类型
     */
    public static DatabaseErrorType fromSQLException(java.sql.SQLException e) {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();
        String message = e.getMessage().toLowerCase();

        // 首先根据错误信息判断（优先级更高）
        if (message.contains("timeout")) {
            return CONNECTION_TIMEOUT;
        }
        if (message.contains("pool") && message.contains("exhausted")) {
            return CONNECTION_POOL_EXHAUSTED;
        }
        if (message.contains("permission") || message.contains("access denied")) {
            return PERMISSION_DENIED;
        }
        if (message.contains("too long") || message.contains("data truncation")) {
            return DATA_TOO_LONG;
        }

        // 根据SQL状态码判断错误类型
        if (sqlState != null && sqlState.length() >= 2) {
            switch (sqlState.substring(0, 2)) {
                case "08": // Connection Exception
                    return CONNECTION_FAILED;
                case "28": // Invalid Authorization Specification
                    return AUTHENTICATION_FAILED;
                case "42": // Syntax Error or Access Rule Violation
                    if (message.contains("table") && message.contains("not found")) {
                        return TABLE_NOT_FOUND;
                    }
                    return SYNTAX_ERROR;
                case "23": // Integrity Constraint Violation
                    if (message.contains("duplicate") || message.contains("unique")) {
                        return DUPLICATE_KEY;
                    }
                    if (message.contains("foreign key")) {
                        return FOREIGN_KEY_VIOLATION;
                    }
                    return CONSTRAINT_VIOLATION;
                case "40": // Transaction Rollback
                    if (message.contains("deadlock")) {
                        return DEADLOCK;
                    }
                    return TRANSACTION_ROLLBACK;
            }
        }

        return UNKNOWN_ERROR;
    }
}