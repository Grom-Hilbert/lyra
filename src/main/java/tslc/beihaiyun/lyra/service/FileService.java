package tslc.beihaiyun.lyra.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.User;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 文件服务接口
 * 定义文件管理的核心业务逻辑
 */
public interface FileService {

    /**
     * 上传文件
     */
    FileEntity uploadFile(MultipartFile file, String path, Long folderId, User owner);

    /**
     * 批量上传文件
     */
    List<FileEntity> uploadFiles(List<MultipartFile> files, String basePath, Long folderId, User owner);

    /**
     * 下载文件
     */
    InputStream downloadFile(Long fileId, User user);

    /**
     * 根据ID查找文件
     */
    Optional<FileEntity> findById(Long id);

    /**
     * 根据路径查找文件
     */
    Optional<FileEntity> findByPath(String path);

    /**
     * 获取文件夹下的文件列表
     */
    Page<FileEntity> getFilesByFolder(Long folderId, Pageable pageable);

    /**
     * 搜索文件
     */
    List<FileEntity> searchFiles(String keyword, User user);

    /**
     * 移动文件
     */
    FileEntity moveFile(Long fileId, Long targetFolderId, User user);

    /**
     * 复制文件
     */
    FileEntity copyFile(Long fileId, Long targetFolderId, String newName, User user);

    /**
     * 重命名文件
     */
    FileEntity renameFile(Long fileId, String newName, User user);

    /**
     * 删除文件
     */
    void deleteFile(Long fileId, User user);

    /**
     * 获取文件版本历史
     */
    List<FileVersion> getFileVersions(Long fileId);

    /**
     * 创建文件新版本
     */
    FileVersion createNewVersion(Long fileId, MultipartFile file, String description, User user);

    /**
     * 恢复到指定版本
     */
    FileEntity restoreToVersion(Long fileId, Integer versionNumber, User user);

    /**
     * 获取文件内容预览
     */
    String getFilePreview(Long fileId, User user);

    /**
     * 更新文件内容（文本文件）
     */
    FileEntity updateFileContent(Long fileId, String content, User user);

    /**
     * 检查用户对文件的访问权限
     */
    boolean hasReadPermission(Long fileId, User user);

    /**
     * 检查用户对文件的写入权限
     */
    boolean hasWritePermission(Long fileId, User user);

    /**
     * 检查用户对文件的删除权限
     */
    boolean hasDeletePermission(Long fileId, User user);

    /**
     * 获取用户的文件统计信息
     */
    FileStatistics getUserFileStatistics(User user);

    /**
     * 文件统计信息类
     */
    class FileStatistics {
        private Long totalFiles;
        private Long totalSize;
        private Long enterpriseFiles;
        private Long personalFiles;

        // 构造函数、getter和setter
        public FileStatistics(Long totalFiles, Long totalSize, Long enterpriseFiles, Long personalFiles) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.enterpriseFiles = enterpriseFiles;
            this.personalFiles = personalFiles;
        }

        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
        
        public Long getEnterpriseFiles() { return enterpriseFiles; }
        public void setEnterpriseFiles(Long enterpriseFiles) { this.enterpriseFiles = enterpriseFiles; }
        
        public Long getPersonalFiles() { return personalFiles; }
        public void setPersonalFiles(Long personalFiles) { this.personalFiles = personalFiles; }
    }
}