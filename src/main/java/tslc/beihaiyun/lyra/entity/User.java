package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import tslc.beihaiyun.lyra.validation.ValidationGroups;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * 用户实体类
 * 支持多种认证方式和角色管理
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_external_id", columnList = "external_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(unique = true, nullable = false)
    private String username;

    @Email(message = "邮箱格式不正确", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @NotBlank(message = "邮箱不能为空", groups = {ValidationGroups.Create.class})
    @Size(max = 255, message = "邮箱长度不能超过255个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(unique = true, nullable = false)
    private String email;

    @Size(min = 60, max = 60, message = "密码哈希长度必须为60个字符", groups = {ValidationGroups.Create.class})
    @Column(name = "password_hash")
    private String passwordHash;

    @NotBlank(message = "显示名称不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(min = 1, max = 100, message = "显示名称长度必须在1-100个字符之间", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Size(max = 255, message = "外部ID长度不能超过255个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(name = "external_id")
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (roles == null) {
            roles = new HashSet<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 验证用户名格式
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9_-]{3,50}$");
    }

    /**
     * 验证邮箱格式
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") && email.length() <= 255;
    }

    /**
     * 验证显示名称格式
     * @param displayName 显示名称
     * @return 是否有效
     */
    public static boolean isValidDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return false;
        }
        return displayName.trim().length() >= 1 && displayName.length() <= 100;
    }

    /**
     * 验证密码哈希格式（BCrypt）
     * @param passwordHash 密码哈希
     * @return 是否有效
     */
    public static boolean isValidPasswordHash(String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        // BCrypt hash format: $2a$10$... (60 characters total)
        // Format: $2[a|b|y]$[cost]$[22 char salt][31 char hash]
        return passwordHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$") && passwordHash.length() == 60;
    }

    /**
     * 检查用户是否处于活跃状态
     * @return 是否活跃
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查用户是否被暂停
     * @return 是否被暂停
     */
    public boolean isSuspended() {
        return UserStatus.SUSPENDED.equals(this.status);
    }

    /**
     * 检查用户是否待审批
     * @return 是否待审批
     */
    public boolean isPending() {
        return UserStatus.PENDING.equals(this.status);
    }

    /**
     * 检查用户是否使用本地认证
     * @return 是否本地认证
     */
    public boolean isLocalAuth() {
        return AuthProvider.LOCAL.equals(this.authProvider);
    }

    /**
     * 检查用户是否使用外部认证
     * @return 是否外部认证
     */
    public boolean isExternalAuth() {
        return !AuthProvider.LOCAL.equals(this.authProvider);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 激活用户账户
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 暂停用户账户
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验证用户数据完整性
     * @return 验证结果消息，null表示验证通过
     */
    public String validateUserData() {
        if (!isValidUsername(this.username)) {
            return "用户名格式不正确：只能包含字母、数字、下划线和连字符，长度3-50个字符";
        }
        
        if (!isValidEmail(this.email)) {
            return "邮箱格式不正确";
        }
        
        if (!isValidDisplayName(this.displayName)) {
            return "显示名称不能为空且长度不能超过100个字符";
        }
        
        if (isLocalAuth() && !isValidPasswordHash(this.passwordHash)) {
            return "本地认证用户必须设置有效的密码哈希";
        }
        
        if (isExternalAuth() && (this.externalId == null || this.externalId.trim().isEmpty())) {
            return "外部认证用户必须设置外部ID";
        }
        
        return null; // 验证通过
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        PENDING,    // 待审批
        ACTIVE,     // 活跃
        INACTIVE,   // 非活跃
        SUSPENDED   // 暂停
    }

    /**
     * 认证提供者枚举
     */
    public enum AuthProvider {
        LOCAL,      // 本地认证
        OAUTH2,     // OAuth2认证
        OIDC,       // OpenID Connect
        LDAP        // LDAP认证
    }
}