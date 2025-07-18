package tslc.beihaiyun.lyra.repository;

import java.util.List;

/**
 * 自定义存储库接口
 * 定义额外的数据访问操作
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface CustomRepository<T, ID> {

    /**
     * 批量保存实体
     * 
     * @param entities 实体列表
     * @return 保存后的实体列表
     */
    default List<T> batchSave(List<T> entities) {
        // 默认实现：使用saveAll方法
        return saveAll(entities);
    }

    /**
     * 批量更新实体
     * 
     * @param entities 实体列表
     * @return 更新后的实体列表
     */
    default List<T> batchUpdate(List<T> entities) {
        // 默认实现：使用saveAll方法（JPA的save方法会自动判断是插入还是更新）
        return saveAll(entities);
    }

    // 这个方法由JpaRepository提供
    <S extends T> List<S> saveAll(Iterable<S> entities);
}