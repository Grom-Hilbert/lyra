package tslc.beihaiyun.lyra.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tslc.beihaiyun.lyra.entity.User;

/**
 * 认证相关响应DTO集合
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class AuthResponse {

    /**
     * 通用API响应结构
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private List<String> errors;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;

        public ApiResponse() {
            this.timestamp = LocalDateTime.now();
        }

        public ApiResponse(boolean success, String message) {
            this();
            this.success = success;
            this.message = message;
        }

        public ApiResponse(boolean success, String message, T data) {
            this();
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> success(String message) {
            return new ApiResponse<>(true, message);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message);
        }

        public static <T> ApiResponse<T> error(String message, List<String> errors) {
            ApiResponse<T> response = new ApiResponse<>(false, message);
            response.setErrors(errors);
            return response;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * 登录响应DTO
     */
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn; // 访问令牌过期时间（秒）
        private UserInfo userInfo;

        // Getters and Setters
        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public UserInfo getUserInfo() {
            return userInfo;
        }

        public void setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }

    /**
     * 注册响应DTO
     */
    public static class RegisterResponse {
        private Long userId;
        private String username;
        private String email;
        private User.UserStatus status;
        private boolean requiresApproval;
        private boolean emailVerificationSent;
        private String message;

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public User.UserStatus getStatus() {
            return status;
        }

        public void setStatus(User.UserStatus status) {
            this.status = status;
        }

        public boolean isRequiresApproval() {
            return requiresApproval;
        }

        public void setRequiresApproval(boolean requiresApproval) {
            this.requiresApproval = requiresApproval;
        }

        public boolean isEmailVerificationSent() {
            return emailVerificationSent;
        }

        public void setEmailVerificationSent(boolean emailVerificationSent) {
            this.emailVerificationSent = emailVerificationSent;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 刷新令牌响应DTO
     */
    public static class RefreshTokenResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private Long expiresIn;

        // Getters and Setters
        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }

    /**
     * 用户信息DTO
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String avatarUrl;
        private User.UserStatus status;
        private boolean enabled;
        private boolean emailVerified;
        private Long storageQuota;
        private Long storageUsed;
        private Double storageUsageRatio;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastLoginAt;
        
        private List<String> roles;
        private List<String> permissions;

        public static UserInfo fromUser(User user) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setEmail(user.getEmail());
            userInfo.setDisplayName(user.getDisplayName());
            userInfo.setAvatarUrl(user.getAvatarUrl());
            userInfo.setStatus(user.getStatus());
            userInfo.setEnabled(user.getEnabled());
            userInfo.setEmailVerified(user.getEmailVerified());
            userInfo.setStorageQuota(user.getStorageQuota());
            userInfo.setStorageUsed(user.getStorageUsed());
            userInfo.setLastLoginAt(user.getLastLoginAt());
            
            // 计算存储使用率
            if (user.getStorageQuota() != null && user.getStorageQuota() > 0) {
                double ratio = (double) user.getStorageUsed() / user.getStorageQuota();
                userInfo.setStorageUsageRatio(Math.min(1.0, ratio));
            } else {
                userInfo.setStorageUsageRatio(0.0);
            }
            
            return userInfo;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public User.UserStatus getStatus() {
            return status;
        }

        public void setStatus(User.UserStatus status) {
            this.status = status;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public void setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
        }

        public Long getStorageQuota() {
            return storageQuota;
        }

        public void setStorageQuota(Long storageQuota) {
            this.storageQuota = storageQuota;
        }

        public Long getStorageUsed() {
            return storageUsed;
        }

        public void setStorageUsed(Long storageUsed) {
            this.storageUsed = storageUsed;
        }

        public Double getStorageUsageRatio() {
            return storageUsageRatio;
        }

        public void setStorageUsageRatio(Double storageUsageRatio) {
            this.storageUsageRatio = storageUsageRatio;
        }

        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }

        public void setLastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    }
} 