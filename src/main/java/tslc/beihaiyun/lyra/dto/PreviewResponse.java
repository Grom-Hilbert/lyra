package tslc.beihaiyun.lyra.dto;

import tslc.beihaiyun.lyra.service.PreviewService;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件预览响应DTO
 * 用于返回文件预览的结果数据
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
public class PreviewResponse {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 预览类型
     */
    private PreviewService.PreviewType type;

    /**
     * 预览数据
     */
    private Map<String, Object> data;

    /**
     * 文件元数据
     */
    private Map<String, Object> metadata;

    /**
     * 预览生成时间
     */
    private LocalDateTime timestamp;

    // 默认构造函数
    public PreviewResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 构造函数
    public PreviewResponse(boolean success, String message, PreviewService.PreviewType type,
                          Map<String, Object> data, Map<String, Object> metadata) {
        this.success = success;
        this.message = message;
        this.type = type;
        this.data = data;
        this.metadata = metadata;
        this.timestamp = LocalDateTime.now();
    }

    // 从PreviewResult创建响应
    public static PreviewResponse fromPreviewResult(PreviewService.PreviewResult result) {
        return new PreviewResponse(
            result.isSuccess(),
            result.getMessage(),
            result.getType(),
            result.getData(),
            result.getMetadata()
        );
    }

    // 创建成功响应
    public static PreviewResponse success(PreviewService.PreviewType type, String message,
                                        Map<String, Object> data, Map<String, Object> metadata) {
        return new PreviewResponse(true, message, type, data, metadata);
    }

    // 创建失败响应
    public static PreviewResponse error(String message) {
        return new PreviewResponse(false, message, PreviewService.PreviewType.UNSUPPORTED, null, null);
    }

    // 创建失败响应（指定类型）
    public static PreviewResponse error(PreviewService.PreviewType type, String message) {
        return new PreviewResponse(false, message, type, null, null);
    }

    // Getter和Setter方法
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PreviewService.PreviewType getType() {
        return type;
    }

    public void setType(PreviewService.PreviewType type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PreviewResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", type=" + type +
                ", data=" + data +
                ", metadata=" + metadata +
                ", timestamp=" + timestamp +
                '}';
    }
} 