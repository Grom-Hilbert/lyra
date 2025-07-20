package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限实体类
 * 定义系统中的基础权限信息
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Entity
@Table(
    name = "permissions",
    indexes = {
        @Index(name = "idx_permission_code", columnList = "code"),
        @Index(name = "idx_permission_resource_type", columnList = "resource_type"),
        @Index(name = "idx_permission_category", columnList = "category")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
    }
)
@SQLRestriction("is_deleted = false")
public class Permission extends BaseEntity {

    /**
     * 权限ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 权限代码（唯一标识）
     */
    @NotBlank(message = "权限代码不能为空")
    @Size(min = 2, max = 100, message = "权限代码长度必须在2-100个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9._:]*$", message = "权限代码必须以小写字母开头，只能包含小写字母、数字、点号、下划线和冒号")
    @Column(name = "code", length = 100, nullable = false)
    private String code;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 100, message = "权限名称长度必须在2-100个字符之间")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 权限描述
     */
    @Size(max = 500, message = "权限描述长度不能超过500个字符")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 资源类型
     * FILE: 文件权限
     * FOLDER: 文件夹权限
     * SPACE: 空间权限
     * SYSTEM: 系统权限
     */
    @NotBlank(message = "资源类型不能为空")
    @Pattern(regexp = "^(FILE|FOLDER|SPACE|SYSTEM)$", message = "资源类型必须是FILE、FOLDER、SPACE或SYSTEM")
    @Column(name = "resource_type", length = 20, nullable = false)
    private String resourceType;

    /**
     * 权限类别
     * READ: 读取权限
     * WRITE: 写入权限
     * DELETE: 删除权限
     * ADMIN: 管理权限
     * SHARE: 分享权限
     */
    @NotBlank(message = "权限类别不能为空")
    @Pattern(regexp = "^(READ|WRITE|DELETE|ADMIN|SHARE)$", message = "权限类别必须是READ、WRITE、DELETE、ADMIN或SHARE")
    @Column(name = "category", length = 20, nullable = false)
    private String category;

    /**
     * 权限级别（优先级）
     * 数值越大优先级越高，用于权限覆盖判断
     */
    @NotNull(message = "权限级别不能为空")
    @Min(value = 1, message = "权限级别必须大于0")
    @Max(value = 100, message = "权限级别不能超过100")
    @Column(name = "level", nullable = false)
    private Integer level;

    /**
     * 是否为系统权限
     * 系统权限不可删除和修改
     */
    @NotNull(message = "系统权限标识不能为空")
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    /**
     * 权限组（用于权限分组管理）
     */
    @Size(max = 50, message = "权限组名称长度不能超过50个字符")
    @Column(name = "permission_group", length = 50)
    private String permissionGroup;

    /**
     * 依赖的权限（JSON格式存储权限代码列表）
     * 表示拥有此权限需要先拥有的其他权限
     */
    @Size(max = 1000, message = "依赖权限信息长度不能超过1000个字符")
    @Column(name = "dependencies", length = 1000)
    private String dependencies;

    /**
     * 权限的角色关联
     * 一个权限可以被多个角色拥有
     */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

    /**
     * 默认构造函数
     */
    public Permission() {
        super();
    }

    /**
     * 构造函数
     * 
     * @param code 权限代码
     * @param name 权限名称
     * @param resourceType 资源类型
     * @param category 权限类别
     * @param level 权限级别
     */
    public Permission(String code, String name, String resourceType, String category, Integer level) {
        this();
        this.code = code;
        this.name = name;
        this.resourceType = resourceType;
        this.category = category;
        this.level = level;
    }

    // Getter和Setter方法

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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getPermissionGroup() {
        return permissionGroup;
    }

    public void setPermissionGroup(String permissionGroup) {
        this.permissionGroup = permissionGroup;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // 业务方法

    /**
     * 检查是否为读权限
     * 
     * @return true如果是读权限
     */
    public boolean isReadPermission() {
        return "READ".equals(this.category);
    }

    /**
     * 检查是否为写权限
     * 
     * @return true如果是写权限
     */
    public boolean isWritePermission() {
        return "WRITE".equals(this.category);
    }

    /**
     * 检查是否为删除权限
     * 
     * @return true如果是删除权限
     */
    public boolean isDeletePermission() {
        return "DELETE".equals(this.category);
    }

    /**
     * 检查是否为管理权限
     * 
     * @return true如果是管理权限
     */
    public boolean isAdminPermission() {
        return "ADMIN".equals(this.category);
    }

    /**
     * 检查是否为分享权限
     * 
     * @return true如果是分享权限
     */
    public boolean isSharePermission() {
        return "SHARE".equals(this.category);
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
     * 检查是否为系统权限
     * 
     * @return true如果是系统权限
     */
    public boolean isSystemResourcePermission() {
        return "SYSTEM".equals(this.resourceType);
    }

    /**
     * 比较权限级别
     * 
     * @param other 其他权限
     * @return 正数表示当前权限级别更高，负数表示更低，0表示相等
     */
    public int compareLevel(Permission other) {
        if (other == null) {
            return 1;
        }
        return this.level.compareTo(other.level);
    }

    /**
     * 检查权限是否兼容指定的资源类型和类别
     * 
     * @param resourceType 资源类型
     * @param category 权限类别
     * @return true如果兼容
     */
    public boolean isCompatible(String resourceType, String category) {
        return this.resourceType.equals(resourceType) && this.category.equals(category);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", category='" + category + '\'' +
                ", level=" + level +
                ", isSystem=" + isSystem +
                ", isEnabled=" + isEnabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;

        return code != null ? code.equals(that.code) : that.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
} 