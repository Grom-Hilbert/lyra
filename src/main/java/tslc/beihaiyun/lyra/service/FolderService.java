package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FolderEntity;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹服务接口
 * 定义文件夹管理的核心业务逻辑
 */
public interface FolderService {

    /**
     * 创建文件夹
     */
    FolderEntity createFolder(String name, String description, Long parentId, 
                             FileEntity.SpaceType spaceType, User owner);

    /**
     * 根据ID查找文件夹
     */
    Optional<FolderEntity> findById(Long id);

    /**
     * 根据路径查找文件夹
     */
    Optional<FolderEntity> findByPath(String path);

    /**
     * 获取子文件夹列表
     */
    List<FolderEntity> getSubFolders(Long parentId);

    /**
     * 获取用户的根文件夹
     */
    List<FolderEntity> getRootFolders(User user, FileEntity.SpaceType spaceType);

    /**
     * 移动文件夹
     */
    FolderEntity moveFolder(Long folderId, Long targetParentId, User user);

    /**
     * 重命名文件夹
     */
    FolderEntity renameFolder(Long folderId, String newName, User user);

    /**
     * 删除文件夹
     */
    void deleteFolder(Long folderId, User user);

    /**
     * 复制文件夹
     */
    FolderEntity copyFolder(Long folderId, Long targetParentId, String newName, User user);

    /**
     * 获取文件夹的完整路径
     */
    String getFullPath(Long folderId);

    /**
     * 获取文件夹的祖先路径
     */
    List<FolderEntity> getAncestors(Long folderId);

    /**
     * 搜索文件夹
     */
    List<FolderEntity> searchFolders(String keyword, User user);

    /**
     * 检查用户对文件夹的读取权限
     */
    boolean hasReadPermission(Long folderId, User user);

    /**
     * 检查用户对文件夹的写入权限
     */
    boolean hasWritePermission(Long folderId, User user);

    /**
     * 检查用户对文件夹的删除权限
     */
    boolean hasDeletePermission(Long folderId, User user);

    /**
     * 获取文件夹统计信息
     */
    FolderStatistics getFolderStatistics(Long folderId);

    /**
     * 初始化用户默认文件夹结构
     */
    void initializeUserFolders(User user);

    /**
     * 文件夹统计信息类
     */
    class FolderStatistics {
        private Long totalSubFolders;
        private Long totalFiles;
        private Long totalSize;

        public FolderStatistics(Long totalSubFolders, Long totalFiles, Long totalSize) {
            this.totalSubFolders = totalSubFolders;
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
        }

        public Long getTotalSubFolders() { return totalSubFolders; }
        public void setTotalSubFolders(Long totalSubFolders) { this.totalSubFolders = totalSubFolders; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    }
}