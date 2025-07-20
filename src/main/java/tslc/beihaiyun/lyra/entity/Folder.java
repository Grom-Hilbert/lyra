package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 文件夹实体类
 * 支持层级结构和文件管理
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Entity
@Table(
    name = "folders",
    indexes = {
        @Index(name = "idx_folder_parent_id", columnList = "parent_id"),
        @Index(name = "idx_folder_space_id", columnList = "space_id"),
        @Index(name = "idx_folder_path", columnList = "path")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_folder_space_path", columnNames = {"space_id", "path"})
    }
)
public class Folder extends BaseEntity {

    /**
     * 文件夹ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    @Size(min = 1, max = 255, message = "文件夹名称长度必须在1-255个字符之间")
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * 文件夹路径
     */
    @NotBlank(message = "文件夹路径不能为空")
    @Size(max = 1000, message = "文件夹路径长度不能超过1000个字符")
    @Column(name = "path", length = 1000, nullable = false)
    private String path;

    /**
     * 父文件夹
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    /**
     * 所属空间
     */
    @NotNull(message = "所属空间不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    /**
     * 文件夹层级
     */
    @NotNull(message = "文件夹层级不能为空")
    @Min(value = 0, message = "文件夹层级不能为负数")
    @Column(name = "level", nullable = false)
    private Integer level = 0;

    /**
     * 是否为根文件夹
     */
    @NotNull(message = "是否为根文件夹不能为空")
    @Column(name = "is_root", nullable = false)
    private Boolean isRoot = false;

    /**
     * 文件夹大小（字节）
     */
    @NotNull(message = "文件夹大小不能为空")
    @Min(value = 0, message = "文件夹大小不能为负数")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes = 0L;

    /**
     * 文件数量
     */
    @NotNull(message = "文件数量不能为空")
    @Min(value = 0, message = "文件数量不能为负数")
    @Column(name = "file_count", nullable = false)
    private Integer fileCount = 0;

    /**
     * 子文件夹集合
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Folder> children = new HashSet<>();

    /**
     * 文件夹下的文件集合
     */
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileEntity> files = new HashSet<>();

    /**
     * 默认构造函数
     */
    public Folder() {
    }

    /**
     * 构造函数
     * 
     * @param name 文件夹名称
     * @param path 文件夹路径
     * @param space 所属空间
     */
    public Folder(String name, String path, Space space) {
        this.name = name;
        this.path = path;
        this.space = space;
    }

    /**
     * 构造函数
     * 
     * @param name 文件夹名称
     * @param path 文件夹路径
     * @param parent 父文件夹
     * @param space 所属空间
     */
    public Folder(String name, String path, Folder parent, Space space) {
        this.name = name;
        this.path = path;
        this.parent = parent;
        this.space = space;
        this.level = parent != null ? parent.getLevel() + 1 : 0;
    }

    // ========== Getter 和 Setter 方法 ==========

    /**
     * 获取文件夹ID
     * 
     * @return 文件夹ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置文件夹ID
     * 
     * @param id 文件夹ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取文件夹名称
     * 
     * @return 文件夹名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置文件夹名称
     * 
     * @param name 文件夹名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取文件夹路径
     * 
     * @return 文件夹路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置文件夹路径
     * 
     * @param path 文件夹路径
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取父文件夹
     * 
     * @return 父文件夹
     */
    public Folder getParent() {
        return parent;
    }

    /**
     * 设置父文件夹
     * 
     * @param parent 父文件夹
     */
    public void setParent(Folder parent) {
        this.parent = parent;
        this.level = parent != null ? parent.getLevel() + 1 : 0;
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
     * 获取文件夹层级
     * 
     * @return 文件夹层级
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * 设置文件夹层级
     * 
     * @param level 文件夹层级
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * 获取是否为根文件夹
     * 
     * @return 是否为根文件夹
     */
    public Boolean getIsRoot() {
        return isRoot;
    }

    /**
     * 设置是否为根文件夹
     * 
     * @param isRoot 是否为根文件夹
     */
    public void setIsRoot(Boolean isRoot) {
        this.isRoot = isRoot;
    }

    /**
     * 获取文件夹大小
     * 
     * @return 文件夹大小（字节）
     */
    public Long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * 设置文件夹大小
     * 
     * @param sizeBytes 文件夹大小（字节）
     */
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    /**
     * 获取文件数量
     * 
     * @return 文件数量
     */
    public Integer getFileCount() {
        return fileCount;
    }

    /**
     * 设置文件数量
     * 
     * @param fileCount 文件数量
     */
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * 获取子文件夹集合
     * 
     * @return 子文件夹集合
     */
    public Set<Folder> getChildren() {
        return children;
    }

    /**
     * 设置子文件夹集合
     * 
     * @param children 子文件夹集合
     */
    public void setChildren(Set<Folder> children) {
        this.children = children;
    }

    /**
     * 获取文件夹下的文件集合
     * 
     * @return 文件集合
     */
    public Set<FileEntity> getFiles() {
        return files;
    }

    /**
     * 设置文件夹下的文件集合
     * 
     * @param files 文件集合
     */
    public void setFiles(Set<FileEntity> files) {
        this.files = files;
    }

    // ========== 业务方法 ==========

    /**
     * 添加子文件夹
     * 
     * @param child 子文件夹
     */
    public void addChild(Folder child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * 移除子文件夹
     * 
     * @param child 子文件夹
     */
    public void removeChild(Folder child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * 添加文件
     * 
     * @param file 文件
     */
    public void addFile(FileEntity file) {
        files.add(file);
        file.setFolder(this);
        updateStatistics();
    }

    /**
     * 移除文件
     * 
     * @param file 文件
     */
    public void removeFile(FileEntity file) {
        files.remove(file);
        file.setFolder(null);
        updateStatistics();
    }

    /**
     * 更新统计信息（大小和文件数量）
     */
    public void updateStatistics() {
        long totalSize = files.stream()
                .mapToLong(FileEntity::getSizeBytes)
                .sum();
        this.sizeBytes = totalSize;
        this.fileCount = files.size();
    }

    /**
     * 检查是否为根文件夹
     * 
     * @return 是否为根文件夹
     */
    public boolean isRootFolder() {
        return Boolean.TRUE.equals(this.isRoot);
    }

    /**
     * 检查是否有父文件夹
     * 
     * @return 是否有父文件夹
     */
    public boolean hasParent() {
        return this.parent != null;
    }

    /**
     * 检查是否有子文件夹
     * 
     * @return 是否有子文件夹
     */
    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    /**
     * 检查是否有文件
     * 
     * @return 是否有文件
     */
    public boolean hasFiles() {
        return !this.files.isEmpty();
    }

    /**
     * 获取文件夹的完整路径
     * 
     * @return 完整路径
     */
    public String getFullPath() {
        if (parent == null || isRootFolder()) {
            return "/" + name;
        }
        return parent.getFullPath() + "/" + name;
    }

    // ========== 重写方法 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Folder)) return false;
        Folder folder = (Folder) o;
        return id != null && id.equals(folder.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", level=" + level +
                ", sizeBytes=" + sizeBytes +
                ", fileCount=" + fileCount +
                ", isRoot=" + isRoot +
                '}';
    }
} 