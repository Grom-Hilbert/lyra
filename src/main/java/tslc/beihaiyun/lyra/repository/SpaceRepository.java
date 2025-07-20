package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 空间Repository接口
 * 提供空间数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    /**
     * 根据空间名称查找空间
     * 
     * @param name 空间名称
     * @return 空间实体（可选）
     */
    Optional<Space> findByName(String name);

    /**
     * 根据拥有者查找空间列表
     * 
     * @param owner 空间拥有者
     * @return 空间列表
     */
    List<Space> findByOwner(User owner);

    /**
     * 根据拥有者和空间类型查找空间列表
     * 
     * @param owner 空间拥有者
     * @param type 空间类型
     * @return 空间列表
     */
    List<Space> findByOwnerAndType(User owner, Space.SpaceType type);

    /**
     * 根据空间类型查找空间列表
     * 
     * @param type 空间类型
     * @return 空间列表
     */
    List<Space> findByType(Space.SpaceType type);

    /**
     * 根据空间状态查找空间列表
     * 
     * @param status 空间状态
     * @return 空间列表
     */
    List<Space> findByStatus(Space.SpaceStatus status);

    /**
     * 根据拥有者和空间状态查找空间列表
     * 
     * @param owner 空间拥有者
     * @param status 空间状态
     * @return 空间列表
     */
    List<Space> findByOwnerAndStatus(User owner, Space.SpaceStatus status);

    /**
     * 检查空间名称是否已存在
     * 
     * @param name 空间名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查空间名称是否已存在（排除指定空间ID）
     * 
     * @param name 空间名称
     * @param spaceId 排除的空间ID
     * @return 是否存在
     */
    boolean existsByNameAndIdNot(String name, Long spaceId);

    /**
     * 分页查询用户的空间列表
     * 
     * @param owner 空间拥有者
     * @param pageable 分页参数
     * @return 空间分页结果
     */
    Page<Space> findByOwner(User owner, Pageable pageable);

    /**
     * 分页查询特定类型的空间列表
     * 
     * @param type 空间类型
     * @param pageable 分页参数
     * @return 空间分页结果
     */
    Page<Space> findByType(Space.SpaceType type, Pageable pageable);

    /**
     * 根据空间名称模糊查询
     * 
     * @param name 空间名称关键字
     * @return 空间列表
     */
    List<Space> findByNameContainingIgnoreCase(String name);

    /**
     * 根据空间名称模糊查询（分页）
     * 
     * @param name 空间名称关键字
     * @param pageable 分页参数
     * @return 空间分页结果
     */
    Page<Space> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 查询用户的活跃空间列表
     * 
     * @param owner 空间拥有者
     * @return 活跃空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.owner = :owner AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Space> findActiveSpacesByOwner(@Param("owner") User owner);

    /**
     * 查询超过配额限制的空间列表
     * 
     * @return 超配额空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.quotaUsed > s.quotaLimit")
    List<Space> findSpacesOverQuota();

    /**
     * 统计用户的空间数量
     * 
     * @param owner 空间拥有者
     * @return 空间数量
     */
    long countByOwner(User owner);

    /**
     * 统计用户特定类型的空间数量
     * 
     * @param owner 空间拥有者
     * @param type 空间类型
     * @return 空间数量
     */
    long countByOwnerAndType(User owner, Space.SpaceType type);

    /**
     * 统计特定类型的空间数量
     * 
     * @param type 空间类型
     * @return 空间数量
     */
    long countByType(Space.SpaceType type);

    /**
     * 计算用户所有空间的总使用配额
     * 
     * @param owner 空间拥有者
     * @return 总使用配额
     */
    @Query("SELECT COALESCE(SUM(s.quotaUsed), 0) FROM Space s WHERE s.owner = :owner")
    Long sumQuotaUsedByOwner(@Param("owner") User owner);

    /**
     * 计算用户所有空间的总配额限制
     * 
     * @param owner 空间拥有者
     * @return 总配额限制
     */
    @Query("SELECT COALESCE(SUM(s.quotaLimit), 0) FROM Space s WHERE s.owner = :owner")
    Long sumQuotaLimitByOwner(@Param("owner") User owner);

    /**
     * 查询配额使用率超过指定阈值的空间
     * 
     * @param threshold 配额使用率阈值（0.0-1.0）
     * @return 空间列表
     */
    @Query("SELECT s FROM Space s WHERE (CAST(s.quotaUsed AS double) / CAST(s.quotaLimit AS double)) > :threshold")
    List<Space> findSpacesWithHighQuotaUsage(@Param("threshold") double threshold);

    /**
     * 根据拥有者和空间名称查找空间
     * 
     * @param owner 空间拥有者
     * @param name 空间名称
     * @return 空间实体（可选）
     */
    Optional<Space> findByOwnerAndName(User owner, String name);

    /**
     * 查询用户的个人空间
     * 
     * @param owner 空间拥有者
     * @return 个人空间（可选）
     */
    @Query("SELECT s FROM Space s WHERE s.owner = :owner AND s.type = 'PERSONAL'")
    Optional<Space> findPersonalSpaceByOwner(@Param("owner") User owner);

    /**
     * 查询系统中所有的企业空间
     * 
     * @return 企业空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.type = 'ENTERPRISE' AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Space> findAllEnterpriseSpaces();

    /**
     * 查询系统中所有的共享空间
     * 
     * @return 共享空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.type = 'SHARED' AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Space> findAllSharedSpaces();

    /**
     * 查询需要清理的归档空间（超过指定天数）
     * 
     * @param daysAgo 天数前
     * @return 需要清理的空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.status = 'ARCHIVED' AND s.updatedAt < :daysAgo")
    List<Space> findArchivedSpacesOlderThan(@Param("daysAgo") java.time.LocalDateTime daysAgo);

    /**
     * 批量更新空间状态
     * 
     * @param spaceIds 空间ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    @Query("UPDATE Space s SET s.status = :status WHERE s.id IN :spaceIds")
    int updateStatusByIds(@Param("spaceIds") List<Long> spaceIds, @Param("status") Space.SpaceStatus status);

    /**
     * 根据用户ID查找空间列表（用于级联查询）
     * 
     * @param userId 用户ID
     * @return 空间列表
     */
    @Query("SELECT s FROM Space s WHERE s.owner.id = :userId")
    List<Space> findByOwnerId(@Param("userId") Long userId);
} 