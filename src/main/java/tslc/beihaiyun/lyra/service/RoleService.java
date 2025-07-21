package tslc.beihaiyun.lyra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.PermissionRepository;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 角色管理服务类
 * 处理角色分配和管理、权限继承策略、权限覆盖逻辑以及角色权限的动态检查
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;

    // ========== 角色基础CRUD操作 ==========

    /**
     * 创建新角色
     * 
     * @param role 角色对象
     * @return 保存后的角色
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public Role createRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("角色对象不能为空");
        }

        // 验证角色代码唯一性
        if (roleRepository.existsByCode(role.getCode())) {
            throw new IllegalArgumentException("角色代码已存在: " + role.getCode());
        }

        // 验证角色名称唯一性
        if (roleRepository.existsByName(role.getName())) {
            throw new IllegalArgumentException("角色名称已存在: " + role.getName());
        }

        // 设置默认值
        if (role.getSortOrder() == null || role.getSortOrder() <= 0) {
            Integer maxOrder = roleRepository.findMaxSortOrder();
            role.setSortOrder(maxOrder != null ? maxOrder + 1 : 1);
        }

        Role savedRole = roleRepository.save(role);
        log.info("创建角色成功: {}", savedRole);
        return savedRole;
    }

    /**
     * 更新角色信息
     * 
     * @param roleId 角色ID
     * @param updateRole 更新的角色信息
     * @return 更新后的角色
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public Role updateRole(Long roleId, Role updateRole) {
        Role existingRole = getRoleById(roleId);

        // 检查系统角色保护
        if (Boolean.TRUE.equals(existingRole.getSystem()) && 
            (!existingRole.getCode().equals(updateRole.getCode()) || 
             !existingRole.getName().equals(updateRole.getName()))) {
            throw new IllegalStateException("系统角色的代码和名称不能修改");
        }

        // 验证唯一性（排除当前角色）
        if (!existingRole.getCode().equals(updateRole.getCode()) && 
            roleRepository.existsByCodeAndIdNot(updateRole.getCode(), roleId)) {
            throw new IllegalArgumentException("角色代码已存在: " + updateRole.getCode());
        }

        if (!existingRole.getName().equals(updateRole.getName()) && 
            roleRepository.existsByNameAndIdNot(updateRole.getName(), roleId)) {
            throw new IllegalArgumentException("角色名称已存在: " + updateRole.getName());
        }

        // 更新角色信息
        existingRole.setCode(updateRole.getCode());
        existingRole.setName(updateRole.getName());
        existingRole.setDescription(updateRole.getDescription());
        existingRole.setType(updateRole.getType());
        existingRole.setEnabled(updateRole.getEnabled());
        existingRole.setSortOrder(updateRole.getSortOrder());

        Role savedRole = roleRepository.save(existingRole);
        log.info("更新角色成功: {}", savedRole);
        return savedRole;
    }

    /**
     * 根据ID获取角色
     * 
     * @param roleId 角色ID
     * @return 角色对象
     */
    @Cacheable(value = CacheConfig.ROLE_CACHE, key = "#roleId")
    public Role getRoleById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));
    }

    /**
     * 根据代码获取角色
     * 
     * @param code 角色代码
     * @return 角色对象
     */
    @Cacheable(value = CacheConfig.ROLE_CACHE, key = "'code:' + #code")
    public Optional<Role> getRoleByCode(String code) {
        return roleRepository.findByCode(code);
    }

    /**
     * 删除角色
     * 
     * @param roleId 角色ID
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public void deleteRole(Long roleId) {
        Role role = getRoleById(roleId);

        // 检查系统角色保护
        if (Boolean.TRUE.equals(role.getSystem())) {
            throw new IllegalStateException("系统角色不能删除");
        }

        // 检查是否有用户关联
        long userCount = userRoleRepository.countByRoleId(roleId);
        if (userCount > 0) {
            throw new IllegalStateException("角色仍有用户关联，无法删除。请先移除所有用户关联");
        }

        // 软删除角色
        role.setDeleted(true);
        roleRepository.save(role);
        log.info("删除角色成功: {}", role);
    }

    /**
     * 获取所有启用的角色
     * 
     * @return 启用的角色列表
     */
    @Cacheable(value = CacheConfig.ROLE_CACHE, key = "'enabled'")
    public List<Role> getEnabledRoles() {
        return roleRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    /**
     * 分页查询角色
     * 
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    public Page<Role> getRoles(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }

    /**
     * 搜索角色
     * 
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    public Page<Role> searchRoles(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return getRoles(pageable);
        }
        return roleRepository.searchRoles(keyword.trim(), pageable);
    }

    // ========== 角色分配管理 ==========

    /**
     * 为用户分配角色
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param assignedBy 分配人
     * @param reason 分配原因
     * @param expiresAt 过期时间（可选）
     * @return 用户角色关联
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.USER_ROLES_CACHE, CacheConfig.PERMISSION_CHECK_CACHE}, allEntries = true)
    public UserRole assignRoleToUser(Long userId, Long roleId, String assignedBy, 
                                   String reason, LocalDateTime expiresAt) {
        // 验证用户和角色存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        Role role = getRoleById(roleId);

        // 检查角色是否可用
        if (!role.isAvailable()) {
            throw new IllegalStateException("角色不可用: " + role.getName());
        }

        // 检查是否已经分配
        Optional<UserRole> existingUserRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
        if (existingUserRole.isPresent() && !existingUserRole.get().isDeleted()) {
            UserRole existing = existingUserRole.get();
            if (UserRole.AssignmentStatus.ACTIVE.equals(existing.getStatus())) {
                throw new IllegalStateException("用户已拥有该角色");
            }
            // 如果是其他状态，重新激活
            existing.setStatus(UserRole.AssignmentStatus.ACTIVE);
            existing.setAssignedBy(assignedBy);
            existing.setAssignmentReason(reason);
            existing.setExpiresAt(expiresAt);
            existing.setEffectiveAt(LocalDateTime.now());
            return userRoleRepository.save(existing);
        }

        // 创建新的用户角色关联
        UserRole userRole = new UserRole(userId, roleId, assignedBy, reason);
        userRole.setExpiresAt(expiresAt);

        UserRole savedUserRole = userRoleRepository.save(userRole);
        log.info("为用户[{}]分配角色[{}]成功，分配人: {}", userId, roleId, assignedBy);
        
        // 清除权限缓存
        permissionService.clearUserPermissionCache(userId);
        
        return savedUserRole;
    }

    /**
     * 批量为用户分配角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param assignedBy 分配人
     * @param reason 分配原因
     * @return 用户角色关联列表
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.USER_ROLES_CACHE, CacheConfig.PERMISSION_CHECK_CACHE}, allEntries = true)
    public List<UserRole> assignRolesToUser(Long userId, List<Long> roleIds, 
                                          String assignedBy, String reason) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        List<UserRole> userRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            try {
                UserRole userRole = assignRoleToUser(userId, roleId, assignedBy, reason, null);
                userRoles.add(userRole);
            } catch (Exception e) {
                log.warn("分配角色失败: userId={}, roleId={}, error={}", userId, roleId, e.getMessage());
            }
        }

        return userRoles;
    }

    /**
     * 撤销用户角色
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param revokedBy 撤销人
     * @param reason 撤销原因
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.USER_ROLES_CACHE, CacheConfig.PERMISSION_CHECK_CACHE}, allEntries = true)
    public void revokeRoleFromUser(Long userId, Long roleId, String revokedBy, String reason) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("用户角色关联不存在"));

        // 检查系统角色保护
        Role role = getRoleById(roleId);
        if (Boolean.TRUE.equals(role.getSystem()) && role.isSystemAdmin()) {
            long adminCount = userRoleRepository.countByRoleIdAndStatus(roleId, UserRole.AssignmentStatus.ACTIVE);
            if (adminCount <= 1) {
                throw new IllegalStateException("系统必须至少保留一个系统管理员");
            }
        }

        userRole.revoke(revokedBy);
        userRole.setAssignmentReason(reason);
        userRoleRepository.save(userRole);

        log.info("撤销用户[{}]角色[{}]成功，撤销人: {}", userId, roleId, revokedBy);
        
        // 清除权限缓存
        permissionService.clearUserPermissionCache(userId);
    }

    /**
     * 更新用户角色过期时间
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param expiresAt 新的过期时间
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.USER_ROLES_CACHE, CacheConfig.PERMISSION_CHECK_CACHE}, allEntries = true)
    public void updateRoleExpiration(Long userId, Long roleId, LocalDateTime expiresAt) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("用户角色关联不存在"));

        userRole.setExpiresAt(expiresAt);
        userRoleRepository.save(userRole);

        log.info("更新用户[{}]角色[{}]过期时间: {}", userId, roleId, expiresAt);
    }

    // ========== 权限继承策略实现 ==========

    /**
     * 获取用户的有效权限（包含角色继承）
     * 
     * @param userId 用户ID
     * @return 有效权限集合
     */
    @Cacheable(value = CacheConfig.USER_PERMISSIONS_CACHE, key = "'inherited:' + #userId")
    public Set<Permission> getUserEffectivePermissions(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 首先尝试使用JOIN FETCH查询
            List<UserRole> validUserRoles;
            try {
                validUserRoles = userRoleRepository.findValidUserRolesWithRole(userId, now);
            } catch (Exception e) {
                log.warn("JOIN FETCH查询失败，使用简单查询: userId={}", userId);
                // 如果JOIN FETCH失败，使用简单查询
                validUserRoles = userRoleRepository.findValidUserRoles(userId, now);
            }
            
            if (validUserRoles.isEmpty()) {
                return Collections.emptySet();
            }

            // 收集所有角色的权限
            Set<Permission> effectivePermissions = new HashSet<>();
            Map<String, Permission> permissionMap = new HashMap<>();

            for (UserRole userRole : validUserRoles) {
                Role role = userRole.getRole();
                if (role == null) {
                    // 如果关联查询没有获取到Role，则手动查询
                    try {
                        role = getRoleById(userRole.getRoleId());
                    } catch (Exception ex) {
                        log.warn("获取角色失败，跳过: roleId={}", userRole.getRoleId());
                        continue;
                    }
                }
                
                if (role != null && role.isAvailable()) {
                    Set<Permission> rolePermissions = role.getPermissions();
                    
                    for (Permission permission : rolePermissions) {
                        if (permission.getIsEnabled()) {
                            // 实现权限覆盖逻辑
                            String permissionKey = generatePermissionKey(permission);
                            Permission existing = permissionMap.get(permissionKey);
                            
                            if (existing == null || permission.compareLevel(existing) > 0) {
                                permissionMap.put(permissionKey, permission);
                            }
                        }
                    }
                }
            }

            effectivePermissions.addAll(permissionMap.values());
            return effectivePermissions;

        } catch (Exception e) {
            log.error("获取用户有效权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取用户在角色层级中的有效权限代码
     * 
     * @param userId 用户ID
     * @return 权限代码集合
     */
    @Cacheable(value = CacheConfig.USER_PERMISSIONS_CACHE, key = "'codes:' + #userId")
    public Set<String> getUserEffectivePermissionCodes(Long userId) {
        return getUserEffectivePermissions(userId).stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否拥有指定权限（考虑角色继承）
     * 
     * @param userId 用户ID
     * @param permissionCode 权限代码
     * @return 是否拥有权限
     */
    @Cacheable(value = CacheConfig.PERMISSION_CHECK_CACHE, key = "'role:' + #userId + ':' + #permissionCode")
    public boolean hasPermissionThroughRoles(Long userId, String permissionCode) {
        if (userId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        Set<String> userPermissions = getUserEffectivePermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }

    /**
     * 检查用户是否拥有指定类型和类别的权限
     * 
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param category 权限类别
     * @return 是否拥有权限
     */
    public boolean hasPermissionByTypeAndCategory(Long userId, String resourceType, String category) {
        Set<Permission> permissions = getUserEffectivePermissions(userId);
        return permissions.stream()
                .anyMatch(permission -> permission.isCompatible(resourceType, category));
    }

    // ========== 权限覆盖逻辑 ==========

    /**
     * 解析权限冲突并应用覆盖策略
     * 当用户通过多个角色拥有同一类型但不同级别的权限时，取级别最高的权限
     * 
     * @param permissions 权限列表
     * @return 解析后的权限映射
     */
    public Map<String, Permission> resolvePermissionConflicts(Set<Permission> permissions) {
        Map<String, Permission> resolvedPermissions = new HashMap<>();

        for (Permission permission : permissions) {
            String key = generatePermissionKey(permission);
            Permission existing = resolvedPermissions.get(key);

            if (existing == null) {
                resolvedPermissions.put(key, permission);
            } else {
                // 应用权限覆盖策略：取级别更高的权限
                if (permission.compareLevel(existing) > 0) {
                    resolvedPermissions.put(key, permission);
                    log.debug("权限覆盖: {} 级别 {} 覆盖 {} 级别 {}", 
                             permission.getCode(), permission.getLevel(),
                             existing.getCode(), existing.getLevel());
                }
            }
        }

        return resolvedPermissions;
    }

    /**
     * 生成权限唯一标识键
     * 
     * @param permission 权限对象
     * @return 权限键
     */
    private String generatePermissionKey(Permission permission) {
        return permission.getResourceType() + ":" + permission.getCategory();
    }

    /**
     * 计算用户在指定资源类型上的最高权限级别
     * 
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @return 最高权限级别
     */
    public Integer getHighestPermissionLevel(Long userId, String resourceType) {
        Set<Permission> permissions = getUserEffectivePermissions(userId);
        return permissions.stream()
                .filter(permission -> resourceType.equals(permission.getResourceType()))
                .mapToInt(Permission::getLevel)
                .max()
                .orElse(0);
    }

    // ========== 角色权限动态检查 ==========

    /**
     * 动态检查用户角色权限
     * 
     * @param userId 用户ID
     * @return 权限检查结果
     */
    @Cacheable(value = CacheConfig.PERMISSION_CHECK_CACHE, key = "'dynamic:' + #userId")
    public Map<String, Object> checkUserRolePermissions(Long userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        try {
            // 获取用户的所有角色关联
            List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
            
            // 统计角色状态
            Map<UserRole.AssignmentStatus, Long> statusCount = userRoles.stream()
                    .collect(Collectors.groupingBy(UserRole::getStatus, Collectors.counting()));

            // 检查即将过期的角色
            List<UserRole> expiringSoon = userRoles.stream()
                    .filter(ur -> ur.getExpiresAt() != null)
                    .filter(ur -> ur.getExpiresAt().isAfter(now) && 
                                 ur.getExpiresAt().isBefore(now.plusDays(7)))
                    .collect(Collectors.toList());

            // 获取有效角色和权限
            List<UserRole> validRoles = userRoles.stream()
                    .filter(UserRole::isValid)
                    .collect(Collectors.toList());

            Set<Permission> effectivePermissions = getUserEffectivePermissions(userId);

            // 构建检查结果
            result.put("userId", userId);
            result.put("checkTime", now);
            result.put("totalRoles", userRoles.size());
            result.put("validRoles", validRoles.size());
            result.put("statusDistribution", statusCount);
            result.put("expiringSoonCount", expiringSoon.size());
            result.put("effectivePermissionsCount", effectivePermissions.size());
            result.put("hasAdminPermission", hasAdminPermission(userId));
            result.put("permissionsByResourceType", groupPermissionsByResourceType(effectivePermissions));

            return result;

        } catch (Exception e) {
            log.error("动态检查用户角色权限失败: userId={}", userId, e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 检查用户是否拥有管理员权限
     * 
     * @param userId 用户ID
     * @return 是否为管理员
     */
    public boolean hasAdminPermission(Long userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<UserRole> validUserRoles = userRoleRepository.findValidUserRoles(userId, now);
            
            return validUserRoles.stream()
                    .map(UserRole::getRoleId)
                    .map(roleId -> {
                        try {
                            return getRoleById(roleId);
                        } catch (Exception e) {
                            log.warn("获取角色失败，跳过: roleId={}", roleId);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .anyMatch(Role::isAdminRole);
        } catch (Exception e) {
            log.error("检查管理员权限失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 按资源类型分组权限
     * 
     * @param permissions 权限集合
     * @return 分组结果
     */
    private Map<String, List<String>> groupPermissionsByResourceType(Set<Permission> permissions) {
        return permissions.stream()
                .collect(Collectors.groupingBy(
                    Permission::getResourceType,
                    Collectors.mapping(Permission::getCode, Collectors.toList())
                ));
    }

    // ========== 角色层级管理 ==========

    /**
     * 获取角色层级结构
     * 
     * @return 角色层级映射
     */
    @Cacheable(value = CacheConfig.ROLE_CACHE, key = "'hierarchy'")
    public Map<Role.RoleType, List<Role>> getRoleHierarchy() {
        List<Role> allRoles = roleRepository.findByEnabledTrueOrderBySortOrderAsc();
        return allRoles.stream()
                .collect(Collectors.groupingBy(Role::getType));
    }

    /**
     * 检查角色是否可以分配给用户
     * 基于角色层级和业务规则进行检查
     * 
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否可以分配
     */
    public boolean canAssignRoleToUser(Long userId, Long roleId) {
        try {
            Role role = getRoleById(roleId);
            
            // 检查角色是否可用
            if (!role.isAvailable()) {
                return false;
            }

            // 系统管理员角色需要特殊权限才能分配
            if (role.isSystemAdmin()) {
                // 这里可以添加更严格的检查逻辑
                // 例如：只有超级管理员才能分配系统管理员角色
                return true; // 简化实现
            }

            // 检查用户当前角色是否冲突
            LocalDateTime now = LocalDateTime.now();
            List<UserRole> userRoles = userRoleRepository.findValidUserRoles(userId, now);
            
            // 检查是否存在角色冲突（例如：不能同时拥有管理员和普通用户角色）
            boolean hasAdminRole = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .map(this::getRoleById)
                    .anyMatch(Role::isAdminRole);

            if (role.isUserRole() && hasAdminRole) {
                return false; // 管理员不能降级为普通用户
            }

            return true;

        } catch (Exception e) {
            log.error("检查角色分配权限失败: userId={}, roleId={}", userId, roleId, e);
            return false;
        }
    }

    // ========== 角色状态管理 ==========

    /**
     * 启用角色
     * 
     * @param roleId 角色ID
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public void enableRole(Long roleId) {
        Role role = getRoleById(roleId);
        role.enable();
        roleRepository.save(role);
        log.info("启用角色: {}", role);
    }

    /**
     * 禁用角色
     * 
     * @param roleId 角色ID
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public void disableRole(Long roleId) {
        Role role = getRoleById(roleId);
        
        // 检查系统角色保护
        if (Boolean.TRUE.equals(role.getSystem())) {
            throw new IllegalStateException("系统角色不能禁用");
        }

        role.disable();
        roleRepository.save(role);
        log.info("禁用角色: {}", role);
    }

    /**
     * 批量更新角色状态
     * 
     * @param roleIds 角色ID列表
     * @param enabled 是否启用
     * @return 更新数量
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.ROLE_CACHE, CacheConfig.USER_ROLES_CACHE}, allEntries = true)
    public int batchUpdateRoleStatus(List<Long> roleIds, Boolean enabled) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return 0;
        }

        // 过滤掉系统角色
        List<Long> updatableRoleIds = roleIds.stream()
                .filter(roleId -> {
                    try {
                        Role role = getRoleById(roleId);
                        return !Boolean.TRUE.equals(role.getSystem());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        if (updatableRoleIds.isEmpty()) {
            return 0;
        }

        int updated = roleRepository.updateEnabledByIds(updatableRoleIds, enabled);
        log.info("批量更新角色状态: 数量={}, 启用={}", updated, enabled);
        return updated;
    }

    // ========== 定时任务和维护操作 ==========

    /**
     * 更新过期的用户角色状态
     * 
     * @return 更新数量
     */
    @Transactional
    public int updateExpiredUserRoles() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("开始更新过期用户角色，当前时间: {}", now);
        
        int updated = userRoleRepository.updateExpiredUserRoles(now,
            UserRole.AssignmentStatus.ACTIVE,
            UserRole.AssignmentStatus.EXPIRED);
        if (updated > 0) {
            log.info("更新过期用户角色数量: {}", updated);
            // 清除相关缓存
            clearAllRoleCache();
        } else {
            log.debug("没有找到需要更新的过期用户角色");
        }
        return updated;
    }

    /**
     * 激活到期生效的用户角色
     * 
     * @return 激活数量
     */
    @Transactional
    public int activatePendingUserRoles() {
        LocalDateTime now = LocalDateTime.now();
        int activated = userRoleRepository.activatePendingUserRoles(now,
            UserRole.AssignmentStatus.PENDING,
            UserRole.AssignmentStatus.ACTIVE);
        if (activated > 0) {
            log.info("激活待生效用户角色数量: {}", activated);
            // 清除相关缓存
            clearAllRoleCache();
        }
        return activated;
    }

    // ========== 缓存管理 ==========

    /**
     * 清除角色相关缓存
     */
    @CacheEvict(value = {
        CacheConfig.ROLE_CACHE,
        CacheConfig.USER_ROLES_CACHE,
        CacheConfig.USER_PERMISSIONS_CACHE,
        CacheConfig.PERMISSION_CHECK_CACHE
    }, allEntries = true)
    public void clearAllRoleCache() {
        log.info("已清除所有角色相关缓存");
    }

    /**
     * 清除用户角色缓存
     * 
     * @param userId 用户ID
     */
    @CacheEvict(value = {
        CacheConfig.USER_ROLES_CACHE,
        CacheConfig.USER_PERMISSIONS_CACHE,
        CacheConfig.PERMISSION_CHECK_CACHE
    }, key = "#userId")
    public void clearUserRoleCache(Long userId) {
        log.info("已清除用户角色缓存: userId={}", userId);
    }

    // ========== 统计和报告 ==========

    /**
     * 获取角色统计信息
     * 
     * @return 统计信息
     */
    @Cacheable(value = CacheConfig.ROLE_CACHE, key = "'statistics'")
    public Map<String, Object> getRoleStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 角色数量统计
        stats.put("totalRoles", roleRepository.count());
        stats.put("enabledRoles", roleRepository.countByEnabledTrue());
        stats.put("systemRoles", roleRepository.countBySystemTrue());
        stats.put("customRoles", roleRepository.countBySystemFalse());

        // 按类型统计
        List<Object[]> typeStats = roleRepository.countByType();
        Map<String, Long> typeCount = typeStats.stream()
                .collect(Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));
        stats.put("rolesByType", typeCount);

        // 用户角色关联统计
        List<Object[]> statusStats = userRoleRepository.countByStatus();
        Map<String, Long> statusCount = statusStats.stream()
                .collect(Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));
        stats.put("userRolesByStatus", statusCount);

        return stats;
    }

    /**
     * 获取用户的角色信息
     * 
     * @param userId 用户ID
     * @return 用户角色信息
     */
    @Cacheable(value = CacheConfig.USER_ROLES_CACHE, key = "#userId")
    public Map<String, Object> getUserRoleInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        List<UserRole> allUserRoles = userRoleRepository.findByUserId(userId);
        List<UserRole> validUserRoles = userRoleRepository.findValidUserRoles(userId, now);

        info.put("userId", userId);
        info.put("totalRoles", allUserRoles.size());
        info.put("validRoles", validUserRoles.size());
        info.put("roleDetails", validUserRoles.stream()
                .map(ur -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("roleId", ur.getRoleId());
                    details.put("roleName", getRoleById(ur.getRoleId()).getName());
                    details.put("effectiveAt", ur.getEffectiveAt());
                    details.put("expiresAt", ur.getExpiresAt()); // 可能为null
                    details.put("assignedBy", ur.getAssignedBy()); // 可能为null
                    return details;
                })
                .collect(Collectors.toList()));

        return info;
    }
} 