package tslc.beihaiyun.lyra.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tslc.beihaiyun.lyra.entity.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据完整性验证器
 * 提供业务级别的数据验证功能
 */
@Component
public class DataIntegrityValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,50}$"
    );

    /**
     * 验证用户数据完整性
     */
    public ValidationResult validateUser(User user, boolean isCreate) {
        List<String> errors = new ArrayList<>();

        // 基础字段验证
        if (!StringUtils.hasText(user.getUsername())) {
            errors.add("用户名不能为空");
        } else if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            errors.add("用户名格式不正确，只能包含字母、数字、下划线和连字符，长度3-50个字符");
        }

        if (!StringUtils.hasText(user.getEmail())) {
            errors.add("邮箱不能为空");
        } else if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            errors.add("邮箱格式不正确");
        }

        if (!StringUtils.hasText(user.getDisplayName())) {
            errors.add("显示名称不能为空");
        } else if (user.getDisplayName().length() > 100) {
            errors.add("显示名称不能超过100个字符");
        }

        // 创建时必须有密码（本地认证）
        if (isCreate && user.getAuthProvider() == User.AuthProvider.LOCAL) {
            if (!StringUtils.hasText(user.getPasswordHash())) {
                errors.add("本地认证用户必须设置密码");
            }
        }

        // 外部认证必须有外部ID
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            if (!StringUtils.hasText(user.getExternalId())) {
                errors.add("外部认证用户必须提供外部ID");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证文件数据完整性
     */
    public ValidationResult validateFile(FileEntity file, boolean isCreate) {
        List<String> errors = new ArrayList<>();

        // 基础字段验证
        if (!StringUtils.hasText(file.getName())) {
            errors.add("文件名不能为空");
        } else if (file.getName().length() > 255) {
            errors.add("文件名不能超过255个字符");
        }

        if (!StringUtils.hasText(file.getPath())) {
            errors.add("文件路径不能为空");
        } else if (file.getPath().length() > 1000) {
            errors.add("文件路径不能超过1000个字符");
        }

        if (file.getSize() == null || file.getSize() < 0) {
            errors.add("文件大小必须为非负数");
        }

        if (file.getOwner() == null) {
            errors.add("文件必须有所有者");
        }

        if (file.getSpaceType() == null) {
            errors.add("必须指定空间类型");
        }

        // 版本控制类型验证
        if (file.getVersionControlType() == null) {
            file.setVersionControlType(FileEntity.VersionControlType.NONE);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证文件夹数据完整性
     */
    public ValidationResult validateFolder(FolderEntity folder, boolean isCreate) {
        List<String> errors = new ArrayList<>();

        // 基础字段验证
        if (!StringUtils.hasText(folder.getName())) {
            errors.add("文件夹名称不能为空");
        } else if (folder.getName().length() > 255) {
            errors.add("文件夹名称不能超过255个字符");
        }

        if (!StringUtils.hasText(folder.getPath())) {
            errors.add("文件夹路径不能为空");
        } else if (folder.getPath().length() > 1000) {
            errors.add("文件夹路径不能超过1000个字符");
        }

        if (folder.getOwner() == null) {
            errors.add("文件夹必须有所有者");
        }

        if (folder.getSpaceType() == null) {
            errors.add("必须指定空间类型");
        }

        // 描述长度验证
        if (folder.getDescription() != null && folder.getDescription().length() > 500) {
            errors.add("文件夹描述不能超过500个字符");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证权限数据完整性
     */
    public ValidationResult validatePermission(FilePermission permission) {
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

        // 过期时间验证
        if (permission.getExpiresAt() != null && permission.getExpiresAt().isBefore(LocalDateTime.now())) {
            errors.add("权限过期时间不能早于当前时间");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证模板数据完整性
     */
    public ValidationResult validateTemplate(Template template, boolean isCreate) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(template.getName())) {
            errors.add("模板名称不能为空");
        } else if (template.getName().length() > 100) {
            errors.add("模板名称不能超过100个字符");
        }

        if (template.getDescription() != null && template.getDescription().length() > 500) {
            errors.add("模板描述不能超过500个字符");
        }

        if (template.getTemplateType() == null) {
            errors.add("必须指定模板类型");
        }

        if (template.getCreatedBy() == null) {
            errors.add("模板必须有创建者");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证版本提交数据完整性
     */
    public ValidationResult validateVersionCommit(VersionCommit commit) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(commit.getCommitHash())) {
            errors.add("提交哈希不能为空");
        } else if (commit.getCommitHash().length() != 40) {
            errors.add("提交哈希长度必须为40个字符");
        }

        if (!StringUtils.hasText(commit.getRepositoryPath())) {
            errors.add("仓库路径不能为空");
        }

        if (!StringUtils.hasText(commit.getMessage())) {
            errors.add("提交消息不能为空");
        } else if (commit.getMessage().length() > 1000) {
            errors.add("提交消息不能超过1000个字符");
        }

        if (commit.getAuthor() == null) {
            errors.add("提交必须有作者");
        }

        if (commit.getCommitTime() == null) {
            errors.add("提交时间不能为空");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}