package tslc.beihaiyun.lyra.util;

import org.springframework.util.StringUtils;
import tslc.beihaiyun.lyra.entity.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 实体验证工具类
 * 提供通用的实体验证方法
 */
public final class EntityValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,50}$"
    );
    
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
        "^[^<>:\"|?*\\x00-\\x1f]*$"
    );

    private EntityValidationUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证用户名格式
     */
    public static boolean isValidUsername(String username) {
        return StringUtils.hasText(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证文件路径格式
     */
    public static boolean isValidFilePath(String path) {
        if (!StringUtils.hasText(path) || path.length() > 1000) {
            return false;
        }
        return FILE_PATH_PATTERN.matcher(path).matches();
    }

    /**
     * 验证文件大小
     */
    public static boolean isValidFileSize(Long size) {
        return size != null && size >= 0;
    }

    /**
     * 验证日期范围
     */
    public static boolean isValidDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return true; // 允许空值
        }
        return !start.isAfter(end);
    }

    /**
     * 验证权限过期时间
     */
    public static boolean isValidExpiryTime(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return true; // 允许永不过期
        }
        return expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * 验证字符串长度
     */
    public static boolean isValidLength(String str, int maxLength) {
        return str == null || str.length() <= maxLength;
    }

    /**
     * 验证必填字符串
     */
    public static boolean isRequiredStringValid(String str) {
        return StringUtils.hasText(str);
    }

    /**
     * 验证必填字符串长度
     */
    public static boolean isRequiredStringValid(String str, int maxLength) {
        return StringUtils.hasText(str) && str.length() <= maxLength;
    }

    /**
     * 验证Git提交哈希
     */
    public static boolean isValidGitHash(String hash) {
        return StringUtils.hasText(hash) && 
               hash.length() == 40 && 
               hash.matches("^[a-fA-F0-9]+$");
    }

    /**
     * 验证MIME类型
     */
    public static boolean isValidMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return true; // 允许空值
        }
        return mimeType.matches("^[a-zA-Z0-9][a-zA-Z0-9!#$&\\-\\^_]*\\/[a-zA-Z0-9][a-zA-Z0-9!#$&\\-\\^_]*$");
    }

    /**
     * 验证用户实体的基本字段
     */
    public static List<String> validateUserBasicFields(User user) {
        List<String> errors = new ArrayList<>();

        if (!isRequiredStringValid(user.getUsername())) {
            errors.add("用户名不能为空");
        } else if (!isValidUsername(user.getUsername())) {
            errors.add("用户名格式不正确");
        }

        if (!isRequiredStringValid(user.getEmail())) {
            errors.add("邮箱不能为空");
        } else if (!isValidEmail(user.getEmail())) {
            errors.add("邮箱格式不正确");
        }

        if (!isRequiredStringValid(user.getDisplayName(), 100)) {
            errors.add("显示名称不能为空且不能超过100个字符");
        }

        return errors;
    }

    /**
     * 验证文件实体的基本字段
     */
    public static List<String> validateFileBasicFields(FileEntity file) {
        List<String> errors = new ArrayList<>();

        if (!isRequiredStringValid(file.getName(), 255)) {
            errors.add("文件名不能为空且不能超过255个字符");
        }

        if (!isRequiredStringValid(file.getPath())) {
            errors.add("文件路径不能为空");
        } else if (!isValidFilePath(file.getPath())) {
            errors.add("文件路径格式不正确");
        }

        if (!isValidFileSize(file.getSize())) {
            errors.add("文件大小必须为非负数");
        }

        if (!isValidMimeType(file.getMimeType())) {
            errors.add("MIME类型格式不正确");
        }

        if (file.getOwner() == null) {
            errors.add("文件必须有所有者");
        }

        if (file.getSpaceType() == null) {
            errors.add("必须指定空间类型");
        }

        return errors;
    }

    /**
     * 验证文件夹实体的基本字段
     */
    public static List<String> validateFolderBasicFields(FolderEntity folder) {
        List<String> errors = new ArrayList<>();

        if (!isRequiredStringValid(folder.getName(), 255)) {
            errors.add("文件夹名称不能为空且不能超过255个字符");
        }

        if (!isRequiredStringValid(folder.getPath())) {
            errors.add("文件夹路径不能为空");
        } else if (!isValidFilePath(folder.getPath())) {
            errors.add("文件夹路径格式不正确");
        }

        if (!isValidLength(folder.getDescription(), 500)) {
            errors.add("文件夹描述不能超过500个字符");
        }

        if (folder.getOwner() == null) {
            errors.add("文件夹必须有所有者");
        }

        if (folder.getSpaceType() == null) {
            errors.add("必须指定空间类型");
        }

        return errors;
    }

    /**
     * 验证权限实体的基本字段
     */
    public static List<String> validatePermissionBasicFields(FilePermission permission) {
        List<String> errors = new ArrayList<>();

        if (permission.getFile() == null) {
            errors.add("权限必须关联文件");
        }

        if (permission.getUser() == null && permission.getRole() == null) {
            errors.add("权限必须关联用户或角色");
        }

        if (permission.getUser() != null && permission.getRole() != null) {
            errors.add("权限不能同时关联用户和角色");
        }

        if (permission.getPermissionType() == null) {
            errors.add("必须指定权限类型");
        }

        if (permission.getGrantedBy() == null) {
            errors.add("必须指定权限授予者");
        }

        if (!isValidExpiryTime(permission.getExpiresAt())) {
            errors.add("权限过期时间不能早于当前时间");
        }

        return errors;
    }

    /**
     * 验证模板实体的基本字段
     */
    public static List<String> validateTemplateBasicFields(Template template) {
        List<String> errors = new ArrayList<>();

        if (!isRequiredStringValid(template.getName(), 100)) {
            errors.add("模板名称不能为空且不能超过100个字符");
        }

        if (!isValidLength(template.getDescription(), 500)) {
            errors.add("模板描述不能超过500个字符");
        }

        if (template.getTemplateType() == null) {
            errors.add("必须指定模板类型");
        }

        if (template.getCreatedBy() == null) {
            errors.add("模板必须有创建者");
        }

        return errors;
    }

    /**
     * 验证版本提交实体的基本字段
     */
    public static List<String> validateVersionCommitBasicFields(VersionCommit commit) {
        List<String> errors = new ArrayList<>();

        if (!isRequiredStringValid(commit.getCommitHash())) {
            errors.add("提交哈希不能为空");
        } else if (!isValidGitHash(commit.getCommitHash())) {
            errors.add("提交哈希格式不正确");
        }

        if (!isRequiredStringValid(commit.getRepositoryPath(), 1000)) {
            errors.add("仓库路径不能为空且不能超过1000个字符");
        }

        if (!isRequiredStringValid(commit.getMessage(), 1000)) {
            errors.add("提交消息不能为空且不能超过1000个字符");
        }

        if (commit.getAuthor() == null) {
            errors.add("提交必须有作者");
        }

        if (commit.getCommitTime() == null) {
            errors.add("提交时间不能为空");
        }

        return errors;
    }
}