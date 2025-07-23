package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 编辑器请求DTO
 * 用于接收在线编辑相关的请求参数
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public class EditorRequest {

    /**
     * 开始编辑请求
     */
    public static class StartEditRequest {
        @NotNull(message = "文件ID不能为空")
        @Min(value = 1, message = "文件ID必须大于0")
        private Long fileId;

        public StartEditRequest() {}

        public StartEditRequest(Long fileId) {
            this.fileId = fileId;
        }

        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
    }

    /**
     * 更新内容请求
     */
    public static class UpdateContentRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        @NotNull(message = "内容不能为null")
        private String content;

        private Boolean autoSave = false;

        public UpdateContentRequest() {}

        public UpdateContentRequest(String sessionId, String content, Boolean autoSave) {
            this.sessionId = sessionId;
            this.content = content;
            this.autoSave = autoSave;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Boolean getAutoSave() { return autoSave != null ? autoSave : false; }
        public void setAutoSave(Boolean autoSave) { this.autoSave = autoSave; }
    }

    /**
     * 保存内容请求
     */
    public static class SaveContentRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        @Size(max = 200, message = "保存备注不能超过200个字符")
        private String saveComment;

        public SaveContentRequest() {}

        public SaveContentRequest(String sessionId, String saveComment) {
            this.sessionId = sessionId;
            this.saveComment = saveComment;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSaveComment() { return saveComment; }
        public void setSaveComment(String saveComment) { this.saveComment = saveComment; }
    }

    /**
     * 关闭会话请求
     */
    public static class CloseSessionRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        private Boolean forceClose = false;

        public CloseSessionRequest() {}

        public CloseSessionRequest(String sessionId, Boolean forceClose) {
            this.sessionId = sessionId;
            this.forceClose = forceClose;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Boolean getForceClose() { return forceClose != null ? forceClose : false; }
        public void setForceClose(Boolean forceClose) { this.forceClose = forceClose; }
    }

    /**
     * 版本比较请求
     */
    public static class CompareVersionsRequest {
        @NotBlank(message = "旧版本历史ID不能为空")
        private String oldHistoryId;

        @NotBlank(message = "新版本历史ID不能为空")
        private String newHistoryId;

        public CompareVersionsRequest() {}

        public CompareVersionsRequest(String oldHistoryId, String newHistoryId) {
            this.oldHistoryId = oldHistoryId;
            this.newHistoryId = newHistoryId;
        }

        public String getOldHistoryId() { return oldHistoryId; }
        public void setOldHistoryId(String oldHistoryId) { this.oldHistoryId = oldHistoryId; }
        public String getNewHistoryId() { return newHistoryId; }
        public void setNewHistoryId(String newHistoryId) { this.newHistoryId = newHistoryId; }
    }

    /**
     * 恢复版本请求
     */
    public static class RestoreVersionRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        @NotBlank(message = "历史记录ID不能为空")
        private String historyId;

        public RestoreVersionRequest() {}

        public RestoreVersionRequest(String sessionId, String historyId) {
            this.sessionId = sessionId;
            this.historyId = historyId;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }
    }

    /**
     * 检测语言请求
     */
    public static class DetectLanguageRequest {
        @NotBlank(message = "文件名不能为空")
        private String filename;

        private String content;

        public DetectLanguageRequest() {}

        public DetectLanguageRequest(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * 锁定文件请求
     */
    public static class LockFileRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        public LockFileRequest() {}

        public LockFileRequest(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    /**
     * 解锁文件请求
     */
    public static class UnlockFileRequest {
        @NotBlank(message = "会话ID不能为空")
        private String sessionId;

        public UnlockFileRequest() {}

        public UnlockFileRequest(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
} 