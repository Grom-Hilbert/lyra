package tslc.beihaiyun.lyra.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * 统一处理应用程序中的异常并返回一致的错误响应格式
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-22
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证失败异常
     * 
     * @param ex 验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "参数验证失败");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());
        
        log.warn("参数验证失败: {}", errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理绑定异常
     * 
     * @param ex 绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        List<String> errors = ex.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "数据绑定失败");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());
        
        log.warn("数据绑定失败: {}", errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理方法参数类型不匹配异常
     * 
     * @param ex 类型不匹配异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", String.format("参数类型错误: %s 应该是 %s 类型", 
            ex.getName(), ex.getRequiredType().getSimpleName()));
        response.put("timestamp", System.currentTimeMillis());
        
        log.warn("参数类型不匹配: {} = {}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理权限拒绝异常
     * 
     * @param ex 权限拒绝异常
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "权限不足: " + ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        
        log.warn("权限拒绝: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理非法参数异常
     * 
     * @param ex 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "参数错误: " + ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理资源未找到异常
     *
     * @param ex 资源未找到异常
     * @return 错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "请求的资源不存在");
        response.put("timestamp", System.currentTimeMillis());

        log.warn("资源未找到: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理运行时异常
     *
     * @param ex 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "操作失败: " + ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());

        log.error("运行时异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理通用异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统内部错误");
        response.put("timestamp", System.currentTimeMillis());
        
        log.error("未处理的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 