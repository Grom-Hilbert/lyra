package tslc.beihaiyun.lyra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.SpacePermission;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.SpacePermissionRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 权限管理服务类
 * 处理用户角色和空间权限相关的业务逻辑
 * 
 * @author Lyra Team
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
} 