package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 认证相关请求DTO集合
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class AuthRequest {

    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        
        @NotBlank(message = "用户名或邮箱不能为空")
        @Size(max = 100, message = "用户名或邮箱长度不能超过100个字符")
        private String usernameOrEmail;
        
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        private String password;
        
        private boolean rememberMe = false;
        
        // Getters and Setters
        public String getUsernameOrEmail() {
            return usernameOrEmail;
        }
        
        public void setUsernameOrEmail(String usernameOrEmail) {
            this.usernameOrEmail = usernameOrEmail;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public boolean isRememberMe() {
            return rememberMe;
        }
        
        public void setRememberMe(boolean rememberMe) {
            this.rememberMe = rememberMe;
        }
    }

    /**
     * 注册请求DTO
     */
    public static class RegisterRequest {
        
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和横线")
        private String username;
        
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
                 message = "密码必须包含至少一个小写字母、一个大写字母和一个数字")
        private String password;
        
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
        
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过100个字符")
        private String email;
        
        @Size(max = 100, message = "显示名称长度不能超过100个字符")
        private String displayName;
        
        @Pattern(regexp = "^[0-9-+\\s()]*$", message = "手机号格式不正确")
        @Size(max = 20, message = "手机号长度不能超过20个字符")
        private String phone;
        
        private boolean agreeToTerms = false;
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getConfirmPassword() {
            return confirmPassword;
        }
        
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
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
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public boolean isAgreeToTerms() {
            return agreeToTerms;
        }
        
        public void setAgreeToTerms(boolean agreeToTerms) {
            this.agreeToTerms = agreeToTerms;
        }
    }

    /**
     * 刷新令牌请求DTO
     */
    public static class RefreshTokenRequest {
        
        @NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;
        
        // Getters and Setters
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    /**
     * 密码重置请求DTO
     */
    public static class PasswordResetRequest {
        
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        // Getters and Setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 密码重置确认请求DTO
     */
    public static class PasswordResetConfirmRequest {
        
        @NotBlank(message = "重置令牌不能为空")
        private String resetToken;
        
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
                 message = "密码必须包含至少一个小写字母、一个大写字母和一个数字")
        private String newPassword;
        
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
        
        // Getters and Setters
        public String getResetToken() {
            return resetToken;
        }
        
        public void setResetToken(String resetToken) {
            this.resetToken = resetToken;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
        
        public String getConfirmPassword() {
            return confirmPassword;
        }
        
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

    /**
     * 邮箱验证请求DTO
     */
    public static class EmailVerificationRequest {
        
        @NotBlank(message = "验证令牌不能为空")
        private String verificationToken;
        
        // Getters and Setters
        public String getVerificationToken() {
            return verificationToken;
        }
        
        public void setVerificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
        }
    }
} 