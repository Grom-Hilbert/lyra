package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 在线编辑服务接口
 * 提供文本文件在线编辑功能，包括实时保存、编辑历史、语法高亮等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public interface EditorService {

    /**
     * 编辑会话状态枚举
     */
    enum SessionStatus {
        /** 活跃状态 - 正在编辑 */
        ACTIVE,
        /** 空闲状态 - 暂时停止编辑 */
        IDLE,
        /** 已保存 - 编辑完成并保存 */
        SAVED,
        /** 已过期 - 会话超时 */
        EXPIRED,
        /** 已关闭 - 主动关闭会话 */
        CLOSED
    }

    /**
     * 编辑会话信息
     */
    class EditSession {
        private final String sessionId;
        private final Long fileId;
        private final Long userId;
        private final LocalDateTime startTime;
        private LocalDateTime lastActivityTime;
        private SessionStatus status;
        private String currentContent;
        private boolean hasUnsavedChanges;
        private int version;

        public EditSession(String sessionId, Long fileId, Long userId, String initialContent) {
            this.sessionId = sessionId;
            this.fileId = fileId;
            this.userId = userId;
            this.startTime = LocalDateTime.now();
            this.lastActivityTime = LocalDateTime.now();
            this.status = SessionStatus.ACTIVE;
            this.currentContent = initialContent;
            this.hasUnsavedChanges = false;
            this.version = 1;
        }

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public Long getFileId() { return fileId; }
        public Long getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }
        public SessionStatus getStatus() { return status; }
        public void setStatus(SessionStatus status) { this.status = status; }
        public String getCurrentContent() { return currentContent; }
        public void setCurrentContent(String currentContent) { this.currentContent = currentContent; }
        public boolean hasUnsavedChanges() { return hasUnsavedChanges; }
        public void setHasUnsavedChanges(boolean hasUnsavedChanges) { this.hasUnsavedChanges = hasUnsavedChanges; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }

    /**
     * 编辑历史记录
     */
    class EditHistory {
        private final String historyId;
        private final String sessionId;
        private final String content;
        private final LocalDateTime timestamp;
        private final String changeDescription;
        private final int version;

        public EditHistory(String historyId, String sessionId, String content, 
                          String changeDescription, int version) {
            this.historyId = historyId;
            this.sessionId = sessionId;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.changeDescription = changeDescription;
            this.version = version;
        }

        // Getters
        public String getHistoryId() { return historyId; }
        public String getSessionId() { return sessionId; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getChangeDescription() { return changeDescription; }
        public int getVersion() { return version; }
    }

    /**
     * 编辑操作结果
     */
    class EditResult {
        private final boolean success;
        private final String message;
        private final Object data;
        private final Map<String, Object> metadata;

        public EditResult(boolean success, String message, Object data, Map<String, Object> metadata) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.metadata = metadata;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * 版本对比结果
     */
    class DiffResult {
        private final String oldContent;
        private final String newContent;
        private final List<DiffLine> differences;
        private final DiffStatistics statistics;

        public DiffResult(String oldContent, String newContent, List<DiffLine> differences, DiffStatistics statistics) {
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.differences = differences;
            this.statistics = statistics;
        }

        public String getOldContent() { return oldContent; }
        public String getNewContent() { return newContent; }
        public List<DiffLine> getDifferences() { return differences; }
        public DiffStatistics getStatistics() { return statistics; }
    }

    /**
     * 差异行
     */
    class DiffLine {
        private final int lineNumber;
        private final String content;
        private final DiffType type;

        public DiffLine(int lineNumber, String content, DiffType type) {
            this.lineNumber = lineNumber;
            this.content = content;
            this.type = type;
        }

        public int getLineNumber() { return lineNumber; }
        public String getContent() { return content; }
        public DiffType getType() { return type; }
    }

    /**
     * 差异类型
     */
    enum DiffType {
        /** 新增行 */
        ADDED,
        /** 删除行 */
        DELETED,
        /** 修改行 */
        MODIFIED,
        /** 未变化 */
        UNCHANGED
    }

    /**
     * 差异统计
     */
    class DiffStatistics {
        private final int addedLines;
        private final int deletedLines;
        private final int modifiedLines;
        private final int totalChanges;

        public DiffStatistics(int addedLines, int deletedLines, int modifiedLines) {
            this.addedLines = addedLines;
            this.deletedLines = deletedLines;
            this.modifiedLines = modifiedLines;
            this.totalChanges = addedLines + deletedLines + modifiedLines;
        }

        public int getAddedLines() { return addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public int getModifiedLines() { return modifiedLines; }
        public int getTotalChanges() { return totalChanges; }
    }

    // ==================== 编辑会话管理 ====================

    /**
     * 开始编辑会话
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 编辑会话信息
     */
    EditResult startEditSession(Long fileId, Long userId);

    /**
     * 获取编辑会话
     * 
     * @param sessionId 会话ID
     * @return 编辑会话信息
     */
    Optional<EditSession> getEditSession(String sessionId);

    /**
     * 更新编辑内容
     * 
     * @param sessionId 会话ID
     * @param content 新内容
     * @param autoSave 是否自动保存
     * @return 操作结果
     */
    EditResult updateContent(String sessionId, String content, boolean autoSave);

    /**
     * 保存编辑内容
     * 
     * @param sessionId 会话ID
     * @param saveComment 保存备注
     * @return 操作结果
     */
    EditResult saveContent(String sessionId, String saveComment);

    /**
     * 关闭编辑会话
     * 
     * @param sessionId 会话ID
     * @param forceClose 是否强制关闭（忽略未保存更改）
     * @return 操作结果
     */
    EditResult closeEditSession(String sessionId, boolean forceClose);

    /**
     * 获取用户的活跃编辑会话
     * 
     * @param userId 用户ID
     * @return 编辑会话列表
     */
    List<EditSession> getUserActiveSessions(Long userId);

    /**
     * 清理过期会话
     * 
     * @param timeoutMinutes 超时时间（分钟）
     * @return 清理的会话数量
     */
    int cleanupExpiredSessions(int timeoutMinutes);

    // ==================== 编辑历史管理 ====================

    /**
     * 获取编辑历史
     * 
     * @param sessionId 会话ID
     * @param limit 历史记录数量限制
     * @return 编辑历史列表
     */
    List<EditHistory> getEditHistory(String sessionId, int limit);

    /**
     * 获取文件的编辑历史
     * 
     * @param fileId 文件ID
     * @param limit 历史记录数量限制
     * @return 编辑历史列表
     */
    List<EditHistory> getFileEditHistory(Long fileId, int limit);

    /**
     * 比较两个版本的内容
     * 
     * @param oldHistoryId 旧版本历史ID
     * @param newHistoryId 新版本历史ID
     * @return 版本对比结果
     */
    Optional<DiffResult> compareVersions(String oldHistoryId, String newHistoryId);

    /**
     * 恢复到指定版本
     * 
     * @param sessionId 会话ID
     * @param historyId 历史记录ID
     * @return 操作结果
     */
    EditResult restoreVersion(String sessionId, String historyId);

    // ==================== 语法高亮支持 ====================

    /**
     * 获取支持的语法高亮语言
     * 
     * @return 支持的语言集合
     */
    Set<String> getSupportedLanguages();

    /**
     * 检测文件的编程语言
     * 
     * @param filename 文件名
     * @param content 文件内容（可选）
     * @return 检测到的语言
     */
    Optional<String> detectLanguage(String filename, String content);

    /**
     * 检查文件是否支持在线编辑
     * 
     * @param filename 文件名
     * @param mimeType MIME类型
     * @return 是否支持编辑
     */
    boolean isEditSupported(String filename, String mimeType);

    /**
     * 获取支持编辑的文件扩展名
     * 
     * @return 支持的扩展名集合
     */
    Set<String> getSupportedEditExtensions();

    // ==================== 文件锁定管理 ====================

    /**
     * 检查文件是否被其他用户锁定
     * 
     * @param fileId 文件ID
     * @param userId 当前用户ID
     * @return 是否被锁定
     */
    boolean isFileLocked(Long fileId, Long userId);

    /**
     * 锁定文件
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 锁定结果
     */
    EditResult lockFile(Long fileId, Long userId, String sessionId);

    /**
     * 解锁文件
     * 
     * @param fileId 文件ID
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 解锁结果
     */
    EditResult unlockFile(Long fileId, Long userId, String sessionId);

    /**
     * 获取文件锁定信息
     * 
     * @param fileId 文件ID
     * @return 锁定信息
     */
    Optional<FileLockInfo> getFileLockInfo(Long fileId);

    /**
     * 文件锁定信息
     */
    class FileLockInfo {
        private final Long fileId;
        private final Long userId;
        private final String sessionId;
        private final LocalDateTime lockTime;
        private final String userName;

        public FileLockInfo(Long fileId, Long userId, String sessionId, LocalDateTime lockTime, String userName) {
            this.fileId = fileId;
            this.userId = userId;
            this.sessionId = sessionId;
            this.lockTime = lockTime;
            this.userName = userName;
        }

        public Long getFileId() { return fileId; }
        public Long getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public LocalDateTime getLockTime() { return lockTime; }
        public String getUserName() { return userName; }
    }
} 