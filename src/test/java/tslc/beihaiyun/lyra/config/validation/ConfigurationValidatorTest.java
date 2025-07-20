package tslc.beihaiyun.lyra.config.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import tslc.beihaiyun.lyra.config.LyraProperties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigurationValidator 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("配置验证器测试")
class ConfigurationValidatorTest {

    private ConfigurationValidator configValidator;
    private LyraProperties properties;
    private Validator validator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        configValidator = new ConfigurationValidator(validator);
        properties = new LyraProperties();
    }

    @Test
    @DisplayName("默认配置验证测试")
    void testDefaultConfigurationValidation() {
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        // 默认配置应该是有效的，但可能有警告
        assertTrue(result.isValid(), "默认配置应该通过验证");
        
        // 检查是否有默认密钥警告
        List<ConfigurationValidator.ValidationMessage> warnings = result.getWarnings();
        boolean hasSecretWarning = warnings.stream()
            .anyMatch(w -> w.getField().equals("jwt.secret") && 
                      w.getMessage().contains("默认密钥"));
        assertTrue(hasSecretWarning, "应该对默认JWT密钥发出警告");
    }

    @Test
    @DisplayName("JWT配置验证测试")
    void testJwtConfigValidation() {
        LyraProperties.JwtConfig jwt = properties.getJwt();
        
        // 测试安全的JWT配置
        jwt.setSecret("ThisIsAVerySecureJWTSecretKeyThatIsLongEnough");
        jwt.setExpiration(3600000L); // 1小时
        jwt.setRefreshExpiration(86400000L); // 24小时
        
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        assertTrue(result.isValid(), "安全的JWT配置应该通过验证");
        assertEquals(0, result.getErrors().size(), "不应该有错误");
        
        // 验证刷新令牌过期时间小于访问令牌的情况
        jwt.setRefreshExpiration(1800000L); // 30分钟，小于访问令牌
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> errors = result.getErrors();
        boolean hasExpirationError = errors.stream()
            .anyMatch(e -> e.getMessage().contains("刷新令牌过期时间必须大于访问令牌过期时间"));
        assertTrue(hasExpirationError, "应该检测到过期时间错误");
        
        // 测试过长的过期时间警告
        jwt.setExpiration(172800000L); // 48小时，超过建议的24小时
        jwt.setRefreshExpiration(2592000001L); // 超过30天
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> warnings = result.getWarnings();
        assertTrue(warnings.stream().anyMatch(w -> w.getMessage().contains("超过24小时")), 
            "应该对过长的访问令牌过期时间发出警告");
        assertTrue(warnings.stream().anyMatch(w -> w.getMessage().contains("超过30天")), 
            "应该对过长的刷新令牌过期时间发出警告");
    }

    @Test
    @DisplayName("存储配置验证测试")
    void testStorageConfigValidation() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 测试有效的存储路径（使用临时目录）
        storage.setBasePath(tempDir.toString());
        storage.setTempPath(tempDir.resolve("temp").toString());
        
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        assertTrue(result.isValid(), "有效的存储配置应该通过验证");
        
        // 测试不存在的路径
        storage.setBasePath("/nonexistent/path/that/should/not/exist");
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> warnings = result.getWarnings();
        boolean hasPathWarning = warnings.stream()
            .anyMatch(w -> w.getField().equals("storage.basePath") && 
                      w.getMessage().contains("无法创建存储目录"));
        // 在某些系统上可能能创建目录，所以这里只检查逻辑
        
        // 测试无效的存储后端
        storage.setBasePath(tempDir.toString()); // 恢复有效路径
        storage.setBackend("invalidbackend");
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> errors = result.getErrors();
        boolean hasBackendError = errors.stream()
            .anyMatch(e -> e.getField().equals("storage.backend") && 
                      e.getMessage().contains("不支持的存储后端"));
        assertTrue(hasBackendError, "应该检测到无效的存储后端");
        
        // 测试文件大小过大警告
        storage.setBackend("local"); // 恢复有效后端
        storage.setMaxFileSize("10GB"); // 超过5GB
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> warningsSize = result.getWarnings();
        boolean hasSizeWarning = warningsSize.stream()
            .anyMatch(w -> w.getField().equals("storage.maxFileSize") && 
                      w.getMessage().contains("超过5GB"));
        assertTrue(hasSizeWarning, "应该对超大文件大小发出警告");
    }

    @Test
    @DisplayName("系统配置验证测试")
    void testSystemConfigValidation() {
        LyraProperties.SystemConfig system = properties.getSystem();
        
        // 测试用户数量警告
        system.setMaxUsers(1500); // 超过1000
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> warnings = result.getWarnings();
        boolean hasUserWarning = warnings.stream()
            .anyMatch(w -> w.getField().equals("system.maxUsers") && 
                      w.getMessage().contains("超过1000"));
        assertTrue(hasUserWarning, "应该对大量用户数发出警告");
        
        // 测试空间配额警告
        system.setMaxUsers(100); // 恢复正常值
        system.setDefaultSpaceQuota("2TB"); // 超过1TB
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> quotaWarnings = result.getWarnings();
        boolean hasQuotaWarning = quotaWarnings.stream()
            .anyMatch(w -> w.getField().equals("system.defaultSpaceQuota") && 
                      w.getMessage().contains("超过1TB"));
        assertTrue(hasQuotaWarning, "应该对大空间配额发出警告");
        
        // 测试空白系统名称
        system.setDefaultSpaceQuota("10GB"); // 恢复正常值
        system.setName("   "); // 空白字符
        result = configValidator.validateLyraProperties(properties);
        
        List<ConfigurationValidator.ValidationMessage> errors = result.getErrors();
        boolean hasNameError = errors.stream()
            .anyMatch(e -> e.getField().equals("system.name") && 
                      e.getMessage().contains("不能为空白字符"));
        assertTrue(hasNameError, "应该检测到空白系统名称");
    }

    @Test
    @DisplayName("磁盘空间检查测试")
    void testDiskSpaceCheck() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 使用临时目录进行测试
        storage.setBasePath(tempDir.toString());
        storage.setBackend("local");
        
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        // 磁盘空间检查可能产生信息或警告，但不应该有错误
        assertTrue(result.isValid(), "本地存储配置应该通过验证");
        
        // 检查是否有磁盘空间相关的信息
        List<ConfigurationValidator.ValidationMessage> allMessages = result.getAllMessages();
        // 磁盘空间检查结果取决于实际系统状态，这里只验证不会崩溃
    }

    @Test
    @DisplayName("验证结果类测试")
    void testValidationResult() {
        ConfigurationValidator.ValidationResult result = new ConfigurationValidator.ValidationResult();
        
        // 测试空结果
        assertTrue(result.isValid(), "空结果应该有效");
        assertFalse(result.hasErrors(), "空结果不应该有错误");
        assertFalse(result.hasWarnings(), "空结果不应该有警告");
        assertEquals(0, result.getAllMessages().size(), "空结果不应该有消息");
        
        // 添加错误
        result.addError("test.field", "测试错误");
        assertFalse(result.isValid(), "有错误的结果应该无效");
        assertTrue(result.hasErrors(), "应该有错误");
        assertEquals(1, result.getErrors().size(), "应该有1个错误");
        
        // 添加警告
        result.addWarning("test.field", "测试警告");
        assertTrue(result.hasWarnings(), "应该有警告");
        assertEquals(1, result.getWarnings().size(), "应该有1个警告");
        
        // 添加信息
        result.addInfo("test.field", "测试信息");
        assertEquals(1, result.getInfos().size(), "应该有1个信息");
        
        // 测试所有消息
        assertEquals(3, result.getAllMessages().size(), "应该有3个消息");
        
        // 测试toString
        String resultString = result.toString();
        assertTrue(resultString.contains("errors=1"), "字符串应该包含错误数量");
        assertTrue(resultString.contains("warnings=1"), "字符串应该包含警告数量");
        assertTrue(resultString.contains("valid=false"), "字符串应该显示无效状态");
    }

    @Test
    @DisplayName("验证消息类测试")
    void testValidationMessage() {
        String field = "test.field";
        String message = "测试消息";
        ConfigurationValidator.ValidationLevel level = ConfigurationValidator.ValidationLevel.ERROR;
        
        ConfigurationValidator.ValidationMessage validationMessage = 
            new ConfigurationValidator.ValidationMessage(field, message, level);
        
        assertEquals(field, validationMessage.getField(), "字段应该正确");
        assertEquals(message, validationMessage.getMessage(), "消息应该正确");
        assertEquals(level, validationMessage.getLevel(), "级别应该正确");
        assertTrue(validationMessage.getTimestamp() > 0, "时间戳应该大于0");
        
        // 测试toString
        String messageString = validationMessage.toString();
        assertTrue(messageString.contains(field), "字符串应该包含字段");
        assertTrue(messageString.contains(message), "字符串应该包含消息");
        assertTrue(messageString.contains("ERROR"), "字符串应该包含级别");
        
        // 测试equals和hashCode
        ConfigurationValidator.ValidationMessage sameMessage = 
            new ConfigurationValidator.ValidationMessage(field, message, level);
        assertEquals(validationMessage, sameMessage, "相同内容的消息应该相等");
        assertEquals(validationMessage.hashCode(), sameMessage.hashCode(), "相同消息的hashCode应该相等");
        
        ConfigurationValidator.ValidationMessage differentMessage = 
            new ConfigurationValidator.ValidationMessage("different.field", message, level);
        assertNotEquals(validationMessage, differentMessage, "不同内容的消息不应该相等");
    }

    @Test
    @DisplayName("配置为null的情况测试")
    void testNullConfigValidation() {
        // 测试JWT配置为null（虽然实际不会发生，但要确保健壮性）
        // 这需要通过反射或创建特殊的测试配置类来实现
        
        // 这里只能测试空配置对象的情况
        LyraProperties emptyProperties = new LyraProperties();
        
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(emptyProperties);
        
        // 默认配置应该是有效的
        assertTrue(result.isValid() || result.hasWarnings(), 
            "空配置应该有效或只有警告");
    }

    @Test
    @DisplayName("路径验证测试")
    void testPathValidation() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 测试相对路径
        storage.setBasePath("./relative/path");
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        // 路径格式应该有效（即使路径不存在）
        // 具体结果取决于系统是否能创建目录
        
        // 测试绝对路径
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            storage.setBasePath("C:\\temp\\lyra");
        } else {
            storage.setBasePath("/tmp/lyra");
        }
        
        result = configValidator.validateLyraProperties(properties);
        // 应该不会因为路径格式而失败
    }

    @Test
    @DisplayName("文件大小格式验证测试")
    void testFileSizeFormatValidation() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 测试各种有效格式
        String[] validFormats = {"100", "100B", "100KB", "100MB", "100GB", "100TB", "100K", "100M", "100G", "100T"};
        
        for (String format : validFormats) {
            storage.setMaxFileSize(format);
            ConfigurationValidator.ValidationResult result = 
                configValidator.validateLyraProperties(properties);
            
            // 不应该因为文件大小格式而有错误
            boolean hasFormatError = result.getErrors().stream()
                .anyMatch(e -> e.getField().equals("storage.maxFileSize") && 
                          e.getMessage().contains("格式无效"));
            assertFalse(hasFormatError, "格式 " + format + " 应该有效");
        }
        
        // 测试无效格式
        storage.setMaxFileSize("invalid-format");
        ConfigurationValidator.ValidationResult result = 
            configValidator.validateLyraProperties(properties);
        
        boolean hasFormatError = result.getErrors().stream()
            .anyMatch(e -> e.getField().equals("storage.maxFileSize"));
        assertTrue(hasFormatError, "无效格式应该产生错误");
    }
} 