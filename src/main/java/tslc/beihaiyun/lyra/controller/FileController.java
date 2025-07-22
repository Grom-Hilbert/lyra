package tslc.beihaiyun.lyra.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tslc.beihaiyun.lyra.dto.FileRequest;
import tslc.beihaiyun.lyra.dto.FileResponse;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.FolderService;

/**
 * 文件操作控制器
 * 处理文件上传、下载、管理等操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FolderService folderService;
    private final SpaceRepository spaceRepository;
    
    // 分块上传会话管理（生产环境应使用Redis或数据库）
    private final Map<String, ChunkedUploadSession> uploadSessions = new ConcurrentHashMap<>();

    /**
     * 分块上传会话信息
     */
    private static class ChunkedUploadSession {
        private final String uploadId;
        private final String filename;
        private final long fileSize;
        private final String fileHash;
        private final int totalChunks;
        private final int chunkSize;
        private final Long spaceId;
        private final Long folderId;
        private final Long uploaderId;
        private final LocalDateTime createdAt;
        private final boolean[] completedChunks;
        
        public ChunkedUploadSession(String uploadId, String filename, long fileSize, String fileHash,
                                   int totalChunks, int chunkSize, Long spaceId, Long folderId, Long uploaderId) {
            this.uploadId = uploadId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.fileHash = fileHash;
            this.totalChunks = totalChunks;
            this.chunkSize = chunkSize;
            this.spaceId = spaceId;
            this.folderId = folderId;
            this.uploaderId = uploaderId;
            this.createdAt = LocalDateTime.now();
            this.completedChunks = new boolean[totalChunks];
        }
        
        // Getters
        public String getUploadId() { return uploadId; }
        public String getFilename() { return filename; }
        public long getFileSize() { return fileSize; }
        public String getFileHash() { return fileHash; }
        public int getTotalChunks() { return totalChunks; }
        public int getChunkSize() { return chunkSize; }
        public Long getSpaceId() { return spaceId; }
        public Long getFolderId() { return folderId; }
        public Long getUploaderId() { return uploaderId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean[] getCompletedChunks() { return completedChunks; }
        
        public void markChunkCompleted(int chunkIndex) {
            if (chunkIndex >= 0 && chunkIndex < totalChunks) {
                completedChunks[chunkIndex] = true;
            }
        }
        
        public boolean isCompleted() {
            for (boolean completed : completedChunks) {
                if (!completed) return false;
            }
            return true;
        }
        
        public List<Integer> getCompletedChunkIndexes() {
            List<Integer> completed = new java.util.ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                if (completedChunks[i]) {
                    completed.add(i);
                }
            }
            return completed;
        }
    }

    // ==================== 文件上传相关接口 ====================

    /**
     * 单文件上传
     * 
     * @param file 上传的文件
     * @param request 上传请求参数
     * @param principal 认证用户信息
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse.FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute FileRequest.FileUploadRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest()
                    .body(new FileResponse.FileUploadResponse(false, "参数验证失败: " + errorMessage));
        }
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new FileResponse.FileUploadResponse(false, "指定的空间不存在"));
            }
            
            Space space = spaceOpt.get();
            Folder folder = null;
            
            // 验证文件夹权限
            if (request.getFolderId() != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(request.getFolderId());
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new FileResponse.FileUploadResponse(false, "指定的文件夹不存在"));
                }
                folder = folderOpt.get();
            }
            
            // 执行上传
            FileService.FileOperationResult result = fileService.uploadFile(file, space, folder, principal.getId());
            
            if (result.isSuccess()) {
                FileResponse.FileInfoResponse fileInfo = new FileResponse.FileInfoResponse(result.getFileEntity());
                FileResponse.FileUploadResponse response = new FileResponse.FileUploadResponse(true, result.getMessage(), fileInfo);
                
                log.info("用户 {} 成功上传文件: {}", principal.getUsername(), result.getFileEntity().getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("文件上传失败: {}", result.getMessage());
                return ResponseEntity.badRequest()
                        .body(new FileResponse.FileUploadResponse(false, result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileResponse.FileUploadResponse(false, "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 初始化分块上传
     * 
     * @param request 分块上传初始化请求
     * @param principal 认证用户信息
     * @return 分块上传响应
     */
    @PostMapping("/upload/chunked/init")
    public ResponseEntity<FileResponse.ChunkedUploadResponse> initChunkedUpload(
            @Valid @RequestBody FileRequest.ChunkedUploadInitRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 计算分块数量
            int totalChunks = (int) Math.ceil((double) request.getFileSize() / request.getChunkSize());
            String uploadId = java.util.UUID.randomUUID().toString();
            
            // 创建上传会话
            ChunkedUploadSession session = new ChunkedUploadSession(
                    uploadId, request.getFilename(), request.getFileSize(), request.getFileHash(),
                    totalChunks, request.getChunkSize(), request.getSpaceId(), request.getFolderId(),
                    principal.getId());
            
            uploadSessions.put(uploadId, session);
            
            // 构建响应
            FileResponse.ChunkedUploadResponse response = new FileResponse.ChunkedUploadResponse();
            response.setUploadId(uploadId);
            response.setTotalChunks(totalChunks);
            response.setChunkSize(request.getChunkSize());
            response.setCompletedChunks(List.of()); // 初始为空
            response.setUploadCompleted(false);
            
            log.info("用户 {} 初始化分块上传: {} (大小: {}, 分块数: {})", 
                    principal.getUsername(), request.getFilename(), request.getFileSize(), totalChunks);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("初始化分块上传异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 上传文件分块
     * 
     * @param uploadId 上传会话ID
     * @param chunkIndex 分块索引
     * @param chunk 分块数据
     * @param principal 认证用户信息
     * @return 上传结果
     */
    @PostMapping("/upload/chunked/{uploadId}/chunk/{chunkIndex}")
    public ResponseEntity<FileResponse.ChunkedUploadResponse> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable int chunkIndex,
            @RequestParam("chunk") MultipartFile chunk,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            ChunkedUploadSession session = uploadSessions.get(uploadId);
            if (session == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // 验证用户权限
            if (!session.getUploaderId().equals(principal.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // 验证分块索引
            if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
                return ResponseEntity.badRequest().build();
            }
            
            // TODO: 存储分块到临时位置
            // 这里应该将分块保存到临时存储，实际实现需要结合具体的存储策略
            
            // 标记分块完成
            session.markChunkCompleted(chunkIndex);
            
            // 构建响应
            FileResponse.ChunkedUploadResponse response = new FileResponse.ChunkedUploadResponse();
            response.setUploadId(uploadId);
            response.setTotalChunks(session.getTotalChunks());
            response.setChunkSize(session.getChunkSize());
            response.setCompletedChunks(session.getCompletedChunkIndexes());
            response.setUploadCompleted(session.isCompleted());
            
            // 如果所有分块都完成了，合并文件
            if (session.isCompleted()) {
                // TODO: 实现文件合并逻辑
                // 这里应该将所有分块合并成完整文件，并调用fileService保存
                uploadSessions.remove(uploadId);
                log.info("用户 {} 完成分块上传: {}", principal.getUsername(), session.getFilename());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("上传分块异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 文件下载相关接口 ====================

    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     * @param principal 认证用户信息
     * @return 文件流
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            Optional<FileEntity> fileOpt = fileService.getFileById(fileId);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FileEntity file = fileOpt.get();
            
            // TODO: 检查下载权限
            
            Optional<InputStream> contentOpt = fileService.getFileContent(fileId);
            if (contentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            InputStreamResource resource = new InputStreamResource(contentOpt.get());
            
            String encodedFilename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getMimeType() != null ? file.getMimeType() : "application/octet-stream"));
            headers.setContentLength(file.getSizeBytes());
            headers.set("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
            
            log.info("用户 {} 下载文件: {}", principal.getUsername(), file.getName());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("文件下载异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 文件预览
     * 
     * @param fileId 文件ID
     * @param principal 认证用户信息
     * @return 文件内容
     */
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<InputStreamResource> previewFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            Optional<FileEntity> fileOpt = fileService.getFileById(fileId);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FileEntity file = fileOpt.get();
            
            // 检查文件是否支持预览
            if (!isPreviewSupported(file.getMimeType())) {
                return ResponseEntity.badRequest().build();
            }
            
            Optional<InputStream> contentOpt = fileService.getFileContent(fileId);
            if (contentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            InputStreamResource resource = new InputStreamResource(contentOpt.get());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getMimeType()));
            headers.setContentLength(file.getSizeBytes());
            headers.setCacheControl("max-age=3600"); // 缓存1小时
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("文件预览异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 文件信息和管理接口 ====================

    /**
     * 获取文件信息
     * 
     * @param fileId 文件ID
     * @param principal 认证用户信息
     * @return 文件信息
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable Long fileId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        Optional<FileEntity> fileOpt = fileService.getFileById(fileId);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "文件不存在"));
        }
        
        FileEntity file = fileOpt.get();
        // TODO: 检查查看权限
        
        FileResponse.FileInfoResponse fileInfo = new FileResponse.FileInfoResponse(file);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "获取文件信息成功",
            "data", fileInfo
        ));
    }

    /**
     * 更新文件信息
     * 
     * @param fileId 文件ID
     * @param request 更新请求
     * @param principal 认证用户信息
     * @return 更新结果
     */
    @PutMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> updateFileInfo(
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest.FileUpdateRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            FileService.FileOperationResult result = fileService.updateFileInfo(
                    fileId, request.getFilename(), request.getDescription(), principal.getId());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "data", new FileResponse.FileInfoResponse(result.getFileEntity())
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("更新文件信息异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @param principal 认证用户信息
     * @return 删除结果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            boolean success = fileService.deleteFile(fileId, principal.getId());
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文件删除成功"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "文件删除失败"));
            }
            
        } catch (Exception e) {
            log.error("删除文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    // ==================== 文件操作接口 ====================

    /**
     * 移动文件
     * 
     * @param fileId 文件ID
     * @param request 移动请求
     * @param principal 认证用户信息
     * @return 移动结果
     */
    @PostMapping("/{fileId}/move")
    public ResponseEntity<Map<String, Object>> moveFile(
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest.FileMoveRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 获取目标空间和文件夹
            Optional<Space> targetSpaceOpt = spaceRepository.findById(request.getTargetSpaceId());
            if (targetSpaceOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "目标空间不存在"));
            }
            
            Folder targetFolder = null;
            if (request.getTargetFolderId() != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(request.getTargetFolderId());
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "目标文件夹不存在"));
                }
                targetFolder = folderOpt.get();
            }
            
            FileService.FileOperationResult result;
            if (request.isKeepOriginal()) {
                // 复制操作
                result = fileService.copyFile(fileId, targetSpaceOpt.get(), targetFolder, principal.getId());
            } else {
                // 移动操作
                result = fileService.moveFile(fileId, targetSpaceOpt.get(), targetFolder, principal.getId());
            }
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "data", new FileResponse.FileInfoResponse(result.getFileEntity())
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("移动文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }

    /**
     * 复制文件
     * 
     * @param fileId 文件ID
     * @param request 复制请求
     * @param principal 认证用户信息
     * @return 复制结果
     */
    @PostMapping("/{fileId}/copy")
    public ResponseEntity<Map<String, Object>> copyFile(
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest.FileCopyRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 获取目标空间和文件夹
            Optional<Space> targetSpaceOpt = spaceRepository.findById(request.getTargetSpaceId());
            if (targetSpaceOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "目标空间不存在"));
            }
            
            Folder targetFolder = null;
            if (request.getTargetFolderId() != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(request.getTargetFolderId());
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "目标文件夹不存在"));
                }
                targetFolder = folderOpt.get();
            }
            
            FileService.FileOperationResult result = fileService.copyFile(fileId, targetSpaceOpt.get(), targetFolder, principal.getId());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "data", new FileResponse.FileInfoResponse(result.getFileEntity())
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("复制文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }

    /**
     * 重命名文件 (POST方式)
     * 
     * @param fileId 文件ID
     * @param request 重命名请求
     * @param principal 认证用户信息
     * @return 重命名结果
     */
    @PostMapping("/{fileId}/rename")
    public ResponseEntity<Map<String, Object>> renameFile(
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest.FileRenameRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            FileService.FileOperationResult result = fileService.renameFile(fileId, request.getNewFilename(), principal.getId());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "data", new FileResponse.FileInfoResponse(result.getFileEntity())
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("重命名文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }

    /**
     * 重命名文件 (PUT方式) - 为兼容测试
     * 
     * @param fileId 文件ID
     * @param request 重命名请求
     * @param principal 认证用户信息
     * @return 重命名结果
     */
    @PutMapping("/{fileId}/name")
    public ResponseEntity<Map<String, Object>> renameFileByPut(
            @PathVariable Long fileId,
            @Valid @RequestBody FileRequest.FileRenameRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            FileService.FileOperationResult result = fileService.renameFile(fileId, request.getNewFilename(), principal.getId());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "data", new FileResponse.FileInfoResponse(result.getFileEntity())
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("重命名文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }

    // ==================== 批量操作接口 ====================

    /**
     * 批量删除文件
     * 
     * @param request 批量操作请求
     * @param principal 认证用户信息
     * @return 批量操作结果
     */
    @PostMapping("/batch/delete")
    public ResponseEntity<FileResponse.BatchOperationResponse> batchDeleteFiles(
            @Valid @RequestBody FileRequest.FileBatchOperationRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            FileService.BatchOperationResult result = fileService.batchDeleteFiles(request.getFileIds(), principal.getId());
            FileResponse.BatchOperationResponse response = new FileResponse.BatchOperationResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量删除文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 批量移动文件
     * 
     * @param request 批量操作请求
     * @param principal 认证用户信息
     * @return 批量操作结果
     */
    @PostMapping("/batch/move")
    public ResponseEntity<FileResponse.BatchOperationResponse> batchMoveFiles(
            @Valid @RequestBody FileRequest.FileBatchOperationRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 获取目标空间和文件夹
            Optional<Space> targetSpaceOpt = spaceRepository.findById(request.getTargetSpaceId());
            if (targetSpaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Folder targetFolder = null;
            if (request.getTargetFolderId() != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(request.getTargetFolderId());
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                targetFolder = folderOpt.get();
            }
            
            FileService.BatchOperationResult result = fileService.batchMoveFiles(
                    request.getFileIds(), targetSpaceOpt.get(), targetFolder, principal.getId());
            FileResponse.BatchOperationResponse response = new FileResponse.BatchOperationResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量移动文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 批量复制文件
     * 
     * @param request 批量操作请求
     * @param principal 认证用户信息
     * @return 批量操作结果
     */
    @PostMapping("/batch/copy")
    public ResponseEntity<FileResponse.BatchOperationResponse> batchCopyFiles(
            @Valid @RequestBody FileRequest.FileBatchOperationRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 获取目标空间和文件夹
            Optional<Space> targetSpaceOpt = spaceRepository.findById(request.getTargetSpaceId());
            if (targetSpaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Folder targetFolder = null;
            if (request.getTargetFolderId() != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(request.getTargetFolderId());
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                targetFolder = folderOpt.get();
            }
            
            FileService.BatchOperationResult result = fileService.batchCopyFiles(
                    request.getFileIds(), targetSpaceOpt.get(), targetFolder, principal.getId());
            FileResponse.BatchOperationResponse response = new FileResponse.BatchOperationResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("批量复制文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 文件查询和搜索接口 ====================

    /**
     * 获取空间文件列表
     * 
     * @param spaceId 空间ID
     * @param folderId 文件夹ID（可选）
     * @param page 页码
     * @param size 每页大小
     * @param sort 排序字段
     * @param direction 排序方向
     * @param principal 认证用户信息
     * @return 文件列表
     */
    @GetMapping("/space/{spaceId}")
    public ResponseEntity<FileResponse.FileListResponse> getFilesBySpace(
            @PathVariable Long spaceId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Folder folder = null;
            if (folderId != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(folderId);
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                folder = folderOpt.get();
            }
            
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            Page<FileEntity> filePage = fileService.getFilesPaged(spaceOpt.get(), folder, false, pageable);
            
            List<FileResponse.FileInfoResponse> files = filePage.getContent().stream()
                    .map(FileResponse.FileInfoResponse::new)
                    .collect(Collectors.toList());
            
            FileResponse.FileListResponse response = new FileResponse.FileListResponse();
            response.setFiles(files);
            response.setTotalElements((int) filePage.getTotalElements());
            response.setTotalPages(filePage.getTotalPages());
            response.setCurrentPage(filePage.getNumber());
            response.setPageSize(filePage.getSize());
            response.setHasNext(filePage.hasNext());
            response.setHasPrevious(filePage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 搜索文件 (GET方式)
     * 
     * @param spaceId 空间ID
     * @param query 搜索关键词
     * @param mimeType MIME类型过滤
     * @param includeDeleted 是否包含已删除文件
     * @param page 页码
     * @param size 每页大小
     * @param principal 认证用户信息
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFilesGet(
            @RequestParam Long spaceId,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String mimeType,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "指定的空间不存在"));
            }
            
            List<FileEntity> files = fileService.searchFiles(
                    spaceOpt.get(), query, mimeType, includeDeleted);
            
            // 手动分页
            int totalElements = files.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalElements);
            
            List<FileEntity> pagedFiles = files.subList(fromIndex, toIndex);
            List<FileResponse.FileInfoResponse> fileInfos = pagedFiles.stream()
                    .map(FileResponse.FileInfoResponse::new)
                    .collect(Collectors.toList());
            
            Map<String, Object> content = Map.of(
                "content", fileInfos,
                "totalElements", totalElements,
                "totalPages", (int) Math.ceil((double) totalElements / size),
                "currentPage", page,
                "pageSize", size,
                "hasNext", toIndex < totalElements,
                "hasPrevious", page > 0
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "搜索完成",
                "data", content
            ));
            
        } catch (Exception e) {
            log.error("搜索文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索文件 (POST方式)
     * 
     * @param request 搜索请求
     * @param principal 认证用户信息
     * @return 搜索结果
     */
    @PostMapping("/search")
    public ResponseEntity<FileResponse.FileListResponse> searchFiles(
            @Valid @RequestBody FileRequest.FileSearchRequest request,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<FileEntity> files = fileService.searchFiles(
                    spaceOpt.get(), request.getKeyword(), request.getMimeType(), request.isIncludeDeleted());
            
            // 手动分页
            int totalElements = files.size();
            int fromIndex = request.getPage() * request.getSize();
            int toIndex = Math.min(fromIndex + request.getSize(), totalElements);
            
            List<FileEntity> pagedFiles = files.subList(fromIndex, toIndex);
            List<FileResponse.FileInfoResponse> fileInfos = pagedFiles.stream()
                    .map(FileResponse.FileInfoResponse::new)
                    .collect(Collectors.toList());
            
            FileResponse.FileListResponse response = new FileResponse.FileListResponse();
            response.setFiles(fileInfos);
            response.setTotalElements(totalElements);
            response.setTotalPages((int) Math.ceil((double) totalElements / request.getSize()));
            response.setCurrentPage(request.getPage());
            response.setPageSize(request.getSize());
            response.setHasNext(toIndex < totalElements);
            response.setHasPrevious(request.getPage() > 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("搜索文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 批量文件上传
     * 
     * @param files 上传的文件列表
     * @param spaceId 空间ID
     * @param folderId 文件夹ID（可选）
     * @param principal 认证用户信息
     * @return 批量上传结果
     */
    @PostMapping("/batch-upload")
    public ResponseEntity<Map<String, Object>> batchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam Long spaceId,
            @RequestParam(required = false) Long folderId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "指定的空间不存在"));
            }
            
            Space space = spaceOpt.get();
            Folder folder = null;
            
            // 验证文件夹权限
            if (folderId != null) {
                Optional<Folder> folderOpt = folderService.getFolderById(folderId);
                if (folderOpt.isEmpty()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "指定的文件夹不存在"));
                }
                folder = folderOpt.get();
            }
            
            List<Map<String, Object>> uploadResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;
            
            for (MultipartFile file : files) {
                try {
                    FileService.FileOperationResult result = fileService.uploadFile(file, space, folder, principal.getId());
                    
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", file.getOriginalFilename());
                    fileResult.put("success", result.isSuccess());
                    fileResult.put("message", result.getMessage());
                    
                    if (result.isSuccess()) {
                        fileResult.put("fileInfo", new FileResponse.FileInfoResponse(result.getFileEntity()));
                        successCount++;
                    } else {
                        failedCount++;
                    }
                    
                    uploadResults.add(fileResult);
                    
                } catch (Exception e) {
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", file.getOriginalFilename());
                    fileResult.put("success", false);
                    fileResult.put("message", "上传失败: " + e.getMessage());
                    uploadResults.add(fileResult);
                    failedCount++;
                }
            }
            
            Map<String, Object> data = Map.of(
                "uploadResults", uploadResults,
                "totalFiles", files.length,
                "successCount", successCount,
                "failedCount", failedCount
            );
            
            String message = String.format("批量上传完成：成功 %d 个，失败 %d 个", successCount, failedCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "data", data
            ));
            
        } catch (Exception e) {
            log.error("批量上传文件异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "批量上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件统计信息
     * 
     * @param spaceId 空间ID
     * @param principal 认证用户信息
     * @return 统计信息
     */
    @GetMapping("/space/{spaceId}/statistics")
    public ResponseEntity<FileResponse.FileStatisticsResponse> getFileStatistics(
            @PathVariable Long spaceId,
            @AuthenticationPrincipal LyraUserPrincipal principal) {
        
        try {
            // 验证空间访问权限
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            FileService.FileStatistics stats = fileService.getFileStatistics(spaceOpt.get());
            FileResponse.FileStatisticsResponse response = new FileResponse.FileStatisticsResponse(stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取文件统计信息异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查文件是否支持预览
     * 
     * @param mimeType MIME类型
     * @return 是否支持预览
     */
    private boolean isPreviewSupported(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("text/") || 
               mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf") ||
               mimeType.equals("application/json") ||
               mimeType.equals("application/xml");
    }
} 