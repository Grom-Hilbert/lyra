package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹Repository接口
 * 提供文件夹数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    /**
     * 根据空间和路径查找文件夹
     * 
     * @param space 所属空间
     * @param path 文件夹路径
     * @return 文件夹实体（可选）
     */
    Optional<Folder> findBySpaceAndPath(Space space, String path);

    /**
     * 根据空间查找所有文件夹
     * 
     * @param space 所属空间
     * @return 文件夹列表
     */
    List<Folder> findBySpace(Space space);

    /**
     * 根据父文件夹查找子文件夹列表
     * 
     * @param parent 父文件夹
     * @return 子文件夹列表
     */
    List<Folder> findByParent(Folder parent);

    /**
     * 根据空间查找根文件夹列表
     * 
     * @param space 所属空间
     * @return 根文件夹列表
     */
    List<Folder> findBySpaceAndIsRootTrue(Space space);

    /**
     * 根据空间和父文件夹查找子文件夹列表
     * 
     * @param space 所属空间
     * @param parent 父文件夹
     * @return 子文件夹列表
     */
    List<Folder> findBySpaceAndParent(Space space, Folder parent);

    /**
     * 根据空间和层级查找文件夹列表
     * 
     * @param space 所属空间
     * @param level 文件夹层级
     * @return 文件夹列表
     */
    List<Folder> findBySpaceAndLevel(Space space, Integer level);

    /**
     * 根据空间和名称模糊查询文件夹
     * 
     * @param space 所属空间
     * @param name 文件夹名称关键字
     * @return 文件夹列表
     */
    List<Folder> findBySpaceAndNameContainingIgnoreCase(Space space, String name);

    /**
     * 检查指定空间下路径是否已存在
     * 
     * @param space 所属空间
     * @param path 文件夹路径
     * @return 是否存在
     */
    boolean existsBySpaceAndPath(Space space, String path);

    /**
     * 检查指定空间下路径是否已存在（排除指定文件夹ID）
     * 
     * @param space 所属空间
     * @param path 文件夹路径
     * @param folderId 排除的文件夹ID
     * @return 是否存在
     */
    boolean existsBySpaceAndPathAndIdNot(Space space, String path, Long folderId);

    /**
     * 分页查询空间下的文件夹
     * 
     * @param space 所属空间
     * @param pageable 分页参数
     * @return 文件夹分页结果
     */
    Page<Folder> findBySpace(Space space, Pageable pageable);

    /**
     * 分页查询父文件夹下的子文件夹
     * 
     * @param parent 父文件夹
     * @param pageable 分页参数
     * @return 文件夹分页结果
     */
    Page<Folder> findByParent(Folder parent, Pageable pageable);

    /**
     * 根据空间和名称查找文件夹
     * 
     * @param space 所属空间
     * @param name 文件夹名称
     * @return 文件夹实体（可选）
     */
    Optional<Folder> findBySpaceAndName(Space space, String name);

    /**
     * 根据父文件夹和名称查找文件夹
     * 
     * @param parent 父文件夹
     * @param name 文件夹名称
     * @return 文件夹实体（可选）
     */
    Optional<Folder> findByParentAndName(Folder parent, String name);

    /**
     * 查询文件夹及其所有子文件夹
     * 
     * @param folder 文件夹
     * @return 文件夹及其子文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.path LIKE CONCAT(:path, '%')")
    List<Folder> findAllDescendants(@Param("path") String path);

    /**
     * 查询指定文件夹的所有祖先文件夹
     * 
     * @param path 文件夹路径
     * @return 祖先文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE :path LIKE CONCAT(f.path, '%') AND f.path != :path ORDER BY f.level")
    List<Folder> findAllAncestors(@Param("path") String path);

    /**
     * 统计空间下的文件夹数量
     * 
     * @param space 所属空间
     * @return 文件夹数量
     */
    long countBySpace(Space space);

    /**
     * 统计父文件夹下的子文件夹数量
     * 
     * @param parent 父文件夹
     * @return 子文件夹数量
     */
    long countByParent(Folder parent);

    /**
     * 统计空间下指定层级的文件夹数量
     * 
     * @param space 所属空间
     * @param level 文件夹层级
     * @return 文件夹数量
     */
    long countBySpaceAndLevel(Space space, Integer level);

    /**
     * 计算空间下所有文件夹的总大小
     * 
     * @param space 所属空间
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM Folder f WHERE f.space = :space")
    Long sumSizeBytesBySpace(@Param("space") Space space);

    /**
     * 计算空间下所有文件夹的总文件数量
     * 
     * @param space 所属空间
     * @return 总文件数量
     */
    @Query("SELECT COALESCE(SUM(f.fileCount), 0) FROM Folder f WHERE f.space = :space")
    Long sumFileCountBySpace(@Param("space") Space space);

    /**
     * 查询空文件夹（没有文件和子文件夹）
     * 
     * @param space 所属空间
     * @return 空文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.space = :space AND f.fileCount = 0 AND " +
           "NOT EXISTS (SELECT 1 FROM Folder c WHERE c.parent = f)")
    List<Folder> findEmptyFolders(@Param("space") Space space);

    /**
     * 查询大文件夹（超过指定大小阈值）
     * 
     * @param space 所属空间
     * @param sizeThreshold 大小阈值（字节）
     * @return 大文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.space = :space AND f.sizeBytes > :sizeThreshold ORDER BY f.sizeBytes DESC")
    List<Folder> findLargeFolders(@Param("space") Space space, @Param("sizeThreshold") Long sizeThreshold);

    /**
     * 查询深层文件夹（超过指定层级）
     * 
     * @param space 所属空间
     * @param levelThreshold 层级阈值
     * @return 深层文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.space = :space AND f.level > :levelThreshold ORDER BY f.level DESC")
    List<Folder> findDeepFolders(@Param("space") Space space, @Param("levelThreshold") Integer levelThreshold);

    /**
     * 根据路径前缀查找文件夹
     * 
     * @param space 所属空间
     * @param pathPrefix 路径前缀
     * @return 文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.space = :space AND f.path LIKE CONCAT(:pathPrefix, '%')")
    List<Folder> findBySpaceAndPathStartsWith(@Param("space") Space space, @Param("pathPrefix") String pathPrefix);

    /**
     * 批量更新文件夹路径（用于移动操作）
     * 
     * @param oldPathPrefix 旧路径前缀
     * @param newPathPrefix 新路径前缀
     * @return 更新的记录数
     */
    @Query("UPDATE Folder f SET f.path = CONCAT(:newPathPrefix, SUBSTRING(f.path, LENGTH(:oldPathPrefix) + 1)) " +
           "WHERE f.path LIKE CONCAT(:oldPathPrefix, '%')")
    int updatePathsByPrefix(@Param("oldPathPrefix") String oldPathPrefix, @Param("newPathPrefix") String newPathPrefix);

    /**
     * 根据空间ID查找文件夹列表（用于级联查询）
     * 
     * @param spaceId 空间ID
     * @return 文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.space.id = :spaceId")
    List<Folder> findBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 根据父文件夹ID查找子文件夹列表（用于级联查询）
     * 
     * @param parentId 父文件夹ID
     * @return 子文件夹列表
     */
    @Query("SELECT f FROM Folder f WHERE f.parent.id = :parentId")
    List<Folder> findByParentId(@Param("parentId") Long parentId);

    /**
     * 查询文件夹树（层级结构）
     * 
     * @param space 所属空间
     * @param maxLevel 最大层级
     * @return 文件夹列表（按层级和名称排序）
     */
    @Query("SELECT f FROM Folder f WHERE f.space = :space AND f.level <= :maxLevel ORDER BY f.level, f.name")
    List<Folder> findFolderTree(@Param("space") Space space, @Param("maxLevel") Integer maxLevel);

    /**
     * 检查文件夹是否为空（没有文件和子文件夹）
     * 
     * @param folderId 文件夹ID
     * @return 是否为空
     */
    @Query("SELECT CASE WHEN (f.fileCount = 0 AND NOT EXISTS (SELECT 1 FROM Folder c WHERE c.parent.id = :folderId)) " +
           "THEN true ELSE false END FROM Folder f WHERE f.id = :folderId")
    boolean isFolderEmpty(@Param("folderId") Long folderId);
} 