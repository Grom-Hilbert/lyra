package tslc.beihaiyun.lyra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

/**
 * 管理员初始化配置属性
 * 
 * @author Lyra Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "lyra.admin")
@Validated
public class AdminInitializationProperties {

    /**
     * 管理员用户名
     */
    @NotBlank(message = "管理员用户名不能为空")
    private String username = "admin";

    /**
     * 管理员密码（明文，将被自动加密）
     */
    @NotBlank(message = "管理员密码不能为空")
    private String password = "admin123";

    /**
     * 管理员邮箱
     */
    @Email(message = "管理员邮箱格式不正确")
    @NotBlank(message = "管理员邮箱不能为空")
    private String email = "admin@lyra.local";

    /**
     * 管理员显示名称
     */
    @NotBlank(message = "管理员显示名称不能为空")
    private String displayName = "系统管理员";

    /**
     * 管理员存储配额（字节）
     */
    @Min(value = 1, message = "存储配额必须大于0")
    private Long storageQuota = 10737418240L; // 10GB
}
