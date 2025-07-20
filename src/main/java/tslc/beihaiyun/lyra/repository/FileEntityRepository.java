package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件Entity Repository接口
 * 提供文件数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {

    /**
     * 根据空间和路径查找文件
     * 
     * @param space 所属空间
     * @param path 文件路径
     * @return 文件实体（可选）
     */
    Optional<FileEntity> findBySpaceAndPath(Space space, String path);

    /**
     * 根据空间查找所有文件
     * 
     * @param space 所属空间
     * @return 文件列表
     */
    List<FileEntity> findBySpace(Space space);

    /**
     * 根据文件夹查找文件列表
     * 
     * @param folder 所属文件夹
     * @return 文件列表
     */
    List<FileEntity> findByFolder(Folder folder);

    /**
     * 根据空间和文件夹查找文件列表
     * 
     * @param space 所属空间
     * @param folder 所属文件夹
     * @return 文件列表
     */
    List<FileEntity> findBySpaceAndFolder(Space space, Folder folder);

    /**
     * 根据空间查找根目录文件（没有文件夹）
     * 
     * @param space 所属空间
     * @return 根目录文件列表
     */
    List<FileEntity> findBySpaceAndFolderIsNull(Space space);

    /**
     * 根据文件状态查找文件列表
     * 
     * @param status 文件状态
     * @return 文件列表
     */
    List<FileEntity> findByStatus(FileEntity.FileStatus status);

    /**
     * 根据空间和文件状态查找文件列表
     * 
     * @param space 所属空间
     * @param status 文件状态
     * @return 文件列表
     */
    List<FileEntity> findBySpaceAndStatus(Space space, FileEntity.FileStatus status);

    /**
     * 根据文件哈希值查找文件
     * 
     * @param fileHash 文件哈希值
     * @return 文件列表
     */
    List<FileEntity> findByFileHash(String fileHash);

    /**
     * 根据MIME类型查找文件列表
     * 
     * @param mimeType MIME类型
     * @return 文件列表
     */
    List<FileEntity> findByMimeType(String mimeType);

    /**
     * 根据空间和名称模糊查询文件
     * 
     * @param space 所属空间
     * @param name 文件名称关键字
     * @return 文件列表
     */
    List<FileEntity> findBySpaceAndNameContainingIgnoreCase(Space space, String name);

    /**
     * 检查指定空间下路径是否已存在
     * 
     * @param space 所属空间
     * @param path 文件路径
     * @return 是否存在
     */
    boolean existsBySpaceAndPath(Space space, String path);

    /**
     * 检查指定空间下路径是否已存在（排除指定文件ID）
     * 
     * @param space 所属空间
     * @param path 文件路径
     * @param fileId 排除的文件ID
     * @return 是否存在
     */
    boolean existsBySpaceAndPathAndIdNot(Space space, String path, Long fileId);

    /**
     * 分页查询空间下的文件
     * 
     * @param space 所属空间
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<FileEntity> findBySpace(Space space, Pageable pageable);

    /**
     * 分页查询文件夹下的文件
     * 
     * @param folder 所属文件夹
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<FileEntity> findByFolder(Folder folder, Pageable pageable);

    /**
     * 分页查询特定状态的文件
     * 
     * @param status 文件状态
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    Page<FileEntity> findByStatus(FileEntity.FileStatus status, Pageable pageable);

    /**
     * 根据空间和名称查找文件
     * 
     * @param space 所属空间
     * @param name 文件名称
     * @return 文件实体（可选）
     */
    Optional<FileEntity> findBySpaceAndName(Space space, String name);

    /**
     * 根据文件夹和名称查找文件
     * 
     * @param folder 所属文件夹
     * @param name 文件名称
     * @return 文件实体（可选）
     */
    Optional<FileEntity> findByFolderAndName(Folder folder, String name);

    /**
     * 查询公开文件列表
     * 
     * @return 公开文件列表
     */
    List<FileEntity> findByIsPublicTrue();

    /**
     * 查询空间下的公开文件列表
     * 
     * @param space 所属空间
     * @return 公开文件列表
     */
    List<FileEntity> findBySpaceAndIsPublicTrue(Space space);

    /**
     * 统计空间下的文件数量
     * 
     * @param space 所属空间
     * @return 文件数量
     */
    long countBySpace(Space space);

    /**
     * 统计文件夹下的文件数量
     * 
     * @param folder 所属文件夹
     * @return 文件数量
     */
    long countByFolder(Folder folder);

    /**
     * 统计空间下特定状态的文件数量
     * 
     * @param space 所属空间
     * @param status 文件状态
     * @return 文件数量
     */
    long countBySpaceAndStatus(Space space, FileEntity.FileStatus status);

    /**
     * 计算空间下所有文件的总大小
     * 
     * @param space 所属空间
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileEntity f WHERE f.space = :space")
    Long sumSizeBytesBySpace(@Param("space") Space space);

    /**
     * 计算文件夹下所有文件的总大小
     * 
     * @param folder 所属文件夹
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileEntity f WHERE f.folder = :folder")
    Long sumSizeBytesByFolder(@Param("folder") Folder folder);

    /**
     * 查询大文件（超过指定大小阈值）
     * 
     * @param space 所属空间
     * @param sizeThreshold 大小阈值（字节）
     * @return 大文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.sizeBytes > :sizeThreshold ORDER BY f.sizeBytes DESC")
    List<FileEntity> findLargeFiles(@Param("space") Space space, @Param("sizeThreshold") Long sizeThreshold);

    /**
     * 查询热门文件（下载次数超过阈值）
     * 
     * @param space 所属空间
     * @param downloadThreshold 下载次数阈值
     * @return 热门文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.downloadCount > :downloadThreshold ORDER BY f.downloadCount DESC")
    List<FileEntity> findPopularFiles(@Param("space") Space space, @Param("downloadThreshold") Integer downloadThreshold);

    /**
     * 查询最近修改的文件
     * 
     * @param space 所属空间
     * @param since 起始时间
     * @return 最近修改的文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.lastModifiedAt > :since ORDER BY f.lastModifiedAt DESC")
    List<FileEntity> findRecentlyModifiedFiles(@Param("space") Space space, @Param("since") LocalDateTime since);

    /**
     * 查询最近上传的文件
     * 
     * @param space 所属空间
     * @param since 起始时间
     * @return 最近上传的文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.createdAt > :since ORDER BY f.createdAt DESC")
    List<FileEntity> findRecentlyUploadedFiles(@Param("space") Space space, @Param("since") LocalDateTime since);

    /**
     * 根据文件扩展名查找文件
     * 
     * @param space 所属空间
     * @param extension 文件扩展名
     * @return 文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND LOWER(f.name) LIKE CONCAT('%.', LOWER(:extension))")
    List<FileEntity> findBySpaceAndExtension(@Param("space") Space space, @Param("extension") String extension);

    /**
     * 查询重复文件（相同哈希值）
     * 
     * @param space 所属空间
     * @return 重复文件组（按哈希值分组）
     */
    @Query("SELECT f.fileHash, COUNT(f) FROM FileEntity f WHERE f.space = :space AND f.fileHash IS NOT NULL GROUP BY f.fileHash HAVING COUNT(f) > 1")
    List<Object[]> findDuplicateFilesByHash(@Param("space") Space space);

    /**
     * 查询孤儿文件（文件夹被删除但文件还在）
     * 
     * @param space 所属空间
     * @return 孤儿文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.folder IS NOT NULL AND " +
           "NOT EXISTS (SELECT 1 FROM Folder folder WHERE folder.id = f.folder.id)")
    List<FileEntity> findOrphanFiles(@Param("space") Space space);

    /**
     * 批量更新文件状态
     * 
     * @param fileIds 文件ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    @Query("UPDATE FileEntity f SET f.status = :status WHERE f.id IN :fileIds")
    int updateStatusByIds(@Param("fileIds") List<Long> fileIds, @Param("status") FileEntity.FileStatus status);

    /**
     * 批量更新文件的文件夹（用于移动操作）
     * 
     * @param fileIds 文件ID列表
     * @param newFolder 新文件夹
     * @return 更新的记录数
     */
    @Query("UPDATE FileEntity f SET f.folder = :newFolder WHERE f.id IN :fileIds")
    int updateFolderByIds(@Param("fileIds") List<Long> fileIds, @Param("newFolder") Folder newFolder);

    /**
     * 根据空间ID查找文件列表（用于级联查询）
     * 
     * @param spaceId 空间ID
     * @return 文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space.id = :spaceId")
    List<FileEntity> findBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 根据文件夹ID查找文件列表（用于级联查询）
     * 
     * @param folderId 文件夹ID
     * @return 文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.folder.id = :folderId")
    List<FileEntity> findByFolderId(@Param("folderId") Long folderId);

    /**
     * 全文搜索文件（文件名和原始文件名）
     * 
     * @param space 所属空间
     * @param searchTerm 搜索关键字
     * @return 文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND " +
           "(LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(f.originalName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<FileEntity> searchFiles(@Param("space") Space space, @Param("searchTerm") String searchTerm);

    /**
     * 分页全文搜索文件
     * 
     * @param space 所属空间
     * @param searchTerm 搜索关键字
     * @param pageable 分页参数
     * @return 文件分页结果
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND " +
           "(LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(f.originalName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<FileEntity> searchFiles(@Param("space") Space space, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * 查询活跃文件列表
     * 
     * @param space 所属空间
     * @return 活跃文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.space = :space AND f.status = 'ACTIVE' ORDER BY f.lastModifiedAt DESC")
    List<FileEntity> findActiveFiles(@Param("space") Space space);

    /**
     * 查询需要清理的已删除文件（超过指定天数）
     * 
     * @param daysAgo 天数前
     * @return 需要清理的文件列表
     */
    @Query("SELECT f FROM FileEntity f WHERE f.status = 'DELETED' AND f.updatedAt < :daysAgo")
    List<FileEntity> findDeletedFilesOlderThan(@Param("daysAgo") LocalDateTime daysAgo);
} 