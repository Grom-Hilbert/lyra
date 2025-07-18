package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import tslc.beihaiyun.lyra.validation.ValidationGroups;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文件实体类
 * 管理文件的元数据和版本信息
 */
@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "文件名不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(max = 255, message = "文件名不能超过255个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "文件路径不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(max = 1000, message = "文件路径不能超过1000个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private String path;

    @Size(max = 100, message = "文件类型不能超过100个字符", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(name = "mime_type")
    private String mimeType;

    @NotNull(message = "文件大小不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Column(nullable = false)
    private Long size;

    @Column(name = "checksum")
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_control_type", nullable = false)
    @Builder.Default
    private VersionControlType versionControlType = VersionControlType.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private FolderEntity folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accessed_at")
    private LocalDateTime accessedAt;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<FileVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<FilePermission> permissions = new ArrayList<>();
    
    @Column(name = "indexed_for_search")
    @Builder.Default
    private Boolean indexedForSearch = false;
    
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        accessedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新文件访问时间
     */
    public void updateAccessTime() {
        this.accessedAt = LocalDateTime.now();
    }
    
    /**
     * 添加文件版本
     * @param version 文件版本
     * @return 添加后的文件版本
     */
    public FileVersion addVersion(FileVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("文件版本不能为空");
        }
        
        // 设置当前版本为非当前版本
        if (version.getIsCurrent()) {
            versions.forEach(v -> v.setIsCurrent(false));
        }
        
        version.setFile(this);
        versions.add(version);
        return version;
    }
    
    /**
     * 获取当前版本
     * @return 当前版本，如果没有则返回null
     */
    public FileVersion getCurrentVersion() {
        return versions.stream()
                .filter(FileVersion::getIsCurrent)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取指定版本号的版本
     * @param versionNumber 版本号
     * @return 指定版本号的版本，如果没有则返回null
     */
    public FileVersion getVersion(Integer versionNumber) {
        return versions.stream()
                .filter(v -> Objects.equals(v.getVersionNumber(), versionNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 添加文件权限
     * @param permission 文件权限
     * @return 添加后的文件权限
     */
    public FilePermission addPermission(FilePermission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("文件权限不能为空");
        }
        
        permission.setFile(this);
        permissions.add(permission);
        return permission;
    }
    
    /**
     * 移除文件权限
     * @param permission 文件权限
     * @return 是否移除成功
     */
    public boolean removePermission(FilePermission permission) {
        if (permission == null) {
            return false;
        }
        
        return permissions.remove(permission);
    }
    
    /**
     * 检查用户是否有指定权限
     * @param user 用户
     * @param permissionType 权限类型
     * @return 是否有权限
     */
    public boolean hasPermission(User user, FilePermission.PermissionType permissionType) {
        if (user == null || permissionType == null) {
            return false;
        }
        
        // 文件所有者拥有所有权限
        if (Objects.equals(owner.getId(), user.getId())) {
            return true;
        }
        
        // 检查用户直接权限
        boolean hasDirectPermission = permissions.stream()
                .filter(p -> p.getUser() != null && Objects.equals(p.getUser().getId(), user.getId()))
                .filter(p -> p.getPermissionType() == permissionType)
                .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now()))
                .anyMatch(p -> true);
                
        if (hasDirectPermission) {
            return true;
        }
        
        // 检查角色权限
        return user.getRoles().stream()
                .anyMatch(role -> permissions.stream()
                        .filter(p -> p.getRole() != null && Objects.equals(p.getRole().getId(), role.getId()))
                        .filter(p -> p.getPermissionType() == permissionType)
                        .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now()))
                        .anyMatch(p -> true));
    }
    
    /**
     * 设置文件为已索引状态
     */
    public void markAsIndexed() {
        this.indexedForSearch = true;
    }
    
    /**
     * 设置文件为未索引状态
     */
    public void markAsNotIndexed() {
        this.indexedForSearch = false;
    }
    
    /**
     * 检查文件是否支持预览
     * @return 是否支持预览
     */
    public boolean isPreviewable() {
        if (mimeType == null) {
            return false;
        }
        
        return mimeType.startsWith("text/") || 
               mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf");
    }
    
    /**
     * 检查文件是否支持在线编辑
     * @return 是否支持在线编辑
     */
    public boolean isEditable() {
        if (mimeType == null) {
            return false;
        }
        
        return mimeType.startsWith("text/");
    }
    
    /**
     * 检查文件是否为文本文件
     * @return 是否为文本文件
     */
    public boolean isTextFile() {
        if (mimeType == null) {
            return false;
        }
        
        return mimeType.startsWith("text/");
    }
    
    /**
     * 检查文件是否为图片文件
     * @return 是否为图片文件
     */
    public boolean isImageFile() {
        if (mimeType == null) {
            return false;
        }
        
        return mimeType.startsWith("image/");
    }
    
    /**
     * 检查文件是否为二进制文件
     * @return 是否为二进制文件
     */
    public boolean isBinaryFile() {
        return !isTextFile();
    }
    
    /**
     * 检查文件是否支持版本控制
     * @return 是否支持版本控制
     */
    public boolean supportsVersionControl() {
        return versionControlType != VersionControlType.NONE;
    }
    
    /**
     * 检查文件是否支持高级版本控制
     * @return 是否支持高级版本控制
     */
    public boolean supportsAdvancedVersionControl() {
        return versionControlType == VersionControlType.ADVANCED;
    }
    
    /**
     * 检查文件是否在企业空间
     * @return 是否在企业空间
     */
    public boolean isInEnterpriseSpace() {
        return spaceType == SpaceType.ENTERPRISE;
    }
    
    /**
     * 检查文件是否在个人空间
     * @return 是否在个人空间
     */
    public boolean isInPersonalSpace() {
        return spaceType == SpaceType.PERSONAL;
    }
    
    /**
     * 生成文件的存储键
     * @return 存储键
     */
    public String generateStorageKey() {
        return String.format("%s/%s/%s", 
            spaceType.name().toLowerCase(),
            owner.getId(),
            java.util.UUID.randomUUID().toString());
    }

    /**
     * 空间类型枚举
     */
    public enum SpaceType {
        ENTERPRISE,     // 企业空间
        PERSONAL        // 个人空间
    }

    /**
     * 版本控制类型枚举
     */
    public enum VersionControlType {
        NONE,           // 无版本控制
        BASIC,          // 基础版本控制（WebDAV兼容）
        ADVANCED        // 高级版本控制（Git基础）
    }
}