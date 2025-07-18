package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件实体类
 * 管理文件的元数据和版本信息
 */
@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "文件路径不能为空")
    @Size(max = 1000, message = "文件路径不能超过1000个字符")
    @Column(nullable = false)
    private String path;

    @Size(max = 100, message = "文件类型不能超过100个字符")
    @Column(name = "mime_type")
    private String mimeType;

    @NotNull(message = "文件大小不能为空")
    @Column(nullable = false)
    private Long size;

    @Column(name = "checksum")
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false)
    private SpaceType spaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_control_type", nullable = false)
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

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileVersion> versions;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FilePermission> permissions;

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