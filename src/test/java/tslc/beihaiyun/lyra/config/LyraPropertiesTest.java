package tslc.beihaiyun.lyra.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LyraProperties 配置类单元测试
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@DisplayName("Lyra配置属性测试")
class LyraPropertiesTest {

    private Validator validator;
    private LyraProperties properties;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        properties = new LyraProperties();
    }

    @Test
    @DisplayName("默认配置应该有效")
    void testDefaultConfigurationShouldBeValid() {
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        
        // 默认配置可能有JWT密钥警告，但应该通过基本验证
        assertTrue(violations.isEmpty(), "默认配置应该通过验证");
    }

    @Test
    @DisplayName("JWT配置验证测试")
    void testJwtConfigValidation() {
        LyraProperties.JwtConfig jwt = properties.getJwt();
        
        // 测试有效的JWT配置
        jwt.setSecret("ThisIsAVeryLongSecretKeyForJWT_MustBe32CharsOrMore");
        jwt.setExpiration(3600000L); // 1小时
        jwt.setRefreshExpiration(86400000L); // 24小时
        
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        assertTrue(violations.isEmpty(), "有效的JWT配置应该通过验证");
        
        // 测试无效的JWT密钥（太短）
        jwt.setSecret("short");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "短JWT密钥应该验证失败");
        
        // 测试空密钥
        jwt.setSecret("");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "空JWT密钥应该验证失败");
        
        // 测试null密钥
        jwt.setSecret(null);
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "null JWT密钥应该验证失败");
    }

    @Test
    @DisplayName("JWT过期时间验证测试")
    void testJwtExpirationValidation() {
        LyraProperties.JwtConfig jwt = properties.getJwt();
        jwt.setSecret("ThisIsAVeryLongSecretKeyForJWT_MustBe32CharsOrMore");
        
        // 测试无效的过期时间（太短）
        jwt.setExpiration(60000L); // 1分钟，少于最小值5分钟
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "过短的过期时间应该验证失败");
        
        // 测试无效的过期时间（太长）
        jwt.setExpiration(604800001L); // 超过7天
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "过长的过期时间应该验证失败");
        
        // 测试刷新令牌过期时间小于访问令牌
        jwt.setExpiration(86400000L); // 24小时
        jwt.setRefreshExpiration(3600000L); // 1小时
        violations = validator.validate(properties);
        // 这个验证需要在业务逻辑中处理，不是在注解验证中
    }

    @Test
    @DisplayName("存储配置验证测试")
    void testStorageConfigValidation() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 测试无效的文件大小格式
        storage.setMaxFileSize("invalid");
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "无效的文件大小格式应该验证失败");
        
        // 测试有效的文件大小格式
        storage.setMaxFileSize("100MB");
        violations = validator.validate(properties);
        assertEquals(0, violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("maxFileSize"))
            .count(), "有效的文件大小格式应该通过验证");
        
        // 测试不同的文件大小格式
        String[] validSizes = {"100", "100B", "100KB", "100MB", "100GB", "100TB"};
        for (String size : validSizes) {
            storage.setMaxFileSize(size);
            violations = validator.validate(properties);
            assertEquals(0, violations.stream()
                .filter(v -> v.getPropertyPath().toString().contains("maxFileSize"))
                .count(), "文件大小格式 " + size + " 应该有效");
        }
        
        // 测试空路径
        storage.setBasePath("");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "空存储路径应该验证失败");
        
        // 测试无效的存储后端
        storage.setBasePath("./data/files"); // 恢复有效路径
        storage.setBackend("invalid");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "无效的存储后端应该验证失败");
        
        // 测试有效的存储后端
        String[] validBackends = {"local", "nfs", "s3"};
        for (String backend : validBackends) {
            storage.setBackend(backend);
            violations = validator.validate(properties);
            assertEquals(0, violations.stream()
                .filter(v -> v.getPropertyPath().toString().contains("backend"))
                .count(), "存储后端 " + backend + " 应该有效");
        }
    }

    @Test
    @DisplayName("系统配置验证测试")
    void testSystemConfigValidation() {
        LyraProperties.SystemConfig system = properties.getSystem();
        
        // 测试无效的最大用户数
        system.setMaxUsers(0);
        Set<ConstraintViolation<LyraProperties>> violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "0用户数应该验证失败");
        
        system.setMaxUsers(10001);
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "超过10000用户数应该验证失败");
        
        // 测试有效的最大用户数
        system.setMaxUsers(100);
        violations = validator.validate(properties);
        assertEquals(0, violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("maxUsers"))
            .count(), "有效的用户数应该通过验证");
        
        // 测试无效的空间配额格式
        system.setDefaultSpaceQuota("invalid");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "无效的空间配额格式应该验证失败");
        
        // 测试有效的空间配额格式
        system.setDefaultSpaceQuota("10GB");
        violations = validator.validate(properties);
        assertEquals(0, violations.stream()
            .filter(v -> v.getPropertyPath().toString().contains("defaultSpaceQuota"))
            .count(), "有效的空间配额格式应该通过验证");
        
        // 测试空系统名称
        system.setName("");
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "空系统名称应该验证失败");
        
        // 测试过长的系统名称
        system.setName("a".repeat(51));
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "过长的系统名称应该验证失败");
        
        // 测试过长的系统描述
        system.setDescription("a".repeat(201));
        violations = validator.validate(properties);
        assertFalse(violations.isEmpty(), "过长的系统描述应该验证失败");
    }

    @Test
    @DisplayName("文件大小转换测试")
    void testFileSizeConversion() {
        LyraProperties.StorageConfig storage = properties.getStorage();
        
        // 测试各种文件大小转换
        storage.setMaxFileSize("100");
        assertEquals(100L, storage.getMaxFileSizeInBytes());
        
        storage.setMaxFileSize("100B");
        assertEquals(100L, storage.getMaxFileSizeInBytes());
        
        storage.setMaxFileSize("100KB");
        assertEquals(102400L, storage.getMaxFileSizeInBytes());
        
        storage.setMaxFileSize("100MB");
        assertEquals(104857600L, storage.getMaxFileSizeInBytes());
        
        storage.setMaxFileSize("1GB");
        assertEquals(1073741824L, storage.getMaxFileSizeInBytes());
        
        storage.setMaxFileSize("1TB");
        assertEquals(1099511627776L, storage.getMaxFileSizeInBytes());
        
        // 测试无效格式返回默认值
        storage.setMaxFileSize("invalid");
        assertEquals(104857600L, storage.getMaxFileSizeInBytes()); // 默认100MB
        
        storage.setMaxFileSize(null);
        assertEquals(104857600L, storage.getMaxFileSizeInBytes()); // 默认100MB
    }

    @Test
    @DisplayName("空间配额转换测试")
    void testSpaceQuotaConversion() {
        LyraProperties.SystemConfig system = properties.getSystem();
        
        // 测试各种空间配额转换
        system.setDefaultSpaceQuota("10GB");
        assertEquals(10737418240L, system.getDefaultSpaceQuotaInBytes());
        
        system.setDefaultSpaceQuota("1TB");
        assertEquals(1099511627776L, system.getDefaultSpaceQuotaInBytes());
        
        // 测试无效格式返回默认值
        system.setDefaultSpaceQuota("invalid");
        assertEquals(10737418240L, system.getDefaultSpaceQuotaInBytes()); // 默认10GB
    }

    @Test
    @DisplayName("JWT配置业务逻辑验证测试")
    void testJwtConfigBusinessLogic() {
        LyraProperties.JwtConfig jwt = properties.getJwt();
        
        // 设置有效的基础配置
        jwt.setSecret("ThisIsAVeryLongSecretKeyForJWT_MustBe32CharsOrMore");
        jwt.setExpiration(3600000L); // 1小时
        jwt.setRefreshExpiration(86400000L); // 24小时
        
        assertTrue(jwt.isValid(), "有效的JWT配置应该通过业务逻辑验证");
        
        // 测试刷新令牌过期时间不大于访问令牌
        jwt.setRefreshExpiration(1800000L); // 30分钟，小于访问令牌的1小时
        assertFalse(jwt.isValid(), "刷新令牌过期时间小于访问令牌时应该验证失败");
        
        // 测试空密钥
        jwt.setSecret(null);
        jwt.setRefreshExpiration(86400000L); // 恢复正常值
        assertFalse(jwt.isValid(), "空密钥应该验证失败");
        
        // 测试空白密钥
        jwt.setSecret("   ");
        assertFalse(jwt.isValid(), "空白密钥应该验证失败");
    }

    @Test
    @DisplayName("配置对象创建测试")
    void testConfigurationObjectCreation() {
        // 测试默认配置对象的创建
        assertNotNull(properties.getJwt(), "JWT配置对象应该被创建");
        assertNotNull(properties.getStorage(), "存储配置对象应该被创建");
        assertNotNull(properties.getSystem(), "系统配置对象应该被创建");
        
        // 测试默认值
        assertEquals("DefaultSecretKey_Please_Change_In_Production_Environment_32Characters", 
            properties.getJwt().getSecret(), "JWT密钥应该有默认值");
        assertEquals(86400000L, properties.getJwt().getExpiration(), 
            "JWT过期时间应该有默认值");
        assertEquals(604800000L, properties.getJwt().getRefreshExpiration(), 
            "JWT刷新过期时间应该有默认值");
        
        assertEquals("./data/files", properties.getStorage().getBasePath(), 
            "存储基础路径应该有默认值");
        assertEquals("./data/temp", properties.getStorage().getTempPath(), 
            "临时文件路径应该有默认值");
        assertEquals("100MB", properties.getStorage().getMaxFileSize(), 
            "最大文件大小应该有默认值");
        assertEquals("*", properties.getStorage().getAllowedTypes(), 
            "允许的文件类型应该有默认值");
        assertEquals("local", properties.getStorage().getBackend(), 
            "存储后端应该有默认值");
        assertTrue(properties.getStorage().getEnableDeduplication(), 
            "文件去重应该默认启用");
        
        assertEquals(100, properties.getSystem().getMaxUsers(), 
            "最大用户数应该有默认值");
        assertEquals("10GB", properties.getSystem().getDefaultSpaceQuota(), 
            "默认空间配额应该有默认值");
        assertTrue(properties.getSystem().getEnableVersionControl(), 
            "版本控制应该默认启用");
        assertEquals("Lyra Document Management System", properties.getSystem().getName(), 
            "系统名称应该有默认值");
        assertEquals("企业级云原生文档管理系统", properties.getSystem().getDescription(), 
            "系统描述应该有默认值");
        assertFalse(properties.getSystem().getAllowUserRegistration(), 
            "用户注册应该默认关闭");
        assertFalse(properties.getSystem().getMaintenanceMode(), 
            "维护模式应该默认关闭");
    }
} 