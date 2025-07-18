package tslc.beihaiyun.lyra.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionDTO extends BaseDTO {

    private String name;
    private String description;
    private String resource;
    private String action;
}