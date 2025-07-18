package tslc.beihaiyun.lyra.constants;

/**
 * Lyra 系统常量定义
 */
public final class LyraConstants {

    private LyraConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 文件相关常量
     */
    public static final class File {
        public static final long MAX_FILE_SIZE = 100L * 1024 * 1024 * 1024; // 100GB
        public static final int MAX_FILENAME_LENGTH = 255;
        public static final int MAX_PATH_LENGTH = 1000;
        public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
        
        // 支持预览的文件类型
        public static final String[] PREVIEWABLE_TEXT_TYPES = {
            "text/plain", "text/html", "text/css", "text/javascript",
            "application/json", "application/xml", "text/xml",
            "text/markdown", "text/csv"
        };
        
        public static final String[] PREVIEWABLE_IMAGE_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "image/svg+xml", "image/bmp"
        };
        
        // 可编辑的文件类型
        public static final String[] EDITABLE_TYPES = {
            "text/plain", "text/html", "text/css", "text/javascript",
            "application/json", "application/xml", "text/xml",
            "text/markdown", "text/csv", "application/yaml"
        };
    }

    /**
     * 用户相关常量
     */
    public static final class User {
        public static final int MIN_USERNAME_LENGTH = 3;
        public static final int MAX_USERNAME_LENGTH = 50;
        public static final int MAX_DISPLAY_NAME_LENGTH = 100;
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_LOGIN_ATTEMPTS = 5;
        public static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    }

    /**
     * 权限相关常量
     */
    public static final class Permission {
        public static final String READ = "READ";
        public static final String WRITE = "WRITE";
        public static final String DELETE = "DELETE";
        public static final String SHARE = "SHARE";
        public static final String ADMIN = "ADMIN";
        
        // 默认权限过期时间（天）
        public static final int DEFAULT_EXPIRY_DAYS = 30;
        public static final int MAX_EXPIRY_DAYS = 365;
    }

    /**
     * 版本控制相关常量
     */
    public static final class Version {
        public static final int MAX_COMMIT_MESSAGE_LENGTH = 1000;
        public static final int MAX_VERSIONS_PER_FILE = 100;
        public static final String DEFAULT_BRANCH = "main";
        public static final int GIT_COMMIT_HASH_LENGTH = 40;
    }

    /**
     * 模板相关常量
     */
    public static final class Template {
        public static final int MAX_TEMPLATE_NAME_LENGTH = 100;
        public static final int MAX_TEMPLATE_DESCRIPTION_LENGTH = 500;
        public static final int MAX_TEMPLATE_FILES = 1000;
    }

    /**
     * API相关常量
     */
    public static final class Api {
        public static final String API_VERSION = "v1";
        public static final String API_PREFIX = "/api/" + API_VERSION;
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        
        // HTTP头
        public static final String HEADER_AUTHORIZATION = "Authorization";
        public static final String HEADER_CONTENT_TYPE = "Content-Type";
        public static final String HEADER_ACCEPT = "Accept";
        public static final String HEADER_USER_AGENT = "User-Agent";
        public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
        
        // 响应状态码
        public static final String SUCCESS_CODE = "SUCCESS";
        public static final String ERROR_CODE = "ERROR";
        public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
        public static final String PERMISSION_DENIED_CODE = "PERMISSION_DENIED";
        public static final String NOT_FOUND_CODE = "NOT_FOUND";
    }

    /**
     * 缓存相关常量
     */
    public static final class Cache {
        public static final String USER_CACHE = "users";
        public static final String FILE_CACHE = "files";
        public static final String PERMISSION_CACHE = "permissions";
        public static final String TEMPLATE_CACHE = "templates";
        
        // 缓存过期时间（秒）
        public static final int DEFAULT_TTL = 3600; // 1小时
        public static final int USER_TTL = 1800; // 30分钟
        public static final int FILE_TTL = 600; // 10分钟
        public static final int PERMISSION_TTL = 300; // 5分钟
    }

    /**
     * WebDAV相关常量
     */
    public static final class WebDAV {
        public static final String WEBDAV_PATH = "/webdav";
        public static final String ENTERPRISE_SPACE_PATH = "/enterprise";
        public static final String PERSONAL_SPACE_PATH = "/personal";
        
        // WebDAV方法
        public static final String METHOD_PROPFIND = "PROPFIND";
        public static final String METHOD_PROPPATCH = "PROPPATCH";
        public static final String METHOD_MKCOL = "MKCOL";
        public static final String METHOD_COPY = "COPY";
        public static final String METHOD_MOVE = "MOVE";
        public static final String METHOD_LOCK = "LOCK";
        public static final String METHOD_UNLOCK = "UNLOCK";
    }

    /**
     * 系统配置相关常量
     */
    public static final class Config {
        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
        public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
        public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
        public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
        
        // 配置键
        public static final String STORAGE_TYPE = "lyra.storage.type";
        public static final String STORAGE_PATH = "lyra.storage.path";
        public static final String DATABASE_TYPE = "lyra.database.type";
        public static final String CACHE_TYPE = "lyra.cache.type";
        public static final String WEBDAV_ENABLED = "lyra.webdav.enabled";
        public static final String VERSION_CONTROL_ENABLED = "lyra.version.enabled";
    }

    /**
     * 审计日志相关常量
     */
    public static final class Audit {
        // 操作类型
        public static final String ACTION_LOGIN = "LOGIN";
        public static final String ACTION_LOGOUT = "LOGOUT";
        public static final String ACTION_FILE_UPLOAD = "FILE_UPLOAD";
        public static final String ACTION_FILE_DOWNLOAD = "FILE_DOWNLOAD";
        public static final String ACTION_FILE_DELETE = "FILE_DELETE";
        public static final String ACTION_FILE_SHARE = "FILE_SHARE";
        public static final String ACTION_PERMISSION_GRANT = "PERMISSION_GRANT";
        public static final String ACTION_PERMISSION_REVOKE = "PERMISSION_REVOKE";
        public static final String ACTION_VERSION_COMMIT = "VERSION_COMMIT";
        public static final String ACTION_TEMPLATE_CREATE = "TEMPLATE_CREATE";
        public static final String ACTION_TEMPLATE_APPLY = "TEMPLATE_APPLY";
        
        // 日志保留时间（天）
        public static final int LOG_RETENTION_DAYS = 90;
        public static final int SECURITY_LOG_RETENTION_DAYS = 365;
    }

    /**
     * 错误消息常量
     */
    public static final class ErrorMessage {
        public static final String USER_NOT_FOUND = "用户不存在";
        public static final String FILE_NOT_FOUND = "文件不存在";
        public static final String FOLDER_NOT_FOUND = "文件夹不存在";
        public static final String PERMISSION_DENIED = "权限不足";
        public static final String INVALID_CREDENTIALS = "用户名或密码错误";
        public static final String ACCOUNT_LOCKED = "账户已被锁定";
        public static final String ACCOUNT_DISABLED = "账户已被禁用";
        public static final String FILE_TOO_LARGE = "文件大小超出限制";
        public static final String INVALID_FILE_TYPE = "不支持的文件类型";
        public static final String STORAGE_FULL = "存储空间不足";
        public static final String VERSION_CONFLICT = "版本冲突";
        public static final String TEMPLATE_NOT_FOUND = "模板不存在";
        public static final String INVALID_TEMPLATE = "模板格式不正确";
    }
}