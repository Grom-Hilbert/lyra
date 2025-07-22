package tslc.beihaiyun.lyra.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.VersionService;

/**
 * WebDAV 版本控制服务
 * 
 * 提供WebDAV协议的版本控制扩展功能
 * 包括版本历史访问、版本比较、版本回滚等操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class WebDavVersionControlService {

    private static final Logger logger = LoggerFactory.getLogger(WebDavVersionControlService.class);

    private final VersionService versionService;
    private final FileService fileService;
    private final LyraWebDavResourceService resourceService;

    public WebDavVersionControlService(
            VersionService versionService,
            FileService fileService,
            LyraWebDavResourceService resourceService) {
        this.versionService = versionService;
        this.fileService = fileService;
        this.resourceService = resourceService;
    }

    /**
     * 版本历史访问路径格式：
     * /webdav/versions/{space_type}/{space_name}/{file_path}
     * 例如：/webdav/versions/personal/my-space/docs/readme.txt
     */
    public static final String VERSION_PATH_PREFIX = "/webdav/versions/";

    /**
     * 特定版本访问路径格式：
     * /webdav/versions/{space_type}/{space_name}/{file_path}@{version_number}
     * 例如：/webdav/versions/personal/my-space/docs/readme.txt@5
     */
    public static final String VERSION_ACCESS_PATTERN = "@";

    /**
     * 检查路径是否为版本控制路径
     * 
     * @param path WebDAV路径
     * @return 是否为版本控制路径
     */
    public boolean isVersionControlPath(String path) {
        return path != null && path.startsWith(VERSION_PATH_PREFIX);
    }

    /**
     * 解析版本控制路径
     * 
     * @param path WebDAV版本控制路径
     * @return 版本路径信息
     */
    public VersionPathInfo parseVersionPath(String path) {
        if (!isVersionControlPath(path)) {
            return null;
        }

        String versionPath = path.substring(VERSION_PATH_PREFIX.length());
        
        // 检查是否包含版本号
        String filePath;
        Integer versionNumber = null;
        
        int versionIndex = versionPath.lastIndexOf(VERSION_ACCESS_PATTERN);
        if (versionIndex > 0) {
            String versionStr = versionPath.substring(versionIndex + 1);
            try {
                versionNumber = Integer.parseInt(versionStr);
                filePath = versionPath.substring(0, versionIndex);
            } catch (NumberFormatException e) {
                filePath = versionPath;
            }
        } else {
            filePath = versionPath;
        }

        // 解析原始文件路径
        String originalPath = "/webdav/" + filePath;
        
        return new VersionPathInfo(originalPath, versionNumber, path);
    }

    /**
     * 获取版本历史列表
     * 
     * @param originalPath 原始文件路径
     * @param currentUser 当前用户
     * @return 版本历史资源列表
     */
    public List<LyraResource> getVersionHistoryList(String originalPath, User currentUser) {
        List<LyraResource> versionResources = new ArrayList<>();

        try {
            // 获取原始文件
            LyraResource originalResource = resourceService.getResource(originalPath);
            if (originalResource == null || !originalResource.isResource()) {
                logger.warn("原始文件不存在或不是文件: {}", originalPath);
                return versionResources;
            }

            FileEntity fileEntity = originalResource.getFileEntity();
            if (fileEntity == null) {
                return versionResources;
            }

            // 获取所有版本
            List<FileVersion> versions = versionService.getAllVersions(fileEntity, true);
            
            for (FileVersion version : versions) {
                String versionPath = createVersionPath(originalPath, version.getVersionNumber());
                
                LyraResource versionResource = LyraResource.file()
                        .name(fileEntity.getName() + "@" + version.getVersionNumber())
                        .path(versionPath)
                        .href(versionPath)
                        .size(version.getSizeBytes())
                        .contentType(fileEntity.getMimeType())
                        .lastModified(java.util.Date.from(version.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .creationDate(java.util.Date.from(version.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .etag("\"" + Math.abs(java.util.Objects.hash(version.getId(), version.getCreatedAt())) + "\"")
                        .fileEntity(fileEntity)
                        .currentVersionNumber(version.getVersionNumber())
                        .totalVersionCount((long) versions.size())
                        .latestVersionComment(version.getChangeComment())
                        .latestVersionDate(java.util.Date.from(version.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .build();
                
                versionResources.add(versionResource);
            }

            logger.debug("获取版本历史成功: 文件={}, 版本数={}", originalPath, versionResources.size());

        } catch (Exception e) {
            logger.error("获取版本历史失败: 文件={}", originalPath, e);
        }

        return versionResources;
    }

    /**
     * 获取特定版本的文件内容
     * 
     * @param originalPath 原始文件路径
     * @param versionNumber 版本号
     * @param currentUser 当前用户
     * @return 版本文件输入流
     * @throws IOException IO异常
     */
    public Optional<InputStream> getVersionContent(String originalPath, Integer versionNumber, User currentUser) throws IOException {
        try {
            // 获取原始文件
            LyraResource originalResource = resourceService.getResource(originalPath);
            if (originalResource == null || !originalResource.isResource()) {
                return Optional.empty();
            }

            FileEntity fileEntity = originalResource.getFileEntity();
            if (fileEntity == null) {
                return Optional.empty();
            }

            // 获取指定版本内容
            return versionService.getVersionContent(fileEntity, versionNumber);

        } catch (Exception e) {
            logger.error("获取版本内容失败: 文件={}, 版本={}", originalPath, versionNumber, e);
            throw new IOException("获取版本内容失败: " + e.getMessage());
        }
    }

    /**
     * 回滚文件到指定版本
     * 
     * @param originalPath 原始文件路径
     * @param targetVersionNumber 目标版本号
     * @param currentUser 当前用户
     * @param createNewVersion 是否创建新版本
     * @return 操作是否成功
     */
    public boolean rollbackToVersion(String originalPath, Integer targetVersionNumber, User currentUser, boolean createNewVersion) {
        try {
            // 获取原始文件
            LyraResource originalResource = resourceService.getResource(originalPath);
            if (originalResource == null || !originalResource.isResource()) {
                logger.warn("原始文件不存在或不是文件: {}", originalPath);
                return false;
            }

            FileEntity fileEntity = originalResource.getFileEntity();
            if (fileEntity == null) {
                return false;
            }

            // 执行回滚
            VersionService.VersionOperationResult result = versionService.rollbackToVersion(
                    fileEntity, targetVersionNumber, currentUser.getId(), createNewVersion);

            if (result.isSuccess()) {
                logger.info("WebDAV版本回滚成功: 文件={}, 目标版本={}, 用户={}", 
                           originalPath, targetVersionNumber, currentUser.getId());
                return true;
            } else {
                logger.warn("WebDAV版本回滚失败: 文件={}, 目标版本={}, 错误={}", 
                           originalPath, targetVersionNumber, result.getMessage());
                return false;
            }

        } catch (Exception e) {
            logger.error("WebDAV版本回滚异常: 文件={}, 目标版本={}", originalPath, targetVersionNumber, e);
            return false;
        }
    }

    /**
     * 获取版本比较信息
     * 
     * @param originalPath 原始文件路径
     * @param fromVersion 源版本
     * @param toVersion 目标版本
     * @param currentUser 当前用户
     * @return 版本比较结果
     */
    public Optional<String> compareVersions(String originalPath, Integer fromVersion, Integer toVersion, User currentUser) {
        try {
            // 获取原始文件
            LyraResource originalResource = resourceService.getResource(originalPath);
            if (originalResource == null || !originalResource.isResource()) {
                return Optional.empty();
            }

            FileEntity fileEntity = originalResource.getFileEntity();
            if (fileEntity == null) {
                return Optional.empty();
            }

            // 执行版本比较
            return versionService.compareVersions(fileEntity, fromVersion, toVersion);

        } catch (Exception e) {
            logger.error("版本比较失败: 文件={}, 从版本={}, 到版本={}", originalPath, fromVersion, toVersion, e);
            return Optional.empty();
        }
    }

    /**
     * 创建版本访问路径
     * 
     * @param originalPath 原始文件路径
     * @param versionNumber 版本号
     * @return 版本访问路径
     */
    private String createVersionPath(String originalPath, Integer versionNumber) {
        // 将 /webdav/... 转换为 /webdav/versions/...@version
        String filePath = originalPath.substring("/webdav/".length());
        return VERSION_PATH_PREFIX + filePath + VERSION_ACCESS_PATTERN + versionNumber;
    }

    /**
     * 版本路径信息
     */
    public static class VersionPathInfo {
        private final String originalPath;
        private final Integer versionNumber;
        private final String versionPath;

        public VersionPathInfo(String originalPath, Integer versionNumber, String versionPath) {
            this.originalPath = originalPath;
            this.versionNumber = versionNumber;
            this.versionPath = versionPath;
        }

        public String getOriginalPath() { return originalPath; }
        public Integer getVersionNumber() { return versionNumber; }
        public String getVersionPath() { return versionPath; }
        public boolean hasVersionNumber() { return versionNumber != null; }

        @Override
        public String toString() {
            return String.format("VersionPathInfo{originalPath='%s', versionNumber=%s, versionPath='%s'}",
                               originalPath, versionNumber, versionPath);
        }
    }
} 