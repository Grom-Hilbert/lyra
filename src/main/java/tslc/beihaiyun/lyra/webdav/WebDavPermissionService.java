package tslc.beihaiyun.lyra.webdav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.PermissionService;
import tslc.beihaiyun.lyra.service.UserService;

/**
 * WebDAV 权限检查服务
 * 
 * 复用现有的权限系统，为WebDAV操作提供细粒度的权限控制
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Service
public class WebDavPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(WebDavPermissionService.class);

    // WebDAV 操作权限代码
    public static final String WEBDAV_READ = "webdav.read";
    public static final String WEBDAV_WRITE = "webdav.write";
    public static final String WEBDAV_DELETE = "webdav.delete";
    public static final String WEBDAV_CREATE = "webdav.create";
    public static final String WEBDAV_MOVE = "webdav.move";
    public static final String WEBDAV_COPY = "webdav.copy";
    public static final String WEBDAV_LOCK = "webdav.lock";
    public static final String WEBDAV_PROPERTIES = "webdav.properties";

    // 文件系统权限代码
    public static final String FILE_READ = "file.read";
    public static final String FILE_WRITE = "file.write";
    public static final String FILE_DELETE = "file.delete";
    public static final String FILE_CREATE = "file.create";
    public static final String FOLDER_READ = "folder.read";
    public static final String FOLDER_WRITE = "folder.write";
    public static final String FOLDER_DELETE = "folder.delete";
    public static final String FOLDER_CREATE = "folder.create";
    public static final String SPACE_READ = "space.read";
    public static final String SPACE_WRITE = "space.write";

    private final PermissionService permissionService;
    private final UserService userService;

    public WebDavPermissionService(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
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
     * 检查WebDAV读取权限
     * 
     * @param space 空间
     * @param folder 文件夹（可为null）
     * @param file 文件（可为null）
     * @return 是否有读取权限
     */
    public boolean hasReadPermission(Space space, Folder folder, FileEntity file) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝读取权限");
            return false;
        }

        try {
            // 检查空间级别的读取权限
            if (!hasSpacePermission(currentUser, space, SPACE_READ)) {
                logger.debug("用户[{}]在空间[{}]中无读取权限", currentUser.getUsername(), space.getName());
                return false;
            }

            if (file != null) {
                // 检查文件读取权限
                return hasFilePermission(currentUser, space, file, FILE_READ);
            } else if (folder != null) {
                // 检查文件夹读取权限
                return hasFolderPermission(currentUser, space, folder, FOLDER_READ);
            } else {
                // 空间根目录读取权限
                return true;
            }

        } catch (Exception e) {
            logger.error("检查读取权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV写入权限
     * 
     * @param space 空间
     * @param folder 文件夹（可为null）
     * @param file 文件（可为null）
     * @return 是否有写入权限
     */
    public boolean hasWritePermission(Space space, Folder folder, FileEntity file) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝写入权限");
            return false;
        }

        try {
            // 检查空间级别的写入权限
            if (!hasSpacePermission(currentUser, space, SPACE_WRITE)) {
                logger.debug("用户[{}]在空间[{}]中无写入权限", currentUser.getUsername(), space.getName());
                return false;
            }

            if (file != null) {
                // 检查文件写入权限
                return hasFilePermission(currentUser, space, file, FILE_WRITE);
            } else if (folder != null) {
                // 检查文件夹写入权限
                return hasFolderPermission(currentUser, space, folder, FOLDER_WRITE);
            } else {
                // 空间根目录写入权限
                return hasSpacePermission(currentUser, space, SPACE_WRITE);
            }

        } catch (Exception e) {
            logger.error("检查写入权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV删除权限
     * 
     * @param space 空间
     * @param folder 文件夹（可为null）
     * @param file 文件（可为null）
     * @return 是否有删除权限
     */
    public boolean hasDeletePermission(Space space, Folder folder, FileEntity file) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝删除权限");
            return false;
        }

        try {
            if (file != null) {
                // 检查文件删除权限
                return hasFilePermission(currentUser, space, file, FILE_DELETE);
            } else if (folder != null) {
                // 检查文件夹删除权限
                return hasFolderPermission(currentUser, space, folder, FOLDER_DELETE);
            } else {
                // 不允许删除空间根目录
                logger.debug("不允许删除空间根目录");
                return false;
            }

        } catch (Exception e) {
            logger.error("检查删除权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV创建权限
     * 
     * @param space 空间
     * @param parentFolder 父文件夹（可为null，表示空间根目录）
     * @param isFile 是否为文件（true为文件，false为文件夹）
     * @return 是否有创建权限
     */
    public boolean hasCreatePermission(Space space, Folder parentFolder, boolean isFile) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝创建权限");
            return false;
        }

        try {
            // 检查空间级别的写入权限
            if (!hasSpacePermission(currentUser, space, SPACE_WRITE)) {
                logger.debug("用户[{}]在空间[{}]中无创建权限", currentUser.getUsername(), space.getName());
                return false;
            }

            if (parentFolder != null) {
                // 检查父文件夹的写入权限
                if (!hasFolderPermission(currentUser, space, parentFolder, FOLDER_WRITE)) {
                    logger.debug("用户[{}]在文件夹[{}]中无创建权限", currentUser.getUsername(), parentFolder.getName());
                    return false;
                }
            }

            // 检查具体的创建权限
            String permissionCode = isFile ? FILE_CREATE : FOLDER_CREATE;
            if (parentFolder != null) {
                return hasFolderPermission(currentUser, space, parentFolder, permissionCode);
            } else {
                return hasSpacePermission(currentUser, space, permissionCode);
            }

        } catch (Exception e) {
            logger.error("检查创建权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV移动权限
     * 
     * @param sourceSpace 源空间
     * @param sourceFolder 源文件夹
     * @param sourceFile 源文件
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹
     * @return 是否有移动权限
     */
    public boolean hasMovePermission(Space sourceSpace, Folder sourceFolder, FileEntity sourceFile,
                                   Space targetSpace, Folder targetFolder) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝移动权限");
            return false;
        }

        try {
            // 检查源位置的删除权限
            boolean hasSourceDeletePermission = hasDeletePermission(sourceSpace, sourceFolder, sourceFile);
            if (!hasSourceDeletePermission) {
                logger.debug("用户[{}]对源资源无删除权限", currentUser.getUsername());
                return false;
            }

            // 检查目标位置的创建权限
            boolean isFile = sourceFile != null;
            boolean hasTargetCreatePermission = hasCreatePermission(targetSpace, targetFolder, isFile);
            if (!hasTargetCreatePermission) {
                logger.debug("用户[{}]在目标位置无创建权限", currentUser.getUsername());
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("检查移动权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV复制权限
     * 
     * @param sourceSpace 源空间
     * @param sourceFolder 源文件夹
     * @param sourceFile 源文件
     * @param targetSpace 目标空间
     * @param targetFolder 目标文件夹
     * @return 是否有复制权限
     */
    public boolean hasCopyPermission(Space sourceSpace, Folder sourceFolder, FileEntity sourceFile,
                                   Space targetSpace, Folder targetFolder) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝复制权限");
            return false;
        }

        try {
            // 检查源位置的读取权限
            boolean hasSourceReadPermission = hasReadPermission(sourceSpace, sourceFolder, sourceFile);
            if (!hasSourceReadPermission) {
                logger.debug("用户[{}]对源资源无读取权限", currentUser.getUsername());
                return false;
            }

            // 检查目标位置的创建权限
            boolean isFile = sourceFile != null;
            boolean hasTargetCreatePermission = hasCreatePermission(targetSpace, targetFolder, isFile);
            if (!hasTargetCreatePermission) {
                logger.debug("用户[{}]在目标位置无创建权限", currentUser.getUsername());
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("检查复制权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查WebDAV锁定权限
     * 
     * @param space 空间
     * @param folder 文件夹
     * @param file 文件
     * @return 是否有锁定权限
     */
    public boolean hasLockPermission(Space space, Folder folder, FileEntity file) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.debug("用户未认证，拒绝锁定权限");
            return false;
        }

        try {
            // 锁定权限通常需要写入权限
            return hasWritePermission(space, folder, file);

        } catch (Exception e) {
            logger.error("检查锁定权限时发生错误", e);
            return false;
        }
    }

    // 私有辅助方法

    /**
     * 检查用户在空间中的权限
     */
    private boolean hasSpacePermission(User user, Space space, String permissionCode) {
        return permissionService.hasResourcePermission(
            user.getId(), space.getId(), "SPACE", null, permissionCode);
    }

    /**
     * 检查用户对文件夹的权限
     */
    private boolean hasFolderPermission(User user, Space space, Folder folder, String permissionCode) {
        return permissionService.hasResourcePermission(
            user.getId(), space.getId(), "FOLDER", folder.getId(), permissionCode);
    }

    /**
     * 检查用户对文件的权限
     */
    private boolean hasFilePermission(User user, Space space, FileEntity file, String permissionCode) {
        return permissionService.hasResourcePermission(
            user.getId(), space.getId(), "FILE", file.getId(), permissionCode);
    }

    /**
     * 检查用户是否为空间管理员
     * 
     * @param space 空间
     * @return 是否为空间管理员
     */
    public boolean isSpaceAdmin(Space space) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return permissionService.isSpaceAdmin(currentUser.getId(), space.getId()) ||
               permissionService.isAdmin(currentUser.getId());
    }

    /**
     * 检查用户是否为系统管理员
     * 
     * @return 是否为系统管理员
     */
    public boolean isSystemAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        return permissionService.isAdmin(currentUser.getId());
    }

    /**
     * 检查用户是否为资源所有者
     * 
     * @param space 空间
     * @param folder 文件夹（可为null）
     * @param file 文件（可为null）
     * @return 是否为资源所有者
     */
    public boolean isResourceOwner(Space space, Folder folder, FileEntity file) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        try {
            if (file != null) {
                return currentUser.getId().equals(file.getCreatedBy());
            } else if (folder != null) {
                return currentUser.getId().equals(folder.getCreatedBy());
            } else {
                return currentUser.getId().equals(space.getOwner().getId());
            }

        } catch (Exception e) {
            logger.error("检查资源所有者时发生错误", e);
            return false;
        }
    }
} 