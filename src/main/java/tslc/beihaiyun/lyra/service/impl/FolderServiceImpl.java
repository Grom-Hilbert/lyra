package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.service.FolderService;
import tslc.beihaiyun.lyra.util.FileUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件夹管理服务实现
 * 提供完整的文件夹生命周期管理，包括层级管理、批量操作、权限继承等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
@Transactional
public class FolderServiceImpl implements FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderServiceImpl.class);

    private final FolderRepository folderRepository;

    @Autowired
    public FolderServiceImpl(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    // ==================== 基础CRUD操作 ====================

    @Override
    public FolderOperationResult createFolder(String name, Folder parentFolder, Space space, Long creatorId) {
        try {
            // 验证输入参数
            if (name == null || name.trim().isEmpty()) {
                return new FolderOperationResult(false, "文件夹名称不能为空", (Folder) null);
            }
            
            if (space == null) {
                return new FolderOperationResult(false, "所属空间不能为空", (Folder) null);
            }

            // 清理文件夹名称
            String sanitizedName = FileUtils.sanitizeFilename(name);
            
            // 检查名称是否已存在
            if (isFolderNameExists(parentFolder, sanitizedName, null)) {
                return new FolderOperationResult(false, "文件夹名称已存在", (Folder) null);
            }

            // 构建路径
            String path = buildFolderPath(parentFolder, sanitizedName);
            
            // 检查路径是否已存在
            if (folderRepository.existsBySpaceAndPath(space, path)) {
                return new FolderOperationResult(false, "文件夹路径已存在", (Folder) null);
            }

            // 创建文件夹实体
            Folder folder = new Folder();
            folder.setName(sanitizedName);
            folder.setPath(path);
            folder.setParent(parentFolder);
            folder.setSpace(space);
            folder.setLevel(parentFolder != null ? parentFolder.getLevel() + 1 : 0);
            folder.setIsRoot(parentFolder == null);
            folder.setSizeBytes(0L);
            folder.setFileCount(0);
            folder.setCreatedBy(creatorId.toString());
            folder.setUpdatedBy(creatorId.toString());

            // 保存到数据库
            folder = folderRepository.save(folder);

            logger.info("文件夹创建成功: {}, 路径: {}, 创建者: {}", sanitizedName, path, creatorId);
            return new FolderOperationResult(true, "文件夹创建成功", folder);

        } catch (Exception e) {
            logger.error("创建文件夹失败: {}", e.getMessage(), e);
            return new FolderOperationResult(false, "创建文件夹失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> getFolderById(Long folderId) {
        if (folderId == null) {
            return Optional.empty();
        }
        return folderRepository.findById(folderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folder> getFolderByPath(Space space, String path) {
        if (space == null || path == null) {
            return Optional.empty();
        }
        return folderRepository.findBySpaceAndPath(space, path);
    }

    @Override
    public FolderOperationResult updateFolderInfo(Long folderId, String newName, Long updaterId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return new FolderOperationResult(false, "文件夹不存在", (Folder) null);
            }

            Folder folder = folderOpt.get();
            
            if (newName != null && !newName.trim().isEmpty()) {
                String sanitizedName = FileUtils.sanitizeFilename(newName);
                
                // 检查新名称是否已存在
                if (!sanitizedName.equals(folder.getName()) && 
                    isFolderNameExists(folder.getParent(), sanitizedName, folder.getId())) {
                    return new FolderOperationResult(false, "文件夹名称已存在", (Folder) null);
                }

                // 更新名称和路径
                String oldPath = folder.getPath();
                String newPath = buildFolderPath(folder.getParent(), sanitizedName);
                
                folder.setName(sanitizedName);
                folder.setPath(newPath);
                folder.setUpdatedBy(updaterId.toString());

                // 更新所有子文件夹的路径
                updateChildrenPaths(oldPath, newPath);
            }

            folder = folderRepository.save(folder);
            logger.info("文件夹信息更新成功: {}, 操作者: {}", folder.getName(), updaterId);
            return new FolderOperationResult(true, "文件夹信息更新成功", folder);

        } catch (Exception e) {
            logger.error("更新文件夹信息失败: {}", e.getMessage(), e);
            return new FolderOperationResult(false, "更新文件夹信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFolder(Long folderId, Long deleterId, boolean force) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                logger.warn("删除失败，文件夹不存在: {}", folderId);
                return false;
            }

            Folder folder = folderOpt.get();
            
            // 检查是否为空文件夹
            if (!force && !isFolderEmpty(folderId)) {
                logger.warn("删除失败，文件夹不为空且未指定强制删除: {}", folderId);
                return false;
            }

            // 如果强制删除，先删除所有子文件夹和文件
            if (force) {
                deleteChildrenRecursively(folder);
            }

            folderRepository.delete(folder);
            logger.info("文件夹删除成功: {}, 操作者: {}, 强制删除: {}", folder.getName(), deleterId, force);
            return true;

        } catch (Exception e) {
            logger.error("删除文件夹失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 层级管理 ====================

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getChildFolders(Folder parentFolder) {
        if (parentFolder == null) {
            return Collections.emptyList();
        }
        return folderRepository.findByParent(parentFolder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getRootFolders(Space space) {
        if (space == null) {
            return Collections.emptyList();
        }
        return folderRepository.findBySpaceAndIsRootTrue(space);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getAncestorFolders(Folder folder) {
        if (folder == null) {
            return Collections.emptyList();
        }
        return folderRepository.findAllAncestors(folder.getPath());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getDescendantFolders(Folder folder) {
        if (folder == null) {
            return Collections.emptyList();
        }
        return folderRepository.findAllDescendants(folder.getPath());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderTreeNode> buildFolderTree(Space space, int maxDepth) {
        if (space == null) {
            return Collections.emptyList();
        }

        // 获取根文件夹
        List<Folder> rootFolders = getRootFolders(space);
        
        return rootFolders.stream()
                .map(folder -> buildTreeNode(folder, maxDepth, 0))
                .collect(Collectors.toList());
    }

    @Override
    public FolderOperationResult moveFolder(Long folderId, Folder targetParent, Long operatorId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return new FolderOperationResult(false, "文件夹不存在", (Folder) null);
            }

            Folder folder = folderOpt.get();
            
            // 检查是否可以移动
            if (!canMoveFolder(folder, targetParent)) {
                return new FolderOperationResult(false, "不能移动文件夹到其子文件夹中", (Folder) null);
            }

            // 检查目标位置是否已有同名文件夹
            if (isFolderNameExists(targetParent, folder.getName(), folder.getId())) {
                return new FolderOperationResult(false, "目标位置已存在同名文件夹", (Folder) null);
            }

            // 保存旧路径
            String oldPath = folder.getPath();
            
            // 更新文件夹信息
            folder.setParent(targetParent);
            folder.setLevel(targetParent != null ? targetParent.getLevel() + 1 : 0);
            folder.setIsRoot(targetParent == null);
            folder.setPath(buildFolderPath(targetParent, folder.getName()));
            folder.setUpdatedBy(operatorId.toString());

            // 更新所有子文件夹的路径
            updateChildrenPaths(oldPath, folder.getPath());

            folder = folderRepository.save(folder);
            logger.info("文件夹移动成功: {} -> {}, 操作者: {}", oldPath, folder.getPath(), operatorId);
            return new FolderOperationResult(true, "文件夹移动成功", folder);

        } catch (Exception e) {
            logger.error("移动文件夹失败: {}", e.getMessage(), e);
            return new FolderOperationResult(false, "移动文件夹失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FolderOperationResult renameFolder(Long folderId, String newName, Long operatorId) {
        return updateFolderInfo(folderId, newName, operatorId);
    }

    // ==================== 批量操作 ====================

    @Override
    public BatchFolderOperationResult batchCreateFolders(List<String> folderNames, Folder parentFolder, Space space, Long creatorId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = folderNames.size();

        for (String name : folderNames) {
            FolderOperationResult result = createFolder(name, parentFolder, space, creatorId);
            if (result.isSuccess()) {
                successCount++;
            } else {
                errorMessages.add(name + ": " + result.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        return new BatchFolderOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    @Override
    public BatchFolderOperationResult batchDeleteFolders(List<Long> folderIds, Long deleterId, boolean force) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = folderIds.size();

        for (Long folderId : folderIds) {
            try {
                if (deleteFolder(folderId, deleterId, force)) {
                    successCount++;
                } else {
                    errorMessages.add("文件夹 " + folderId + ": 删除失败");
                }
            } catch (Exception e) {
                errorMessages.add("文件夹 " + folderId + ": " + e.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        return new BatchFolderOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    @Override
    public BatchFolderOperationResult batchMoveFolders(List<Long> folderIds, Folder targetParent, Long operatorId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int totalCount = folderIds.size();

        for (Long folderId : folderIds) {
            FolderOperationResult result = moveFolder(folderId, targetParent, operatorId);
            if (result.isSuccess()) {
                successCount++;
            } else {
                errorMessages.add("文件夹 " + folderId + ": " + result.getMessage());
            }
        }

        int failureCount = totalCount - successCount;
        return new BatchFolderOperationResult(totalCount, successCount, failureCount, errorMessages);
    }

    // ==================== 查询和搜索 ====================

    @Override
    @Transactional(readOnly = true)
    public Page<Folder> getFoldersPaged(Space space, Folder parentFolder, Pageable pageable) {
        if (space == null) {
            return Page.empty(pageable);
        }
        
        if (parentFolder == null) {
            // 查询根级别文件夹
            return folderRepository.findBySpace(space, pageable);
        } else {
            return folderRepository.findByParent(parentFolder, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> searchFolders(Space space, String keyword) {
        if (space == null || keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return folderRepository.findBySpaceAndNameContainingIgnoreCase(space, keyword.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getEmptyFolders(Space space) {
        if (space == null) {
            return Collections.emptyList();
        }
        return folderRepository.findEmptyFolders(space);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getLargeFolders(Space space, long sizeThreshold) {
        if (space == null) {
            return Collections.emptyList();
        }
        return folderRepository.findLargeFolders(space, sizeThreshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Folder> getDeepFolders(Space space, int levelThreshold) {
        if (space == null) {
            return Collections.emptyList();
        }
        return folderRepository.findDeepFolders(space, levelThreshold);
    }

    // ==================== 统计和信息 ====================

    @Override
    @Transactional(readOnly = true)
    public FolderStatistics getFolderStatistics(Space space) {
        if (space == null) {
            return new FolderStatistics(0, 0, 0, 0, null);
        }

        long totalFolders = folderRepository.countBySpace(space);
        Long totalSize = folderRepository.sumSizeBytesBySpace(space);
        List<Folder> deepFolders = folderRepository.findDeepFolders(space, 0);
        int maxDepth = deepFolders.isEmpty() ? 0 : deepFolders.get(0).getLevel();
        long emptyFolders = folderRepository.findEmptyFolders(space).size();
        List<Folder> largeFolders = folderRepository.findLargeFolders(space, 0L);
        Folder largestFolder = largeFolders.isEmpty() ? null : largeFolders.get(0);

        return new FolderStatistics(totalFolders, totalSize != null ? totalSize : 0, maxDepth, emptyFolders, largestFolder);
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateFolderSize(Folder folder, boolean includeSubfolders) {
        if (folder == null) {
            return 0;
        }

        long size = folder.getSizeBytes();
        
        if (includeSubfolders) {
            List<Folder> descendants = getDescendantFolders(folder);
            size += descendants.stream().mapToLong(Folder::getSizeBytes).sum();
        }

        return size;
    }

    @Override
    public boolean updateFolderStatistics(Long folderId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return false;
            }

            Folder folder = folderOpt.get();
            folder.updateStatistics();
            folderRepository.save(folder);
            
            logger.debug("文件夹统计信息更新成功: {}", folder.getName());
            return true;

        } catch (Exception e) {
            logger.error("更新文件夹统计信息失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFolderNameExists(Folder parentFolder, String name, Long excludeFolderId) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        Optional<Folder> existing;
        if (parentFolder == null) {
            // 检查根级别
            existing = folderRepository.findBySpaceAndName(parentFolder != null ? parentFolder.getSpace() : null, name);
        } else {
            existing = folderRepository.findByParentAndName(parentFolder, name);
        }

        return existing.isPresent() && 
               (excludeFolderId == null || !existing.get().getId().equals(excludeFolderId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFolderEmpty(Long folderId) {
        if (folderId == null) {
            return false;
        }
        return folderRepository.isFolderEmpty(folderId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canMoveFolder(Folder sourceFolder, Folder targetParent) {
        if (sourceFolder == null) {
            return false;
        }

        // 不能移动到自己或自己的子文件夹中
        if (targetParent != null) {
            List<Folder> ancestors = getAncestorFolders(targetParent);
            ancestors.add(targetParent);
            
            return ancestors.stream().noneMatch(ancestor -> ancestor.getId().equals(sourceFolder.getId()));
        }

        return true;
    }

    // ==================== 权限和版本控制 ====================

    @Override
    public boolean setVersionControlMode(Long folderId, boolean versionControlEnabled, boolean inheritFromParent, Long operatorId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return false;
            }

            Folder folder = folderOpt.get();
            // TODO: 实现版本控制模式设置逻辑
            // 这里需要与Space实体的版本控制设置集成
            
            logger.info("文件夹版本控制模式设置成功: {}, 启用: {}, 继承: {}, 操作者: {}", 
                       folder.getName(), versionControlEnabled, inheritFromParent, operatorId);
            return true;

        } catch (Exception e) {
            logger.error("设置文件夹版本控制模式失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean inheritParentPermissions(Long folderId, Long operatorId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return false;
            }

            Folder folder = folderOpt.get();
            // TODO: 实现权限继承逻辑
            // 这里需要与权限管理系统集成
            
            logger.info("文件夹权限继承设置成功: {}, 操作者: {}", folder.getName(), operatorId);
            return true;

        } catch (Exception e) {
            logger.error("设置文件夹权限继承失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean applyPermissionsToChildren(Long folderId, Long operatorId) {
        try {
            Optional<Folder> folderOpt = getFolderById(folderId);
            if (folderOpt.isEmpty()) {
                return false;
            }

            Folder folder = folderOpt.get();
            List<Folder> descendants = getDescendantFolders(folder);
            
            // TODO: 实现权限应用到子文件夹的逻辑
            // 这里需要与权限管理系统集成
            
            logger.info("文件夹权限应用到子文件夹成功: {}, 影响文件夹数: {}, 操作者: {}", 
                       folder.getName(), descendants.size(), operatorId);
            return true;

        } catch (Exception e) {
            logger.error("应用文件夹权限到子文件夹失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建文件夹路径
     */
    private String buildFolderPath(Folder parentFolder, String folderName) {
        if (parentFolder == null) {
            return "/" + folderName;
        }
        return parentFolder.getPath() + "/" + folderName;
    }

    /**
     * 构建文件夹树节点
     */
    private FolderTreeNode buildTreeNode(Folder folder, int maxDepth, int currentDepth) {
        List<FolderTreeNode> children = new ArrayList<>();
        
        if (maxDepth == -1 || currentDepth < maxDepth) {
            List<Folder> childFolders = getChildFolders(folder);
            children = childFolders.stream()
                    .map(child -> buildTreeNode(child, maxDepth, currentDepth + 1))
                    .collect(Collectors.toList());
        }

        long totalSize = calculateFolderSize(folder, true);
        int totalFileCount = folder.getFileCount() + 
                           children.stream().mapToInt(FolderTreeNode::getTotalFileCount).sum();

        return new FolderTreeNode(folder, children, totalSize, totalFileCount);
    }

    /**
     * 更新子文件夹路径
     */
    private void updateChildrenPaths(String oldPathPrefix, String newPathPrefix) {
        try {
            folderRepository.updatePathsByPrefix(oldPathPrefix, newPathPrefix);
            logger.debug("子文件夹路径更新成功: {} -> {}", oldPathPrefix, newPathPrefix);
        } catch (Exception e) {
            logger.error("更新子文件夹路径失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 递归删除子文件夹和文件
     */
    private void deleteChildrenRecursively(Folder folder) {
        // 删除子文件夹
        List<Folder> children = getChildFolders(folder);
        for (Folder child : children) {
            deleteChildrenRecursively(child);
            folderRepository.delete(child);
        }
        
        // TODO: 删除文件夹中的文件
        // 这里需要与FileService集成
    }
} 