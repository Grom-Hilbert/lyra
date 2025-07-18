package tslc.beihaiyun.lyra.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础控制器类
 * 提供通用的响应格式和工具方法
 */
public abstract class BaseController {

    /**
     * 成功响应
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 成功响应（无数据）
     */
    protected ResponseEntity<ApiResponse<Void>> success() {
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 成功响应（带消息）
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * 错误响应
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * 错误响应（指定状态码）
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    /**
     * 未找到响应
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(message));
    }

    /**
     * 未授权响应
     */
    protected <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(message));
    }

    /**
     * 禁止访问响应
     */
    protected <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(message));
    }

    /**
     * 分页响应
     */
    protected <T> ResponseEntity<ApiResponse<PageResponse<T>>> page(org.springframework.data.domain.Page<T> page) {
        PageResponse<T> pageResponse = new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
        return success(pageResponse);
    }

    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private String error;
        private LocalDateTime timestamp;

        private ApiResponse(boolean success, T data, String message, String error) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.error = error;
            this.timestamp = LocalDateTime.now();
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, data, null, null);
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, data, message, null);
        }

        public static ApiResponse<Void> success() {
            return new ApiResponse<>(true, null, null, null);
        }

        public static <T> ApiResponse<T> error(String error) {
            return new ApiResponse<>(false, null, null, error);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getMessage() { return message; }
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * 分页响应类
     */
    public static class PageResponse<T> {
        private java.util.List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public PageResponse(java.util.List<T> content, int page, int size, long totalElements, int totalPages) {
            this.content = content;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        // Getters
        public java.util.List<T> getContent() { return content; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
    }
}