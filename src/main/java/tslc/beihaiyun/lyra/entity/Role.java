package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 定义系统中的角色信息
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "roles",
    indexes = {
        @Index(name = "idx_role_code", columnList = "code"),
        @Index(name = "idx_role_type", columnList = "type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_code", columnNames = "code")
    }
)
@SQLRestriction("is_deleted = false")
public class Role extends BaseEntity {

    /**
     * 角色ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 角色代码（唯一标识）
     */
    @NotBlank(message = "角色代码不能为空")
    @Size(min = 2, max = 50, message = "角色代码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色代码必须以大写字母开头，只能包含大写字母、数字和下划线")
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 100, message = "角色名称长度必须在2-100个字符之间")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 角色描述
     */
    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 角色类型
     */
    @NotNull(message = "角色类型不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private RoleType type = RoleType.CUSTOM;

    /**
     * 是否为系统内置角色
     */
    @Column(name = "is_system", nullable = false)
    private Boolean system = false;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 排序顺序
     */
    @Min(value = 0, message = "排序顺序不能为负数")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 用户角色关联
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * 角色类型枚举
     */
    public enum RoleType {
        /**
         * 系统管理员
         */
        SYSTEM_ADMIN,
        /**
         * 企业管理员
         */
        ORGANIZATION_ADMIN,
        /**
         * 部门管理员
         */
        DEPARTMENT_ADMIN,
        /**
         * 普通用户
         */
        USER,
        /**
         * 访客
         */
        GUEST,
        /**
         * 自定义角色
         */
        CUSTOM
    }

    // 构造函数
    public Role() {
    }

    public Role(String code, String name, RoleType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public Role(String code, String name, String description, RoleType type) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    // 业务方法

    /**
     * 检查角色是否可用（已启用且未删除）
     * 
     * @return 是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled) && !isDeleted();
    }

    /**
     * 检查是否为管理员角色
     * 
     * @return 是否为管理员角色
     */
    public boolean isAdminRole() {
        return RoleType.SYSTEM_ADMIN.equals(type) || 
               RoleType.ORGANIZATION_ADMIN.equals(type) || 
               RoleType.DEPARTMENT_ADMIN.equals(type);
    }

    /**
     * 检查是否为系统级管理员
     * 
     * @return 是否为系统级管理员
     */
    public boolean isSystemAdmin() {
        return RoleType.SYSTEM_ADMIN.equals(type);
    }

    /**
     * 检查是否为普通用户角色
     * 
     * @return 是否为普通用户角色
     */
    public boolean isUserRole() {
        return RoleType.USER.equals(type);
    }

    /**
     * 检查是否为访客角色
     * 
     * @return 是否为访客角色
     */
    public boolean isGuestRole() {
        return RoleType.GUEST.equals(type);
    }

    /**
     * 启用角色
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 禁用角色
     */
    public void disable() {
        this.enabled = false;
    }

    // 常用的系统角色常量
    public static final String SYSTEM_ADMIN_CODE = "SYSTEM_ADMIN";
    public static final String ORG_ADMIN_CODE = "ORG_ADMIN";
    public static final String DEPT_ADMIN_CODE = "DEPT_ADMIN";
    public static final String USER_CODE = "USER";
    public static final String GUEST_CODE = "GUEST";

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", system=" + system +
                ", enabled=" + enabled +
                "} " + super.toString();
    }
} 