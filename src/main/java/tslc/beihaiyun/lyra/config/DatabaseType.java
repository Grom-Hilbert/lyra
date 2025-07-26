package tslc.beihaiyun.lyra.config;

/**
 * 支持的数据库类型枚举
 * 
 * @author Lyra Team
 * @since 1.0.0
 */
public enum DatabaseType {
    
    /**
     * SQLite数据库 - 适合开发环境和小型部署
     */
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:", 0),
    
    /**
     * MySQL数据库 - 适合生产环境
     */
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://", 3306),
    
    /**
     * PostgreSQL数据库 - 适合企业级部署
     */
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://", 5432);
    
    private final String driverClassName;
    private final String urlPrefix;
    private final int defaultPort;
    
    DatabaseType(String driverClassName, String urlPrefix, int defaultPort) {
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
        this.defaultPort = defaultPort;
    }
    
    /**
     * 获取数据库驱动类名
     */
    public String getDriverClassName() {
        return driverClassName;
    }
    
    /**
     * 获取JDBC URL前缀
     */
    public String getUrlPrefix() {
        return urlPrefix;
    }
    
    /**
     * 获取默认端口
     */
    public int getDefaultPort() {
        return defaultPort;
    }
    
    /**
     * 构建数据库连接URL
     * 
     * @param host 主机地址
     * @param port 端口号
     * @param database 数据库名称
     * @param filePath 文件路径（仅SQLite使用）
     * @return 完整的JDBC URL
     */
    public String buildUrl(String host, Integer port, String database, String filePath) {
        switch (this) {
            case SQLITE:
                return urlPrefix + (filePath != null ? filePath : "./data/lyra.db");
            case MYSQL:
                return urlPrefix + host + ":" + (port != null ? port : defaultPort) + "/" + database 
                       + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
            case POSTGRESQL:
                return urlPrefix + host + ":" + (port != null ? port : defaultPort) + "/" + database;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + this);
        }
    }
    
    /**
     * 检查是否为文件型数据库
     */
    public boolean isFileDatabase() {
        return this == SQLITE;
    }
    
    /**
     * 检查是否为网络型数据库
     */
    public boolean isNetworkDatabase() {
        return this == MYSQL || this == POSTGRESQL;
    }
}
