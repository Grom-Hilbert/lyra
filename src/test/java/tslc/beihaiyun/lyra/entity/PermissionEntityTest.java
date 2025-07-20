package tslc.beihaiyun.lyra.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Permission实体类单元测试
 * 验证Permission实体的字段约束、关联关系和业务逻辑
 *
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@DisplayName("Permission实体测试")
class PermissionEntityTest {

    private Validator validator;
    private Permission permission;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        permission = new Permission();
        permission.setCode("file.read");
        permission.setName("文件读取权限");
        permission.setDescription("允许读取文件内容");
        permission.setResourceType("FILE");
        permission.setCategory("READ");
        permission.setLevel(10);
    }

    @Test
    @DisplayName("创建有效权限应该通过验证")
    void shouldCreateValidPermission() {
        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isEmpty();
        assertThat(permission.getCode()).isEqualTo("file.read");
        assertThat(permission.getName()).isEqualTo("文件读取权限");
        assertThat(permission.getResourceType()).isEqualTo("FILE");
        assertThat(permission.getCategory()).isEqualTo("READ");
        assertThat(permission.getLevel()).isEqualTo(10);
        assertThat(permission.getIsSystem()).isFalse();
        assertThat(permission.getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("权限代码为空应该验证失败")
    void shouldFailValidationWhenCodeIsBlank() {
        // Given
        permission.setCode("");

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).hasSize(3); // 空字符串会触发3个验证错误
        assertThat(violations).extracting(violation -> violation.getMessage())
                .containsAnyOf(
                    "权限代码长度必须在2-100个字符之间",
                    "权限代码必须以小写字母开头，只能包含小写字母、数字、点号、下划线和冒号",
                    "权限代码不能为空"
                );
    }

    @Test
    @DisplayName("权限代码为null应该验证失败")
    void shouldFailValidationWhenCodeIsNull() {
        // Given
        permission.setCode(null);

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限代码不能为空");
    }

    @Test
    @DisplayName("权限代码格式不正确应该验证失败")
    void shouldFailValidationWhenCodeFormatIsInvalid() {
        // Given
        permission.setCode("FILE_READ"); // 应该以小写字母开头

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限代码必须以小写字母开头");
    }

    @Test
    @DisplayName("权限名称为空应该验证失败")
    void shouldFailValidationWhenNameIsBlank() {
        // Given
        permission.setName("");

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).hasSize(2); // 空字符串会触发2个验证错误
        assertThat(violations).extracting(violation -> violation.getMessage())
                .containsAnyOf(
                    "权限名称长度必须在2-100个字符之间",
                    "权限名称不能为空"
                );
    }

    @Test
    @DisplayName("权限名称为null应该验证失败")
    void shouldFailValidationWhenNameIsNull() {
        // Given
        permission.setName(null);

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限名称不能为空");
    }

    @Test
    @DisplayName("资源类型无效应该验证失败")
    void shouldFailValidationWhenResourceTypeIsInvalid() {
        // Given
        permission.setResourceType("INVALID");

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("资源类型必须是FILE、FOLDER、SPACE或SYSTEM");
    }

    @Test
    @DisplayName("权限类别无效应该验证失败")
    void shouldFailValidationWhenCategoryIsInvalid() {
        // Given
        permission.setCategory("INVALID");

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限类别必须是READ、WRITE、DELETE、ADMIN或SHARE");
    }

    @Test
    @DisplayName("权限级别超出范围应该验证失败")
    void shouldFailValidationWhenLevelIsOutOfRange() {
        // Given
        permission.setLevel(101); // 超出最大值100

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限级别不能超过100");
    }

    @Test
    @DisplayName("描述过长应该验证失败")
    void shouldFailValidationWhenDescriptionIsTooLong() {
        // Given
        String longDescription = "a".repeat(501);
        permission.setDescription(longDescription);

        // When
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限描述长度不能超过500个字符");
    }

    @Test
    @DisplayName("测试权限类型判断方法")
    void shouldCorrectlyIdentifyPermissionTypes() {
        // 测试读权限
        permission.setCategory("READ");
        assertThat(permission.isReadPermission()).isTrue();
        assertThat(permission.isWritePermission()).isFalse();
        assertThat(permission.isDeletePermission()).isFalse();
        assertThat(permission.isAdminPermission()).isFalse();
        assertThat(permission.isSharePermission()).isFalse();

        // 测试写权限
        permission.setCategory("WRITE");
        assertThat(permission.isReadPermission()).isFalse();
        assertThat(permission.isWritePermission()).isTrue();

        // 测试删除权限
        permission.setCategory("DELETE");
        assertThat(permission.isDeletePermission()).isTrue();

        // 测试管理权限
        permission.setCategory("ADMIN");
        assertThat(permission.isAdminPermission()).isTrue();

        // 测试分享权限
        permission.setCategory("SHARE");
        assertThat(permission.isSharePermission()).isTrue();
    }

    @Test
    @DisplayName("测试资源类型判断方法")
    void shouldCorrectlyIdentifyResourceTypes() {
        // 测试文件权限
        permission.setResourceType("FILE");
        assertThat(permission.isFilePermission()).isTrue();
        assertThat(permission.isFolderPermission()).isFalse();
        assertThat(permission.isSpacePermission()).isFalse();
        assertThat(permission.isSystemResourcePermission()).isFalse();

        // 测试文件夹权限
        permission.setResourceType("FOLDER");
        assertThat(permission.isFilePermission()).isFalse();
        assertThat(permission.isFolderPermission()).isTrue();

        // 测试空间权限
        permission.setResourceType("SPACE");
        assertThat(permission.isSpacePermission()).isTrue();

        // 测试系统权限
        permission.setResourceType("SYSTEM");
        assertThat(permission.isSystemResourcePermission()).isTrue();
    }

    @Test
    @DisplayName("测试权限级别比较")
    void shouldCorrectlyCompareLevels() {
        Permission higherPermission = new Permission();
        higherPermission.setLevel(20);

        Permission lowerPermission = new Permission();
        lowerPermission.setLevel(5);

        // 当前权限级别为10
        assertThat(permission.compareLevel(higherPermission)).isNegative();
        assertThat(permission.compareLevel(lowerPermission)).isPositive();
        assertThat(permission.compareLevel(permission)).isZero();
        assertThat(permission.compareLevel(null)).isPositive();
    }

    @Test
    @DisplayName("测试权限兼容性检查")
    void shouldCorrectlyCheckCompatibility() {
        // 当前权限：FILE + READ
        assertThat(permission.isCompatible("FILE", "READ")).isTrue();
        assertThat(permission.isCompatible("FILE", "WRITE")).isFalse();
        assertThat(permission.isCompatible("FOLDER", "READ")).isFalse();
        assertThat(permission.isCompatible("FOLDER", "WRITE")).isFalse();
    }

    @Test
    @DisplayName("测试构造函数")
    void shouldCreatePermissionWithConstructor() {
        // When
        Permission newPermission = new Permission("folder.write", "文件夹写入权限", "FOLDER", "WRITE", 15);

        // Then
        assertThat(newPermission.getCode()).isEqualTo("folder.write");
        assertThat(newPermission.getName()).isEqualTo("文件夹写入权限");
        assertThat(newPermission.getResourceType()).isEqualTo("FOLDER");
        assertThat(newPermission.getCategory()).isEqualTo("WRITE");
        assertThat(newPermission.getLevel()).isEqualTo(15);
        assertThat(newPermission.getIsSystem()).isFalse();
        assertThat(newPermission.getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("测试角色关联")
    void shouldManageRoleAssociations() {
        // Given
        Role role1 = new Role("ADMIN", "管理员", Role.RoleType.SYSTEM_ADMIN);
        Role role2 = new Role("USER", "普通用户", Role.RoleType.USER);

        // When
        permission.getRoles().add(role1);
        permission.getRoles().add(role2);

        // Then
        assertThat(permission.getRoles()).hasSize(2);
        assertThat(permission.getRoles()).containsExactlyInAnyOrder(role1, role2);
    }

    @Test
    @DisplayName("测试系统权限设置")
    void shouldHandleSystemPermissionFlag() {
        // Given
        permission.setIsSystem(true);

        // Then
        assertThat(permission.getIsSystem()).isTrue();
    }

    @Test
    @DisplayName("测试权限启用状态")
    void shouldHandleEnabledFlag() {
        // Given
        permission.setIsEnabled(false);

        // Then
        assertThat(permission.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("测试权限组设置")
    void shouldHandlePermissionGroup() {
        // Given
        permission.setPermissionGroup("文件管理");

        // Then
        assertThat(permission.getPermissionGroup()).isEqualTo("文件管理");
    }

    @Test
    @DisplayName("测试依赖权限设置")
    void shouldHandleDependencies() {
        // Given
        String dependencies = "[\"file.read\", \"folder.read\"]";
        permission.setDependencies(dependencies);

        // Then
        assertThat(permission.getDependencies()).isEqualTo(dependencies);
    }

    @Test
    @DisplayName("测试equals和hashCode方法")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        Permission permission1 = new Permission();
        permission1.setCode("test.permission");

        Permission permission2 = new Permission();
        permission2.setCode("test.permission");

        Permission permission3 = new Permission();
        permission3.setCode("other.permission");

        // Then
        assertThat(permission1).isEqualTo(permission2);
        assertThat(permission1).isNotEqualTo(permission3);
        assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
    }

    @Test
    @DisplayName("测试toString方法")
    void shouldGenerateCorrectToString() {
        // When
        String toString = permission.toString();

        // Then
        assertThat(toString).contains("Permission{");
        assertThat(toString).contains("code='file.read'");
        assertThat(toString).contains("name='文件读取权限'");
        assertThat(toString).contains("resourceType='FILE'");
        assertThat(toString).contains("category='READ'");
        assertThat(toString).contains("level=10");
    }

    @Test
    @DisplayName("测试所有资源类型的有效值")
    void shouldAcceptAllValidResourceTypes() {
        String[] validResourceTypes = {"FILE", "FOLDER", "SPACE", "SYSTEM"};
        
        for (String resourceType : validResourceTypes) {
            permission.setResourceType(resourceType);
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试所有权限类别的有效值")
    void shouldAcceptAllValidCategories() {
        String[] validCategories = {"READ", "WRITE", "DELETE", "ADMIN", "SHARE"};
        
        for (String category : validCategories) {
            permission.setCategory(category);
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试权限级别边界值")
    void shouldAcceptValidLevelBoundaryValues() {
        // 测试最小值
        permission.setLevel(1);
        Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
        assertThat(violations).isEmpty();

        // 测试最大值
        permission.setLevel(100);
        violations = validator.validate(permission);
        assertThat(violations).isEmpty();

        // 测试超出最小值
        permission.setLevel(0);
        violations = validator.validate(permission);
        assertThat(violations).isNotEmpty();

        // 测试超出最大值
        permission.setLevel(101);
        violations = validator.validate(permission);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("测试有效的权限代码格式")
    void shouldAcceptValidCodeFormats() {
        String[] validCodes = {
            "file.read",
            "folder.write",
            "space.admin",
            "system.manage",
            "user.profile",
            "data.export",
            "api.access",
            "webhook.manage"
        };
        
        for (String code : validCodes) {
            permission.setCode(code);
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试无效的权限代码格式")
    void shouldRejectInvalidCodeFormats() {
        String[] invalidCodes = {
            "FILE.READ",     // 大写开头
            "1file.read",    // 数字开头
            "_file.read",    // 下划线开头
            "file read",     // 包含空格
            "file-read",     // 包含连字符
            "file@read"      // 包含特殊字符
        };
        
        for (String code : invalidCodes) {
            permission.setCode(code);
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            assertThat(violations).isNotEmpty();
        }
    }
} 