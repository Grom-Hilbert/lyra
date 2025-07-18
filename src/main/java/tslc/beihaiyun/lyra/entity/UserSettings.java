package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户设置实体类
 * 管理用户的个性化配置
 */
@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false)
    private Theme theme = Theme.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private Language language = Language.ZH_CN;

    @Column(name = "timezone")
    private String timezone = "Asia/Shanghai";

    @Column(name = "date_format")
    private String dateFormat = "yyyy-MM-dd";

    @Column(name = "time_format")
    private String timeFormat = "HH:mm:ss";

    @Column(name = "file_list_view", nullable = false)
    private Boolean fileListView = true; // true: 列表视图, false: 网格视图

    @Column(name = "show_hidden_files", nullable = false)
    private Boolean showHiddenFiles = false;

    @Column(name = "auto_save", nullable = false)
    private Boolean autoSave = true;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(name = "custom_settings", columnDefinition = "TEXT")
    private String customSettings; // JSON格式的自定义设置

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 主题枚举
     */
    public enum Theme {
        LIGHT,          // 浅色主题
        DARK,           // 深色主题
        AUTO            // 自动主题
    }

    /**
     * 语言枚举
     */
    public enum Language {
        ZH_CN,          // 简体中文
        EN_US,          // 英语
        JA_JP           // 日语
    }
}