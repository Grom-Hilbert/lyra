package tslc.beihaiyun.lyra.storage;

/**
 * 存储异常
 */
public class StorageException extends Exception {
    
    private final StorageErrorType errorType;
    
    public StorageException(String message) {
        super(message);
        this.errorType = StorageErrorType.UNKNOWN;
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = StorageErrorType.UNKNOWN;
    }
    
    public StorageException(StorageErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public StorageException(StorageErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public StorageErrorType getErrorType() {
        return errorType;
    }
}