package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 空间权限关联实体类
 * 定义用户在特定空间中的权限关联关系
 * 支持权限继承和覆盖机制
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "space_permissions",
    indexes = {
        @Index(name = "idx_space_permission_user_id", columnList = "user_id"),
        @Index(name = "idx_space_permission_space_id", columnList = "space_id"),
        @Index(name = "idx_space_permission_permission_id", columnList = "permission_id"),
        @Index(name = "idx_space_permission_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_space_permission_inherit", columnList = "inherit_from_parent"),
        @Index(name = "idx_space_permission_status", columnList = "status"),
        @Index(name = "idx_space_permission_grant_type", columnList = "grant_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_space_permission", columnNames = {"user_id", "space_id", "permission_id", "resource_type", "resource_id"})
    }
)
@SQLRestriction("is_deleted = false")
public class SpacePermission extends BaseEntity {

    /**
     * 空间权限ID（主键）
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
     * 空间ID
     */
    @NotNull(message = "空间ID不能为空")
    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    /**
     * 权限ID
     */
    @NotNull(message = "权限ID不能为空")
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    /**
     * 资源类型
     * FILE: 文件
     * FOLDER: 文件夹
     * SPACE: 整个空间
     */
    @NotBlank(message = "资源类型不能为空")
    @Pattern(regexp = "^(FILE|FOLDER|SPACE)$", message = "资源类型必须是FILE、FOLDER或SPACE")
    @Column(name = "resource_type", length = 20, nullable = false)
    private String resourceType;

    /**
     * 资源ID
     * 当resourceType为FILE时，对应file_id
     * 当resourceType为FOLDER时，对应folder_id
     * 当resourceType为SPACE时，值为NULL（整个空间权限）
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * 授权状态
     * GRANTED: 授权
     * DENIED: 拒绝
     * INHERITED: 继承
     */
    @NotBlank(message = "授权状态不能为空")
    @Pattern(regexp = "^(GRANTED|DENIED|INHERITED)$", message = "授权状态必须是GRANTED、DENIED或INHERITED")
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * 授权类型
     * DIRECT: 直接授权
     * INHERITED: 继承授权
     * ROLE_BASED: 基于角色的授权
     */
    @NotBlank(message = "授权类型不能为空")
    @Pattern(regexp = "^(DIRECT|INHERITED|ROLE_BASED)$", message = "授权类型必须是DIRECT、INHERITED或ROLE_BASED")
    @Column(name = "grant_type", length = 20, nullable = false)
    private String grantType;

    /**
     * 是否从父级继承
     */
    @NotNull(message = "继承标识不能为空")
    @Column(name = "inherit_from_parent", nullable = false)
    private Boolean inheritFromParent = true;

    /**
     * 权限路径（用于权限继承计算）
     * 格式：/空间ID/文件夹ID1/文件夹ID2/.../资源ID
     */
    @Size(max = 1000, message = "权限路径长度不能超过1000个字符")
    @Column(name = "permission_path", length = 1000)
    private String permissionPath;

    /**
     * 权限级别（用于权限覆盖判断）
     * 数值越大优先级越高
     */
    @NotNull(message = "权限级别不能为空")
    @Min(value = 1, message = "权限级别必须大于0")
    @Max(value = 100, message = "权限级别不能超过100")
    @Column(name = "permission_level", nullable = false)
    private Integer permissionLevel = 50;

    /**
     * 授权者ID
     * 记录是谁授予的这个权限
     */
    @Column(name = "granted_by")
    private Long grantedBy;

    /**
     * 授权时间
     */
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    /**
     * 权限过期时间
     * NULL表示永不过期
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 备注信息
     */
    @Size(max = 500, message = "备注信息长度不能超过500个字符")
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 条件表达式（用于复杂权限控制）
     * JSON格式存储条件表达式，如时间限制、IP限制等
     */
    @Size(max = 2000, message = "条件表达式长度不能超过2000个字符")
    @Column(name = "conditions", length = 2000)
    private String conditions;

    /**
     * 用户关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 空间关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", insertable = false, updatable = false)
    private Space space;

    /**
     * 权限关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private Permission permission;

    /**
     * 授权者关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", insertable = false, updatable = false)
    private User grantor;

    /**
     * 默认构造函数
     */
    public SpacePermission() {
        super();
    }

    /**
     * 构造函数
     * 
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param permissionId 权限ID
     * @param resourceType 资源类型
     * @param status 授权状态
     * @param grantType 授权类型
     */
    public SpacePermission(Long userId, Long spaceId, Long permissionId, String resourceType, String status, String grantType) {
        this();
        this.userId = userId;
        this.spaceId = spaceId;
        this.permissionId = permissionId;
        this.resourceType = resourceType;
        this.status = status;
        this.grantType = grantType;
        this.grantedAt = LocalDateTime.now();
    }

    // Getter和Setter方法

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

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public Boolean getInheritFromParent() {
        return inheritFromParent;
    }

    public void setInheritFromParent(Boolean inheritFromParent) {
        this.inheritFromParent = inheritFromParent;
    }

    public String getPermissionPath() {
        return permissionPath;
    }

    public void setPermissionPath(String permissionPath) {
        this.permissionPath = permissionPath;
    }

    public Integer getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(Integer permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Long getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(Long grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Space getSpace() {
        return space;
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public User getGrantor() {
        return grantor;
    }

    public void setGrantor(User grantor) {
        this.grantor = grantor;
    }

    // 业务方法

    /**
     * 检查权限是否已过期
     * 
     * @return true如果权限已过期
     */
    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 检查是否为授权状态
     * 
     * @return true如果是授权状态
     */
    public boolean isGranted() {
        return "GRANTED".equals(this.status) && !isExpired();
    }

    /**
     * 检查是否为拒绝状态
     * 
     * @return true如果是拒绝状态
     */
    public boolean isDenied() {
        return "DENIED".equals(this.status);
    }

    /**
     * 检查是否为继承状态
     * 
     * @return true如果是继承状态
     */
    public boolean isInherited() {
        return "INHERITED".equals(this.status);
    }

    /**
     * 检查是否为直接授权
     * 
     * @return true如果是直接授权
     */
    public boolean isDirectGrant() {
        return "DIRECT".equals(this.grantType);
    }

    /**
     * 检查是否为继承授权
     * 
     * @return true如果是继承授权
     */
    public boolean isInheritedGrant() {
        return "INHERITED".equals(this.grantType);
    }

    /**
     * 检查是否为基于角色的授权
     * 
     * @return true如果是基于角色的授权
     */
    public boolean isRoleBasedGrant() {
        return "ROLE_BASED".equals(this.grantType);
    }

    /**
     * 检查是否为文件权限
     * 
     * @return true如果是文件权限
     */
    public boolean isFilePermission() {
        return "FILE".equals(this.resourceType);
    }

    /**
     * 检查是否为文件夹权限
     * 
     * @return true如果是文件夹权限
     */
    public boolean isFolderPermission() {
        return "FOLDER".equals(this.resourceType);
    }

    /**
     * 检查是否为空间权限
     * 
     * @return true如果是空间权限
     */
    public boolean isSpacePermission() {
        return "SPACE".equals(this.resourceType);
    }

    /**
     * 比较权限级别
     * 
     * @param other 其他空间权限
     * @return 正数表示当前权限级别更高，负数表示更低，0表示相等
     */
    public int compareLevel(SpacePermission other) {
        if (other == null) {
            return 1;
        }
        return this.permissionLevel.compareTo(other.permissionLevel);
    }

    /**
     * 检查权限是否适用于指定的资源
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return true如果适用
     */
    public boolean appliesToResource(String resourceType, Long resourceId) {
        if (!this.resourceType.equals(resourceType)) {
            return false;
        }
        
        // 空间级权限适用于所有资源
        if ("SPACE".equals(this.resourceType) && this.resourceId == null) {
            return true;
        }
        
        // 精确匹配资源ID
        return this.resourceId != null && this.resourceId.equals(resourceId);
    }

    /**
     * 构建权限路径
     * 
     * @param spaceId 空间ID
     * @param parentPath 父级路径
     * @param resourceId 资源ID
     * @return 构建的权限路径
     */
    public static String buildPermissionPath(Long spaceId, String parentPath, Long resourceId) {
        StringBuilder path = new StringBuilder();
        path.append("/").append(spaceId);
        
        if (parentPath != null && !parentPath.trim().isEmpty() && !"/".equals(parentPath.trim())) {
            if (!parentPath.startsWith("/")) {
                path.append("/");
            }
            path.append(parentPath);
        }
        
        if (resourceId != null) {
            path.append("/").append(resourceId);
        }
        
        return path.toString();
    }

    /**
     * 检查路径是否为子路径
     * 
     * @param parentPath 父路径
     * @param childPath 子路径
     * @return true如果是子路径
     */
    public static boolean isSubPath(String parentPath, String childPath) {
        if (parentPath == null || childPath == null) {
            return false;
        }
        
        if (parentPath.equals(childPath)) {
            return true;
        }
        
        String normalizedParent = parentPath.endsWith("/") ? parentPath : parentPath + "/";
        return childPath.startsWith(normalizedParent);
    }

    @Override
    public String toString() {
        return "SpacePermission{" +
                "id=" + id +
                ", userId=" + userId +
                ", spaceId=" + spaceId +
                ", permissionId=" + permissionId +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId=" + resourceId +
                ", status='" + status + '\'' +
                ", grantType='" + grantType + '\'' +
                ", permissionLevel=" + permissionLevel +
                ", grantedAt=" + grantedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpacePermission that = (SpacePermission) o;

        if (!userId.equals(that.userId)) return false;
        if (!spaceId.equals(that.spaceId)) return false;
        if (!permissionId.equals(that.permissionId)) return false;
        if (!resourceType.equals(that.resourceType)) return false;
        return resourceId != null ? resourceId.equals(that.resourceId) : that.resourceId == null;
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + spaceId.hashCode();
        result = 31 * result + permissionId.hashCode();
        result = 31 * result + resourceType.hashCode();
        result = 31 * result + (resourceId != null ? resourceId.hashCode() : 0);
        return result;
    }
} 