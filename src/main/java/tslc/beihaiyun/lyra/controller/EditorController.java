package tslc.beihaiyun.lyra.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tslc.beihaiyun.lyra.dto.EditorRequest;
import tslc.beihaiyun.lyra.dto.EditorResponse;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.EditorService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 在线编辑控制器
 * 提供文件在线编辑相关的REST API端点
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@RestController
@RequestMapping("/api/editor")
public class EditorController {

    private static final Logger logger = LoggerFactory.getLogger(EditorController.class);

    private final EditorService editorService;

    @Autowired
    public EditorController(EditorService editorService) {
        this.editorService = editorService;
    }

    /**
     * 开始编辑会话
     * 
     * @param request 开始编辑请求
     * @param authentication 认证信息
     * @return 编辑会话信息
     */
    @PostMapping("/start")
    public ResponseEntity<EditorResponse.BaseEditorResponse> startEditSession(
            @Valid @RequestBody EditorRequest.StartEditRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 开始编辑文件 {}", userId, request.getFileId());
            
            EditorService.EditResult result = editorService.startEditSession(request.getFileId(), userId);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("开始编辑会话失败: fileId={}", request.getFileId(), e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("开始编辑会话失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取编辑会话信息
     * 
     * @param sessionId 会话ID
     * @param authentication 认证信息
     * @return 会话信息
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<EditorResponse.EditSessionResponse> getEditSession(
            @PathVariable @NotBlank String sessionId,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.debug("用户 {} 获取编辑会话 {}", userId, sessionId);
            
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            EditorService.EditSession session = sessionOpt.get();
            
            // 验证会话所有者
            if (!session.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
            
            // TODO: 获取文件名和语言，这里简化处理
            String filename = "file.txt";
            String language = editorService.detectLanguage(filename, session.getCurrentContent()).orElse("text");
            
            EditorResponse.EditSessionResponse response = EditorResponse.EditSessionResponse.fromEditSession(session, filename, language);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取编辑会话失败: sessionId={}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新编辑内容
     * 
     * @param sessionId 会话ID
     * @param request 更新内容请求
     * @param authentication 认证信息
     * @return 更新结果
     */
    @PutMapping("/session/{sessionId}/content")
    public ResponseEntity<EditorResponse.BaseEditorResponse> updateContent(
            @PathVariable @NotBlank String sessionId,
            @Valid @RequestBody EditorRequest.UpdateContentRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.debug("用户 {} 更新编辑内容: sessionId={}", userId, sessionId);
            
            // 验证会话所有者
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("无权访问该编辑会话");
                return ResponseEntity.status(403).body(response);
            }
            
            EditorService.EditResult result = editorService.updateContent(
                sessionId, 
                request.getContent(), 
                request.getAutoSave()
            );
            
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新编辑内容失败: sessionId={}", sessionId, e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("更新编辑内容失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 保存编辑内容
     * 
     * @param sessionId 会话ID
     * @param request 保存请求
     * @param authentication 认证信息
     * @return 保存结果
     */
    @PostMapping("/session/{sessionId}/save")
    public ResponseEntity<EditorResponse.BaseEditorResponse> saveContent(
            @PathVariable @NotBlank String sessionId,
            @Valid @RequestBody EditorRequest.SaveContentRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 保存编辑内容: sessionId={}", userId, sessionId);
            
            // 验证会话所有者
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("无权访问该编辑会话");
                return ResponseEntity.status(403).body(response);
            }
            
            EditorService.EditResult result = editorService.saveContent(sessionId, request.getSaveComment());
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存编辑内容失败: sessionId={}", sessionId, e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("保存编辑内容失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 关闭编辑会话
     * 
     * @param sessionId 会话ID
     * @param request 关闭会话请求
     * @param authentication 认证信息
     * @return 关闭结果
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<EditorResponse.BaseEditorResponse> closeEditSession(
            @PathVariable @NotBlank String sessionId,
            @Valid @RequestBody EditorRequest.CloseSessionRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 关闭编辑会话: sessionId={}", userId, sessionId);
            
            // 验证会话所有者
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("无权访问该编辑会话");
                return ResponseEntity.status(403).body(response);
            }
            
            EditorService.EditResult result = editorService.closeEditSession(sessionId, request.getForceClose());
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("关闭编辑会话失败: sessionId={}", sessionId, e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("关闭编辑会话失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取编辑历史
     * 
     * @param sessionId 会话ID
     * @param limit 历史记录数量限制
     * @param authentication 认证信息
     * @return 编辑历史列表
     */
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<EditorResponse.EditHistoryResponse>> getEditHistory(
            @PathVariable @NotBlank String sessionId,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.debug("用户 {} 获取编辑历史: sessionId={}", userId, sessionId);
            
            // 验证会话所有者
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
            
            List<EditorService.EditHistory> histories = editorService.getEditHistory(sessionId, limit);
            List<EditorResponse.EditHistoryResponse> responses = histories.stream()
                .map(EditorResponse.EditHistoryResponse::fromEditHistory)
                .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("获取编辑历史失败: sessionId={}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 比较版本差异
     * 
     * @param request 版本比较请求
     * @param authentication 认证信息
     * @return 差异结果
     */
    @PostMapping("/compare")
    public ResponseEntity<EditorResponse.DiffResponse> compareVersions(
            @Valid @RequestBody EditorRequest.CompareVersionsRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.debug("用户 {} 比较版本: {} vs {}", userId, request.getOldHistoryId(), request.getNewHistoryId());
            
            Optional<EditorService.DiffResult> diffOpt = editorService.compareVersions(
                request.getOldHistoryId(), 
                request.getNewHistoryId()
            );
            
            if (diffOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            EditorResponse.DiffResponse response = EditorResponse.DiffResponse.fromDiffResult(diffOpt.get());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("版本比较失败: {} vs {}", request.getOldHistoryId(), request.getNewHistoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 恢复到指定版本
     * 
     * @param sessionId 会话ID
     * @param request 恢复版本请求
     * @param authentication 认证信息
     * @return 恢复结果
     */
    @PostMapping("/session/{sessionId}/restore")
    public ResponseEntity<EditorResponse.BaseEditorResponse> restoreVersion(
            @PathVariable @NotBlank String sessionId,
            @Valid @RequestBody EditorRequest.RestoreVersionRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 恢复版本: sessionId={}, historyId={}", userId, sessionId, request.getHistoryId());
            
            // 验证会话所有者
            Optional<EditorService.EditSession> sessionOpt = editorService.getEditSession(sessionId);
            if (sessionOpt.isEmpty() || !sessionOpt.get().getUserId().equals(userId)) {
                EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("无权访问该编辑会话");
                return ResponseEntity.status(403).body(response);
            }
            
            EditorService.EditResult result = editorService.restoreVersion(sessionId, request.getHistoryId());
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("恢复版本失败: sessionId={}, historyId={}", sessionId, request.getHistoryId(), e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("恢复版本失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取支持的编程语言列表
     * 
     * @return 支持的语言列表
     */
    @GetMapping("/supported-languages")
    public ResponseEntity<EditorResponse.SupportedLanguagesResponse> getSupportedLanguages() {
        try {
            EditorResponse.SupportedLanguagesResponse response = new EditorResponse.SupportedLanguagesResponse(
                editorService.getSupportedLanguages(),
                editorService.getSupportedEditExtensions()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取支持的语言列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 检测文件编程语言
     * 
     * @param request 语言检测请求
     * @return 检测到的语言
     */
    @PostMapping("/detect-language")
    public ResponseEntity<Map<String, Object>> detectLanguage(
            @Valid @RequestBody EditorRequest.DetectLanguageRequest request) {
        
        try {
            Optional<String> language = editorService.detectLanguage(request.getFilename(), request.getContent());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("language", language.orElse("text"));
            response.put("supported", language.isPresent());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检测文件语言失败: filename={}", request.getFilename(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检测文件语言失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取文件锁信息
     * 
     * @param fileId 文件ID
     * @param authentication 认证信息
     * @return 文件锁信息
     */
    @GetMapping("/file/{fileId}/lock")
    public ResponseEntity<EditorResponse.FileLockResponse> getFileLockInfo(
            @PathVariable @NotNull @Min(1) Long fileId,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.debug("用户 {} 获取文件锁信息: fileId={}", userId, fileId);
            
            Optional<EditorService.FileLockInfo> lockInfoOpt = editorService.getFileLockInfo(fileId);
            
            EditorResponse.FileLockResponse response;
            if (lockInfoOpt.isPresent()) {
                response = EditorResponse.FileLockResponse.fromFileLockInfo(lockInfoOpt.get());
            } else {
                response = EditorResponse.FileLockResponse.unlocked(fileId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取文件锁信息失败: fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 锁定文件
     * 
     * @param fileId 文件ID
     * @param request 锁定请求
     * @param authentication 认证信息
     * @return 锁定结果
     */
    @PostMapping("/file/{fileId}/lock")
    public ResponseEntity<EditorResponse.BaseEditorResponse> lockFile(
            @PathVariable @NotNull @Min(1) Long fileId,
            @Valid @RequestBody EditorRequest.LockFileRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 锁定文件: fileId={}, sessionId={}", userId, fileId, request.getSessionId());
            
            EditorService.EditResult result = editorService.lockFile(fileId, userId, request.getSessionId());
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("锁定文件失败: fileId={}", fileId, e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("锁定文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 解锁文件
     * 
     * @param fileId 文件ID
     * @param request 解锁请求
     * @param authentication 认证信息
     * @return 解锁结果
     */
    @DeleteMapping("/file/{fileId}/lock")
    public ResponseEntity<EditorResponse.BaseEditorResponse> unlockFile(
            @PathVariable @NotNull @Min(1) Long fileId,
            @Valid @RequestBody EditorRequest.UnlockFileRequest request,
            Authentication authentication) {
        
        try {
            Long userId = getCurrentUserId(authentication);
            logger.info("用户 {} 解锁文件: fileId={}, sessionId={}", userId, fileId, request.getSessionId());
            
            EditorService.EditResult result = editorService.unlockFile(fileId, userId, request.getSessionId());
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.fromEditResult(result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("解锁文件失败: fileId={}", fileId, e);
            EditorResponse.BaseEditorResponse response = EditorResponse.BaseEditorResponse.error("解锁文件失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取当前用户ID
     * 
     * @param authentication 认证信息
     * @return 用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof LyraUserPrincipal) {
            return ((LyraUserPrincipal) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }
} 