package tslc.beihaiyun.lyra.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tslc.beihaiyun.lyra.entity.Role;

import java.util.List;

/**
 * 角色数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleDTO extends BaseDTO {

    private String name;
    private String description;
    private Role.RoleType type;
    private List<PermissionDTO> permissions;

    /**
     * 角色创建请求DTO
     */
    @Data
    public static class CreateRequest {
        private String name;
        private String description;
        private Role.RoleType type;
        private List<Long> permissionIds;
    }

    /**
     * 角色更新请求DTO
     */
    @Data
    public static class UpdateRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;
    }

    /**
     * 角色权限分配请求DTO
     */
    @Data
    public static class AssignPermissionsRequest {
        private Long roleId;
        private List<Long> permissionIds;
    }

    /**
     * 角色搜索条件DTO
     */
    @Data
    public static class SearchCriteria {
        private String keyword;
        private Role.RoleType type;
    }
}