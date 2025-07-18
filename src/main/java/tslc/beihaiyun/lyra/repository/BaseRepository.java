package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基础存储库接口
 * 提供通用的数据访问操作，所有具体存储库都应继承此接口
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * 根据ID查找实体，如果不存在则抛出异常
     * 
     * @param id 实体ID
     * @return 实体对象
     * @throws jakarta.persistence.EntityNotFoundException 如果实体不存在
     */
    default T findByIdOrThrow(ID id) {
        return findById(id).orElseThrow(() -> 
            new jakarta.persistence.EntityNotFoundException("Entity not found with id: " + id));
    }

    /**
     * 批量查找实体
     * 
     * @param ids ID列表
     * @return 实体列表
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids")
    List<T> findByIds(@Param("ids") List<ID> ids);

    /**
     * 软删除实体（如果实体支持软删除）
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @param id 实体ID
     * @return 更新的记录数
     */
    default int softDeleteById(ID id, LocalDateTime deletedAt) {
        // 默认实现为空，子接口可以重写此方法
        return 0;
    }

    /**
     * 批量软删除实体
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @param ids ID列表
     * @param deletedAt 删除时间
     * @return 更新的记录数
     */
    default int softDeleteByIds(List<ID> ids, LocalDateTime deletedAt) {
        // 默认实现为空，子接口可以重写此方法
        return 0;
    }

    /**
     * 查找未删除的实体
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @param pageable 分页参数
     * @return 未删除的实体分页结果
     */
    default Page<T> findAllNotDeleted(Pageable pageable) {
        // 默认返回所有实体
        return findAll(pageable);
    }

    /**
     * 查找未删除的所有实体
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @return 未删除的实体列表
     */
    default List<T> findAllNotDeleted() {
        // 默认返回所有实体
        return findAll();
    }

    /**
     * 统计未删除的实体数量
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @return 未删除的实体数量
     */
    default long countNotDeleted() {
        // 默认返回所有实体数量
        return count();
    }

    /**
     * 检查实体是否存在且未删除
     * 注意：只有支持软删除的实体才能使用此方法
     * 
     * @param id 实体ID
     * @return 是否存在且未删除
     */
    default boolean existsByIdAndNotDeleted(ID id) {
        // 默认检查实体是否存在
        return existsById(id);
    }

    // 注意：以下方法只适用于有相应字段的实体
    // 由于不是所有实体都有createdAt和updatedAt字段，这些方法被移除
    // 如果需要，可以在具体的存储库接口中定义
}