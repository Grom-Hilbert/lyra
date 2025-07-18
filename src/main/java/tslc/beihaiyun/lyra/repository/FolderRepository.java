package tslc.beihaiyun.lyra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FolderEntity;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹数据访问接口
 * 提供文件夹相关的数据库操作
 */
@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {

    /**
     * 根据路径查找文件夹
     */
    Optional<FolderEntity> findByPath(String path);

    /**
     * 根据父文件夹查找子文件夹列表
     */
    List<FolderEntity> findByParent(FolderEntity parent);

    /**
     * 根据父文件夹ID查找子文件夹列表
     */
    List<FolderEntity> findByParentId(Long parentId);

    /**
     * 查找根文件夹（无父文件夹）
     */
    List<FolderEntity> findByParentIsNull();

    /**
     * 根据所有者查找文件夹列表
     */
    List<FolderEntity> findByOwner(User owner);

    /**
     * 根据空间类型查找文件夹列表
     */
    List<FolderEntity> findBySpaceType(FileEntity.SpaceType spaceType);

    /**
     * 根据文件夹名称模糊搜索
     */
    @Query("SELECT f FROM FolderEntity f WHERE f.name LIKE %:name%")
    List<FolderEntity> findByNameContaining(@Param("name") String name);

    /**
     * 查找用户在特定空间的文件夹
     */
    @Query("SELECT f FROM FolderEntity f WHERE f.owner = :owner AND f.spaceType = :spaceType")
    List<FolderEntity> findByOwnerAndSpaceType(@Param("owner") User owner, @Param("spaceType") FileEntity.SpaceType spaceType);

    /**
     * 查找用户的根文件夹
     */
    @Query("SELECT f FROM FolderEntity f WHERE f.owner = :owner AND f.parent IS NULL AND f.spaceType = :spaceType")
    List<FolderEntity> findRootFoldersByOwnerAndSpaceType(@Param("owner") User owner, @Param("spaceType") FileEntity.SpaceType spaceType);

    /**
     * 检查路径是否存在
     */
    boolean existsByPath(String path);

    /**
     * 统计文件夹下的子文件夹数量
     */
    @Query("SELECT COUNT(f) FROM FolderEntity f WHERE f.parent = :parent")
    Long countByParent(@Param("parent") FolderEntity parent);

    /**
     * 查找文件夹的所有祖先文件夹
     */
    @Query(value = "WITH RECURSIVE folder_hierarchy AS (" +
           "SELECT id, name, path, parent_id, 0 as level FROM folders WHERE id = :folderId " +
           "UNION ALL " +
           "SELECT f.id, f.name, f.path, f.parent_id, fh.level + 1 " +
           "FROM folders f INNER JOIN folder_hierarchy fh ON f.id = fh.parent_id" +
           ") SELECT * FROM folder_hierarchy WHERE level > 0", nativeQuery = true)
    List<FolderEntity> findAncestors(@Param("folderId") Long folderId);
}