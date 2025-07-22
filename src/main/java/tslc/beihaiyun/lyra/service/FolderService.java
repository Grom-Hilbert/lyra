package tslc.beihaiyun.lyra.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹管理服务接口
 * 提供文件夹的完整生命周期管理，包括层级管理、批量操作、权限继承等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public interface FolderService {

    /**
     * 文件夹操作结果
     */
    class FolderOperationResult {
        private final boolean success;
        private final String message;
        private final Folder folder;
        private final Exception exception;

        public FolderOperationResult(boolean success, String message, Folder folder) {
            this.success = success;
            this.message = message;
            this.folder = folder;
            this.exception = null;
        }

        public FolderOperationResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.folder = null;
            this.exception = exception;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Folder getFolder() { return folder; }
        public Exception getException() { return exception; }
    }

    /**
     * 批量操作结果
     */
    class BatchFolderOperationResult {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final List<String> errorMessages;

        public BatchFolderOperationResult(int totalCount, int successCount, int failureCount, List<String> errorMessages) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessages = errorMessages;
        }

        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getErrorMessages() { return errorMessages; }
        public boolean isAllSuccess() { return failureCount == 0; }
        public double getSuccessRate() { return totalCount > 0 ? (double) successCount / totalCount : 0; }
    }

    /**
     * 文件夹树节点
     */
    class FolderTreeNode {
        private final Folder folder;
        private final List<FolderTreeNode> children;
        private final long totalSize;
        private final int totalFileCount;

        public FolderTreeNode(Folder folder, List<FolderTreeNode> children, long totalSize, int totalFileCount) {
            this.folder = folder;
            this.children = children;
            this.totalSize = totalSize;
            this.totalFileCount = totalFileCount;
        }

        public Folder getFolder() { return folder; }
        public List<FolderTreeNode> getChildren() { return children; }
        public long getTotalSize() { return totalSize; }
        public int getTotalFileCount() { return totalFileCount; }
    }

    /**
     * 文件夹统计信息
     */
    class FolderStatistics {
        private final long totalFolders;
        private final long totalSize;
        private final int maxDepth;
        private final long emptyFolders;
        private final long fileCount;
        private final Folder largestFolder;

        public FolderStatistics(long totalFolders, long totalSize, int maxDepth, long emptyFolders, long fileCount, Folder largestFolder) {
            this.totalFolders = totalFolders;
            this.totalSize = totalSize;
            this.maxDepth = maxDepth;
            this.emptyFolders = emptyFolders;
            this.fileCount = fileCount;
            this.largestFolder = largestFolder;
        }

        public long getTotalFolders() { return totalFolders; }
        public long getTotalSize() { return totalSize; }
        public int getMaxDepth() { return maxDepth; }
        public long getEmptyFolders() { return emptyFolders; }
        public long getFileCount() { return fileCount; }
        public Folder getLargestFolder() { return largestFolder; }
    }

    // ==================== 基础CRUD操作 ====================

    /**
     * 创建文件夹
     * 
     * @param name 文件夹名称
     * @param parentFolder 父文件夹（null表示根目录）
     * @param space 所属空间
     * @param creatorId 创建者ID
     * @return 文件夹操作结果
     */
    FolderOperationResult createFolder(String name, Folder parentFolder, Space space, Long creatorId);

    /**
     * 根据ID获取文件夹
     * 
     * @param folderId 文件夹ID
     * @return 文件夹实体（可选）
     */
    Optional<Folder> getFolderById(Long folderId);

    /**
     * 根据空间和路径获取文件夹
     * 
     * @param space 所属空间
     * @param path 文件夹路径
     * @return 文件夹实体（可选）
     */
    Optional<Folder> getFolderByPath(Space space, String path);

    /**
     * 更新文件夹信息
     * 
     * @param folderId 文件夹ID
     * @param newName 新名称（可选）
     * @param updaterId 更新者ID
     * @return 文件夹操作结果
     */
    FolderOperationResult updateFolderInfo(Long folderId, String newName, Long updaterId);

    /**
     * 删除文件夹
     * 
     * @param folderId 文件夹ID
     * @param deleterId 删除者ID
     * @param force 是否强制删除（包含子文件夹和文件）
     * @return 操作是否成功
     */
    boolean deleteFolder(Long folderId, Long deleterId, boolean force);

    // ==================== 层级管理 ====================

    /**
     * 获取子文件夹列表
     * 
     * @param parentFolder 父文件夹
     * @return 子文件夹列表
     */
    List<Folder> getChildFolders(Folder parentFolder);

    /**
     * 获取空间根文件夹列表
     * 
     * @param space 所属空间
     * @return 根文件夹列表
     */
    List<Folder> getRootFolders(Space space);

    /**
     * 获取文件夹的所有祖先路径
     * 
     * @param folder 文件夹
     * @return 祖先文件夹列表（从根到父级）
     */
    List<Folder> getAncestorFolders(Folder folder);

    /**
     * 获取文件夹的所有后代
     * 
     * @param folder 文件夹
     * @return 后代文件夹列表
     */
    List<Folder> getDescendantFolders(Folder folder);

    /**
     * 构建文件夹树
     * 
     * @param space 所属空间
     * @param maxDepth 最大深度（-1表示无限制）
     * @return 文件夹树节点列表
     */
    List<FolderTreeNode> buildFolderTree(Space space, int maxDepth);

    /**
     * 移动文件夹
     * 
     * @param folderId 文件夹ID
     * @param targetParent 目标父文件夹（null表示根目录）
     * @param operatorId 操作者ID
     * @return 文件夹操作结果
     */
    FolderOperationResult moveFolder(Long folderId, Folder targetParent, Long operatorId);

    /**
     * 重命名文件夹
     * 
     * @param folderId 文件夹ID
     * @param newName 新名称
     * @param operatorId 操作者ID
     * @return 文件夹操作结果
     */
    FolderOperationResult renameFolder(Long folderId, String newName, Long operatorId);

    // ==================== 批量操作 ====================

    /**
     * 批量创建文件夹
     * 
     * @param folderNames 文件夹名称列表
     * @param parentFolder 父文件夹
     * @param space 所属空间
     * @param creatorId 创建者ID
     * @return 批量操作结果
     */
    BatchFolderOperationResult batchCreateFolders(List<String> folderNames, Folder parentFolder, Space space, Long creatorId);

    /**
     * 批量删除文件夹
     * 
     * @param folderIds 文件夹ID列表
     * @param deleterId 删除者ID
     * @param force 是否强制删除
     * @return 批量操作结果
     */
    BatchFolderOperationResult batchDeleteFolders(List<Long> folderIds, Long deleterId, boolean force);

    /**
     * 批量移动文件夹
     * 
     * @param folderIds 文件夹ID列表
     * @param targetParent 目标父文件夹
     * @param operatorId 操作者ID
     * @return 批量操作结果
     */
    BatchFolderOperationResult batchMoveFolders(List<Long> folderIds, Folder targetParent, Long operatorId);

    // ==================== 查询和搜索 ====================

    /**
     * 分页查询文件夹
     * 
     * @param space 所属空间
     * @param parentFolder 父文件夹（null表示根级别）
     * @param pageable 分页参数
     * @return 文件夹分页结果
     */
    Page<Folder> getFoldersPaged(Space space, Folder parentFolder, Pageable pageable);

    /**
     * 搜索文件夹
     * 
     * @param space 搜索空间
     * @param keyword 关键字
     * @return 文件夹列表
     */
    List<Folder> searchFolders(Space space, String keyword);

    /**
     * 获取空文件夹列表
     * 
     * @param space 所属空间
     * @return 空文件夹列表
     */
    List<Folder> getEmptyFolders(Space space);

    /**
     * 获取大文件夹列表
     * 
     * @param space 所属空间
     * @param sizeThreshold 大小阈值（字节）
     * @return 大文件夹列表
     */
    List<Folder> getLargeFolders(Space space, long sizeThreshold);

    /**
     * 获取深层文件夹列表
     * 
     * @param space 所属空间
     * @param levelThreshold 层级阈值
     * @return 深层文件夹列表
     */
    List<Folder> getDeepFolders(Space space, int levelThreshold);

    // ==================== 统计和信息 ====================

    /**
     * 获取空间文件夹统计信息
     * 
     * @param space 所属空间
     * @return 文件夹统计信息
     */
    FolderStatistics getFolderStatistics(Space space);

    /**
     * 计算文件夹大小（包含所有子文件夹和文件）
     * 
     * @param folder 文件夹
     * @param includeSubfolders 是否包含子文件夹
     * @return 文件夹大小（字节）
     */
    long calculateFolderSize(Folder folder, boolean includeSubfolders);

    /**
     * 更新文件夹统计信息
     * 
     * @param folderId 文件夹ID
     * @return 操作是否成功
     */
    boolean updateFolderStatistics(Long folderId);

    /**
     * 检查文件夹名称是否已存在
     * 
     * @param parentFolder 父文件夹
     * @param name 文件夹名称
     * @param excludeFolderId 排除的文件夹ID（用于重命名时检查）
     * @return 是否已存在
     */
    boolean isFolderNameExists(Folder parentFolder, String name, Long excludeFolderId);

    /**
     * 检查文件夹是否为空
     * 
     * @param folderId 文件夹ID
     * @return 是否为空
     */
    boolean isFolderEmpty(Long folderId);

    /**
     * 检查是否可以移动文件夹（避免循环引用）
     * 
     * @param sourceFolder 源文件夹
     * @param targetParent 目标父文件夹
     * @return 是否可以移动
     */
    boolean canMoveFolder(Folder sourceFolder, Folder targetParent);

    // ==================== 权限和版本控制 ====================

    /**
     * 设置文件夹版本控制模式
     * 
     * @param folderId 文件夹ID
     * @param versionControlEnabled 是否启用版本控制
     * @param inheritFromParent 是否从父文件夹继承
     * @param operatorId 操作者ID
     * @return 操作是否成功
     */
    boolean setVersionControlMode(Long folderId, boolean versionControlEnabled, boolean inheritFromParent, Long operatorId);

    /**
     * 继承父文件夹的权限设置
     * 
     * @param folderId 文件夹ID
     * @param operatorId 操作者ID
     * @return 操作是否成功
     */
    boolean inheritParentPermissions(Long folderId, Long operatorId);

    /**
     * 应用权限到所有子文件夹
     * 
     * @param folderId 文件夹ID
     * @param operatorId 操作者ID
     * @return 操作是否成功
     */
    boolean applyPermissionsToChildren(Long folderId, Long operatorId);
} 