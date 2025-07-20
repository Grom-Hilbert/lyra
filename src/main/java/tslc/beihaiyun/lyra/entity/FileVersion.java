package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * 文件版本实体类
 * 存储文件的历史版本信息
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "file_versions",
    indexes = {
        @Index(name = "idx_file_version_file_id", columnList = "file_id"),
        @Index(name = "idx_file_version_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_file_version_number", columnNames = {"file_id", "version_number"})
    }
)
public class FileVersion extends BaseEntity {

    /**
     * 版本ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 关联文件
     */
    @NotNull(message = "关联文件不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    /**
     * 版本号
     */
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号必须大于0")
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 0, message = "文件大小不能为负数")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

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
     * 变更注释
     */
    @Size(max = 1000, message = "变更注释长度不能超过1000个字符")
    @Column(name = "change_comment", columnDefinition = "TEXT")
    private String changeComment;

    /**
     * 默认构造函数
     */
    public FileVersion() {
    }

    /**
     * 构造函数
     * 
     * @param file 关联文件
     * @param versionNumber 版本号
     * @param sizeBytes 文件大小
     * @param storagePath 存储路径
     */
    public FileVersion(FileEntity file, Integer versionNumber, Long sizeBytes, String storagePath) {
        this.file = file;
        this.versionNumber = versionNumber;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    /**
     * 构造函数
     * 
     * @param file 关联文件
     * @param versionNumber 版本号
     * @param sizeBytes 文件大小
     * @param storagePath 存储路径
     * @param changeComment 变更注释
     */
    public FileVersion(FileEntity file, Integer versionNumber, Long sizeBytes, String storagePath, String changeComment) {
        this.file = file;
        this.versionNumber = versionNumber;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.changeComment = changeComment;
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取版本ID
     * 
     * @return 版本ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置版本ID
     * 
     * @param id 版本ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取关联文件
     * 
     * @return 关联文件
     */
    public FileEntity getFile() {
        return file;
    }

    /**
     * 设置关联文件
     * 
     * @param file 关联文件
     */
    public void setFile(FileEntity file) {
        this.file = file;
    }

    /**
     * 获取版本号
     * 
     * @return 版本号
     */
    public Integer getVersionNumber() {
        return versionNumber;
    }

    /**
     * 设置版本号
     * 
     * @param versionNumber 版本号
     */
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
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
     * 获取变更注释
     * 
     * @return 变更注释
     */
    public String getChangeComment() {
        return changeComment;
    }

    /**
     * 设置变更注释
     * 
     * @param changeComment 变更注释
     */
    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }

    // ========== 业务方法 ==========

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
     * 检查是否有变更注释
     * 
     * @return 是否有变更注释
     */
    public boolean hasChangeComment() {
        return changeComment != null && !changeComment.trim().isEmpty();
    }

    /**
     * 检查是否为首个版本
     * 
     * @return 是否为首个版本
     */
    public boolean isFirstVersion() {
        return versionNumber != null && versionNumber == 1;
    }

    // ========== 重写方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileVersion)) return false;
        FileVersion that = (FileVersion) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FileVersion{" +
                "id=" + id +
                ", versionNumber=" + versionNumber +
                ", sizeBytes=" + sizeBytes +
                ", fileHash='" + fileHash + '\'' +
                ", storagePath='" + storagePath + '\'' +
                ", changeComment='" + changeComment + '\'' +
                '}';
    }
} 