package tslc.beihaiyun.lyra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tslc.beihaiyun.lyra.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends BaseDTO {

    private String username;
    private String email;
    private String displayName;
    private User.UserStatus status;
    private User.AuthProvider authProvider;
    private String externalId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    private List<RoleDTO> roles;

    /**
     * 用户创建请求DTO
     */
    @Data
    public static class CreateRequest {
        private String username;
        private String email;
        private String displayName;
        private String password;
        private User.AuthProvider authProvider = User.AuthProvider.LOCAL;
        private String externalId;
        private List<Long> roleIds;
    }

    /**
     * 用户更新请求DTO
     */
    @Data
    public static class UpdateRequest {
        private String displayName;
        private User.UserStatus status;
        private List<Long> roleIds;
    }

    /**
     * 用户登录请求DTO
     */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private Boolean rememberMe = false;
    }

    /**
     * 用户登录响应DTO
     */
    @Data
    public static class LoginResponse {
        private UserDTO user;
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
    }

    /**
     * 修改密码请求DTO
     */
    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }

    /**
     * 用户搜索条件DTO
     */
    @Data
    public static class SearchCriteria {
        private String keyword;
        private User.UserStatus status;
        private User.AuthProvider authProvider;
        private Long roleId;
    }
}