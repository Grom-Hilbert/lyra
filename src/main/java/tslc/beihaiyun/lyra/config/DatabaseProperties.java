package tslc.beihaiyun.lyra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 数据库配置属性
 * 
 * @author Lyra Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "lyra.database")
@Validated
public class DatabaseProperties {

    /**
     * 数据库类型
     */
    @NotNull(message = "数据库类型不能为空")
    private DatabaseType type = DatabaseType.SQLITE;

    /**
     * 数据库主机地址（网络数据库使用）
     */
    private String host = "localhost";

    /**
     * 数据库端口（网络数据库使用）
     */
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号必须小于65536")
    private Integer port;

    /**
     * 数据库名称（网络数据库使用）
     */
    private String name = "lyra";

    /**
     * 数据库用户名（网络数据库使用）
     */
    private String username = "lyra";

    /**
     * 数据库密码（网络数据库使用）
     */
    private String password = "password";

    /**
     * 数据库文件路径（SQLite使用）
     */
    private String filePath = "./data/lyra.db";

    /**
     * 连接池最大连接数
     */
    @Min(value = 1, message = "最大连接数必须大于0")
    private Integer maxPoolSize = 10;

    /**
     * 连接池最小空闲连接数
     */
    @Min(value = 0, message = "最小空闲连接数不能小于0")
    private Integer minIdle = 2;

    /**
     * 连接超时时间（毫秒）
     */
    @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
    private Long connectionTimeout = 30000L;

    /**
     * 空闲连接超时时间（毫秒）
     */
    @Min(value = 60000, message = "空闲连接超时时间不能小于60000毫秒")
    private Long idleTimeout = 600000L;

    /**
     * 连接最大生命周期（毫秒）
     */
    @Min(value = 300000, message = "连接最大生命周期不能小于300000毫秒")
    private Long maxLifetime = 1800000L;

    /**
     * 获取有效的端口号
     */
    public Integer getEffectivePort() {
        if (port != null) {
            return port;
        }
        return type.getDefaultPort();
    }

    /**
     * 构建数据库连接URL
     */
    public String buildJdbcUrl() {
        return type.buildUrl(host, getEffectivePort(), name, filePath);
    }

    /**
     * 获取数据库驱动类名
     */
    public String getDriverClassName() {
        return type.getDriverClassName();
    }

    /**
     * 检查是否为文件型数据库
     */
    public boolean isFileDatabase() {
        return type.isFileDatabase();
    }

    /**
     * 检查是否为网络型数据库
     */
    public boolean isNetworkDatabase() {
        return type.isNetworkDatabase();
    }

    /**
     * 获取用于网络数据库的用户名
     */
    public String getEffectiveUsername() {
        return isNetworkDatabase() ? username : null;
    }

    /**
     * 获取用于网络数据库的密码
     */
    public String getEffectivePassword() {
        return isNetworkDatabase() ? password : null;
    }
}
