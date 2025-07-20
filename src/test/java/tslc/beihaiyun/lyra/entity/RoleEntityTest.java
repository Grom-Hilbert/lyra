package tslc.beihaiyun.lyra.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Role实体类单元测试
 * 验证字段约束、验证规则和业务方法
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@DisplayName("Role实体类测试")
class RoleEntityTest {

    private Validator validator;
    private Role role;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // 创建一个有效的角色对象
        role = new Role();
        role.setCode("TEST_ROLE");
        role.setName("测试角色");
        role.setDescription("这是一个测试角色");
        role.setType(Role.RoleType.CUSTOM);
    }

    @Test
    @DisplayName("创建有效角色 - 无验证错误")
    void testValidRole() {
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.isEmpty(), "有效角色不应该有验证错误");
    }

    @Test
    @DisplayName("角色代码验证 - 空值验证")
    void testCodeNotBlank() {
        role.setCode("");
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        
        boolean hasCodeError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("code"));
        assertTrue(hasCodeError, "空角色代码应该产生验证错误");
    }

    @Test
    @DisplayName("角色代码验证 - 长度限制")
    void testCodeLength() {
        // 测试角色代码过短
        role.setCode("A");
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "过短角色代码应该产生验证错误");

        // 测试角色代码过长
        role.setCode("A".repeat(51));
        violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "过长角色代码应该产生验证错误");

        // 测试有效长度
        role.setCode("VALID_ROLE");
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "有效长度角色代码不应该产生验证错误");
    }

    @Test
    @DisplayName("角色代码验证 - 格式验证")
    void testCodePattern() {
        // 测试小写字母开头
        role.setCode("test_role");
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "小写字母开头的角色代码应该产生验证错误");

        // 测试包含特殊字符
        role.setCode("TEST@ROLE");
        violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "包含特殊字符的角色代码应该产生验证错误");

        // 测试包含空格
        role.setCode("TEST ROLE");
        violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "包含空格的角色代码应该产生验证错误");

        // 测试有效格式
        role.setCode("TEST_ROLE_123");
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("code")), "有效格式角色代码不应该产生验证错误");
    }

    @Test
    @DisplayName("角色名称验证 - 空值验证")
    void testNameNotBlank() {
        role.setName("");
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        
        boolean hasNameError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        assertTrue(hasNameError, "空角色名称应该产生验证错误");
    }

    @Test
    @DisplayName("角色名称验证 - 长度限制")
    void testNameLength() {
        // 测试角色名称过短
        role.setName("A");
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("name")), "过短角色名称应该产生验证错误");

        // 测试角色名称过长
        role.setName("A".repeat(101));
        violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("name")), "过长角色名称应该产生验证错误");

        // 测试有效长度
        role.setName("有效角色名称");
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("name")), "有效长度角色名称不应该产生验证错误");
    }

    @Test
    @DisplayName("角色描述验证 - 长度限制")
    void testDescriptionLength() {
        // 测试角色描述过长
        role.setDescription("A".repeat(501));
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("description")), "过长角色描述应该产生验证错误");

        // 测试有效长度
        role.setDescription("这是一个有效长度的角色描述");
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("description")), "有效长度角色描述不应该产生验证错误");

        // 测试空描述（应该允许）
        role.setDescription(null);
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("description")), "空角色描述应该被允许");
    }

    @Test
    @DisplayName("角色类型枚举测试")
    void testRoleTypeEnum() {
        assertNotNull(Role.RoleType.SYSTEM_ADMIN, "SYSTEM_ADMIN类型应该存在");
        assertNotNull(Role.RoleType.ORGANIZATION_ADMIN, "ORGANIZATION_ADMIN类型应该存在");
        assertNotNull(Role.RoleType.DEPARTMENT_ADMIN, "DEPARTMENT_ADMIN类型应该存在");
        assertNotNull(Role.RoleType.USER, "USER类型应该存在");
        assertNotNull(Role.RoleType.GUEST, "GUEST类型应该存在");
        assertNotNull(Role.RoleType.CUSTOM, "CUSTOM类型应该存在");
        
        // 测试默认类型
        Role newRole = new Role();
        assertEquals(Role.RoleType.CUSTOM, newRole.getType(), "新角色默认类型应该是CUSTOM");
    }

    @Test
    @DisplayName("业务方法测试 - isAvailable")
    void testIsAvailableMethod() {
        // 测试可用角色
        role.setEnabled(true);
        role.setDeleted(false);
        assertTrue(role.isAvailable(), "启用且未删除的角色应该可用");

        // 测试未启用角色
        role.setEnabled(false);
        assertFalse(role.isAvailable(), "未启用角色应该不可用");

        // 测试已删除角色
        role.setEnabled(true);
        role.setDeleted(true);
        assertFalse(role.isAvailable(), "已删除角色应该不可用");
    }

    @Test
    @DisplayName("业务方法测试 - isAdminRole")
    void testIsAdminRoleMethod() {
        // 测试系统管理员
        role.setType(Role.RoleType.SYSTEM_ADMIN);
        assertTrue(role.isAdminRole(), "系统管理员应该是管理员角色");

        // 测试企业管理员
        role.setType(Role.RoleType.ORGANIZATION_ADMIN);
        assertTrue(role.isAdminRole(), "企业管理员应该是管理员角色");

        // 测试部门管理员
        role.setType(Role.RoleType.DEPARTMENT_ADMIN);
        assertTrue(role.isAdminRole(), "部门管理员应该是管理员角色");

        // 测试普通用户
        role.setType(Role.RoleType.USER);
        assertFalse(role.isAdminRole(), "普通用户不应该是管理员角色");

        // 测试访客
        role.setType(Role.RoleType.GUEST);
        assertFalse(role.isAdminRole(), "访客不应该是管理员角色");

        // 测试自定义角色
        role.setType(Role.RoleType.CUSTOM);
        assertFalse(role.isAdminRole(), "自定义角色不应该是管理员角色");
    }

    @Test
    @DisplayName("业务方法测试 - isSystemAdmin")
    void testIsSystemAdminMethod() {
        // 测试系统管理员
        role.setType(Role.RoleType.SYSTEM_ADMIN);
        assertTrue(role.isSystemAdmin(), "系统管理员角色应该返回true");

        // 测试其他角色
        role.setType(Role.RoleType.ORGANIZATION_ADMIN);
        assertFalse(role.isSystemAdmin(), "企业管理员不应该是系统管理员");

        role.setType(Role.RoleType.USER);
        assertFalse(role.isSystemAdmin(), "普通用户不应该是系统管理员");
    }

    @Test
    @DisplayName("业务方法测试 - isUserRole")
    void testIsUserRoleMethod() {
        // 测试普通用户角色
        role.setType(Role.RoleType.USER);
        assertTrue(role.isUserRole(), "普通用户角色应该返回true");

        // 测试其他角色
        role.setType(Role.RoleType.SYSTEM_ADMIN);
        assertFalse(role.isUserRole(), "管理员角色不应该是普通用户角色");

        role.setType(Role.RoleType.GUEST);
        assertFalse(role.isUserRole(), "访客角色不应该是普通用户角色");
    }

    @Test
    @DisplayName("业务方法测试 - isGuestRole")
    void testIsGuestRoleMethod() {
        // 测试访客角色
        role.setType(Role.RoleType.GUEST);
        assertTrue(role.isGuestRole(), "访客角色应该返回true");

        // 测试其他角色
        role.setType(Role.RoleType.USER);
        assertFalse(role.isGuestRole(), "普通用户角色不应该是访客角色");

        role.setType(Role.RoleType.SYSTEM_ADMIN);
        assertFalse(role.isGuestRole(), "管理员角色不应该是访客角色");
    }

    @Test
    @DisplayName("业务方法测试 - 启用和禁用")
    void testEnableAndDisable() {
        // 测试启用
        role.setEnabled(false);
        role.enable();
        assertTrue(role.getEnabled(), "启用后角色应该处于启用状态");

        // 测试禁用
        role.disable();
        assertFalse(role.getEnabled(), "禁用后角色应该处于禁用状态");
    }

    @Test
    @DisplayName("构造函数测试")
    void testConstructors() {
        // 测试无参构造函数
        Role emptyRole = new Role();
        assertNotNull(emptyRole, "无参构造函数应该创建对象");
        assertEquals(Role.RoleType.CUSTOM, emptyRole.getType(), "默认类型应该为CUSTOM");
        assertFalse(emptyRole.getSystem(), "默认系统标记应该为false");
        assertTrue(emptyRole.getEnabled(), "默认启用状态应该为true");

        // 测试三参构造函数
        Role paramRole = new Role("TEST_CODE", "测试角色", Role.RoleType.USER);
        assertEquals("TEST_CODE", paramRole.getCode(), "角色代码应该被正确设置");
        assertEquals("测试角色", paramRole.getName(), "角色名称应该被正确设置");
        assertEquals(Role.RoleType.USER, paramRole.getType(), "角色类型应该被正确设置");

        // 测试四参构造函数
        Role fullParamRole = new Role("FULL_CODE", "完整角色", "角色描述", Role.RoleType.CUSTOM);
        assertEquals("FULL_CODE", fullParamRole.getCode(), "角色代码应该被正确设置");
        assertEquals("完整角色", fullParamRole.getName(), "角色名称应该被正确设置");
        assertEquals("角色描述", fullParamRole.getDescription(), "角色描述应该被正确设置");
        assertEquals(Role.RoleType.CUSTOM, fullParamRole.getType(), "角色类型应该被正确设置");
    }

    @Test
    @DisplayName("默认值测试")
    void testDefaultValues() {
        Role newRole = new Role();
        
        assertEquals(Role.RoleType.CUSTOM, newRole.getType(), "默认类型应该为CUSTOM");
        assertFalse(newRole.getSystem(), "默认系统标记应该为false");
        assertTrue(newRole.getEnabled(), "默认启用状态应该为true");
        assertEquals(0, newRole.getSortOrder(), "默认排序顺序应该为0");
        assertNotNull(newRole.getUserRoles(), "用户角色集合应该被初始化");
        assertTrue(newRole.getUserRoles().isEmpty(), "用户角色集合应该为空");
    }

    @Test
    @DisplayName("系统角色常量测试")
    void testSystemRoleConstants() {
        assertEquals("SYSTEM_ADMIN", Role.SYSTEM_ADMIN_CODE, "系统管理员常量应该正确");
        assertEquals("ORG_ADMIN", Role.ORG_ADMIN_CODE, "企业管理员常量应该正确");
        assertEquals("DEPT_ADMIN", Role.DEPT_ADMIN_CODE, "部门管理员常量应该正确");
        assertEquals("USER", Role.USER_CODE, "用户常量应该正确");
        assertEquals("GUEST", Role.GUEST_CODE, "访客常量应该正确");
    }

    @Test
    @DisplayName("equals和hashCode测试")
    void testEqualsAndHashCode() {
        Role role1 = new Role();
        role1.setId(1L);
        role1.setCode("ROLE1");

        Role role2 = new Role();
        role2.setId(1L);
        role2.setCode("ROLE2"); // 不同代码但相同ID

        Role role3 = new Role();
        role3.setId(2L);
        role3.setCode("ROLE1"); // 相同代码但不同ID

        Role role4 = new Role();
        // ID为null

        // 测试相同ID的对象
        assertEquals(role1, role2, "相同ID的角色应该相等");
        assertEquals(role1.hashCode(), role2.hashCode(), "相同ID的角色hashCode应该相等");

        // 测试不同ID的对象
        assertNotEquals(role1, role3, "不同ID的角色应该不相等");

        // 测试null ID的对象
        assertNotEquals(role1, role4, "ID为null的角色与有ID的角色应该不相等");
        assertEquals(role4, role4, "对象与自身应该相等");

        // 测试与null的比较
        assertNotEquals(role1, null, "角色与null应该不相等");

        // 测试与不同类型对象的比较
        assertNotEquals(role1, "string", "角色与字符串应该不相等");
    }

    @Test
    @DisplayName("toString测试")
    void testToString() {
        role.setId(1L);
        role.setCode("TEST_ROLE");
        role.setName("测试角色");
        role.setType(Role.RoleType.CUSTOM);
        role.setSystem(false);
        role.setEnabled(true);

        String toString = role.toString();
        
        assertNotNull(toString, "toString不应该返回null");
        assertTrue(toString.contains("TEST_ROLE"), "toString应该包含角色代码");
        assertTrue(toString.contains("测试角色"), "toString应该包含角色名称");
        assertTrue(toString.contains("CUSTOM"), "toString应该包含角色类型");
        assertTrue(toString.contains("false"), "toString应该包含系统标记");
        assertTrue(toString.contains("true"), "toString应该包含启用状态");
    }

    @Test
    @DisplayName("排序顺序验证")
    void testSortOrderValidation() {
        // 测试负数排序顺序
        role.setSortOrder(-1);
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("sortOrder")), "负数排序顺序应该产生验证错误");

        // 测试有效排序顺序
        role.setSortOrder(10);
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("sortOrder")), "有效排序顺序不应该产生验证错误");

        // 测试零排序顺序
        role.setSortOrder(0);
        violations = validator.validate(role);
        assertFalse(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("sortOrder")), "零排序顺序应该被允许");
    }

    @Test
    @DisplayName("角色类型验证")
    void testRoleTypeValidation() {
        // 测试null类型
        role.setType(null);
        Set<ConstraintViolation<Role>> violations = validator.validate(role);
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("type")), "null角色类型应该产生验证错误");

        // 测试有效类型
        for (Role.RoleType type : Role.RoleType.values()) {
            role.setType(type);
            violations = validator.validate(role);
            assertFalse(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("type")), 
                "有效角色类型 " + type + " 不应该产生验证错误");
        }
    }
} 