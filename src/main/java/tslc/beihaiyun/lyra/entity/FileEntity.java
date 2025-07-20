package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件实体类
 * 存储文件基本信息和元数据
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Entity
@Table(
    name = "files",
    indexes = {
        @Index(name = "idx_file_folder_id", columnList = "folder_id"),
        @Index(name = "idx_file_space_id", columnList = "space_id"),
        @Index(name = "idx_file_status", columnList = "status"),
        @Index(name = "idx_file_hash", columnList = "file_hash")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_file_space_path", columnNames = {"space_id", "path"})
    }
)
public class FileEntity extends BaseEntity {

    /**
     * 文件状态枚举
     */
    public enum FileStatus {
        /** 活跃状态 */
        ACTIVE,
        /** 已删除 */
        DELETED,
        /** 已归档 */
        ARCHIVED
    }

    /**
     * 文件ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 文件名（显示名称）
     */
    @NotBlank(message = "文件名不能为空")
    @Size(min = 1, max = 255, message = "文件名长度必须在1-255个字符之间")
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * 原始文件名
     */
    @NotBlank(message = "原始文件名不能为空")
    @Size(min = 1, max = 255, message = "原始文件名长度必须在1-255个字符之间")
    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    @Size(max = 1000, message = "文件路径长度不能超过1000个字符")
    @Column(name = "path", length = 1000, nullable = false)
    private String path;

    /**
     * 所属文件夹
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    /**
     * 所属空间
     */
    @NotNull(message = "所属空间不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 0, message = "文件大小不能为负数")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * MIME类型
     */
    @Size(max = 100, message = "MIME类型长度不能超过100个字符")
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 文件哈希值（SHA-256）
     */
    @Size(max = 64, message = "文件哈希值长度不能超过64个字符")
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    /**
     * 存储路径
     */
    @NotBlank(message = "存储路径不能为空")
    @Size(max = 1000, message = "存储路径长度不能超过1000个字符")
    @Column(name = "storage_path", length = 1000, nullable = false)
    private String storagePath;

    /**
     * 文件版本号
     */
    @NotNull(message = "文件版本号不能为空")
    @Min(value = 1, message = "文件版本号必须大于0")
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * 文件状态
     */
    @NotNull(message = "文件状态不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FileStatus status = FileStatus.ACTIVE;

    /**
     * 是否公开
     */
    @NotNull(message = "是否公开不能为空")
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    /**
     * 下载次数
     */
    @NotNull(message = "下载次数不能为空")
    @Min(value = 0, message = "下载次数不能为负数")
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    /**
     * 最后修改时间
     */
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    /**
     * 文件版本集合
     */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileVersion> versions = new HashSet<>();

    /**
     * 分享链接集合
     */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ShareLink> shareLinks = new HashSet<>();

    /**
     * 默认构造函数
     */
    public FileEntity() {
    }

    /**
     * 构造函数
     * 
     * @param name 文件名
     * @param originalName 原始文件名
     * @param path 文件路径
     * @param space 所属空间
     * @param sizeBytes 文件大小
     * @param storagePath 存储路径
     */
    public FileEntity(String name, String originalName, String path, Space space, Long sizeBytes, String storagePath) {
        this.name = name;
        this.originalName = originalName;
        this.path = path;
        this.space = space;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.lastModifiedAt = LocalDateTime.now();
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取文件ID
     * 
     * @return 文件ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置文件ID
     * 
     * @param id 文件ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取文件名
     * 
     * @return 文件名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置文件名
     * 
     * @param name 文件名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取原始文件名
     * 
     * @return 原始文件名
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * 设置原始文件名
     * 
     * @param originalName 原始文件名
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    /**
     * 获取文件路径
     * 
     * @return 文件路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置文件路径
     * 
     * @param path 文件路径
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取所属文件夹
     * 
     * @return 所属文件夹
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * 设置所属文件夹
     * 
     * @param folder 所属文件夹
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    /**
     * 获取所属空间
     * 
     * @return 所属空间
     */
    public Space getSpace() {
        return space;
    }

    /**
     * 设置所属空间
     * 
     * @param space 所属空间
     */
    public void setSpace(Space space) {
        this.space = space;
    }

    /**
     * 获取文件大小
     * 
     * @return 文件大小（字节）
     */
    public Long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * 设置文件大小
     * 
     * @param sizeBytes 文件大小（字节）
     */
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    /**
     * 获取MIME类型
     * 
     * @return MIME类型
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 设置MIME类型
     * 
     * @param mimeType MIME类型
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * 获取文件哈希值
     * 
     * @return 文件哈希值
     */
    public String getFileHash() {
        return fileHash;
    }

    /**
     * 设置文件哈希值
     * 
     * @param fileHash 文件哈希值
     */
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    /**
     * 获取存储路径
     * 
     * @return 存储路径
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 设置存储路径
     * 
     * @param storagePath 存储路径
     */
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * 获取文件版本号
     * 
     * @return 文件版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置文件版本号
     * 
     * @param version 文件版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 获取文件状态
     * 
     * @return 文件状态
     */
    public FileStatus getStatus() {
        return status;
    }

    /**
     * 设置文件状态
     * 
     * @param status 文件状态
     */
    public void setStatus(FileStatus status) {
        this.status = status;
    }

    /**
     * 获取是否公开
     * 
     * @return 是否公开
     */
    public Boolean getIsPublic() {
        return isPublic;
    }

    /**
     * 设置是否公开
     * 
     * @param isPublic 是否公开
     */
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * 获取下载次数
     * 
     * @return 下载次数
     */
    public Integer getDownloadCount() {
        return downloadCount;
    }

    /**
     * 设置下载次数
     * 
     * @param downloadCount 下载次数
     */
    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    /**
     * 获取最后修改时间
     * 
     * @return 最后修改时间
     */
    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * 设置最后修改时间
     * 
     * @param lastModifiedAt 最后修改时间
     */
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * 获取文件版本集合
     * 
     * @return 文件版本集合
     */
    public Set<FileVersion> getVersions() {
        return versions;
    }

    /**
     * 设置文件版本集合
     * 
     * @param versions 文件版本集合
     */
    public void setVersions(Set<FileVersion> versions) {
        this.versions = versions;
    }

    /**
     * 获取分享链接集合
     * 
     * @return 分享链接集合
     */
    public Set<ShareLink> getShareLinks() {
        return shareLinks;
    }

    /**
     * 设置分享链接集合
     * 
     * @param shareLinks 分享链接集合
     */
    public void setShareLinks(Set<ShareLink> shareLinks) {
        this.shareLinks = shareLinks;
    }

    // ========== 业务方法 ==========

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * 增加文件版本
     */
    public void incrementVersion() {
        this.version++;
    }

    /**
     * 检查文件是否活跃
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return FileStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查文件是否已删除
     * 
     * @return 是否已删除
     */
    public boolean isDeleted() {
        return FileStatus.DELETED.equals(this.status);
    }

    /**
     * 检查文件是否已归档
     * 
     * @return 是否已归档
     */
    public boolean isArchived() {
        return FileStatus.ARCHIVED.equals(this.status);
    }

    /**
     * 检查文件是否公开
     * 
     * @return 是否公开
     */
    public boolean isPublicFile() {
        return Boolean.TRUE.equals(this.isPublic);
    }

    /**
     * 获取文件扩展名
     * 
     * @return 文件扩展名
     */
    public String getFileExtension() {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取文件大小（人类可读格式）
     * 
     * @return 文件大小字符串
     */
    public String getHumanReadableSize() {
        if (sizeBytes == null) {
            return "0 B";
        }
        
        long bytes = sizeBytes;
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 检查是否为图片文件
     * 
     * @return 是否为图片文件
     */
    public boolean isImageFile() {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("image/");
    }

    /**
     * 检查是否为文本文件
     * 
     * @return 是否为文本文件
     */
    public boolean isTextFile() {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("text/") || 
               mimeType.equals("application/json") ||
               mimeType.equals("application/xml");
    }

    /**
     * 检查是否为视频文件
     * 
     * @return 是否为视频文件
     */
    public boolean isVideoFile() {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("video/");
    }

    /**
     * 检查是否为音频文件
     * 
     * @return 是否为音频文件
     */
    public boolean isAudioFile() {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("audio/");
    }

    // ========== 重写方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEntity)) return false;
        FileEntity that = (FileEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", version=" + version +
                ", status=" + status +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
} 