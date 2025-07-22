package tslc.beihaiyun.lyra.webdav;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

/**
 * Lyra WebDAV 资源
 * 
 * 统一的WebDAV资源抽象，支持文件和文件夹的统一表示
 * 提供WebDAV协议所需的所有属性和操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class LyraResource {

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        /** 文件夹/集合 */
        COLLECTION,
        /** 文件 */
        RESOURCE,
        /** 空间根目录 */
        SPACE_ROOT,
        /** 系统根目录 */
        SYSTEM_ROOT
    }

    /**
     * 空间类型枚举
     */
    public enum SpaceType {
        /** 个人空间 */
        PERSONAL,
        /** 企业空间 */
        ENTERPRISE,
        /** 系统根 */
        SYSTEM
    }

    // 基础属性
    private final String name;
    private final String path;
    private final String href;
    private final ResourceType resourceType;
    private final SpaceType spaceType;
    
    // 文件属性
    private final Long size;
    private final String contentType;
    private final Date lastModified;
    private final Date creationDate;
    private final String etag;
    
    // 实体引用
    private final FileEntity fileEntity;
    private final Folder folder;
    private final Space space;
    
    // 子资源
    private final List<LyraResource> children;
    
    // 内容提供者
    private final InputStream contentStream;
    
    // 版本控制属性
    private final Integer currentVersionNumber;
    private final Long totalVersionCount;
    private final String latestVersionComment;
    private final Date latestVersionDate;

    /**
     * 私有构造器，使用Builder模式创建
     */
    private LyraResource(Builder builder) {
        this.name = builder.name;
        this.path = builder.path;
        this.href = builder.href;
        this.resourceType = builder.resourceType;
        this.spaceType = builder.spaceType;
        this.size = builder.size;
        this.contentType = builder.contentType;
        this.lastModified = builder.lastModified;
        this.creationDate = builder.creationDate;
        this.etag = builder.etag;
        this.fileEntity = builder.fileEntity;
        this.folder = builder.folder;
        this.space = builder.space;
        this.children = new ArrayList<>(builder.children);
        this.contentStream = builder.contentStream;
        this.currentVersionNumber = builder.currentVersionNumber;
        this.totalVersionCount = builder.totalVersionCount;
        this.latestVersionComment = builder.latestVersionComment;
        this.latestVersionDate = builder.latestVersionDate;
    }

    // Getter 方法
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getHref() { return href; }
    public ResourceType getResourceType() { return resourceType; }
    public SpaceType getSpaceType() { return spaceType; }
    public Long getSize() { return size; }
    public String getContentType() { return contentType; }
    public Date getLastModified() { return lastModified; }
    public Date getCreationDate() { return creationDate; }
    public String getEtag() { return etag; }
    public FileEntity getFileEntity() { return fileEntity; }
    public Folder getFolder() { return folder; }
    public Space getSpace() { return space; }
    public List<LyraResource> getChildren() { return new ArrayList<>(children); }
    public InputStream getContentStream() { return contentStream; }
    
    // 版本控制属性的getter方法
    public Integer getCurrentVersionNumber() { return currentVersionNumber; }
    public Long getTotalVersionCount() { return totalVersionCount; }
    public String getLatestVersionComment() { return latestVersionComment; }
    public Date getLatestVersionDate() { return latestVersionDate; }

    /**
     * 判断是否为集合（文件夹）
     */
    public boolean isCollection() {
        return resourceType == ResourceType.COLLECTION || 
               resourceType == ResourceType.SPACE_ROOT || 
               resourceType == ResourceType.SYSTEM_ROOT;
    }

    /**
     * 判断是否为文件
     */
    public boolean isResource() {
        return resourceType == ResourceType.RESOURCE;
    }

    /**
     * 判断是否为空间根目录
     */
    public boolean isSpaceRoot() {
        return resourceType == ResourceType.SPACE_ROOT;
    }

    /**
     * 判断是否为系统根目录
     */
    public boolean isSystemRoot() {
        return resourceType == ResourceType.SYSTEM_ROOT;
    }

    /**
     * 获取实际大小，如果是集合则返回0
     */
    public long getActualSize() {
        return isCollection() ? 0L : (size != null ? size : 0L);
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (isSystemRoot()) {
            return "Lyra WebDAV";
        } else if (isSpaceRoot()) {
            return spaceType == SpaceType.PERSONAL ? "个人空间" : "企业空间";
        }
        return name;
    }

    /**
     * 获取WebDAV资源类型XML
     */
    public String getResourceTypeXml() {
        if (isCollection()) {
            return "<D:collection/>";
        }
        return "";
    }

    /**
     * 获取内容类型，默认为application/octet-stream
     */
    public String getActualContentType() {
        if (isCollection()) {
            return "httpd/unix-directory";
        }
        return contentType != null ? contentType : "application/octet-stream";
    }

    /**
     * 创建文件资源Builder
     */
    public static Builder file() {
        return new Builder().resourceType(ResourceType.RESOURCE);
    }

    /**
     * 创建集合资源Builder
     */
    public static Builder collection() {
        return new Builder().resourceType(ResourceType.COLLECTION);
    }

    /**
     * 创建空间根目录Builder
     */
    public static Builder spaceRoot() {
        return new Builder().resourceType(ResourceType.SPACE_ROOT);
    }

    /**
     * 创建系统根目录Builder
     */
    public static Builder systemRoot() {
        return new Builder().resourceType(ResourceType.SYSTEM_ROOT);
    }

    /**
     * 从FileEntity创建LyraResource
     */
    public static LyraResource fromFileEntity(FileEntity fileEntity, String webdavPath) {
        return file()
                .name(fileEntity.getName())
                .path(webdavPath)
                .href(webdavPath)
                .spaceType(SpaceType.PERSONAL) // 根据文件所属空间类型确定
                .size(fileEntity.getSizeBytes())
                .contentType(fileEntity.getMimeType())
                .lastModified(toDate(fileEntity.getUpdatedAt()))
                .creationDate(toDate(fileEntity.getCreatedAt()))
                .etag(generateEtag(fileEntity))
                .fileEntity(fileEntity)
                .build();
    }

    /**
     * 从Folder创建LyraResource
     */
    public static LyraResource fromFolder(Folder folder, String webdavPath) {
        return collection()
                .name(folder.getName())
                .path(webdavPath)
                .href(webdavPath)
                .spaceType(SpaceType.PERSONAL) // 根据文件夹所属空间类型确定
                .lastModified(toDate(folder.getUpdatedAt()))
                .creationDate(toDate(folder.getCreatedAt()))
                .etag(generateEtag(folder))
                .folder(folder)
                .build();
    }

    /**
     * 从Space创建LyraResource
     */
    public static LyraResource fromSpace(Space space, String webdavPath) {
        SpaceType spaceType = space.getType() == Space.SpaceType.PERSONAL ? 
                             SpaceType.PERSONAL : SpaceType.ENTERPRISE;
        
        return spaceRoot()
                .name(space.getName())
                .path(webdavPath)
                .href(webdavPath)
                .spaceType(spaceType)
                .lastModified(toDate(space.getUpdatedAt()))
                .creationDate(toDate(space.getCreatedAt()))
                .etag(generateEtag(space))
                .space(space)
                .build();
    }

    /**
     * 创建系统根目录资源
     */
    public static LyraResource createSystemRoot() {
        return systemRoot()
                .name("")
                .path("/webdav")
                .href("/webdav")
                .spaceType(SpaceType.SYSTEM)
                .lastModified(new Date())
                .creationDate(new Date())
                .etag("system-root")
                .build();
    }

    // 辅助方法
    private static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return new Date();
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String generateEtag(Object entity) {
        if (entity == null) return "default";
        return "\"" + Math.abs(entity.hashCode()) + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LyraResource)) return false;
        LyraResource that = (LyraResource) o;
        return Objects.equals(path, that.path) &&
               Objects.equals(resourceType, that.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, resourceType);
    }

    @Override
    public String toString() {
        return String.format("LyraResource{name='%s', path='%s', type=%s, spaceType=%s, size=%d}",
                           name, path, resourceType, spaceType, getActualSize());
    }

    /**
     * 创建一个基于当前资源的Builder，用于修改属性
     */
    public Builder toBuilder() {
        return new Builder()
                .name(this.name)
                .path(this.path)
                .href(this.href)
                .resourceType(this.resourceType)
                .spaceType(this.spaceType)
                .size(this.size)
                .contentType(this.contentType)
                .lastModified(this.lastModified)
                .creationDate(this.creationDate)
                .etag(this.etag)
                .fileEntity(this.fileEntity)
                .folder(this.folder)
                .space(this.space)
                .children(this.children)
                .contentStream(this.contentStream)
                .currentVersionNumber(this.currentVersionNumber)
                .totalVersionCount(this.totalVersionCount)
                .latestVersionComment(this.latestVersionComment)
                .latestVersionDate(this.latestVersionDate);
    }

    /**
     * LyraResource Builder
     */
    public static class Builder {
        private String name;
        private String path;
        private String href;
        private ResourceType resourceType;
        private SpaceType spaceType = SpaceType.SYSTEM;
        private Long size;
        private String contentType;
        private Date lastModified = new Date();
        private Date creationDate = new Date();
        private String etag;
        private FileEntity fileEntity;
        private Folder folder;
        private Space space;
        private List<LyraResource> children = new ArrayList<>();
        private InputStream contentStream;
        private Integer currentVersionNumber;
        private Long totalVersionCount;
        private String latestVersionComment;
        private Date latestVersionDate;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder href(String href) {
            this.href = href;
            return this;
        }

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder spaceType(SpaceType spaceType) {
            this.spaceType = spaceType;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder lastModified(Date lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder creationDate(Date creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder etag(String etag) {
            this.etag = etag;
            return this;
        }

        public Builder fileEntity(FileEntity fileEntity) {
            this.fileEntity = fileEntity;
            return this;
        }

        public Builder folder(Folder folder) {
            this.folder = folder;
            return this;
        }

        public Builder space(Space space) {
            this.space = space;
            return this;
        }

        public Builder children(List<LyraResource> children) {
            this.children = new ArrayList<>(children);
            return this;
        }

        public Builder addChild(LyraResource child) {
            this.children.add(child);
            return this;
        }

        public Builder contentStream(InputStream contentStream) {
            this.contentStream = contentStream;
            return this;
        }

        public Builder currentVersionNumber(Integer currentVersionNumber) {
            this.currentVersionNumber = currentVersionNumber;
            return this;
        }

        public Builder totalVersionCount(Long totalVersionCount) {
            this.totalVersionCount = totalVersionCount;
            return this;
        }

        public Builder latestVersionComment(String latestVersionComment) {
            this.latestVersionComment = latestVersionComment;
            return this;
        }

        public Builder latestVersionDate(Date latestVersionDate) {
            this.latestVersionDate = latestVersionDate;
            return this;
        }

        public LyraResource build() {
            // 验证必要字段
            Objects.requireNonNull(resourceType, "resourceType cannot be null");
            Objects.requireNonNull(path, "path cannot be null");
            
            // 如果href为空，使用path
            if (href == null) {
                href = path;
            }
            
            // 如果etag为空，生成默认值
            if (etag == null) {
                etag = "\"" + Math.abs(Objects.hash(path, lastModified)) + "\"";
            }
            
            return new LyraResource(this);
        }
    }
} 