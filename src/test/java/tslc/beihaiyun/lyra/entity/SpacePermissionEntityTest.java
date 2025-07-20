package tslc.beihaiyun.lyra.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpacePermission实体类单元测试
 * 验证SpacePermission实体的字段约束、关联关系和业务逻辑
 * 特别测试权限继承和覆盖机制
 *
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("SpacePermission实体测试")
class SpacePermissionEntityTest {

    private Validator validator;
    private SpacePermission spacePermission;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        spacePermission = new SpacePermission();
        spacePermission.setUserId(1L);
        spacePermission.setSpaceId(1L);
        spacePermission.setPermissionId(1L);
        spacePermission.setResourceType("FILE");
        spacePermission.setResourceId(1L);
        spacePermission.setStatus("GRANTED");
        spacePermission.setGrantType("DIRECT");
        spacePermission.setPermissionLevel(50);
        spacePermission.setGrantedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建有效空间权限应该通过验证")
    void shouldCreateValidSpacePermission() {
        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isEmpty();
        assertThat(spacePermission.getUserId()).isEqualTo(1L);
        assertThat(spacePermission.getSpaceId()).isEqualTo(1L);
        assertThat(spacePermission.getPermissionId()).isEqualTo(1L);
        assertThat(spacePermission.getResourceType()).isEqualTo("FILE");
        assertThat(spacePermission.getResourceId()).isEqualTo(1L);
        assertThat(spacePermission.getStatus()).isEqualTo("GRANTED");
        assertThat(spacePermission.getGrantType()).isEqualTo("DIRECT");
        assertThat(spacePermission.getPermissionLevel()).isEqualTo(50);
        assertThat(spacePermission.getInheritFromParent()).isTrue();
    }

    @Test
    @DisplayName("用户ID为空应该验证失败")
    void shouldFailValidationWhenUserIdIsNull() {
        // Given
        spacePermission.setUserId(null);

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("用户ID不能为空");
    }

    @Test
    @DisplayName("空间ID为空应该验证失败")
    void shouldFailValidationWhenSpaceIdIsNull() {
        // Given
        spacePermission.setSpaceId(null);

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("空间ID不能为空");
    }

    @Test
    @DisplayName("权限ID为空应该验证失败")
    void shouldFailValidationWhenPermissionIdIsNull() {
        // Given
        spacePermission.setPermissionId(null);

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限ID不能为空");
    }

    @Test
    @DisplayName("资源类型无效应该验证失败")
    void shouldFailValidationWhenResourceTypeIsInvalid() {
        // Given
        spacePermission.setResourceType("INVALID");

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("资源类型必须是FILE、FOLDER或SPACE");
    }

    @Test
    @DisplayName("授权状态无效应该验证失败")
    void shouldFailValidationWhenStatusIsInvalid() {
        // Given
        spacePermission.setStatus("INVALID");

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("授权状态必须是GRANTED、DENIED或INHERITED");
    }

    @Test
    @DisplayName("授权类型无效应该验证失败")
    void shouldFailValidationWhenGrantTypeIsInvalid() {
        // Given
        spacePermission.setGrantType("INVALID");

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("授权类型必须是DIRECT、INHERITED或ROLE_BASED");
    }

    @Test
    @DisplayName("权限级别超出范围应该验证失败")
    void shouldFailValidationWhenPermissionLevelIsOutOfRange() {
        // Given
        spacePermission.setPermissionLevel(101);

        // When
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("权限级别不能超过100");
    }

    @Test
    @DisplayName("测试权限过期检查")
    void shouldCorrectlyCheckExpiration() {
        // 测试未过期的权限
        spacePermission.setExpiresAt(LocalDateTime.now().plusDays(1));
        assertThat(spacePermission.isExpired()).isFalse();

        // 测试已过期的权限
        spacePermission.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertThat(spacePermission.isExpired()).isTrue();

        // 测试永不过期的权限
        spacePermission.setExpiresAt(null);
        assertThat(spacePermission.isExpired()).isFalse();
    }

    @Test
    @DisplayName("测试权限授权状态检查")
    void shouldCorrectlyCheckGrantStatus() {
        // 测试授权状态
        spacePermission.setStatus("GRANTED");
        spacePermission.setExpiresAt(null);
        assertThat(spacePermission.isGranted()).isTrue();

        // 测试拒绝状态
        spacePermission.setStatus("DENIED");
        assertThat(spacePermission.isDenied()).isTrue();
        assertThat(spacePermission.isGranted()).isFalse();

        // 测试继承状态
        spacePermission.setStatus("INHERITED");
        assertThat(spacePermission.isInherited()).isTrue();
        assertThat(spacePermission.isGranted()).isFalse();

        // 测试已过期的授权
        spacePermission.setStatus("GRANTED");
        spacePermission.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertThat(spacePermission.isGranted()).isFalse();
    }

    @Test
    @DisplayName("测试授权类型检查")
    void shouldCorrectlyCheckGrantType() {
        // 测试直接授权
        spacePermission.setGrantType("DIRECT");
        assertThat(spacePermission.isDirectGrant()).isTrue();
        assertThat(spacePermission.isInheritedGrant()).isFalse();
        assertThat(spacePermission.isRoleBasedGrant()).isFalse();

        // 测试继承授权
        spacePermission.setGrantType("INHERITED");
        assertThat(spacePermission.isDirectGrant()).isFalse();
        assertThat(spacePermission.isInheritedGrant()).isTrue();

        // 测试基于角色的授权
        spacePermission.setGrantType("ROLE_BASED");
        assertThat(spacePermission.isRoleBasedGrant()).isTrue();
    }

    @Test
    @DisplayName("测试资源类型检查")
    void shouldCorrectlyCheckResourceType() {
        // 测试文件权限
        spacePermission.setResourceType("FILE");
        assertThat(spacePermission.isFilePermission()).isTrue();
        assertThat(spacePermission.isFolderPermission()).isFalse();
        assertThat(spacePermission.isSpacePermission()).isFalse();

        // 测试文件夹权限
        spacePermission.setResourceType("FOLDER");
        assertThat(spacePermission.isFilePermission()).isFalse();
        assertThat(spacePermission.isFolderPermission()).isTrue();

        // 测试空间权限
        spacePermission.setResourceType("SPACE");
        assertThat(spacePermission.isSpacePermission()).isTrue();
    }

    @Test
    @DisplayName("测试权限级别比较")
    void shouldCorrectlyCompareLevels() {
        SpacePermission higherPermission = new SpacePermission();
        higherPermission.setPermissionLevel(80);

        SpacePermission lowerPermission = new SpacePermission();
        lowerPermission.setPermissionLevel(20);

        // 当前权限级别为50
        assertThat(spacePermission.compareLevel(higherPermission)).isNegative();
        assertThat(spacePermission.compareLevel(lowerPermission)).isPositive();
        assertThat(spacePermission.compareLevel(spacePermission)).isZero();
        assertThat(spacePermission.compareLevel(null)).isPositive();
    }

    @Test
    @DisplayName("测试权限适用性检查")
    void shouldCorrectlyCheckResourceApplicability() {
        // 精确匹配
        assertThat(spacePermission.appliesToResource("FILE", 1L)).isTrue();
        assertThat(spacePermission.appliesToResource("FILE", 2L)).isFalse();
        assertThat(spacePermission.appliesToResource("FOLDER", 1L)).isFalse();

        // 空间级权限适用于所有资源
        spacePermission.setResourceType("SPACE");
        spacePermission.setResourceId(null);
        assertThat(spacePermission.appliesToResource("SPACE", 1L)).isTrue();
        assertThat(spacePermission.appliesToResource("SPACE", 2L)).isTrue();
    }

    @Test
    @DisplayName("测试权限路径构建")
    void shouldBuildPermissionPathCorrectly() {
        // 基本路径构建
        String path1 = SpacePermission.buildPermissionPath(1L, null, 100L);
        assertThat(path1).isEqualTo("/1/100");

        // 带父路径的构建
        String path2 = SpacePermission.buildPermissionPath(1L, "/folder1/folder2", 100L);
        assertThat(path2).isEqualTo("/1/folder1/folder2/100");

        // 空父路径
        String path3 = SpacePermission.buildPermissionPath(1L, "", 100L);
        assertThat(path3).isEqualTo("/1/100");

        // 空资源ID
        String path4 = SpacePermission.buildPermissionPath(1L, "/folder1", null);
        assertThat(path4).isEqualTo("/1/folder1");
    }

    @Test
    @DisplayName("测试子路径检查")
    void shouldCorrectlyCheckSubPath() {
        // 完全匹配
        assertThat(SpacePermission.isSubPath("/1/folder1", "/1/folder1")).isTrue();

        // 子路径
        assertThat(SpacePermission.isSubPath("/1/folder1", "/1/folder1/folder2")).isTrue();
        assertThat(SpacePermission.isSubPath("/1/folder1", "/1/folder1/folder2/file1")).isTrue();

        // 非子路径
        assertThat(SpacePermission.isSubPath("/1/folder1", "/1/folder2")).isFalse();
        assertThat(SpacePermission.isSubPath("/1/folder1", "/2/folder1")).isFalse();

        // 空值处理
        assertThat(SpacePermission.isSubPath(null, "/1/folder1")).isFalse();
        assertThat(SpacePermission.isSubPath("/1/folder1", null)).isFalse();
    }

    @Test
    @DisplayName("测试构造函数")
    void shouldCreateSpacePermissionWithConstructor() {
        // When
        SpacePermission newPermission = new SpacePermission(2L, 3L, 4L, "FOLDER", "GRANTED", "DIRECT");

        // Then
        assertThat(newPermission.getUserId()).isEqualTo(2L);
        assertThat(newPermission.getSpaceId()).isEqualTo(3L);
        assertThat(newPermission.getPermissionId()).isEqualTo(4L);
        assertThat(newPermission.getResourceType()).isEqualTo("FOLDER");
        assertThat(newPermission.getStatus()).isEqualTo("GRANTED");
        assertThat(newPermission.getGrantType()).isEqualTo("DIRECT");
        assertThat(newPermission.getGrantedAt()).isNotNull();
        assertThat(newPermission.getInheritFromParent()).isTrue();
        assertThat(newPermission.getPermissionLevel()).isEqualTo(50);
    }

    @Test
    @DisplayName("测试实体关联")
    void shouldManageEntityAssociations() {
        // Given
        User user = new User();
        user.setId(1L);
        
        Space space = new Space();
        space.setId(1L);
        
        Permission permission = new Permission();
        permission.setId(1L);
        
        User grantor = new User();
        grantor.setId(2L);

        // When
        spacePermission.setUser(user);
        spacePermission.setSpace(space);
        spacePermission.setPermission(permission);
        spacePermission.setGrantor(grantor);

        // Then
        assertThat(spacePermission.getUser()).isEqualTo(user);
        assertThat(spacePermission.getSpace()).isEqualTo(space);
        assertThat(spacePermission.getPermission()).isEqualTo(permission);
        assertThat(spacePermission.getGrantor()).isEqualTo(grantor);
    }

    @Test
    @DisplayName("测试权限路径设置")
    void shouldHandlePermissionPath() {
        // Given
        String path = "/1/folder1/folder2/file1";
        spacePermission.setPermissionPath(path);

        // Then
        assertThat(spacePermission.getPermissionPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("测试权限条件设置")
    void shouldHandleConditions() {
        // Given
        String conditions = "{\"timeRestriction\": \"09:00-18:00\", \"ipRestriction\": \"192.168.1.0/24\"}";
        spacePermission.setConditions(conditions);

        // Then
        assertThat(spacePermission.getConditions()).isEqualTo(conditions);
    }

    @Test
    @DisplayName("测试备注信息设置")
    void shouldHandleRemark() {
        // Given
        String remark = "临时授权，项目结束后收回";
        spacePermission.setRemark(remark);

        // Then
        assertThat(spacePermission.getRemark()).isEqualTo(remark);
    }

    @Test
    @DisplayName("测试equals和hashCode方法")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        SpacePermission permission1 = new SpacePermission(1L, 1L, 1L, "FILE", "GRANTED", "DIRECT");
        permission1.setResourceId(1L);

        SpacePermission permission2 = new SpacePermission(1L, 1L, 1L, "FILE", "GRANTED", "DIRECT");
        permission2.setResourceId(1L);

        SpacePermission permission3 = new SpacePermission(2L, 1L, 1L, "FILE", "GRANTED", "DIRECT");
        permission3.setResourceId(1L);

        // Then
        assertThat(permission1).isEqualTo(permission2);
        assertThat(permission1).isNotEqualTo(permission3);
        assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
    }

    @Test
    @DisplayName("测试toString方法")
    void shouldGenerateCorrectToString() {
        // When
        String toString = spacePermission.toString();

        // Then
        assertThat(toString).contains("SpacePermission{");
        assertThat(toString).contains("userId=1");
        assertThat(toString).contains("spaceId=1");
        assertThat(toString).contains("permissionId=1");
        assertThat(toString).contains("resourceType='FILE'");
        assertThat(toString).contains("status='GRANTED'");
        assertThat(toString).contains("grantType='DIRECT'");
    }

    @Test
    @DisplayName("测试所有资源类型的有效值")
    void shouldAcceptAllValidResourceTypes() {
        String[] validResourceTypes = {"FILE", "FOLDER", "SPACE"};
        
        for (String resourceType : validResourceTypes) {
            spacePermission.setResourceType(resourceType);
            Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试所有授权状态的有效值")
    void shouldAcceptAllValidStatuses() {
        String[] validStatuses = {"GRANTED", "DENIED", "INHERITED"};
        
        for (String status : validStatuses) {
            spacePermission.setStatus(status);
            Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试所有授权类型的有效值")
    void shouldAcceptAllValidGrantTypes() {
        String[] validGrantTypes = {"DIRECT", "INHERITED", "ROLE_BASED"};
        
        for (String grantType : validGrantTypes) {
            spacePermission.setGrantType(grantType);
            Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("测试权限级别边界值")
    void shouldAcceptValidPermissionLevelBoundaryValues() {
        // 测试最小值
        spacePermission.setPermissionLevel(1);
        Set<ConstraintViolation<SpacePermission>> violations = validator.validate(spacePermission);
        assertThat(violations).isEmpty();

        // 测试最大值
        spacePermission.setPermissionLevel(100);
        violations = validator.validate(spacePermission);
        assertThat(violations).isEmpty();

        // 测试超出最小值
        spacePermission.setPermissionLevel(0);
        violations = validator.validate(spacePermission);
        assertThat(violations).isNotEmpty();

        // 测试超出最大值
        spacePermission.setPermissionLevel(101);
        violations = validator.validate(spacePermission);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("测试权限继承设置")
    void shouldHandleInheritanceFlag() {
        // 默认值应该是true
        assertThat(spacePermission.getInheritFromParent()).isTrue();

        // 设置为false
        spacePermission.setInheritFromParent(false);
        assertThat(spacePermission.getInheritFromParent()).isFalse();
    }

    @Test
    @DisplayName("测试授权者和授权时间设置")
    void shouldHandleGrantorAndGrantTime() {
        // Given
        Long grantorId = 123L;
        LocalDateTime grantTime = LocalDateTime.now();

        // When
        spacePermission.setGrantedBy(grantorId);
        spacePermission.setGrantedAt(grantTime);

        // Then
        assertThat(spacePermission.getGrantedBy()).isEqualTo(grantorId);
        assertThat(spacePermission.getGrantedAt()).isEqualTo(grantTime);
    }

    @Test
    @DisplayName("测试过期时间设置")
    void shouldHandleExpirationTime() {
        // Given
        LocalDateTime expireTime = LocalDateTime.now().plusDays(30);

        // When
        spacePermission.setExpiresAt(expireTime);

        // Then
        assertThat(spacePermission.getExpiresAt()).isEqualTo(expireTime);
    }
} 