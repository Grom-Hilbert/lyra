package tslc.beihaiyun.lyra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.SpacePermission;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.PermissionRepository;
import tslc.beihaiyun.lyra.repository.SpacePermissionRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理服务类
 * 处理用户角色和空间权限相关的业务逻辑
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final SpacePermissionRepository spacePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;

    // ========== SpacePermission 业务方法 ==========

    /**
     * 检查权限是否已过期
     * 
     * @param permission 权限对象
     * @return 是否已过期
     */
    public boolean isPermissionExpired(SpacePermission permission) {
        LocalDateTime expiresAt = permission.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查权限是否已授权
     * 
     * @param permission 权限对象
     * @return 是否已授权
     */
    public boolean isPermissionGranted(SpacePermission permission) {
        return "GRANTED".equals(permission.getStatus()) && !isPermissionExpired(permission);
    }

    /**
     * 检查权限是否被拒绝
     * 
     * @param permission 权限对象
     * @return 是否被拒绝
     */
    public boolean isPermissionDenied(SpacePermission permission) {
        return "DENIED".equals(permission.getStatus());
    }

    /**
     * 检查权限是否为继承权限
     * 
     * @param permission 权限对象
     * @return 是否为继承权限
     */
    public boolean isInheritedPermission(SpacePermission permission) {
        return Boolean.TRUE.equals(permission.getInheritFromParent());
    }

    /**
     * 检查权限是否为直接授权
     * 
     * @param permission 权限对象
     * @return 是否为直接授权
     */
    public boolean isDirectGrant(SpacePermission permission) {
        return "DIRECT".equals(permission.getGrantType());
    }

    /**
     * 检查权限是否为继承授权
     * 
     * @param permission 权限对象
     * @return 是否为继承授权
     */
    public boolean isInheritedGrant(SpacePermission permission) {
        return "INHERITED".equals(permission.getGrantType());
    }

    /**
     * 检查权限是否为基于角色的授权
     * 
     * @param permission 权限对象
     * @return 是否为基于角色的授权
     */
    public boolean isRoleBasedGrant(SpacePermission permission) {
        return "ROLE_BASED".equals(permission.getGrantType());
    }

    /**
     * 检查是否为文件权限
     * 
     * @param permission 权限对象
     * @return 是否为文件权限
     */
    public boolean isFilePermission(SpacePermission permission) {
        return "FILE".equals(permission.getResourceType());
    }

    /**
     * 检查是否为文件夹权限
     * 
     * @param permission 权限对象
     * @return 是否为文件夹权限
     */
    public boolean isFolderPermission(SpacePermission permission) {
        return "FOLDER".equals(permission.getResourceType());
    }

    /**
     * 检查是否为空间权限
     * 
     * @param permission 权限对象
     * @return 是否为空间权限
     */
    public boolean isSpacePermission(SpacePermission permission) {
        return "SPACE".equals(permission.getResourceType());
    }

    // ========== UserRole 业务方法 ==========

    /**
     * 检查用户角色是否有效
     * 
     * @param userRole 用户角色对象
     * @return 是否有效
     */
    public boolean isUserRoleValid(UserRole userRole) {
        return UserRole.AssignmentStatus.ACTIVE.equals(userRole.getStatus()) && 
               !isUserRoleExpired(userRole);
    }

    /**
     * 检查用户角色是否已过期
     * 
     * @param userRole 用户角色对象
     * @return 是否已过期
     */
    public boolean isUserRoleExpired(UserRole userRole) {
        LocalDateTime expiresAt = userRole.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查用户角色是否为待处理状态
     * 
     * @param userRole 用户角色对象
     * @return 是否为待处理状态
     */
    public boolean isUserRolePending(UserRole userRole) {
        return UserRole.AssignmentStatus.PENDING.equals(userRole.getStatus());
    }

    /**
     * 激活用户角色
     * 
     * @param userRoleId 用户角色ID
     * @param reason 激活原因
     * @return 更新后的用户角色
     */
    @Transactional
    public UserRole activateUserRole(Long userRoleId, String reason) {
        Optional<UserRole> userRoleOpt = userRoleRepository.findById(userRoleId);
        if (userRoleOpt.isPresent()) {
            UserRole userRole = userRoleOpt.get();
            userRole.setStatus(UserRole.AssignmentStatus.ACTIVE);
            if (userRole.getEffectiveAt() == null) {
                userRole.setEffectiveAt(LocalDateTime.now());
            }
            
            log.info("用户角色[ID:{}]已激活，原因: {}", userRoleId, reason);
            return userRoleRepository.save(userRole);
        }
        throw new IllegalArgumentException("用户角色不存在: " + userRoleId);
    }

    /**
     * 暂停用户角色
     * 
     * @param userRoleId 用户角色ID
     * @param reason 暂停原因
     * @return 更新后的用户角色
     */
    @Transactional
    public UserRole suspendUserRole(Long userRoleId, String reason) {
        Optional<UserRole> userRoleOpt = userRoleRepository.findById(userRoleId);
        if (userRoleOpt.isPresent()) {
            UserRole userRole = userRoleOpt.get();
            userRole.setStatus(UserRole.AssignmentStatus.SUSPENDED);
            
            log.warn("用户角色[ID:{}]已暂停，原因: {}", userRoleId, reason);
            return userRoleRepository.save(userRole);
        }
        throw new IllegalArgumentException("用户角色不存在: " + userRoleId);
    }

    /**
     * 授权空间权限
     * 
     * @param permissionId 权限ID
     * @param granterId 授权人ID
     * @param reason 授权原因
     * @return 更新后的权限
     */
    @Transactional
    public SpacePermission grantSpacePermission(Long permissionId, Long granterId, String reason) {
        Optional<SpacePermission> permOpt = spacePermissionRepository.findById(permissionId);
        if (permOpt.isPresent()) {
            SpacePermission permission = permOpt.get();
            permission.setStatus("GRANTED");
            permission.setGrantedBy(granterId);
            permission.setGrantedAt(LocalDateTime.now());
            permission.setRemark(reason);
            
            log.info("空间权限[ID:{}]已授权，授权人: {}, 原因: {}", permissionId, granterId, reason);
            return spacePermissionRepository.save(permission);
        }
        throw new IllegalArgumentException("权限不存在: " + permissionId);
    }

    /**
     * 拒绝空间权限
     * 
     * @param permissionId 权限ID
     * @param granterId 操作人ID
     * @param reason 拒绝原因
     * @return 更新后的权限
     */
    @Transactional
    public SpacePermission denySpacePermission(Long permissionId, Long granterId, String reason) {
        Optional<SpacePermission> permOpt = spacePermissionRepository.findById(permissionId);
        if (permOpt.isPresent()) {
            SpacePermission permission = permOpt.get();
            permission.setStatus("DENIED");
            permission.setGrantedBy(granterId);
            permission.setGrantedAt(LocalDateTime.now());
            permission.setRemark(reason);
            
            log.warn("空间权限[ID:{}]已拒绝，操作人: {}, 原因: {}", permissionId, granterId, reason);
            return spacePermissionRepository.save(permission);
        }
        throw new IllegalArgumentException("权限不存在: " + permissionId);
    }

    /**
     * 撤销空间权限
     * 
     * @param permissionId 权限ID
     * @param operatorId 操作人ID
     * @param reason 撤销原因
     * @return 更新后的权限
     */
    @Transactional
    public SpacePermission revokeSpacePermission(Long permissionId, Long operatorId, String reason) {
        Optional<SpacePermission> permOpt = spacePermissionRepository.findById(permissionId);
        if (permOpt.isPresent()) {
            SpacePermission permission = permOpt.get();
            permission.setStatus("REVOKED");
            permission.setRemark(reason);
            permission.setExpiresAt(LocalDateTime.now()); // 设置过期时间为当前时间
            
            log.info("空间权限[ID:{}]已撤销，操作人: {}, 原因: {}", permissionId, operatorId, reason);
            return spacePermissionRepository.save(permission);
        }
        throw new IllegalArgumentException("权限不存在: " + permissionId);
    }

    /**
     * 检查用户是否有指定权限级别
     * 
     * @param permission 权限对象
     * @param requiredLevel 所需权限级别
     * @return 是否有足够权限
     */
    public boolean hasRequiredPermissionLevel(SpacePermission permission, Integer requiredLevel) {
        if (!isPermissionGranted(permission) || requiredLevel == null) {
            return false;
        }
        
        Integer currentLevel = permission.getPermissionLevel();
        return currentLevel != null && currentLevel >= requiredLevel;
    }

    // ========== 核心权限检查方法 ==========

    /**
     * 检查用户是否拥有指定权限
     * 
     * @param userId 用户ID
     * @param permissionCode 权限代码
     * @return 是否拥有权限
     */
    @Cacheable(value = CacheConfig.PERMISSION_CHECK_CACHE, key = "#userId + ':' + #permissionCode")
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        try {
            // 首先检查用户是否通过角色拥有该权限
            List<Permission> userPermissions = permissionRepository.findPermissionsByUserId(userId);
            return userPermissions.stream()
                    .anyMatch(permission -> permission.getCode().equals(permissionCode) && permission.getIsEnabled());
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permissionCode={}", userId, permissionCode, e);
            return false;
        }
    }

    /**
     * 检查用户在指定空间对指定资源是否拥有权限
     * 
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param permissionCode 权限代码
     * @return 是否拥有权限
     */
    @Cacheable(value = CacheConfig.PERMISSION_CHECK_CACHE, 
               key = "#userId + ':' + #spaceId + ':' + #resourceType + ':' + #resourceId + ':' + #permissionCode")
    public boolean hasResourcePermission(Long userId, Long spaceId, String resourceType, 
                                       Long resourceId, String permissionCode) {
        if (userId == null || spaceId == null || !StringUtils.hasText(resourceType) || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        try {
            // 查找权限定义
            Optional<Permission> permissionOpt = permissionRepository.findByCode(permissionCode);
            if (permissionOpt.isEmpty() || !permissionOpt.get().getIsEnabled()) {
                return false;
            }

            Permission permission = permissionOpt.get();
            LocalDateTime now = LocalDateTime.now();

            // 查找用户在该资源上的有效权限
            List<SpacePermission> effectivePermissions = spacePermissionRepository
                    .findEffectiveResourcePermissions(userId, spaceId, resourceType, resourceId, now);

            // 检查直接权限
            boolean hasDirectPermission = effectivePermissions.stream()
                    .anyMatch(sp -> sp.getPermissionId().equals(permission.getId()) && 
                                   "GRANTED".equals(sp.getStatus()));

            if (hasDirectPermission) {
                return true;
            }

            // 如果没有直接权限，检查继承权限
            return checkInheritedPermission(userId, spaceId, resourceType, resourceId, permission);

        } catch (Exception e) {
            log.error("检查资源权限失败: userId={}, spaceId={}, resourceType={}, resourceId={}, permissionCode={}", 
                     userId, spaceId, resourceType, resourceId, permissionCode, e);
            return false;
        }
    }

    /**
     * 检查继承权限
     * 
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @param permission 权限对象
     * @return 是否拥有继承权限
     */
    private boolean checkInheritedPermission(Long userId, Long spaceId, String resourceType, 
                                           Long resourceId, Permission permission) {
        try {
            // 构建资源路径
            String resourcePath = buildResourcePath(spaceId, resourceType, resourceId);
            
            // 查找可继承的权限
            List<SpacePermission> inheritablePermissions = spacePermissionRepository
                    .findInheritablePermissions(userId, spaceId, getParentPath(resourcePath), resourceType);

            // 检查是否有匹配的可继承权限
            return inheritablePermissions.stream()
                    .anyMatch(sp -> sp.getPermissionId().equals(permission.getId()) && 
                                   "GRANTED".equals(sp.getStatus()) && 
                                   Boolean.TRUE.equals(sp.getInheritFromParent()));

        } catch (Exception e) {
            log.error("检查继承权限失败: userId={}, spaceId={}, resourceType={}, resourceId={}", 
                     userId, spaceId, resourceType, resourceId, e);
            return false;
        }
    }

    /**
     * 获取用户的所有权限代码
     * 
     * @param userId 用户ID
     * @return 权限代码集合
     */
    @Cacheable(value = CacheConfig.USER_PERMISSIONS_CACHE, key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        try {
            List<Permission> permissions = permissionRepository.findPermissionsByUserId(userId);
            return permissions.stream()
                    .filter(Permission::getIsEnabled)
                    .map(Permission::getCode)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取用户在指定空间的权限
     * 
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @return 空间权限列表
     */
    @Cacheable(value = CacheConfig.USER_PERMISSIONS_CACHE, key = "#userId + ':space:' + #spaceId")
    public List<SpacePermission> getUserSpacePermissions(Long userId, Long spaceId) {
        if (userId == null || spaceId == null) {
            return Collections.emptyList();
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            return spacePermissionRepository.findEffectivePermissions(userId, spaceId, now);
        } catch (Exception e) {
            log.error("获取用户空间权限失败: userId={}, spaceId={}", userId, spaceId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查用户是否有管理员权限
     * 
     * @param userId 用户ID
     * @return 是否为管理员
     */
    @Cacheable(value = CacheConfig.PERMISSION_CHECK_CACHE, key = "'admin:' + #userId")
    public boolean isAdmin(Long userId) {
        return hasPermission(userId, "system.admin") || hasPermission(userId, "system.super_admin");
    }

    /**
     * 检查用户是否有空间管理权限
     * 
     * @param userId 用户ID
     * @param spaceId 空间ID
     * @return 是否有空间管理权限
     */
    public boolean isSpaceAdmin(Long userId, Long spaceId) {
        return hasResourcePermission(userId, spaceId, "SPACE", null, "space.admin");
    }

    /**
     * 清除用户权限缓存
     * 
     * @param userId 用户ID
     */
    @CacheEvict(value = {
        CacheConfig.USER_PERMISSIONS_CACHE,
        CacheConfig.PERMISSION_CHECK_CACHE,
        CacheConfig.INHERITED_PERMISSIONS_CACHE
    }, key = "#userId")
    public void clearUserPermissionCache(Long userId) {
        log.info("已清除用户权限缓存: userId={}", userId);
    }

    /**
     * 清除所有权限缓存
     */
    @CacheEvict(value = {
        CacheConfig.USER_PERMISSIONS_CACHE,
        CacheConfig.PERMISSION_CHECK_CACHE,
        CacheConfig.INHERITED_PERMISSIONS_CACHE
    }, allEntries = true)
    public void clearAllPermissionCache() {
        log.info("已清除所有权限缓存");
    }

    // ========== 权限继承辅助方法 ==========

    /**
     * 构建资源路径
     * 
     * @param spaceId 空间ID
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return 资源路径
     */
    private String buildResourcePath(Long spaceId, String resourceType, Long resourceId) {
        StringBuilder path = new StringBuilder();
        path.append("/").append(spaceId);
        
        if (resourceId != null) {
            path.append("/").append(resourceType.toLowerCase()).append("/").append(resourceId);
        }
        
        return path.toString();
    }

    /**
     * 获取父路径
     * 
     * @param path 完整路径
     * @return 父路径
     */
    private String getParentPath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return null;
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlash);
    }

    /**
     * 检查权限级别是否足够
     * 
     * @param userPermissionLevel 用户权限级别
     * @param requiredLevel 所需权限级别
     * @return 是否足够
     */
    public boolean isPermissionLevelSufficient(Integer userPermissionLevel, Integer requiredLevel) {
        if (userPermissionLevel == null || requiredLevel == null) {
            return false;
        }
        return userPermissionLevel >= requiredLevel;
    }

    /**
     * 计算有效权限级别
     * 对于有冲突的权限，取级别最高的权限
     * 
     * @param permissions 权限列表
     * @return 有效权限级别
     */
    public Integer calculateEffectivePermissionLevel(List<SpacePermission> permissions) {
        return permissions.stream()
                .filter(this::isPermissionGranted)
                .mapToInt(SpacePermission::getPermissionLevel)
                .max()
                .orElse(0);
    }
} 