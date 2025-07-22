package tslc.beihaiyun.lyra.dto;

import tslc.beihaiyun.lyra.service.EditorService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编辑器响应DTO
 * 用于返回在线编辑相关的响应数据
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public class EditorResponse {

    /**
     * 基础编辑响应
     */
    public static class BaseEditorResponse {
        private boolean success;
        private String message;
        private Object data;
        private Map<String, Object> metadata;
        private LocalDateTime timestamp;

        public BaseEditorResponse() {
            this.timestamp = LocalDateTime.now();
        }

        public BaseEditorResponse(boolean success, String message, Object data, Map<String, Object> metadata) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.metadata = metadata;
            this.timestamp = LocalDateTime.now();
        }

        // 从EditResult创建响应
        public static BaseEditorResponse fromEditResult(EditorService.EditResult result) {
            return new BaseEditorResponse(
                result.isSuccess(),
                result.getMessage(),
                result.getData(),
                result.getMetadata()
            );
        }

        // 创建成功响应
        public static BaseEditorResponse success(String message, Object data) {
            return new BaseEditorResponse(true, message, data, null);
        }

        // 创建失败响应
        public static BaseEditorResponse error(String message) {
            return new BaseEditorResponse(false, message, null, null);
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 编辑会话响应
     */
    public static class EditSessionResponse {
        private String sessionId;
        private Long fileId;
        private String filename;
        private String content;
        private String language;
        private EditorService.SessionStatus status;
        private boolean hasUnsavedChanges;
        private int version;
        private LocalDateTime startTime;
        private LocalDateTime lastActivityTime;

        public EditSessionResponse() {}

        // 从EditSession创建响应
        public static EditSessionResponse fromEditSession(EditorService.EditSession session, String filename, String language) {
            EditSessionResponse response = new EditSessionResponse();
            response.setSessionId(session.getSessionId());
            response.setFileId(session.getFileId());
            response.setFilename(filename);
            response.setContent(session.getCurrentContent());
            response.setLanguage(language);
            response.setStatus(session.getStatus());
            response.setHasUnsavedChanges(session.hasUnsavedChanges());
            response.setVersion(session.getVersion());
            response.setStartTime(session.getStartTime());
            response.setLastActivityTime(session.getLastActivityTime());
            return response;
        }

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public EditorService.SessionStatus getStatus() { return status; }
        public void setStatus(EditorService.SessionStatus status) { this.status = status; }
        public boolean isHasUnsavedChanges() { return hasUnsavedChanges; }
        public void setHasUnsavedChanges(boolean hasUnsavedChanges) { this.hasUnsavedChanges = hasUnsavedChanges; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }
    }

    /**
     * 编辑历史响应
     */
    public static class EditHistoryResponse {
        private String historyId;
        private String sessionId;
        private String changeDescription;
        private int version;
        private LocalDateTime timestamp;
        private int contentLength;

        public EditHistoryResponse() {}

        // 从EditHistory创建响应
        public static EditHistoryResponse fromEditHistory(EditorService.EditHistory history) {
            EditHistoryResponse response = new EditHistoryResponse();
            response.setHistoryId(history.getHistoryId());
            response.setSessionId(history.getSessionId());
            response.setChangeDescription(history.getChangeDescription());
            response.setVersion(history.getVersion());
            response.setTimestamp(history.getTimestamp());
            response.setContentLength(history.getContent() != null ? history.getContent().length() : 0);
            return response;
        }

        // Getters and Setters
        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getChangeDescription() { return changeDescription; }
        public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public int getContentLength() { return contentLength; }
        public void setContentLength(int contentLength) { this.contentLength = contentLength; }
    }

    /**
     * 版本比较响应
     */
    public static class DiffResponse {
        private List<DiffLineResponse> differences;
        private DiffStatisticsResponse statistics;

        public DiffResponse() {}

        // 从DiffResult创建响应
        public static DiffResponse fromDiffResult(EditorService.DiffResult diffResult) {
            DiffResponse response = new DiffResponse();
            
            List<DiffLineResponse> differences = diffResult.getDifferences().stream()
                .map(DiffLineResponse::fromDiffLine)
                .toList();
            response.setDifferences(differences);
            
            response.setStatistics(DiffStatisticsResponse.fromDiffStatistics(diffResult.getStatistics()));
            
            return response;
        }

        // Getters and Setters
        public List<DiffLineResponse> getDifferences() { return differences; }
        public void setDifferences(List<DiffLineResponse> differences) { this.differences = differences; }
        public DiffStatisticsResponse getStatistics() { return statistics; }
        public void setStatistics(DiffStatisticsResponse statistics) { this.statistics = statistics; }
    }

    /**
     * 差异行响应
     */
    public static class DiffLineResponse {
        private int lineNumber;
        private String content;
        private EditorService.DiffType type;

        public DiffLineResponse() {}

        public static DiffLineResponse fromDiffLine(EditorService.DiffLine diffLine) {
            DiffLineResponse response = new DiffLineResponse();
            response.setLineNumber(diffLine.getLineNumber());
            response.setContent(diffLine.getContent());
            response.setType(diffLine.getType());
            return response;
        }

        // Getters and Setters
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public EditorService.DiffType getType() { return type; }
        public void setType(EditorService.DiffType type) { this.type = type; }
    }

    /**
     * 差异统计响应
     */
    public static class DiffStatisticsResponse {
        private int addedLines;
        private int deletedLines;
        private int modifiedLines;
        private int totalChanges;

        public DiffStatisticsResponse() {}

        public static DiffStatisticsResponse fromDiffStatistics(EditorService.DiffStatistics statistics) {
            DiffStatisticsResponse response = new DiffStatisticsResponse();
            response.setAddedLines(statistics.getAddedLines());
            response.setDeletedLines(statistics.getDeletedLines());
            response.setModifiedLines(statistics.getModifiedLines());
            response.setTotalChanges(statistics.getTotalChanges());
            return response;
        }

        // Getters and Setters
        public int getAddedLines() { return addedLines; }
        public void setAddedLines(int addedLines) { this.addedLines = addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public void setDeletedLines(int deletedLines) { this.deletedLines = deletedLines; }
        public int getModifiedLines() { return modifiedLines; }
        public void setModifiedLines(int modifiedLines) { this.modifiedLines = modifiedLines; }
        public int getTotalChanges() { return totalChanges; }
        public void setTotalChanges(int totalChanges) { this.totalChanges = totalChanges; }
    }

    /**
     * 支持的语言响应
     */
    public static class SupportedLanguagesResponse {
        private Set<String> languages;
        private Set<String> editExtensions;

        public SupportedLanguagesResponse() {}

        public SupportedLanguagesResponse(Set<String> languages, Set<String> editExtensions) {
            this.languages = languages;
            this.editExtensions = editExtensions;
        }

        // Getters and Setters
        public Set<String> getLanguages() { return languages; }
        public void setLanguages(Set<String> languages) { this.languages = languages; }
        public Set<String> getEditExtensions() { return editExtensions; }
        public void setEditExtensions(Set<String> editExtensions) { this.editExtensions = editExtensions; }
    }

    /**
     * 文件锁定信息响应
     */
    public static class FileLockResponse {
        private Long fileId;
        private Long userId;
        private String userName;
        private String sessionId;
        private LocalDateTime lockTime;
        private boolean isLocked;

        public FileLockResponse() {}

        public static FileLockResponse fromFileLockInfo(EditorService.FileLockInfo lockInfo) {
            FileLockResponse response = new FileLockResponse();
            response.setFileId(lockInfo.getFileId());
            response.setUserId(lockInfo.getUserId());
            response.setUserName(lockInfo.getUserName());
            response.setSessionId(lockInfo.getSessionId());
            response.setLockTime(lockInfo.getLockTime());
            response.setLocked(true);
            return response;
        }

        public static FileLockResponse unlocked(Long fileId) {
            FileLockResponse response = new FileLockResponse();
            response.setFileId(fileId);
            response.setLocked(false);
            return response;
        }

        // Getters and Setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public LocalDateTime getLockTime() { return lockTime; }
        public void setLockTime(LocalDateTime lockTime) { this.lockTime = lockTime; }
        public boolean isLocked() { return isLocked; }
        public void setLocked(boolean locked) { isLocked = locked; }
    }
} 