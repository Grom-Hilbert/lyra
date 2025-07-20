package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类
 * 存储用户基本信息和认证相关数据
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
    }
)
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    /**
     * 用户ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和横线")
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    /**
     * 密码（加密后）
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 255, message = "密码长度不能超过255个字符")
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    /**
     * 昵称/显示名称
     */
    @Size(max = 100, message = "昵称长度不能超过100个字符")
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 手机号码
     */
    @Pattern(regexp = "^[0-9-+\\s()]*$", message = "手机号格式不正确")
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 用户状态
     */
    @NotNull(message = "用户状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.PENDING;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    /**
     * 账户是否未过期
     */
    @Column(name = "account_non_expired", nullable = false)
    private Boolean accountNonExpired = true;

    /**
     * 账户是否未锁定
     */
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    /**
     * 凭证是否未过期
     */
    @Column(name = "credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired = true;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    @Size(max = 45, message = "IP地址长度不能超过45个字符")
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    /**
     * 登录失败次数
     */
    @Min(value = 0, message = "登录失败次数不能为负数")
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    /**
     * 账户锁定时间
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * 邮箱验证状态
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * 邮箱验证时间
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * 存储配额（字节）
     */
    @Min(value = 0, message = "存储配额不能为负数")
    @Column(name = "storage_quota", nullable = false)
    private Long storageQuota = 10737418240L; // 10GB 默认配额

    /**
     * 已使用存储（字节）
     */
    @Min(value = 0, message = "已使用存储不能为负数")
    @Column(name = "storage_used", nullable = false)
    private Long storageUsed = 0L;

    /**
     * 用户角色关联
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        /**
         * 待审核
         */
        PENDING,
        /**
         * 已激活
         */
        ACTIVE,
        /**
         * 已禁用
         */
        DISABLED,
        /**
         * 已锁定
         */
        LOCKED,
        /**
         * 已注销
         */
        DEACTIVATED
    }

    // 构造函数
    public User() {
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // 业务方法

    /**
     * 检查用户是否活跃（已启用且未锁定）
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled) && 
               Boolean.TRUE.equals(accountNonLocked) && 
               UserStatus.ACTIVE.equals(status);
    }

    /**
     * 增加登录失败次数
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;
    }

    /**
     * 重置登录失败次数
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    /**
     * 锁定账户
     */
    public void lockAccount() {
        this.accountNonLocked = false;
        this.lockedAt = LocalDateTime.now();
        this.status = UserStatus.LOCKED;
    }

    /**
     * 解锁账户
     */
    public void unlockAccount() {
        this.accountNonLocked = true;
        this.lockedAt = null;
        this.failedLoginAttempts = 0;
        if (UserStatus.LOCKED.equals(this.status)) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * 激活账户
     */
    public void activateAccount() {
        this.enabled = true;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 禁用账户
     */
    public void disableAccount() {
        this.enabled = false;
        this.status = UserStatus.DISABLED;
    }

    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    /**
     * 更新最后登录信息
     * 
     * @param loginIp 登录IP
     */
    public void updateLastLogin(String loginIp) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = loginIp;
        this.resetFailedLoginAttempts();
    }

    /**
     * 检查存储配额是否超限
     * 
     * @param additionalSize 额外需要的存储大小
     * @return 是否超限
     */
    public boolean isStorageQuotaExceeded(long additionalSize) {
        return (storageUsed + additionalSize) > storageQuota;
    }

    /**
     * 增加已使用存储
     * 
     * @param size 增加的大小
     */
    public void addStorageUsed(long size) {
        this.storageUsed = Math.max(0, this.storageUsed + size);
    }

    /**
     * 减少已使用存储
     * 
     * @param size 减少的大小
     */
    public void subtractStorageUsed(long size) {
        this.storageUsed = Math.max(0, this.storageUsed - size);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(Boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(Boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Long getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(Long storageQuota) {
        this.storageQuota = storageQuota;
    }

    public Long getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(Long storageUsed) {
        this.storageUsed = storageUsed;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * 判断用户是否被锁定
     */
    public Boolean getLocked() {
        return lockedAt != null;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status=" + status +
                ", enabled=" + enabled +
                ", emailVerified=" + emailVerified +
                "} " + super.toString();
    }
} 