package tslc.beihaiyun.lyra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tslc.beihaiyun.lyra.entity.FilePermission;

import java.time.LocalDateTime;

/**
 * 文件权限数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FilePermissionDTO extends BaseDTO {

    private Long fileId;
    private UserDTO user;
    private RoleDTO role;
    private FilePermission.PermissionType permissionType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime grantedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private UserDTO grantedBy;

    /**
     * 权限授予请求DTO
     */
    @Data
    public static class GrantRequest {
        private Long resourceId;
        private String resourceType; // FILE or FOLDER
        private Long userId;
        private Long roleId;
        private FilePermission.PermissionType permissionType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime expiresAt;
    }

    /**
     * 权限撤销请求DTO
     */
    @Data
    public static class RevokeRequest {
        private Long resourceId;
        private String resourceType; // FILE or FOLDER
        private Long userId;
        private Long roleId;
        private FilePermission.PermissionType permissionType;
    }

    /**
     * 权限检查请求DTO
     */
    @Data
    public static class CheckRequest {
        private Long resourceId;
        private String resourceType; // FILE or FOLDER
        private String action;
    }

    /**
     * 权限检查响应DTO
     */
    @Data
    public static class CheckResponse {
        private Boolean hasPermission;
        private String reason;
    }
}