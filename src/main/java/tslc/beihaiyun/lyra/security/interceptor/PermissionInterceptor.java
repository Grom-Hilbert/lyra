package tslc.beihaiyun.lyra.security.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.security.annotation.RequiresPermission;
import tslc.beihaiyun.lyra.service.PermissionService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 权限拦截器
 * 实现方法级权限控制的AOP逻辑
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionInterceptor {

    private final PermissionService permissionService;

    /**
     * 权限检查切点
     * 拦截所有标记了@RequiresPermission注解的方法
     * 
     * @param joinPoint 连接点
     * @param requiresPermission 权限注解
     */
    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("用户未认证");
        }

        Long userId = getCurrentUserId(authentication);
        if (userId == null) {
            throw new AccessDeniedException("无法获取用户ID");
        }

        // 检查权限
        boolean hasPermission = checkUserPermission(userId, requiresPermission, joinPoint);
        
        if (!hasPermission) {
            String message = StringUtils.hasText(requiresPermission.message()) ? 
                           requiresPermission.message() : "权限不足，无法访问该资源";
            
            log.warn("用户权限检查失败: userId={}, permissions={}, method={}", 
                    userId, String.join(",", requiresPermission.value()), 
                    joinPoint.getSignature().getName());
            
            throw new AccessDeniedException(message);
        }

        log.debug("用户权限检查通过: userId={}, permissions={}, method={}", 
                 userId, String.join(",", requiresPermission.value()), 
                 joinPoint.getSignature().getName());
    }

    /**
     * 检查用户权限
     * 
     * @param userId 用户ID
     * @param requiresPermission 权限注解
     * @param joinPoint 连接点
     * @return 是否有权限
     */
    private boolean checkUserPermission(Long userId, RequiresPermission requiresPermission, JoinPoint joinPoint) {
        String[] permissions = requiresPermission.value();
        if (permissions.length == 0) {
            return true; // 没有指定权限要求，直接通过
        }

        // 检查资源权限
        if (requiresPermission.checkResource()) {
            return checkResourcePermission(userId, requiresPermission, joinPoint);
        }

        // 检查普通权限
        return checkGeneralPermission(userId, permissions, requiresPermission.logical());
    }

    /**
     * 检查普通权限
     * 
     * @param userId 用户ID
     * @param permissions 权限代码数组
     * @param logical 逻辑关系
     * @return 是否有权限
     */
    private boolean checkGeneralPermission(Long userId, String[] permissions, RequiresPermission.Logical logical) {
        if (logical == RequiresPermission.Logical.OR) {
            // 逻辑或：拥有任意一个权限即可
            for (String permission : permissions) {
                if (permissionService.hasPermission(userId, permission)) {
                    return true;
                }
            }
            return false;
        } else {
            // 逻辑与：需要同时拥有所有权限
            for (String permission : permissions) {
                if (!permissionService.hasPermission(userId, permission)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 检查资源权限
     * 
     * @param userId 用户ID
     * @param requiresPermission 权限注解
     * @param joinPoint 连接点
     * @return 是否有权限
     */
    private boolean checkResourcePermission(Long userId, RequiresPermission requiresPermission, JoinPoint joinPoint) {
        String resourceType = requiresPermission.resourceType();
        if (!StringUtils.hasText(resourceType)) {
            log.error("检查资源权限时资源类型不能为空");
            return false;
        }

        // 从方法参数中提取资源信息
        ResourceInfo resourceInfo = extractResourceInfo(joinPoint, resourceType);
        if (resourceInfo == null) {
            log.error("无法从方法参数中提取资源信息: resourceType={}, method={}", 
                     resourceType, joinPoint.getSignature().getName());
            return false;
        }

        String[] permissions = requiresPermission.value();
        RequiresPermission.Logical logical = requiresPermission.logical();

        if (logical == RequiresPermission.Logical.OR) {
            // 逻辑或：拥有任意一个权限即可
            for (String permission : permissions) {
                if (permissionService.hasResourcePermission(userId, resourceInfo.spaceId, 
                                                          resourceInfo.resourceType, resourceInfo.resourceId, permission)) {
                    return true;
                }
            }
            return false;
        } else {
            // 逻辑与：需要同时拥有所有权限
            for (String permission : permissions) {
                if (!permissionService.hasResourcePermission(userId, resourceInfo.spaceId, 
                                                           resourceInfo.resourceType, resourceInfo.resourceId, permission)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 从方法参数中提取资源信息
     * 
     * @param joinPoint 连接点
     * @param resourceType 资源类型
     * @return 资源信息
     */
    private ResourceInfo extractResourceInfo(JoinPoint joinPoint, String resourceType) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        Long spaceId = null;
        Long resourceId = null;

        // 尝试从参数中提取spaceId和resourceId
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            if (arg == null) {
                continue;
            }

            String paramName = parameter.getName().toLowerCase();
            
            // 提取spaceId
            if (spaceId == null && (paramName.contains("spaceid") || paramName.equals("space"))) {
                spaceId = extractLongValue(arg);
            }
            
            // 提取resourceId
            if (resourceId == null) {
                String resourceTypeLower = resourceType.toLowerCase();
                if (paramName.contains(resourceTypeLower + "id") || 
                    paramName.equals(resourceTypeLower) ||
                    paramName.equals("resourceid") ||
                    paramName.equals("id")) {
                    resourceId = extractLongValue(arg);
                }
            }
        }

        if (spaceId != null) {
            return new ResourceInfo(spaceId, resourceType, resourceId);
        }

        return null;
    }

    /**
     * 提取Long类型的值
     * 
     * @param obj 对象
     * @return Long值
     */
    private Long extractLongValue(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前用户ID
     * 
     * @param authentication 认证信息
     * @return 用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof LyraUserPrincipal) {
            return ((LyraUserPrincipal) principal).getId();
        }
        
        // 如果是其他类型的Principal，尝试从用户名获取
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.warn("无法从用户名解析用户ID: {}", principal);
            }
        }
        
        return null;
    }

    /**
     * 资源信息内部类
     */
    private static class ResourceInfo {
        final Long spaceId;
        final String resourceType;
        final Long resourceId;

        ResourceInfo(Long spaceId, String resourceType, Long resourceId) {
            this.spaceId = spaceId;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }
    }
} 