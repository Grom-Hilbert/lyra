package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FolderEntity;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 文件数据访问接口
 * 提供文件相关的数据库操作
 */
@Repository
public interface FileRepository extends BaseRepository<FileEntity, Long>, CustomRepository<FileEntity, Long> {

    /**
     * 根据路径查找文件
     */
    Optional<FileEntity> findByPath(String path);

    /**
     * 根据文件夹查找文件列表
     */
    List<FileEntity> findByFolder(FolderEntity folder);

    /**
     * 根据文件夹ID查找文件列表（分页）
     */
    Page<FileEntity> findByFolderId(Long folderId, Pageable pageable);

    /**
     * 根据所有者查找文件列表
     */
    List<FileEntity> findByOwner(User owner);

    /**
     * 根据空间类型查找文件列表
     */
    List<FileEntity> findBySpaceType(FileEntity.SpaceType spaceType);

    /**
     * 根据文件名模糊搜索
     */
    @Query("SELECT f FROM FileEntity f WHERE f.name LIKE %:name%")
    List<FileEntity> findByNameContaining(@Param("name") String name);

    /**
     * 根据MIME类型查找文件
     */
    List<FileEntity> findByMimeType(String mimeType);

    /**
     * 根据校验和查找文件
     */
    Optional<FileEntity> findByChecksum(String checksum);

    /**
     * 查找用户在特定空间的文件
     */
    @Query("SELECT f FROM FileEntity f WHERE f.owner = :owner AND f.spaceType = :spaceType")
    List<FileEntity> findByOwnerAndSpaceType(@Param("owner") User owner, @Param("spaceType") FileEntity.SpaceType spaceType);

    /**
     * 查找特定版本控制类型的文件
     */
    List<FileEntity> findByVersionControlType(FileEntity.VersionControlType versionControlType);

    /**
     * 统计用户文件总大小
     */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.owner = :owner")
    Long getTotalSizeByOwner(@Param("owner") User owner);

    /**
     * 统计文件夹下的文件数量
     */
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.folder = :folder")
    Long countByFolder(@Param("folder") FolderEntity folder);
}