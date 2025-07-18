package tslc.beihaiyun.lyra.database;

/**
 * 数据库连接异常类
 * 用于封装数据库连接相关的异常信息
 */
public class DatabaseConnectionException extends Exception {

    private final String errorCode;
    private final DatabaseErrorType errorType;

    public DatabaseConnectionException(String message) {
        super(message);
        this.errorCode = "DB_CONNECTION_ERROR";
        this.errorType = DatabaseErrorType.CONNECTION_FAILED;
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DB_CONNECTION_ERROR";
        this.errorType = DatabaseErrorType.CONNECTION_FAILED;
    }

    public DatabaseConnectionException(String message, String errorCode, DatabaseErrorType errorType) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public DatabaseConnectionException(String message, Throwable cause, String errorCode, DatabaseErrorType errorType) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public DatabaseErrorType getErrorType() {
        return errorType;
    }

    /**
     * 获取详细的错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("错误类型: ").append(errorType.getDescription()).append("\n");
        sb.append("错误代码: ").append(errorCode).append("\n");
        sb.append("错误信息: ").append(getMessage());
        
        if (getCause() != null) {
            sb.append("\n原因: ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
}