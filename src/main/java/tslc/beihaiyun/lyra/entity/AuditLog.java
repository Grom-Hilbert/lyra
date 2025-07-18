package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * 记录系统操作的审计信息
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank(message = "操作不能为空")
    @Size(max = 100, message = "操作不能超过100个字符")
    @Column(nullable = false)
    private String action;

    @Size(max = 1000, message = "资源路径不能超过1000个字符")
    @Column(name = "resource_path")
    private String resourcePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionResult result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData; // JSON格式的请求数据

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData; // JSON格式的响应数据

    @Column(name = "execution_time")
    private Long executionTime; // 执行时间（毫秒）

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        FILE,           // 文件
        FOLDER,         // 文件夹
        USER,           // 用户
        ROLE,           // 角色
        PERMISSION,     // 权限
        TEMPLATE,       // 模板
        SYSTEM          // 系统
    }

    /**
     * 操作结果枚举
     */
    public enum ActionResult {
        SUCCESS,        // 成功
        FAILURE,        // 失败
        PARTIAL         // 部分成功
    }
}