package tslc.beihaiyun.lyra.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import tslc.beihaiyun.lyra.dto.FolderRequest;
import tslc.beihaiyun.lyra.dto.FolderResponse;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FolderService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件夹操作控制器
 * 提供文件夹管理的RESTful API接口
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final SpaceRepository spaceRepository;

    /**
     * 创建文件夹
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> createFolder(
            @Valid @RequestBody FolderRequest.CreateFolderRequest request,
            @AuthenticationPrincipal LyraUserPrincipal user,
            BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(
                FolderResponse.ApiResponse.error("参数验证失败: " + errorMessage)
            );
        }

        try {
            log.info("用户 {} 尝试创建文件夹: {}", user.getId(), request.getName());

            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            Folder parentFolder = null;
            if (request.getParentFolderId() != null) {
                Optional<Folder> parentOpt = folderService.getFolderById(request.getParentFolderId());
                if (parentOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        FolderResponse.ApiResponse.error("指定的父文件夹不存在")
                    );
                }
                parentFolder = parentOpt.get();
            }

            FolderService.FolderOperationResult result = folderService.createFolder(
                request.getName(), parentFolder, space, user.getId()
            );

            FolderResponse.FolderOperationResponse response = 
                FolderResponse.FolderOperationResponse.fromResult(result);

            if (result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    FolderResponse.ApiResponse.success("文件夹创建成功", response)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error(result.getMessage(), response)
                );
            }

        } catch (Exception e) {
            log.error("创建文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 获取文件夹详情
     */
    @GetMapping("/{folderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderDetailResponse>> getFolderDetail(
            @PathVariable Long folderId,
            @AuthenticationPrincipal LyraUserPrincipal user) {

        try {
            Optional<Folder> folderOpt = folderService.getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    FolderResponse.ApiResponse.error("文件夹不存在")
                );
            }

            FolderResponse.FolderDetailResponse response = 
                FolderResponse.FolderDetailResponse.fromEntity(folderOpt.get());

            return ResponseEntity.ok(
                FolderResponse.ApiResponse.success(response)
            );

        } catch (Exception e) {
            log.error("获取文件夹详情时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 更新文件夹信息
     */
    @PutMapping("/{folderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody FolderRequest.UpdateFolderRequest request,
            @AuthenticationPrincipal LyraUserPrincipal user,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(
                FolderResponse.ApiResponse.error("参数验证失败: " + errorMessage)
            );
        }

        try {
            Optional<Folder> folderOpt = folderService.getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    FolderResponse.ApiResponse.error("文件夹不存在")
                );
            }

            FolderService.FolderOperationResult result = folderService.updateFolderInfo(
                folderId, request.getName(), user.getId()
            );

            FolderResponse.FolderOperationResponse response = 
                FolderResponse.FolderOperationResponse.fromResult(result);

            if (result.isSuccess()) {
                return ResponseEntity.ok(
                    FolderResponse.ApiResponse.success("文件夹更新成功", response)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error(result.getMessage(), response)
                );
            }

        } catch (Exception e) {
            log.error("更新文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/{folderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<Void>> deleteFolder(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "false") boolean force,
            @AuthenticationPrincipal LyraUserPrincipal user) {

        try {
            Optional<Folder> folderOpt = folderService.getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    FolderResponse.ApiResponse.error("文件夹不存在")
                );
            }

            boolean success = folderService.deleteFolder(folderId, user.getId(), force);

            if (success) {
                return ResponseEntity.ok(
                    FolderResponse.ApiResponse.success("文件夹删除成功", null)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("文件夹删除失败")
                );
            }

        } catch (Exception e) {
            log.error("删除文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 获取文件夹列表
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<Page<FolderResponse.FolderSummaryResponse>>> getFolders(
            @RequestParam(required = false) Long parentId,
            @RequestParam Long spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @AuthenticationPrincipal LyraUserPrincipal user) {

        try {
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            Folder parentFolder = null;
            if (parentId != null) {
                Optional<Folder> parentOpt = folderService.getFolderById(parentId);
                if (parentOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        FolderResponse.ApiResponse.error("指定的父文件夹不存在")
                    );
                }
                parentFolder = parentOpt.get();
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<Folder> folders = folderService.getFoldersPaged(space, parentFolder, pageable);
            Page<FolderResponse.FolderSummaryResponse> response = folders.map(
                FolderResponse.FolderSummaryResponse::fromEntity
            );

            return ResponseEntity.ok(
                FolderResponse.ApiResponse.success(response)
            );

        } catch (Exception e) {
            log.error("获取文件夹列表时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 构建文件夹树
     */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<List<FolderResponse.FolderTreeResponse>>> getFolderTree(
            @RequestParam Long spaceId,
            @RequestParam(defaultValue = "-1") int maxDepth,
            @AuthenticationPrincipal LyraUserPrincipal user) {

        try {
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            List<FolderService.FolderTreeNode> treeNodes = folderService.buildFolderTree(space, maxDepth);
            
            List<FolderResponse.FolderTreeResponse> response = treeNodes.stream()
                .map(FolderResponse.FolderTreeResponse::fromTreeNode)
                .collect(Collectors.toList());

            return ResponseEntity.ok(
                FolderResponse.ApiResponse.success(response)
            );

        } catch (Exception e) {
            log.error("构建文件夹树时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 移动文件夹
     */
    @PostMapping("/move")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> moveFolder(
            @Valid @RequestBody FolderRequest.MoveFolderRequest request,
            @AuthenticationPrincipal LyraUserPrincipal user,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(
                FolderResponse.ApiResponse.error("参数验证失败: " + errorMessage)
            );
        }

        try {
            Optional<Folder> folderOpt = folderService.getFolderById(request.getFolderId());
            if (folderOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("源文件夹不存在")
                );
            }

            Folder targetParent = null;
            if (request.getTargetParentFolderId() != null) {
                Optional<Folder> targetOpt = folderService.getFolderById(request.getTargetParentFolderId());
                if (targetOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        FolderResponse.ApiResponse.error("目标父文件夹不存在")
                    );
                }
                targetParent = targetOpt.get();
            }

            FolderService.FolderOperationResult result = folderService.moveFolder(
                request.getFolderId(), targetParent, user.getId()
            );

            FolderResponse.FolderOperationResponse response = 
                FolderResponse.FolderOperationResponse.fromResult(result);

            if (result.isSuccess()) {
                return ResponseEntity.ok(
                    FolderResponse.ApiResponse.success("文件夹移动成功", response)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error(result.getMessage(), response)
                );
            }

        } catch (Exception e) {
            log.error("移动文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 批量创建文件夹
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.BatchOperationResponse>> batchCreateFolders(
            @Valid @RequestBody FolderRequest.BatchCreateFolderRequest request,
            @AuthenticationPrincipal LyraUserPrincipal user,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(
                FolderResponse.ApiResponse.error("参数验证失败: " + errorMessage)
            );
        }

        try {
            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            Folder parentFolder = null;
            if (request.getParentFolderId() != null) {
                Optional<Folder> parentOpt = folderService.getFolderById(request.getParentFolderId());
                if (parentOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                        FolderResponse.ApiResponse.error("指定的父文件夹不存在")
                    );
                }
                parentFolder = parentOpt.get();
            }

            FolderService.BatchFolderOperationResult result = folderService.batchCreateFolders(
                request.getFolderNames(), parentFolder, space, user.getId()
            );

            FolderResponse.BatchOperationResponse response = 
                FolderResponse.BatchOperationResponse.fromResult(result);

            if (result.isAllSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    FolderResponse.ApiResponse.success("批量创建文件夹成功", response)
                );
            } else {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(
                    FolderResponse.ApiResponse.success("批量创建文件夹部分成功", response)
                );
            }

        } catch (Exception e) {
            log.error("批量创建文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 搜索文件夹
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<List<FolderResponse.FolderSummaryResponse>>> searchFolders(
            @Valid @RequestBody FolderRequest.SearchFolderRequest request,
            @AuthenticationPrincipal LyraUserPrincipal user,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(
                FolderResponse.ApiResponse.error("参数验证失败: " + errorMessage)
            );
        }

        try {
            Optional<Space> spaceOpt = spaceRepository.findById(request.getSpaceId());
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            List<Folder> folders = folderService.searchFolders(space, request.getKeyword());
            
            List<FolderResponse.FolderSummaryResponse> response = folders.stream()
                .map(FolderResponse.FolderSummaryResponse::fromEntity)
                .collect(Collectors.toList());

            return ResponseEntity.ok(
                FolderResponse.ApiResponse.success(response)
            );

        } catch (Exception e) {
            log.error("搜索文件夹时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }

    /**
     * 获取文件夹统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderStatisticsResponse>> getFolderStatistics(
            @RequestParam Long spaceId,
            @AuthenticationPrincipal LyraUserPrincipal user) {

        try {
            Optional<Space> spaceOpt = spaceRepository.findById(spaceId);
            if (spaceOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    FolderResponse.ApiResponse.error("指定的空间不存在")
                );
            }

            Space space = spaceOpt.get();
            FolderService.FolderStatistics statistics = folderService.getFolderStatistics(space);
            
            FolderResponse.FolderStatisticsResponse response = 
                FolderResponse.FolderStatisticsResponse.fromStatistics(statistics);

            return ResponseEntity.ok(
                FolderResponse.ApiResponse.success(response)
            );

        } catch (Exception e) {
            log.error("获取文件夹统计信息时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FolderResponse.ApiResponse.error("服务器内部错误: " + e.getMessage())
            );
        }
    }
} 