package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * 分享链接实体类
 * 支持文件和文件夹的分享功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Entity
@Table(
    name = "share_links",
    indexes = {
        @Index(name = "idx_share_link_token", columnList = "token"),
        @Index(name = "idx_share_link_file_id", columnList = "file_id"),
        @Index(name = "idx_share_link_folder_id", columnList = "folder_id"),
        @Index(name = "idx_share_link_space_id", columnList = "space_id"),
        @Index(name = "idx_share_link_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_share_link_token", columnNames = "token")
    }
)
public class ShareLink extends BaseEntity {

    /**
     * 访问类型枚举
     */
    public enum AccessType {
        /** 只读访问 */
        READ,
        /** 读写访问 */
        WRITE
    }

    /**
     * 分享链接ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 分享令牌
     */
    @NotBlank(message = "分享令牌不能为空")
    @Size(min = 32, max = 32, message = "分享令牌长度必须为32个字符")
    @Column(name = "token", length = 32, nullable = false)
    private String token;

    /**
     * 分享的文件
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    /**
     * 分享的文件夹
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
     * 访问类型
     */
    @NotNull(message = "访问类型不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", length = 20, nullable = false)
    private AccessType accessType = AccessType.READ;

    /**
     * 访问密码（加密后）
     */
    @Size(max = 255, message = "访问密码长度不能超过255个字符")
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * 下载限制次数
     */
    @Min(value = 0, message = "下载限制次数不能为负数")
    @Column(name = "download_limit")
    private Integer downloadLimit;

    /**
     * 已下载次数
     */
    @NotNull(message = "已下载次数不能为空")
    @Min(value = 0, message = "已下载次数不能为负数")
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 是否活跃
     */
    @NotNull(message = "是否活跃不能为空")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 默认构造函数
     */
    public ShareLink() {
    }

    /**
     * 构造函数 - 文件分享
     * 
     * @param token 分享令牌
     * @param file 分享的文件
     * @param space 所属空间
     * @param accessType 访问类型
     */
    public ShareLink(String token, FileEntity file, Space space, AccessType accessType) {
        this.token = token;
        this.file = file;
        this.space = space;
        this.accessType = accessType;
    }

    /**
     * 构造函数 - 文件夹分享
     * 
     * @param token 分享令牌
     * @param folder 分享的文件夹
     * @param space 所属空间
     * @param accessType 访问类型
     */
    public ShareLink(String token, Folder folder, Space space, AccessType accessType) {
        this.token = token;
        this.folder = folder;
        this.space = space;
        this.accessType = accessType;
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取分享链接ID
     * 
     * @return 分享链接ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置分享链接ID
     * 
     * @param id 分享链接ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取分享令牌
     * 
     * @return 分享令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置分享令牌
     * 
     * @param token 分享令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取分享的文件
     * 
     * @return 分享的文件
     */
    public FileEntity getFile() {
        return file;
    }

    /**
     * 设置分享的文件
     * 
     * @param file 分享的文件
     */
    public void setFile(FileEntity file) {
        this.file = file;
    }

    /**
     * 获取分享的文件夹
     * 
     * @return 分享的文件夹
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * 设置分享的文件夹
     * 
     * @param folder 分享的文件夹
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
     * 获取访问类型
     * 
     * @return 访问类型
     */
    public AccessType getAccessType() {
        return accessType;
    }

    /**
     * 设置访问类型
     * 
     * @param accessType 访问类型
     */
    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    /**
     * 获取访问密码
     * 
     * @return 访问密码（加密后）
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置访问密码
     * 
     * @param passwordHash 访问密码（加密后）
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取下载限制次数
     * 
     * @return 下载限制次数
     */
    public Integer getDownloadLimit() {
        return downloadLimit;
    }

    /**
     * 设置下载限制次数
     * 
     * @param downloadLimit 下载限制次数
     */
    public void setDownloadLimit(Integer downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    /**
     * 获取已下载次数
     * 
     * @return 已下载次数
     */
    public Integer getDownloadCount() {
        return downloadCount;
    }

    /**
     * 设置已下载次数
     * 
     * @param downloadCount 已下载次数
     */
    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    /**
     * 获取过期时间
     * 
     * @return 过期时间
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 设置过期时间
     * 
     * @param expiresAt 过期时间
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 获取是否活跃
     * 
     * @return 是否活跃
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * 设置是否活跃
     * 
     * @param isActive 是否活跃
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // ========== 业务方法 ==========

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * 检查是否过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否活跃且未过期
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }

    /**
     * 检查是否需要密码
     * 
     * @return 是否需要密码
     */
    public boolean requiresPassword() {
        return passwordHash != null && !passwordHash.trim().isEmpty();
    }

    /**
     * 检查是否达到下载限制
     * 
     * @return 是否达到下载限制
     */
    public boolean isDownloadLimitReached() {
        return downloadLimit != null && downloadCount >= downloadLimit;
    }

    /**
     * 检查是否为只读访问
     * 
     * @return 是否为只读访问
     */
    public boolean isReadOnly() {
        return AccessType.READ.equals(accessType);
    }

    /**
     * 检查是否为读写访问
     * 
     * @return 是否为读写访问
     */
    public boolean isReadWrite() {
        return AccessType.WRITE.equals(accessType);
    }

    /**
     * 检查是否为文件分享
     * 
     * @return 是否为文件分享
     */
    public boolean isFileShare() {
        return file != null;
    }

    /**
     * 检查是否为文件夹分享
     * 
     * @return 是否为文件夹分享
     */
    public boolean isFolderShare() {
        return folder != null;
    }

    /**
     * 获取分享目标的名称
     * 
     * @return 分享目标名称
     */
    public String getTargetName() {
        if (file != null) {
            return file.getName();
        } else if (folder != null) {
            return folder.getName();
        }
        return "未知";
    }

    /**
     * 获取分享类型描述
     * 
     * @return 分享类型描述
     */
    public String getShareTypeDescription() {
        if (isFileShare()) {
            return "文件分享";
        } else if (isFolderShare()) {
            return "文件夹分享";
        }
        return "未知类型";
    }

    /**
     * 禁用分享链接
     */
    public void disable() {
        this.isActive = false;
    }

    /**
     * 启用分享链接
     */
    public void enable() {
        this.isActive = true;
    }

    // ========== 重写方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShareLink)) return false;
        ShareLink shareLink = (ShareLink) o;
        return id != null && id.equals(shareLink.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ShareLink{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", accessType=" + accessType +
                ", downloadCount=" + downloadCount +
                ", downloadLimit=" + downloadLimit +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                ", shareType='" + getShareTypeDescription() + '\'' +
                ", targetName='" + getTargetName() + '\'' +
                '}';
    }
} 