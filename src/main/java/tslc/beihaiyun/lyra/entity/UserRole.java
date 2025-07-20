package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体类
 * 实现用户与角色的多对多关联关系
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "user_roles",
    indexes = {
        @Index(name = "idx_user_role_user_id", columnList = "user_id"),
        @Index(name = "idx_user_role_role_id", columnList = "role_id"),
        @Index(name = "idx_user_role_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"})
    }
)
@SQLRestriction("is_deleted = false")
public class UserRole extends BaseEntity {

    /**
     * 用户角色关联ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /**
     * 用户实体关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    /**
     * 角色实体关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
    private Role role;

    /**
     * 分配状态
     */
    @NotNull(message = "分配状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    /**
     * 生效时间
     */
    @Column(name = "effective_at")
    private LocalDateTime effectiveAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 分配人
     */
    @Column(name = "assigned_by", length = 50)
    private String assignedBy;

    /**
     * 分配原因
     */
    @Column(name = "assignment_reason", length = 200)
    private String assignmentReason;

    /**
     * 分配状态枚举
     */
    public enum AssignmentStatus {
        /**
         * 生效中
         */
        ACTIVE,
        /**
         * 待生效
         */
        PENDING,
        /**
         * 已暂停
         */
        SUSPENDED,
        /**
         * 已过期
         */
        EXPIRED,
        /**
         * 已撤销
         */
        REVOKED
    }

    // 构造函数
    public UserRole() {
    }

    public UserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
        this.effectiveAt = LocalDateTime.now();
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.userId = user.getId();
        this.roleId = role.getId();
        this.effectiveAt = LocalDateTime.now();
    }

    public UserRole(Long userId, Long roleId, String assignedBy, String assignmentReason) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedBy = assignedBy;
        this.assignmentReason = assignmentReason;
        this.effectiveAt = LocalDateTime.now();
    }

    // 业务方法

    /**
     * 检查角色分配是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        if (!AssignmentStatus.ACTIVE.equals(status)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 检查生效时间
        if (effectiveAt != null && now.isBefore(effectiveAt)) {
            return false;
        }
        
        // 检查过期时间
        if (expiresAt != null && now.isAfter(expiresAt)) {
            return false;
        }
        
        return !isDeleted();
    }

    /**
     * 检查角色分配是否已过期
     * 
     * @return 是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查角色分配是否待生效
     * 
     * @return 是否待生效
     */
    public boolean isPending() {
        return AssignmentStatus.PENDING.equals(status) || 
               (effectiveAt != null && LocalDateTime.now().isBefore(effectiveAt));
    }

    /**
     * 激活角色分配
     */
    public void activate() {
        this.status = AssignmentStatus.ACTIVE;
        if (this.effectiveAt == null) {
            this.effectiveAt = LocalDateTime.now();
        }
    }

    /**
     * 暂停角色分配
     */
    public void suspend() {
        this.status = AssignmentStatus.SUSPENDED;
    }

    /**
     * 撤销角色分配
     * 
     * @param revokedBy 撤销人
     */
    public void revoke(String revokedBy) {
        this.status = AssignmentStatus.REVOKED;
        this.setUpdatedBy(revokedBy);
    }

    /**
     * 设置过期时间
     * 
     * @param expiresAt 过期时间
     */
    public void setExpiration(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        if (isExpired()) {
            this.status = AssignmentStatus.EXPIRED;
        }
    }

    /**
     * 延长有效期
     * 
     * @param newExpiresAt 新的过期时间
     */
    public void extendExpiration(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
        if (AssignmentStatus.EXPIRED.equals(this.status) && !isExpired()) {
            this.status = AssignmentStatus.ACTIVE;
        }
    }

    // 预更新方法，自动更新过期状态
    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        if (isExpired() && AssignmentStatus.ACTIVE.equals(status)) {
            this.status = AssignmentStatus.EXPIRED;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.roleId = role.getId();
        }
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(LocalDateTime effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getAssignmentReason() {
        return assignmentReason;
    }

    public void setAssignmentReason(String assignmentReason) {
        this.assignmentReason = assignmentReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRole)) return false;
        UserRole userRole = (UserRole) o;
        return id != null && id.equals(userRole.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", userId=" + userId +
                ", roleId=" + roleId +
                ", status=" + status +
                ", effectiveAt=" + effectiveAt +
                ", expiresAt=" + expiresAt +
                ", assignedBy='" + assignedBy + '\'' +
                "} " + super.toString();
    }
} 