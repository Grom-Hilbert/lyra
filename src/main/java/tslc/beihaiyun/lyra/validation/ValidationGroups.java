package tslc.beihaiyun.lyra.validation;

/**
 * 验证组接口
 * 用于分组验证不同场景下的数据完整性
 */
public class ValidationGroups {

    /**
     * 创建操作验证组
     */
    public interface Create {}

    /**
     * 更新操作验证组
     */
    public interface Update {}

    /**
     * 删除操作验证组
     */
    public interface Delete {}

    /**
     * 查询操作验证组
     */
    public interface Query {}

    /**
     * 文件上传验证组
     */
    public interface FileUpload {}

    /**
     * 权限操作验证组
     */
    public interface Permission {}

    /**
     * 版本控制验证组
     */
    public interface VersionControl {}

    /**
     * 模板操作验证组
     */
    public interface Template {}
}