package tslc.beihaiyun.lyra.security.interceptor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.security.annotation.RequiresPermission;
import tslc.beihaiyun.lyra.service.PermissionService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PermissionInterceptor 单元测试
 * 测试方法级权限控制的各种场景
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("权限拦截器测试")
class PermissionInterceptorTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Method method;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private LyraUserPrincipal userPrincipal;

    @InjectMocks
    private PermissionInterceptor permissionInterceptor;

    @BeforeEach
    void setUp() {
        // 设置Spring Security上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(1L);

        // 设置JoinPoint基础信息
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getName()).thenReturn("testMethod");
    }

    // ========== 用户认证测试 ==========

    @Test
    @DisplayName("权限检查 - 用户未认证")
    void should_ThrowAccessDeniedException_When_UserNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("用户未认证");
    }

    @Test
    @DisplayName("权限检查 - 认证无效")
    void should_ThrowAccessDeniedException_When_AuthenticationInvalid() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("用户未认证");
    }

    @Test
    @DisplayName("权限检查 - 无法获取用户ID")
    void should_ThrowAccessDeniedException_When_CannotGetUserId() {
        // Given
        when(authentication.getPrincipal()).thenReturn("invalidPrincipal");
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("无法获取用户ID");
    }

    // ========== 普通权限检查测试 ==========

    @Test
    @DisplayName("普通权限检查 - 拥有权限")
    void should_PassCheck_When_UserHasPermission() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(true);

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verify(permissionService).hasPermission(1L, "file.read");
    }

    @Test
    @DisplayName("普通权限检查 - 没有权限")
    void should_ThrowAccessDeniedException_When_UserDoesNotHavePermission() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("权限不足，无法访问该资源");

        verify(permissionService).hasPermission(1L, "file.read");
    }

    @Test
    @DisplayName("普通权限检查 - 自定义错误消息")
    void should_ThrowAccessDeniedExceptionWithCustomMessage_When_UserDoesNotHavePermission() {
        // Given
        String customMessage = "您没有访问该文件的权限";
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read"}, RequiresPermission.Logical.AND, false, "", customMessage);
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage(customMessage);
    }

    // ========== 多权限逻辑测试 ==========

    @Test
    @DisplayName("多权限检查 - 逻辑与，全部拥有")
    void should_PassCheck_When_UserHasAllPermissionsWithAndLogic() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read", "file.write"}, RequiresPermission.Logical.AND);
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(true);
        when(permissionService.hasPermission(1L, "file.write")).thenReturn(true);

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verify(permissionService).hasPermission(1L, "file.read");
        verify(permissionService).hasPermission(1L, "file.write");
    }

    @Test
    @DisplayName("多权限检查 - 逻辑与，部分缺失")
    void should_ThrowAccessDeniedException_When_UserMissesSomePermissionsWithAndLogic() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read", "file.write"}, RequiresPermission.Logical.AND);
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(true);
        when(permissionService.hasPermission(1L, "file.write")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasPermission(1L, "file.read");
        verify(permissionService).hasPermission(1L, "file.write");
    }

    @Test
    @DisplayName("多权限检查 - 逻辑或，拥有其中一个")
    void should_PassCheck_When_UserHasAnyPermissionWithOrLogic() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read", "file.write"}, RequiresPermission.Logical.OR);
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(false);
        when(permissionService.hasPermission(1L, "file.write")).thenReturn(true);

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verify(permissionService).hasPermission(1L, "file.read");
        verify(permissionService).hasPermission(1L, "file.write");
    }

    @Test
    @DisplayName("多权限检查 - 逻辑或，都没有")
    void should_ThrowAccessDeniedException_When_UserHasNoPermissionWithOrLogic() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read", "file.write"}, RequiresPermission.Logical.OR);
        when(permissionService.hasPermission(1L, "file.read")).thenReturn(false);
        when(permissionService.hasPermission(1L, "file.write")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasPermission(1L, "file.read");
        verify(permissionService).hasPermission(1L, "file.write");
    }

    // ========== 资源权限检查测试 ==========

    @Test
    @DisplayName("资源权限检查 - 拥有权限")
    void should_PassCheck_When_UserHasResourcePermission() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read"}, RequiresPermission.Logical.AND, true, "FILE");
        
        setupMethodParameters("testMethod", "spaceId", "fileId");
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 2L});
        when(permissionService.hasResourcePermission(1L, 1L, "FILE", 2L, "file.read")).thenReturn(true);

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verify(permissionService).hasResourcePermission(1L, 1L, "FILE", 2L, "file.read");
    }

    @Test
    @DisplayName("资源权限检查 - 没有权限")
    void should_ThrowAccessDeniedException_When_UserDoesNotHaveResourcePermission() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.write"}, RequiresPermission.Logical.AND, true, "FILE");
        
        setupMethodParameters("testMethod", "spaceId", "fileId");
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 2L});
        when(permissionService.hasResourcePermission(1L, 1L, "FILE", 2L, "file.write")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasResourcePermission(1L, 1L, "FILE", 2L, "file.write");
    }

    @Test
    @DisplayName("资源权限检查 - 资源类型为空")
    void should_ThrowAccessDeniedException_When_ResourceTypeIsEmpty() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read"}, RequiresPermission.Logical.AND, true, "");

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("资源权限检查 - 无法提取资源信息")
    void should_ThrowAccessDeniedException_When_CannotExtractResourceInfo() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(
                new String[]{"file.read"}, RequiresPermission.Logical.AND, true, "FILE");
        
        setupMethodParameters("testMethod", "invalidParam");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"not_a_number"});

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ========== 无权限要求测试 ==========

    @Test
    @DisplayName("权限检查 - 无权限要求")
    void should_PassCheck_When_NoPermissionRequired() {
        // Given
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{});

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verifyNoInteractions(permissionService);
    }

    // ========== 不同Principal类型测试 ==========

    @Test
    @DisplayName("用户ID提取 - String类型Principal")
    void should_ExtractUserIdFromStringPrincipal() {
        // Given
        when(authentication.getPrincipal()).thenReturn("123");
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});
        when(permissionService.hasPermission(123L, "file.read")).thenReturn(true);

        // When & Then
        assertThatCode(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .doesNotThrowAnyException();

        verify(permissionService).hasPermission(123L, "file.read");
    }

    @Test
    @DisplayName("用户ID提取 - 无效String类型Principal")
    void should_ThrowAccessDeniedException_When_InvalidStringPrincipal() {
        // Given
        when(authentication.getPrincipal()).thenReturn("not_a_number");
        RequiresPermission annotation = createRequiresPermissionAnnotation(new String[]{"file.read"});

        // When & Then
        assertThatThrownBy(() -> permissionInterceptor.checkPermission(joinPoint, annotation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("无法获取用户ID");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建RequiresPermission注解实例
     */
    private RequiresPermission createRequiresPermissionAnnotation(String[] permissions) {
        return createRequiresPermissionAnnotation(permissions, RequiresPermission.Logical.AND, 
                                                 false, "", "权限不足，无法访问该资源");
    }

    /**
     * 创建RequiresPermission注解实例（带逻辑类型）
     */
    private RequiresPermission createRequiresPermissionAnnotation(String[] permissions, 
                                                                RequiresPermission.Logical logical) {
        return createRequiresPermissionAnnotation(permissions, logical, false, "", "权限不足，无法访问该资源");
    }

    /**
     * 创建RequiresPermission注解实例（带资源检查）
     */
    private RequiresPermission createRequiresPermissionAnnotation(String[] permissions, 
                                                                RequiresPermission.Logical logical,
                                                                boolean checkResource, 
                                                                String resourceType) {
        return createRequiresPermissionAnnotation(permissions, logical, checkResource, 
                                                 resourceType, "权限不足，无法访问该资源");
    }

    /**
     * 创建RequiresPermission注解实例（完整参数）
     */
    private RequiresPermission createRequiresPermissionAnnotation(String[] permissions, 
                                                                RequiresPermission.Logical logical,
                                                                boolean checkResource, 
                                                                String resourceType,
                                                                String message) {
        return new RequiresPermission() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequiresPermission.class;
            }

            @Override
            public String[] value() {
                return permissions;
            }

            @Override
            public Logical logical() {
                return logical;
            }

            @Override
            public boolean checkResource() {
                return checkResource;
            }

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public String message() {
                return message;
            }
        };
    }

    /**
     * 设置方法参数信息
     */
    private void setupMethodParameters(String methodName, String... paramNames) {
        Parameter[] parameters = new Parameter[paramNames.length];
        for (int i = 0; i < paramNames.length; i++) {
            Parameter param = mock(Parameter.class);
            when(param.getName()).thenReturn(paramNames[i]);
            parameters[i] = param;
        }
        when(method.getParameters()).thenReturn(parameters);
        when(methodSignature.getName()).thenReturn(methodName);
    }
} 