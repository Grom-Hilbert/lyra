package tslc.beihaiyun.lyra.database;

import lombok.Builder;
import lombok.Data;

/**
 * 数据库信息实体类
 * 包含数据库的基本信息和元数据
 */
@Data
@Builder
public class DatabaseInfo {
    
    /**
     * 数据库产品名称
     */
    private String databaseProductName;
    
    /**
     * 数据库产品版本
     */
    private String databaseProductVersion;
    
    /**
     * JDBC驱动名称
     */
    private String driverName;
    
    /**
     * JDBC驱动版本
     */
    private String driverVersion;
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 数据库用户名
     */
    private String userName;
    
    /**
     * 是否支持事务
     */
    private boolean supportsTransactions;
    
    /**
     * 获取数据库类型
     */
    public DatabaseType getDatabaseType() {
        if (databaseProductName == null) {
            return DatabaseType.UNKNOWN;
        }
        
        String productName = databaseProductName.toLowerCase();
        if (productName.contains("sqlite")) {
            return DatabaseType.SQLITE;
        } else if (productName.contains("mysql")) {
            return DatabaseType.MYSQL;
        } else if (productName.contains("postgresql")) {
            return DatabaseType.POSTGRESQL;
        } else if (productName.contains("h2")) {
            return DatabaseType.H2;
        } else {
            return DatabaseType.UNKNOWN;
        }
    }
    
    /**
     * 获取格式化的数据库信息字符串
     */
    public String getFormattedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库类型: ").append(getDatabaseType().getDisplayName()).append("\n");
        sb.append("产品名称: ").append(databaseProductName).append("\n");
        sb.append("产品版本: ").append(databaseProductVersion).append("\n");
        sb.append("驱动名称: ").append(driverName).append("\n");
        sb.append("驱动版本: ").append(driverVersion).append("\n");
        sb.append("连接URL: ").append(maskSensitiveInfo(url)).append("\n");
        sb.append("用户名: ").append(userName).append("\n");
        sb.append("支持事务: ").append(supportsTransactions ? "是" : "否");
        return sb.toString();
    }
    
    /**
     * 屏蔽敏感信息（如密码）
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) return null;
        
        // 简单的密码屏蔽逻辑
        return url.replaceAll("password=[^&;]*", "password=***");
    }
    
    /**
     * 数据库类型枚举
     */
    public enum DatabaseType {
        SQLITE("SQLite"),
        MYSQL("MySQL"),
        POSTGRESQL("PostgreSQL"),
        H2("H2"),
        UNKNOWN("未知");
        
        private final String displayName;
        
        DatabaseType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}