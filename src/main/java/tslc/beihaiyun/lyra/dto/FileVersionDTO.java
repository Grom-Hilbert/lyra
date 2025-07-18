package tslc.beihaiyun.lyra.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件版本数据传输对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileVersionDTO extends BaseDTO {

    private Long fileId;
    private Integer versionNumber;
    private String versionDescription;
    private String filePath;
    private Long size;
    private String checksum;
    private String gitCommitHash;
    private UserDTO createdBy;
    private Boolean isCurrent;
}