package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;

/**
 * 版本控制服务接口
 * 定义版本控制的核心业务逻辑
 */
public interface VersionControlService {

    /**
     * 初始化文件的版本控制
     */
    void initializeVersionControl(Long fileId, FileEntity.VersionControlType type);

    /**
     * 提交文件更改
     */
    FileVersion commitChanges(Long fileId, String commitMessage, User user);

    /**
     * 获取文件的版本历史
     */
    List<FileVersion> getVersionHistory(Long fileId);

    /**
     * 比较两个版本的差异
     */
    VersionDiff compareVersions(Long fileId, Integer version1, Integer version2);

    /**
     * 恢复到指定版本
     */
    FileEntity revertToVersion(Long fileId, Integer versionNumber, User user);

    /**
     * 创建分支（高级版本控制）
     */
    String createBranch(Long fileId, String branchName, User user);

    /**
     * 合并分支（高级版本控制）
     */
    FileVersion mergeBranch(Long fileId, String sourceBranch, String targetBranch, User user);

    /**
     * 获取文件的分支列表
     */
    List<String> getBranches(Long fileId);

    /**
     * 切换分支
     */
    void switchBranch(Long fileId, String branchName, User user);

    /**
     * 获取当前分支
     */
    String getCurrentBranch(Long fileId);

    /**
     * 标记版本（创建标签）
     */
    void tagVersion(Long fileId, Integer versionNumber, String tagName, String description, User user);

    /**
     * 获取版本标签列表
     */
    List<VersionTag> getVersionTags(Long fileId);

    /**
     * 检查文件是否有未提交的更改
     */
    boolean hasUncommittedChanges(Long fileId);

    /**
     * 获取文件的提交日志
     */
    List<CommitLog> getCommitLog(Long fileId, int limit);

    /**
     * 同步到远程仓库（高级版本控制）
     */
    void syncToRemote(Long fileId, String remoteUrl, User user);

    /**
     * 从远程仓库拉取更新
     */
    void pullFromRemote(Long fileId, User user);

    /**
     * 推送到远程仓库
     */
    void pushToRemote(Long fileId, User user);

    /**
     * 版本差异类
     */
    class VersionDiff {
        private String oldContent;
        private String newContent;
        private List<DiffLine> differences;

        public VersionDiff(String oldContent, String newContent, List<DiffLine> differences) {
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.differences = differences;
        }

        public String getOldContent() { return oldContent; }
        public String getNewContent() { return newContent; }
        public List<DiffLine> getDifferences() { return differences; }
    }

    /**
     * 差异行类
     */
    class DiffLine {
        private int lineNumber;
        private String content;
        private DiffType type;

        public DiffLine(int lineNumber, String content, DiffType type) {
            this.lineNumber = lineNumber;
            this.content = content;
            this.type = type;
        }

        public int getLineNumber() { return lineNumber; }
        public String getContent() { return content; }
        public DiffType getType() { return type; }

        public enum DiffType {
            ADDED, REMOVED, MODIFIED, UNCHANGED
        }
    }

    /**
     * 版本标签类
     */
    class VersionTag {
        private String name;
        private String description;
        private Integer versionNumber;
        private User createdBy;

        public VersionTag(String name, String description, Integer versionNumber, User createdBy) {
            this.name = name;
            this.description = description;
            this.versionNumber = versionNumber;
            this.createdBy = createdBy;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Integer getVersionNumber() { return versionNumber; }
        public User getCreatedBy() { return createdBy; }
    }

    /**
     * 提交日志类
     */
    class CommitLog {
        private String commitHash;
        private String message;
        private User author;
        private java.time.LocalDateTime timestamp;

        public CommitLog(String commitHash, String message, User author, java.time.LocalDateTime timestamp) {
            this.commitHash = commitHash;
            this.message = message;
            this.author = author;
            this.timestamp = timestamp;
        }

        public String getCommitHash() { return commitHash; }
        public String getMessage() { return message; }
        public User getAuthor() { return author; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }
}