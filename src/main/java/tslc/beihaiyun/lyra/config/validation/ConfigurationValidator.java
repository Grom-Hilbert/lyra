package tslc.beihaiyun.lyra.config.validation;

import org.springframework.stereotype.Component;
import tslc.beihaiyun.lyra.config.LyraProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 配置验证器
 * 提供系统配置的深度验证功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Component
public class ConfigurationValidator {

    private final Validator validator;
    
    // 文件大小格式验证模式
    private static final Pattern SIZE_PATTERN = Pattern.compile("^\\d+[KMGT]?B?$", Pattern.CASE_INSENSITIVE);
    
    // 路径验证模式（Unix/Windows兼容）
    private static final Pattern PATH_PATTERN = Pattern.compile("^[a-zA-Z]?[:\\\\]?[\\w\\s/\\\\.-]+$");
    
    // 支持的存储后端类型
    private static final Set<String> SUPPORTED_BACKENDS = Set.of("local", "nfs", "s3");

    public ConfigurationValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * 验证Lyra配置的完整性和有效性
     */
    public ValidationResult validateLyraProperties(LyraProperties properties) {
        ValidationResult result = new ValidationResult();
        
        // 1. 使用Jakarta Validation进行基本验证
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        for (ConstraintViolation<LyraProperties> violation : violations) {
            result.addError(violation.getPropertyPath().toString(), violation.getMessage());
        }
        
        // 2. 进行自定义业务逻辑验证
        validateJwtConfig(properties.getJwt(), result);
        validateStorageConfig(properties.getStorage(), result);
        validateSystemConfig(properties.getSystem(), result);
        
        return result;
    }

    /**
     * 验证JWT配置
     */
    private void validateJwtConfig(LyraProperties.JwtConfig jwt, ValidationResult result) {
        if (jwt == null) {
            result.addError("jwt", "JWT配置不能为空");
            return;
        }
        
        // 验证密钥安全性
        if ("DefaultSecretKey".equals(jwt.getSecret()) || 
            jwt.getSecret().contains("default") || 
            jwt.getSecret().contains("Default")) {
            result.addWarning("jwt.secret", "检测到默认密钥，生产环境请务必修改");
        }
        
        // 验证过期时间合理性
        if (jwt.getExpiration() != null && jwt.getRefreshExpiration() != null) {
            if (jwt.getRefreshExpiration() <= jwt.getExpiration()) {
                result.addError("jwt.refreshExpiration", "刷新令牌过期时间必须大于访问令牌过期时间");
            }
            
            // 建议的过期时间范围
            if (jwt.getExpiration() > 86400000L) { // 大于24小时
                result.addWarning("jwt.expiration", "访问令牌过期时间超过24小时，可能存在安全风险");
            }
            
            if (jwt.getRefreshExpiration() > 2592000000L) { // 大于30天
                result.addWarning("jwt.refreshExpiration", "刷新令牌过期时间超过30天，可能存在安全风险");
            }
        }
    }

    /**
     * 验证存储配置
     */
    private void validateStorageConfig(LyraProperties.StorageConfig storage, ValidationResult result) {
        if (storage == null) {
            result.addError("storage", "存储配置不能为空");
            return;
        }
        
        // 验证存储路径
        validateStoragePath(storage.getBasePath(), "storage.basePath", result);
        validateStoragePath(storage.getTempPath(), "storage.tempPath", result);
        
        // 验证文件大小格式
        if (!isValidSizeFormat(storage.getMaxFileSize())) {
            result.addError("storage.maxFileSize", "文件大小格式无效：" + storage.getMaxFileSize());
        }
        
        // 验证存储后端
        if (!SUPPORTED_BACKENDS.contains(storage.getBackend().toLowerCase())) {
            result.addError("storage.backend", "不支持的存储后端类型：" + storage.getBackend());
        }
        
        // 检查磁盘空间（仅本地存储）
        if ("local".equals(storage.getBackend())) {
            checkDiskSpace(storage.getBasePath(), result);
        }
        
        // 验证最大文件大小合理性
        long maxSize = storage.getMaxFileSizeInBytes();
        if (maxSize > 5368709120L) { // 大于5GB
            result.addWarning("storage.maxFileSize", "最大文件大小超过5GB，可能影响系统性能");
        }
    }

    /**
     * 验证系统配置
     */
    private void validateSystemConfig(LyraProperties.SystemConfig system, ValidationResult result) {
        if (system == null) {
            result.addError("system", "系统配置不能为空");
            return;
        }
        
        // 验证用户数量合理性
        if (system.getMaxUsers() != null && system.getMaxUsers() > 1000) {
            result.addWarning("system.maxUsers", "最大用户数超过1000，建议考虑性能优化");
        }
        
        // 验证默认配额合理性
        if (!isValidSizeFormat(system.getDefaultSpaceQuota())) {
            result.addError("system.defaultSpaceQuota", "默认空间配额格式无效：" + system.getDefaultSpaceQuota());
        } else {
            long quota = system.getDefaultSpaceQuotaInBytes();
            if (quota > 1099511627776L) { // 大于1TB
                result.addWarning("system.defaultSpaceQuota", "默认空间配额超过1TB，请确认存储容量足够");
            }
        }
        
        // 验证系统名称和描述
        if (system.getName() != null && system.getName().trim().isEmpty()) {
            result.addError("system.name", "系统名称不能为空白字符");
        }
    }

    /**
     * 验证存储路径
     */
    private void validateStoragePath(String path, String fieldName, ValidationResult result) {
        if (path == null || path.trim().isEmpty()) {
            result.addError(fieldName, "存储路径不能为空");
            return;
        }
        
        // 检查路径格式
        if (!PATH_PATTERN.matcher(path).matches()) {
            result.addError(fieldName, "存储路径格式无效：" + path);
            return;
        }
        
        // 检查路径是否可创建/访问
        try {
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                // 尝试创建目录
                if (!pathFile.mkdirs()) {
                    result.addWarning(fieldName, "无法创建存储目录：" + path);
                }
            } else if (!pathFile.isDirectory()) {
                result.addError(fieldName, "存储路径不是有效目录：" + path);
            } else if (!pathFile.canRead() || !pathFile.canWrite()) {
                result.addError(fieldName, "存储目录权限不足：" + path);
            }
        } catch (Exception e) {
            result.addWarning(fieldName, "无法验证存储路径：" + path + "，错误：" + e.getMessage());
        }
    }

    /**
     * 检查磁盘空间
     */
    private void checkDiskSpace(String path, ValidationResult result) {
        try {
            File pathFile = new File(path);
            if (pathFile.exists()) {
                long freeSpace = pathFile.getFreeSpace();
                long totalSpace = pathFile.getTotalSpace();
                
                if (freeSpace < 1073741824L) { // 小于1GB
                    result.addWarning("storage.basePath", "可用磁盘空间不足1GB，当前：" + formatSize(freeSpace));
                }
                
                double usagePercent = (double) (totalSpace - freeSpace) / totalSpace * 100;
                if (usagePercent > 90) {
                    result.addWarning("storage.basePath", "磁盘使用率超过90%，当前：" + String.format("%.1f%%", usagePercent));
                }
            }
        } catch (Exception e) {
            result.addInfo("storage.basePath", "无法检查磁盘空间：" + e.getMessage());
        }
    }

    /**
     * 验证大小格式
     */
    private boolean isValidSizeFormat(String size) {
        return size != null && SIZE_PATTERN.matcher(size.trim()).matches();
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.1f GB", bytes / 1073741824.0);
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final List<ValidationMessage> errors = new ArrayList<>();
        private final List<ValidationMessage> warnings = new ArrayList<>();
        private final List<ValidationMessage> infos = new ArrayList<>();
        
        public void addError(String field, String message) {
            errors.add(new ValidationMessage(field, message, ValidationLevel.ERROR));
        }
        
        public void addWarning(String field, String message) {
            warnings.add(new ValidationMessage(field, message, ValidationLevel.WARNING));
        }
        
        public void addInfo(String field, String message) {
            infos.add(new ValidationMessage(field, message, ValidationLevel.INFO));
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public List<ValidationMessage> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<ValidationMessage> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public List<ValidationMessage> getInfos() {
            return new ArrayList<>(infos);
        }
        
        public List<ValidationMessage> getAllMessages() {
            List<ValidationMessage> all = new ArrayList<>();
            all.addAll(errors);
            all.addAll(warnings);
            all.addAll(infos);
            return all;
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{");
            sb.append("errors=").append(errors.size());
            sb.append(", warnings=").append(warnings.size());
            sb.append(", infos=").append(infos.size());
            sb.append(", valid=").append(isValid());
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * 验证消息类
     */
    public static class ValidationMessage {
        private final String field;
        private final String message;
        private final ValidationLevel level;
        private final long timestamp;
        
        public ValidationMessage(String field, String message, ValidationLevel level) {
            this.field = field;
            this.message = message;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getField() { return field; }
        public String getMessage() { return message; }
        public ValidationLevel getLevel() { return level; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", level, field, message);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationMessage that = (ValidationMessage) o;
            return Objects.equals(field, that.field) &&
                   Objects.equals(message, that.message) &&
                   level == that.level;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(field, message, level);
        }
    }

    /**
     * 验证级别枚举
     */
    public enum ValidationLevel {
        ERROR,    // 错误，必须修复
        WARNING,  // 警告，建议修复
        INFO      // 信息，仅供参考
    }
} 