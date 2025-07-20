package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 空间实体类
 * 代表用户的个人空间、企业空间或共享空间
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Entity
@Table(
    name = "spaces",
    indexes = {
        @Index(name = "idx_space_owner_id", columnList = "owner_id"),
        @Index(name = "idx_space_type", columnList = "type"),
        @Index(name = "idx_space_status", columnList = "status")
    }
)
public class Space extends BaseEntity {

    /**
     * 空间类型枚举
     */
    public enum SpaceType {
        /** 个人空间 */
        PERSONAL,
        /** 企业空间 */
        ENTERPRISE,
        /** 共享空间 */
        SHARED
    }

    /**
     * 空间状态枚举
     */
    public enum SpaceStatus {
        /** 活跃状态 */
        ACTIVE,
        /** 已归档 */
        ARCHIVED
    }

    /**
     * 版本控制模式枚举
     */
    public enum VersionControlMode {
        /** 普通版本控制 */
        NORMAL,
        /** Git版本控制 */
        GIT
    }

    /**
     * 空间ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 空间名称
     */
    @NotBlank(message = "空间名称不能为空")
    @Size(min = 1, max = 100, message = "空间名称长度必须在1-100个字符之间")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 空间类型
     */
    @NotNull(message = "空间类型不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private SpaceType type = SpaceType.PERSONAL;

    /**
     * 空间拥有者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * 空间描述
     */
    @Size(max = 1000, message = "空间描述长度不能超过1000个字符")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 空间配额限制（字节）
     * 默认10GB
     */
    @NotNull(message = "配额限制不能为空")
    @Min(value = 0, message = "配额限制不能为负数")
    @Column(name = "quota_limit", nullable = false)
    private Long quotaLimit = 10737418240L; // 10GB

    /**
     * 已使用配额（字节）
     */
    @NotNull(message = "已使用配额不能为空")
    @Min(value = 0, message = "已使用配额不能为负数")
    @Column(name = "quota_used", nullable = false)
    private Long quotaUsed = 0L;

    /**
     * 是否启用版本控制
     */
    @NotNull(message = "版本控制启用状态不能为空")
    @Column(name = "version_control_enabled", nullable = false)
    private Boolean versionControlEnabled = true;

    /**
     * 版本控制模式
     */
    @NotNull(message = "版本控制模式不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "version_control_mode", length = 20, nullable = false)
    private VersionControlMode versionControlMode = VersionControlMode.NORMAL;

    /**
     * 空间状态
     */
    @NotNull(message = "空间状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SpaceStatus status = SpaceStatus.ACTIVE;

    /**
     * 空间下的文件夹集合
     */
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Folder> folders = new HashSet<>();

    /**
     * 空间下的文件集合
     */
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileEntity> files = new HashSet<>();

    /**
     * 空间下的分享链接集合
     */
    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ShareLink> shareLinks = new HashSet<>();

    /**
     * 默认构造函数
     */
    public Space() {
    }

    /**
     * 构造函数
     * 
     * @param name 空间名称
     * @param type 空间类型
     * @param owner 空间拥有者
     */
    public Space(String name, SpaceType type, User owner) {
        this.name = name;
        this.type = type;
        this.owner = owner;
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取空间ID
     * 
     * @return 空间ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置空间ID
     * 
     * @param id 空间ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取空间名称
     * 
     * @return 空间名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置空间名称
     * 
     * @param name 空间名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取空间类型
     * 
     * @return 空间类型
     */
    public SpaceType getType() {
        return type;
    }

    /**
     * 设置空间类型
     * 
     * @param type 空间类型
     */
    public void setType(SpaceType type) {
        this.type = type;
    }

    /**
     * 获取空间拥有者
     * 
     * @return 空间拥有者
     */
    public User getOwner() {
        return owner;
    }

    /**
     * 设置空间拥有者
     * 
     * @param owner 空间拥有者
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * 获取空间描述
     * 
     * @return 空间描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置空间描述
     * 
     * @param description 空间描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取配额限制
     * 
     * @return 配额限制（字节）
     */
    public Long getQuotaLimit() {
        return quotaLimit;
    }

    /**
     * 设置配额限制
     * 
     * @param quotaLimit 配额限制（字节）
     */
    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    /**
     * 获取已使用配额
     * 
     * @return 已使用配额（字节）
     */
    public Long getQuotaUsed() {
        return quotaUsed;
    }

    /**
     * 设置已使用配额
     * 
     * @param quotaUsed 已使用配额（字节）
     */
    public void setQuotaUsed(Long quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    /**
     * 获取版本控制启用状态
     * 
     * @return 是否启用版本控制
     */
    public Boolean getVersionControlEnabled() {
        return versionControlEnabled;
    }

    /**
     * 设置版本控制启用状态
     * 
     * @param versionControlEnabled 是否启用版本控制
     */
    public void setVersionControlEnabled(Boolean versionControlEnabled) {
        this.versionControlEnabled = versionControlEnabled;
    }

    /**
     * 获取版本控制模式
     * 
     * @return 版本控制模式
     */
    public VersionControlMode getVersionControlMode() {
        return versionControlMode;
    }

    /**
     * 设置版本控制模式
     * 
     * @param versionControlMode 版本控制模式
     */
    public void setVersionControlMode(VersionControlMode versionControlMode) {
        this.versionControlMode = versionControlMode;
    }

    /**
     * 获取空间状态
     * 
     * @return 空间状态
     */
    public SpaceStatus getStatus() {
        return status;
    }

    /**
     * 设置空间状态
     * 
     * @param status 空间状态
     */
    public void setStatus(SpaceStatus status) {
        this.status = status;
    }

    /**
     * 获取空间下的文件夹集合
     * 
     * @return 文件夹集合
     */
    public Set<Folder> getFolders() {
        return folders;
    }

    /**
     * 设置空间下的文件夹集合
     * 
     * @param folders 文件夹集合
     */
    public void setFolders(Set<Folder> folders) {
        this.folders = folders;
    }

    /**
     * 获取空间下的文件集合
     * 
     * @return 文件集合
     */
    public Set<FileEntity> getFiles() {
        return files;
    }

    /**
     * 设置空间下的文件集合
     * 
     * @param files 文件集合
     */
    public void setFiles(Set<FileEntity> files) {
        this.files = files;
    }

    /**
     * 获取空间下的分享链接集合
     * 
     * @return 分享链接集合
     */
    public Set<ShareLink> getShareLinks() {
        return shareLinks;
    }

    /**
     * 设置空间下的分享链接集合
     * 
     * @param shareLinks 分享链接集合
     */
    public void setShareLinks(Set<ShareLink> shareLinks) {
        this.shareLinks = shareLinks;
    }

    // ========== 业务方法 ==========

    /**
     * 检查配额是否足够
     * 
     * @param additionalSize 需要增加的大小（字节）
     * @return 是否有足够配额
     */
    public boolean hasEnoughQuota(long additionalSize) {
        return (quotaUsed + additionalSize) <= quotaLimit;
    }

    /**
     * 增加已使用配额
     * 
     * @param size 增加的大小（字节）
     */
    public void addUsedQuota(long size) {
        this.quotaUsed += size;
    }

    /**
     * 减少已使用配额
     * 
     * @param size 减少的大小（字节）
     */
    public void reduceUsedQuota(long size) {
        this.quotaUsed = Math.max(0, this.quotaUsed - size);
    }

    /**
     * 获取配额使用率
     * 
     * @return 配额使用率（0.0-1.0）
     */
    public double getQuotaUsageRatio() {
        if (quotaLimit == 0) {
            return 0.0;
        }
        return (double) quotaUsed / quotaLimit;
    }

    /**
     * 检查是否为个人空间
     * 
     * @return 是否为个人空间
     */
    public boolean isPersonalSpace() {
        return SpaceType.PERSONAL.equals(this.type);
    }

    /**
     * 检查是否为企业空间
     * 
     * @return 是否为企业空间
     */
    public boolean isEnterpriseSpace() {
        return SpaceType.ENTERPRISE.equals(this.type);
    }

    /**
     * 检查是否为共享空间
     * 
     * @return 是否为共享空间
     */
    public boolean isSharedSpace() {
        return SpaceType.SHARED.equals(this.type);
    }

    /**
     * 检查空间是否活跃
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return SpaceStatus.ACTIVE.equals(this.status);
    }

    // ========== 重写方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Space)) return false;
        Space space = (Space) o;
        return id != null && id.equals(space.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Space{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", quotaUsed=" + quotaUsed +
                ", quotaLimit=" + quotaLimit +
                ", status=" + status +
                '}';
    }
} 