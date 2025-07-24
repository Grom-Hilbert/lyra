package tslc.beihaiyun.lyra.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.service.UserService;
import tslc.beihaiyun.lyra.service.RoleService;

import jakarta.validation.Valid;

/**
 * 系统管理控制器
 * 提供用户管理、系统管理等后台管理功能的REST API
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class SystemManagementController {

    private static final Logger logger = LoggerFactory.getLogger(SystemManagementController.class);

    private final UserService userService;
    private final RoleService roleService;

    @Autowired
    public SystemManagementController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    // ========== 用户管理 API ==========

    /**
     * 分页查询所有用户
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            // 创建排序对象
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<User> userPage;
            
            // 根据查询条件获取用户
            if (search != null && !search.trim().isEmpty()) {
                userPage = userService.searchUsers(search.trim(), pageable);
                logger.debug("搜索用户: {}, 结果数量: {}", search, userPage.getTotalElements());
            } else if (status != null && !status.trim().isEmpty()) {
                try {
                    User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
                    userPage = userService.findUsersByStatus(userStatus, pageable);
                    logger.debug("按状态查询用户: {}, 结果数量: {}", status, userPage.getTotalElements());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "无效的用户状态: " + status));
                }
            } else {
                userPage = userService.findAllUsers(pageable);
                logger.debug("查询所有用户, 结果数量: {}", userPage.getTotalElements());
            }
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent());
            response.put("currentPage", userPage.getNumber());
            response.put("totalPages", userPage.getTotalPages());
            response.put("totalElements", userPage.getTotalElements());
            response.put("pageSize", userPage.getSize());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询用户列表失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "查询用户列表失败：" + e.getMessage()));
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            
            // 添加存储使用率计算
            double storageUsageRatio = userService.calculateStorageUsageRatio(user);
            response.put("storageUsageRatio", storageUsageRatio);
            response.put("storageUsagePercentage", Math.round(storageUsageRatio * 100));
            
            // 添加用户角色信息
            try {
                response.put("roles", roleService.getUserRoles(userId));
            } catch (Exception e) {
                logger.warn("获取用户角色失败: {}", userId, e);
                response.put("roles", List.of());
            }
            
            logger.debug("获取用户详情: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取用户详情失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取用户详情失败：" + e.getMessage()));
        }
    }

    /**
     * 创建新用户
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody @Valid UserCreateRequest request) {
        try {
            // 构建用户对象
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setDisplayName(request.getDisplayName());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());
            
            // 设置状态
            if (request.getStatus() != null) {
                user.setStatus(request.getStatus());
            } else {
                user.setStatus(User.UserStatus.ACTIVE);
            }
            user.setEnabled(true);
            user.setAccountNonLocked(true);
            user.setEmailVerified(request.isEmailVerified());
            
            // 设置存储配额
            if (request.getStorageQuota() != null) {
                user.setStorageQuota(request.getStorageQuota());
            }
            
            User savedUser = userService.createUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", savedUser);
            response.put("message", "用户创建成功");
            
            logger.info("管理员创建用户成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("创建用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("创建用户失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "创建用户失败：" + e.getMessage()));
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserUpdateRequest request) {
        try {
            // 构建更新对象
            User updateUser = new User();
            updateUser.setUsername(request.getUsername());
            updateUser.setEmail(request.getEmail());
            updateUser.setDisplayName(request.getDisplayName());
            updateUser.setPhone(request.getPhone());
            
            User updatedUser = userService.updateUser(userId, updateUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", updatedUser);
            response.put("message", "用户信息更新成功");
            
            logger.info("管理员更新用户信息: {} (ID: {})", updatedUser.getUsername(), userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户信息失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "更新用户信息失败：" + e.getMessage()));
        }
    }

    /**
     * 更新用户密码
     */
    @PutMapping("/users/{userId}/password")
    public ResponseEntity<Map<String, Object>> updateUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "新密码不能为空"));
            }
            
            User updatedUser = userService.updateUserPassword(userId, newPassword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户密码更新成功");
            response.put("username", updatedUser.getUsername());
            
            logger.info("管理员更新用户密码: {} (ID: {})", updatedUser.getUsername(), userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新用户密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户密码失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "更新用户密码失败：" + e.getMessage()));
        }
    }

    /**
     * 激活用户账户
     */
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        try {
            User activatedUser = userService.activateUserAccount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", activatedUser);
            response.put("message", "用户账户已激活");
            
            logger.info("管理员激活用户: {} (ID: {})", activatedUser.getUsername(), userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("激活用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("激活用户失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "激活用户失败：" + e.getMessage()));
        }
    }

    /**
     * 禁用用户账户
     */
    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : "管理员操作";
            User disabledUser = userService.disableUserAccount(userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", disabledUser);
            response.put("message", "用户账户已禁用");
            response.put("reason", reason);
            
            logger.info("管理员禁用用户: {} (ID: {}), 原因: {}", disabledUser.getUsername(), userId, reason);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("禁用用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("禁用用户失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "禁用用户失败：" + e.getMessage()));
        }
    }

    /**
     * 锁定用户账户
     */
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : "管理员操作";
            User lockedUser = userService.lockUserAccount(userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", lockedUser);
            response.put("message", "用户账户已锁定");
            response.put("reason", reason);
            
            logger.info("管理员锁定用户: {} (ID: {}), 原因: {}", lockedUser.getUsername(), userId, reason);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("锁定用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("锁定用户失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "锁定用户失败：" + e.getMessage()));
        }
    }

    /**
     * 解锁用户账户
     */
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : "管理员操作";
            User unlockedUser = userService.unlockUserAccount(userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", unlockedUser);
            response.put("message", "用户账户已解锁");
            response.put("reason", reason);
            
            logger.info("管理员解锁用户: {} (ID: {}), 原因: {}", unlockedUser.getUsername(), userId, reason);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("解锁用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("解锁用户失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "解锁用户失败：" + e.getMessage()));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : "管理员删除";
            User deletedUser = userService.deleteUser(userId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户已删除");
            response.put("username", deletedUser.getUsername());
            response.put("reason", reason);
            
            logger.info("管理员删除用户: {} (ID: {}), 原因: {}", deletedUser.getUsername(), userId, reason);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("删除用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("删除用户失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "删除用户失败：" + e.getMessage()));
        }
    }

    /**
     * 审批用户注册
     */
    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<Map<String, Object>> approveRegistration(
            @PathVariable Long userId,
            @RequestBody ApprovalRequest request) {
        try {
            User approvedUser = userService.approveRegistration(userId, request.isApproved(), request.getComment());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", approvedUser);
            response.put("approved", request.isApproved());
            response.put("comment", request.getComment());
            response.put("message", request.isApproved() ? "用户注册已批准" : "用户注册已拒绝");
            
            logger.info("管理员审批用户注册: {} (ID: {}), 结果: {}, 意见: {}", 
                approvedUser.getUsername(), userId, request.isApproved(), request.getComment());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("审批用户注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("审批用户注册失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "审批用户注册失败：" + e.getMessage()));
        }
    }

    /**
     * 批量操作用户
     */
    @PostMapping("/users/batch")
    public ResponseEntity<Map<String, Object>> batchOperateUsers(@RequestBody BatchUserOperationRequest request) {
        try {
            List<Long> userIds = request.getUserIds();
            String operation = request.getOperation();
            
            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "用户ID列表不能为空"));
            }
            
            Map<String, Object> response = new HashMap<>();
            
            switch (operation.toLowerCase()) {
                case "activate":
                    int activatedCount = userService.batchUpdateUserStatus(userIds, User.UserStatus.ACTIVE);
                    response.put("success", true);
                    response.put("message", "批量激活用户完成");
                    response.put("affectedCount", activatedCount);
                    logger.info("批量激活用户完成, 影响数量: {}", activatedCount);
                    break;
                    
                case "disable":
                    int disabledCount = userService.batchUpdateUserStatus(userIds, User.UserStatus.DISABLED);
                    response.put("success", true);
                    response.put("message", "批量禁用用户完成");
                    response.put("affectedCount", disabledCount);
                    logger.info("批量禁用用户完成, 影响数量: {}", disabledCount);
                    break;
                    
                case "lock":
                    int lockedCount = userService.batchUpdateUserStatus(userIds, User.UserStatus.LOCKED);
                    response.put("success", true);
                    response.put("message", "批量锁定用户完成");
                    response.put("affectedCount", lockedCount);
                    logger.info("批量锁定用户完成, 影响数量: {}", lockedCount);
                    break;
                    
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "不支持的操作类型: " + operation));
            }
            
            response.put("operation", operation);
            response.put("userIds", userIds);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量操作用户失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "批量操作用户失败：" + e.getMessage()));
        }
    }

    /**
     * 更新用户存储配额
     */
    @PutMapping("/users/{userId}/storage-quota")
    public ResponseEntity<Map<String, Object>> updateStorageQuota(
            @PathVariable Long userId,
            @RequestBody Map<String, Long> request) {
        try {
            Long newQuota = request.get("quota");
            if (newQuota == null || newQuota < 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "存储配额不能为空或负数"));
            }
            
            User updatedUser = userService.updateStorageQuota(userId, newQuota);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", updatedUser);
            response.put("oldQuota", request.get("oldQuota"));
            response.put("newQuota", newQuota);
            response.put("message", "存储配额更新成功");
            
            logger.info("管理员更新用户存储配额: {} (ID: {}), 新配额: {} 字节", 
                updatedUser.getUsername(), userId, newQuota);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新存储配额失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("更新存储配额失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "更新存储配额失败：" + e.getMessage()));
        }
    }

    /**
     * 重新计算用户存储使用量
     */
    @PostMapping("/users/{userId}/recalculate-storage")
    public ResponseEntity<Map<String, Object>> recalculateStorageUsage(@PathVariable Long userId) {
        try {
            User updatedUser = userService.recalculateStorageUsage(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", updatedUser);
            response.put("storageUsed", updatedUser.getStorageUsed());
            response.put("message", "存储使用量重新计算完成");
            
            logger.info("管理员重新计算用户存储使用量: {} (ID: {}), 使用量: {} 字节", 
                updatedUser.getUsername(), userId, updatedUser.getStorageUsed());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("重新计算存储使用量失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("重新计算存储使用量失败: {}", userId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "重新计算存储使用量失败：" + e.getMessage()));
        }
    }

    // ========== 内部DTO类 ==========

    /**
     * 用户创建请求
     */
    public static class UserCreateRequest {
        private String username;
        private String email;
        private String password;
        private String displayName;
        private String phone;
        private User.UserStatus status;
        private Long storageQuota;
        private boolean emailVerified = false;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public User.UserStatus getStatus() { return status; }
        public void setStatus(User.UserStatus status) { this.status = status; }
        
        public Long getStorageQuota() { return storageQuota; }
        public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
        
        public boolean isEmailVerified() { return emailVerified; }
        public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    }

    /**
     * 用户更新请求
     */
    public static class UserUpdateRequest {
        private String username;
        private String email;
        private String displayName;
        private String phone;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    /**
     * 用户审批请求
     */
    public static class ApprovalRequest {
        private boolean approved;
        private String comment;

        // Getters and Setters
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    /**
     * 批量用户操作请求
     */
    public static class BatchUserOperationRequest {
        private List<Long> userIds;
        private String operation;

        // Getters and Setters
        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
    }
} 