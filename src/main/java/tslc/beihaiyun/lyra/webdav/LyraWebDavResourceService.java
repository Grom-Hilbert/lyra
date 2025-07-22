package tslc.beihaiyun.lyra.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.FolderService;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.service.UserService;

/**
 * Lyra WebDAV 资源服务
 * 
 * 提供完整的WebDAV资源管理功能，集成现有的文件系统服务
 * 实现企业空间和个人空间的路径映射和操作
 * 
 * @author SkyFrost
 * @version 2.0.0
 * @since 2025-01-20
 */
@Service
public class LyraWebDavResourceService {

    private static final Logger logger = LoggerFactory.getLogger(LyraWebDavResourceService.class);

    private final UserService userService;
    private final FileService fileService;
    private final FolderService folderService;
    private final StorageService storageService;
    private final SpaceRepository spaceRepository;

    public LyraWebDavResourceService(
            UserService userService,
            FileService fileService,
            FolderService folderService,
            StorageService storageService,
            SpaceRepository spaceRepository) {
        this.userService = userService;
        this.fileService = fileService;
        this.folderService = folderService;
        this.storageService = storageService;
        this.spaceRepository = spaceRepository;
    }

    /**
     * 获取当前认证用户
     * 
     * @return 当前用户，如果未认证则返回null
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LyraUserPrincipal) {
            LyraUserPrincipal principal = (LyraUserPrincipal) auth.getPrincipal();
            return userService.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    /**
     * 解析WebDAV路径
     * 
     * @param path WebDAV路径，如 /webdav/personal/documents/file.txt
     * @return 解析后的路径信息
     */
    public WebDavPathInfo parsePath(String path) {
        logger.debug("解析WebDAV路径: {}", path);
        
        // 移除前缀 /webdav
        if (path.startsWith("/webdav")) {
            path = path.substring(7);
        }
        
        if (path.isEmpty() || path.equals("/")) {
            return new WebDavPathInfo(WebDavPathType.ROOT, null, null, "/");
        }
        
        String[] parts = path.split("/");
        if (parts.length < 2) {
            return new WebDavPathInfo(WebDavPathType.ROOT, null, null, path);
        }
        
        String spaceTypeStr = parts[1];
        String spacePath = "";
        String filePath = "";
        
        if (parts.length > 2) {
            // /personal/space1/folder/file.txt -> spacePath=space1, filePath=folder/file.txt
            spacePath = parts[2];
            if (parts.length > 3) {
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    if (sb.length() > 0) sb.append("/");
                    sb.append(parts[i]);
                }
                filePath = sb.toString();
            }
        }
        
        WebDavPathType pathType;
        if ("personal".equals(spaceTypeStr)) {
            pathType = WebDavPathType.PERSONAL;
        } else if ("enterprise".equals(spaceTypeStr)) {
            pathType = WebDavPathType.ENTERPRISE;
        } else {
            pathType = WebDavPathType.UNKNOWN;
        }
        
        return new WebDavPathInfo(pathType, spacePath, filePath, path);
    }

    /**
     * 获取WebDAV资源
     * 
     * @param path WebDAV路径
     * @return LyraResource对象，如果不存在则返回null
     */
    public LyraResource getResource(String path) {
        WebDavPathInfo pathInfo = parsePath(path);
        User currentUser = getCurrentUser();
        
        logger.debug("获取WebDAV资源: {} -> {}", path, pathInfo);
        
        try {
            switch (pathInfo.getType()) {
                case ROOT:
                    return createSystemRootResource();
                    
                case PERSONAL:
                case ENTERPRISE:
                    return getSpaceResource(pathInfo, currentUser);
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            logger.error("获取WebDAV资源失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查资源是否存在
     * 
     * @param path 文件路径
     * @return 是否存在
     */
    public boolean resourceExists(String path) {
        return getResource(path) != null;
    }

    /**
     * 获取文件内容
     * 
     * @param path 文件路径
     * @return 文件输入流
     * @throws IOException 读取异常
     */
    public InputStream getFileContent(String path) throws IOException {
        LyraResource resource = getResource(path);
        
        if (resource == null || !resource.isResource()) {
            throw new IOException("文件不存在或不是文件类型: " + path);
        }
        
        FileEntity fileEntity = resource.getFileEntity();
        if (fileEntity != null) {
            try {
                Optional<InputStream> inputStreamOpt = storageService.load(fileEntity.getStoragePath());
                if (inputStreamOpt.isPresent()) {
                    return inputStreamOpt.get();
                } else {
                    throw new IOException("无法读取文件内容: 存储路径不存在");
                }
            } catch (Exception e) {
                logger.error("获取文件内容失败: {}", e.getMessage(), e);
                throw new IOException("无法读取文件内容: " + e.getMessage());
            }
        }
        
        throw new IOException("文件内容不可用: " + path);
    }

    /**
     * 获取文件大小
     * 
     * @param path 文件路径
     * @return 文件大小，如果不是文件则返回0
     */
    public long getFileSize(String path) {
        LyraResource resource = getResource(path);
        return resource != null ? resource.getActualSize() : 0L;
    }

    /**
     * 检查路径是否为文件夹
     * 
     * @param path 路径
     * @return 是否为文件夹
     */
    public boolean isDirectory(String path) {
        LyraResource resource = getResource(path);
        return resource != null && resource.isCollection();
    }

    /**
     * 列出文件夹内容
     * 
     * @param path 文件夹路径
     * @return 文件夹内容列表
     */
    public List<WebDavResource> listDirectory(String path) {
        LyraResource resource = getResource(path);
        List<WebDavResource> resources = new ArrayList<>();
        
        if (resource == null || !resource.isCollection()) {
            logger.warn("目录不存在或不是集合类型: {}", path);
            return resources;
        }
        
        try {
            for (LyraResource child : resource.getChildren()) {
                resources.add(new WebDavResource(
                    child.getName(),
                    child.isCollection(),
                    child.getActualSize(),
                    child.getLastModified().getTime()
                ));
            }
            
            logger.debug("列出目录内容: {} -> {} 个项目", path, resources.size());
            
        } catch (Exception e) {
            logger.error("列出文件夹内容时出错: {}", e.getMessage(), e);
        }
        
        return resources;
    }

    /**
     * 创建文件夹
     * 
     * @param path 文件夹路径
     * @return 是否成功创建
     */
    public boolean createDirectory(String path) {
        WebDavPathInfo pathInfo = parsePath(path);
        User currentUser = getCurrentUser();
        
        logger.info("创建文件夹: {} -> {}", path, pathInfo);
        
        if (currentUser == null) {
            logger.warn("用户未认证，无法创建文件夹");
            return false;
        }
        
        try {
            Space space = findSpace(pathInfo, currentUser);
            if (space == null) {
                logger.warn("找不到对应的空间: {}", pathInfo);
                return false;
            }
            
            // 解析父文件夹路径
            Folder parentFolder = null;
            String folderName = extractLastPathComponent(pathInfo.getFilePath());
            String parentPath = extractParentPath(pathInfo.getFilePath());
            
            if (!parentPath.isEmpty()) {
                // 查找父文件夹
                parentFolder = folderService.getFolderByPath(space, parentPath).orElse(null);
                if (parentFolder == null) {
                    logger.warn("父文件夹不存在: {}", parentPath);
                    return false;
                }
            }
            
            // 创建文件夹
            FolderService.FolderOperationResult result = folderService.createFolder(
                folderName, parentFolder, space, currentUser.getId());
            
            return result.isSuccess();
            
        } catch (Exception e) {
            logger.error("创建文件夹时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传文件
     * 
     * @param path 文件路径
     * @param content 文件内容
     * @param contentLength 内容长度
     * @return 是否成功上传
     */
    public boolean uploadFile(String path, InputStream content, long contentLength) {
        WebDavPathInfo pathInfo = parsePath(path);
        User currentUser = getCurrentUser();
        
        logger.info("上传文件: {} (大小: {} 字节) -> {}", path, contentLength, pathInfo);
        
        if (currentUser == null) {
            logger.warn("用户未认证，无法上传文件");
            return false;
        }
        
        try {
            Space space = findSpace(pathInfo, currentUser);
            if (space == null) {
                logger.warn("找不到对应的空间: {}", pathInfo);
                return false;
            }
            
            // 解析文件夹路径
            Folder parentFolder = null;
            String fileName = extractLastPathComponent(pathInfo.getFilePath());
            String parentPath = extractParentPath(pathInfo.getFilePath());
            
            if (!parentPath.isEmpty()) {
                parentFolder = folderService.getFolderByPath(space, parentPath).orElse(null);
                if (parentFolder == null) {
                    logger.warn("父文件夹不存在: {}", parentPath);
                    return false;
                }
            }
            
            // 创建临时MultipartFile包装器进行上传
            WebDavMultipartFile multipartFile = new WebDavMultipartFile(fileName, content, contentLength);
            
            FileService.FileOperationResult result = fileService.uploadFile(
                multipartFile, space, parentFolder, currentUser.getId());
            
            return result.isSuccess();
            
        } catch (Exception e) {
            logger.error("上传文件时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除资源
     * 
     * @param path 资源路径
     * @return 是否成功删除
     */
    public boolean deleteResource(String path) {
        WebDavPathInfo pathInfo = parsePath(path);
        User currentUser = getCurrentUser();
        
        logger.info("删除资源: {} -> {}", path, pathInfo);
        
        if (currentUser == null) {
            logger.warn("用户未认证，无法删除资源");
            return false;
        }
        
        try {
            LyraResource resource = getResource(path);
            if (resource == null) {
                logger.warn("资源不存在: {}", path);
                return false;
            }
            
            if (resource.isResource() && resource.getFileEntity() != null) {
                // 删除文件
                boolean result = fileService.deleteFile(
                    resource.getFileEntity().getId(), currentUser.getId());
                return result;
                
            } else if (resource.isCollection() && resource.getFolder() != null) {
                // 删除文件夹
                boolean result = folderService.deleteFolder(
                    resource.getFolder().getId(), currentUser.getId(), false);
                return result;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("删除资源时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    // 私有辅助方法

    /**
     * 创建系统根目录资源
     */
    private LyraResource createSystemRootResource() {
        List<LyraResource> children = new ArrayList<>();
        
        // 添加个人空间和企业空间入口
        children.add(LyraResource.spaceRoot()
                .name("personal")
                .path("/webdav/personal")
                .spaceType(LyraResource.SpaceType.PERSONAL)
                .build());
                
        children.add(LyraResource.spaceRoot()
                .name("enterprise")
                .path("/webdav/enterprise")
                .spaceType(LyraResource.SpaceType.ENTERPRISE)
                .build());
        
        return LyraResource.systemRoot()
                .name("")
                .path("/webdav")
                .spaceType(LyraResource.SpaceType.SYSTEM)
                .lastModified(new Date())
                .creationDate(new Date())
                .children(children)
                .build();
    }

    /**
     * 获取空间资源
     */
    private LyraResource getSpaceResource(WebDavPathInfo pathInfo, User currentUser) {
        if (pathInfo.getSpacePath() == null || pathInfo.getSpacePath().isEmpty()) {
            // 返回空间列表
            return createSpaceListResource(pathInfo, currentUser);
        }
        
        // 查找具体空间
        Space space = findSpace(pathInfo, currentUser);
        if (space == null) {
            return null;
        }
        
        if (pathInfo.getFilePath() == null || pathInfo.getFilePath().isEmpty()) {
            // 返回空间根目录
            return createSpaceRootResource(space, pathInfo);
        }
        
        // 返回空间内的文件或文件夹
        return createFileSystemResource(space, pathInfo);
    }

    /**
     * 创建空间列表资源
     */
    private LyraResource createSpaceListResource(WebDavPathInfo pathInfo, User currentUser) {
        List<LyraResource> children = new ArrayList<>();
        
        if (currentUser != null) {
            Space.SpaceType spaceType = pathInfo.getType() == WebDavPathType.PERSONAL ? 
                                      Space.SpaceType.PERSONAL : Space.SpaceType.ENTERPRISE;
            
            List<Space> spaces = spaceRepository.findByOwnerAndType(currentUser, spaceType);
            
            for (Space space : spaces) {
                String childPath = "/webdav/" + pathInfo.getType().name().toLowerCase() + "/" + space.getName();
                children.add(LyraResource.fromSpace(space, childPath));
            }
        }
        
        LyraResource.SpaceType resourceSpaceType = pathInfo.getType() == WebDavPathType.PERSONAL ? 
                                                   LyraResource.SpaceType.PERSONAL : LyraResource.SpaceType.ENTERPRISE;
        
        return LyraResource.spaceRoot()
                .name(pathInfo.getType().name().toLowerCase())
                .path("/webdav/" + pathInfo.getType().name().toLowerCase())
                .spaceType(resourceSpaceType)
                .children(children)
                .build();
    }

    /**
     * 创建空间根目录资源
     */
    private LyraResource createSpaceRootResource(Space space, WebDavPathInfo pathInfo) {
        List<LyraResource> children = new ArrayList<>();
        String basePath = "/webdav/" + pathInfo.getType().name().toLowerCase() + "/" + space.getName();
        
        // 获取根文件夹列表
        List<Folder> rootFolders = folderService.getRootFolders(space);
        for (Folder folder : rootFolders) {
            String childPath = basePath + "/" + folder.getName();
            children.add(LyraResource.fromFolder(folder, childPath));
        }
        
        // 获取根文件列表 - 需要实现在服务层
        List<FileEntity> rootFiles = fileService.getFilesBySpace(space, false)
                .stream()
                .filter(file -> file.getFolder() == null)
                .limit(100)
                .toList();
        for (FileEntity file : rootFiles) {
            String childPath = basePath + "/" + file.getName();
            children.add(LyraResource.fromFileEntity(file, childPath));
        }
        
        return LyraResource.fromSpace(space, basePath)
                .toBuilder()
                .children(children)
                .build();
    }

    /**
     * 创建文件系统资源
     */
    private LyraResource createFileSystemResource(Space space, WebDavPathInfo pathInfo) {
        String filePath = pathInfo.getFilePath();
        
        // 首先尝试查找文件
        Optional<FileEntity> fileOpt = fileService.getFileByPath(space, filePath);
        if (fileOpt.isPresent()) {
            String fullPath = "/webdav/" + pathInfo.getType().name().toLowerCase() + 
                            "/" + pathInfo.getSpacePath() + "/" + filePath;
            return LyraResource.fromFileEntity(fileOpt.get(), fullPath);
        }
        
        // 然后尝试查找文件夹
        Optional<Folder> folderOpt = folderService.getFolderByPath(space, filePath);
        if (folderOpt.isPresent()) {
            Folder folder = folderOpt.get();
            String fullPath = "/webdav/" + pathInfo.getType().name().toLowerCase() + 
                            "/" + pathInfo.getSpacePath() + "/" + filePath;
            
            // 获取子资源
            List<LyraResource> children = new ArrayList<>();
            String basePath = fullPath;
            
            // 获取子文件夹
            List<Folder> subFolders = folderService.getChildFolders(folder);
            for (Folder subFolder : subFolders) {
                String childPath = basePath + "/" + subFolder.getName();
                children.add(LyraResource.fromFolder(subFolder, childPath));
            }
            
            // 获取文件 - 需要实现在服务层
            List<FileEntity> files = fileService.getFilesBySpace(space, false)
                    .stream()
                    .filter(file -> folder.equals(file.getFolder()))
                    .limit(100)
                    .toList();
            for (FileEntity file : files) {
                String childPath = basePath + "/" + file.getName();
                children.add(LyraResource.fromFileEntity(file, childPath));
            }
            
            return LyraResource.fromFolder(folder, fullPath)
                    .toBuilder()
                    .children(children)
                    .build();
        }
        
        return null;
    }

    /**
     * 查找空间
     */
    private Space findSpace(WebDavPathInfo pathInfo, User currentUser) {
        if (currentUser == null || pathInfo.getSpacePath() == null) {
            return null;
        }
        
        Space.SpaceType spaceType = pathInfo.getType() == WebDavPathType.PERSONAL ? 
                                  Space.SpaceType.PERSONAL : Space.SpaceType.ENTERPRISE;
        
        // 查找用户的对应类型的空间，然后按名称过滤
        List<Space> spaces = spaceRepository.findByOwnerAndType(currentUser, spaceType);
        return spaces.stream()
                .filter(space -> pathInfo.getSpacePath().equals(space.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 提取路径的最后一个组件
     */
    private String extractLastPathComponent(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 提取父路径
     */
    private String extractParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }

    /**
     * WebDAV 路径信息
     */
    public static class WebDavPathInfo {
        private final WebDavPathType type;
        private final String spacePath;
        private final String filePath;
        private final String fullPath;

        public WebDavPathInfo(WebDavPathType type, String spacePath, String filePath, String fullPath) {
            this.type = type;
            this.spacePath = spacePath;
            this.filePath = filePath;
            this.fullPath = fullPath;
        }

        public WebDavPathType getType() { return type; }
        public String getSpacePath() { return spacePath; }
        public String getFilePath() { return filePath; }
        public String getFullPath() { return fullPath; }

        @Override
        public String toString() {
            return String.format("WebDavPathInfo{type=%s, spacePath='%s', filePath='%s', fullPath='%s'}",
                               type, spacePath, filePath, fullPath);
        }
    }

    /**
     * WebDAV 路径类型
     */
    public enum WebDavPathType {
        ROOT,       // 根目录
        PERSONAL,   // 个人空间
        ENTERPRISE, // 企业空间
        UNKNOWN     // 未知路径
    }

    /**
     * WebDAV 资源
     */
    public static class WebDavResource {
        private final String name;
        private final boolean isDirectory;
        private final long size;
        private final long lastModified;

        public WebDavResource(String name, boolean isDirectory, long size, long lastModified) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
    }
} 