package tslc.beihaiyun.lyra.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 文件路径验证器
 * 验证文件路径的合法性
 */
public class FilePathValidator implements ConstraintValidator<ValidFilePath, String> {

    private static final Pattern INVALID_CHARS = Pattern.compile("[<>:\"|?*\\x00-\\x1f]");
    private static final Pattern RESERVED_NAMES = Pattern.compile("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$", Pattern.CASE_INSENSITIVE);
    
    @Override
    public void initialize(ValidFilePath constraintAnnotation) {
        // 初始化验证器
    }

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        // 检查路径长度
        if (path.length() > 1000) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("文件路径长度不能超过1000个字符")
                   .addConstraintViolation();
            return false;
        }

        // 检查非法字符
        if (INVALID_CHARS.matcher(path).find()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("文件路径包含非法字符")
                   .addConstraintViolation();
            return false;
        }

        // 检查路径组件
        String[] parts = path.split("[/\\\\]");
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }
            
            // 检查保留名称
            if (RESERVED_NAMES.matcher(part).matches()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("文件路径包含系统保留名称: " + part)
                       .addConstraintViolation();
                return false;
            }
            
            // 检查以点开头或结尾
            if (part.startsWith(".") || part.endsWith(".")) {
                if (!part.equals(".") && !part.equals("..")) {
                    // 允许相对路径标识符，但不允许其他以点开头或结尾的名称
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("文件名不能以点开头或结尾: " + part)
                           .addConstraintViolation();
                    return false;
                }
            }
        }

        return true;
    }
}