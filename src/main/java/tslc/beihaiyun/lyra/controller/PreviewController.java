package tslc.beihaiyun.lyra.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tslc.beihaiyun.lyra.dto.PreviewRequest;
import tslc.beihaiyun.lyra.dto.PreviewResponse;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.PreviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * 文件预览控制器
 * 提供文件预览相关的REST API端点
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@RestController
@RequestMapping("/api/preview")
public class PreviewController {

    private static final Logger logger = LoggerFactory.getLogger(PreviewController.class);

    private final PreviewService previewService;

    @Autowired
    public PreviewController(PreviewService previewService) {
        this.previewService = previewService;
    }

    /**
     * 获取文件预览
     * 
     * @param fileId 文件ID
     * @param authentication 认证信息
     * @return 预览结果
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<PreviewResponse> getFilePreview(
            @PathVariable @NotNull @Min(1) Long fileId,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId(authentication);
            
            // 获取文件预览
            PreviewService.PreviewResult result = previewService.getFilePreview(fileId, userId);
            
            // 转换为响应DTO
            PreviewResponse response = PreviewResponse.fromPreviewResult(result);
            
            logger.info("文件预览请求: fileId={}, userId={}, success={}", fileId, userId, result.isSuccess());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("文件预览请求失败: fileId={}", fileId, e);
            PreviewResponse errorResponse = PreviewResponse.error("预览失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 批量获取文件预览信息（仅元数据）
     * 
     * @param request 预览请求
     * @param authentication 认证信息
     * @return 预览结果
     */
    @PostMapping("/batch")
    public ResponseEntity<PreviewResponse> getFilePreviewBatch(
            @RequestBody @Valid PreviewRequest request,
            Authentication authentication) {
        
        try {
            // 获取当前用户ID
            Long userId = getCurrentUserId(authentication);
            
            // 获取文件预览
            PreviewService.PreviewResult result = previewService.getFilePreview(request.getFileId(), userId);
            
            // 转换为响应DTO
            PreviewResponse response = PreviewResponse.fromPreviewResult(result);
            
            logger.info("批量文件预览请求: fileId={}, userId={}, success={}", 
                       request.getFileId(), userId, result.isSuccess());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("批量文件预览请求失败: request={}", request, e);
            PreviewResponse errorResponse = PreviewResponse.error("预览失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 检查文件是否支持预览
     * 
     * @param filename 文件名
     * @param mimeType MIME类型（可选）
     * @return 是否支持预览
     */
    @GetMapping("/check")
    public ResponseEntity<PreviewResponse> checkPreviewSupport(
            @RequestParam String filename,
            @RequestParam(required = false) String mimeType) {
        
        try {
            boolean supported = previewService.isPreviewSupported(filename, mimeType);
            PreviewService.PreviewType type = previewService.getPreviewType(filename, mimeType);
            
            String message = supported ? "文件支持预览" : "文件不支持预览";
            PreviewResponse response = supported 
                ? PreviewResponse.success(type, message, null, null)
                : PreviewResponse.error(type, message);
            
            logger.debug("预览支持检查: filename={}, mimeType={}, supported={}, type={}", 
                        filename, mimeType, supported, type);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("预览支持检查失败: filename={}, mimeType={}", filename, mimeType, e);
            PreviewResponse errorResponse = PreviewResponse.error("检查失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取支持的文件类型列表
     * 
     * @return 支持的文件类型
     */
    @GetMapping("/supported-types")
    public ResponseEntity<SupportedTypesResponse> getSupportedTypes() {
        
        try {
            SupportedTypesResponse response = new SupportedTypesResponse();
            response.setTextExtensions(previewService.getSupportedTextExtensions());
            response.setImageExtensions(previewService.getSupportedImageExtensions());
            response.setMediaExtensions(previewService.getSupportedMediaExtensions());
            
            logger.debug("获取支持的文件类型列表");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取支持的文件类型失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文件预览类型
     * 
     * @param filename 文件名
     * @param mimeType MIME类型（可选）
     * @return 预览类型
     */
    @GetMapping("/type")
    public ResponseEntity<PreviewTypeResponse> getPreviewType(
            @RequestParam String filename,
            @RequestParam(required = false) String mimeType) {
        
        try {
            PreviewService.PreviewType type = previewService.getPreviewType(filename, mimeType);
            
            PreviewTypeResponse response = new PreviewTypeResponse();
            response.setType(type);
            response.setSupported(type != PreviewService.PreviewType.UNSUPPORTED);
            
            logger.debug("获取预览类型: filename={}, mimeType={}, type={}", filename, mimeType, type);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取预览类型失败: filename={}, mimeType={}", filename, mimeType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof LyraUserPrincipal) {
            LyraUserPrincipal principal = (LyraUserPrincipal) authentication.getPrincipal();
            return principal.getId();
        }
        throw new RuntimeException("无法获取当前用户信息");
    }

    // ==================== 内部响应类 ====================

    /**
     * 支持的文件类型响应
     */
    public static class SupportedTypesResponse {
        private Set<String> textExtensions;
        private Set<String> imageExtensions;
        private Set<String> mediaExtensions;

        public Set<String> getTextExtensions() { return textExtensions; }
        public void setTextExtensions(Set<String> textExtensions) { this.textExtensions = textExtensions; }
        
        public Set<String> getImageExtensions() { return imageExtensions; }
        public void setImageExtensions(Set<String> imageExtensions) { this.imageExtensions = imageExtensions; }
        
        public Set<String> getMediaExtensions() { return mediaExtensions; }
        public void setMediaExtensions(Set<String> mediaExtensions) { this.mediaExtensions = mediaExtensions; }
    }

    /**
     * 预览类型响应
     */
    public static class PreviewTypeResponse {
        private PreviewService.PreviewType type;
        private boolean supported;

        public PreviewService.PreviewType getType() { return type; }
        public void setType(PreviewService.PreviewType type) { this.type = type; }
        
        public boolean isSupported() { return supported; }
        public void setSupported(boolean supported) { this.supported = supported; }
    }
} 