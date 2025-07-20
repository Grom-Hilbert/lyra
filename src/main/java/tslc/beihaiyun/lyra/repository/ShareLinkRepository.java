package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.ShareLink;
import tslc.beihaiyun.lyra.entity.Space;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 分享链接Repository接口
 * 提供分享链接数据访问操作
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    /**
     * 根据令牌查找分享链接
     * 
     * @param token 分享令牌
     * @return 分享链接（可选）
     */
    Optional<ShareLink> findByToken(String token);

    /**
     * 根据文件查找分享链接列表
     * 
     * @param file 分享的文件
     * @return 分享链接列表
     */
    List<ShareLink> findByFile(FileEntity file);

    /**
     * 根据文件夹查找分享链接列表
     * 
     * @param folder 分享的文件夹
     * @return 分享链接列表
     */
    List<ShareLink> findByFolder(Folder folder);

    /**
     * 根据空间查找分享链接列表
     * 
     * @param space 所属空间
     * @return 分享链接列表
     */
    List<ShareLink> findBySpace(Space space);

    /**
     * 根据访问类型查找分享链接列表
     * 
     * @param accessType 访问类型
     * @return 分享链接列表
     */
    List<ShareLink> findByAccessType(ShareLink.AccessType accessType);

    /**
     * 根据是否活跃查找分享链接列表
     * 
     * @param isActive 是否活跃
     * @return 分享链接列表
     */
    List<ShareLink> findByIsActive(Boolean isActive);

    /**
     * 检查令牌是否已存在
     * 
     * @param token 分享令牌
     * @return 是否存在
     */
    boolean existsByToken(String token);

    /**
     * 分页查询空间下的分享链接
     * 
     * @param space 所属空间
     * @param pageable 分页参数
     * @return 分享链接分页结果
     */
    Page<ShareLink> findBySpace(Space space, Pageable pageable);

    /**
     * 分页查询文件的分享链接
     * 
     * @param file 分享的文件
     * @param pageable 分页参数
     * @return 分享链接分页结果
     */
    Page<ShareLink> findByFile(FileEntity file, Pageable pageable);

    /**
     * 分页查询文件夹的分享链接
     * 
     * @param folder 分享的文件夹
     * @param pageable 分页参数
     * @return 分享链接分页结果
     */
    Page<ShareLink> findByFolder(Folder folder, Pageable pageable);

    /**
     * 查询活跃的分享链接
     * 
     * @return 活跃分享链接列表
     */
    List<ShareLink> findByIsActiveTrue();

    /**
     * 查询空间下的活跃分享链接
     * 
     * @param space 所属空间
     * @return 活跃分享链接列表
     */
    List<ShareLink> findBySpaceAndIsActiveTrue(Space space);

    /**
     * 查询文件的活跃分享链接
     * 
     * @param file 分享的文件
     * @return 活跃分享链接列表
     */
    List<ShareLink> findByFileAndIsActiveTrue(FileEntity file);

    /**
     * 查询文件夹的活跃分享链接
     * 
     * @param folder 分享的文件夹
     * @return 活跃分享链接列表
     */
    List<ShareLink> findByFolderAndIsActiveTrue(Folder folder);

    /**
     * 统计空间下的分享链接数量
     * 
     * @param space 所属空间
     * @return 分享链接数量
     */
    long countBySpace(Space space);

    /**
     * 统计文件的分享链接数量
     * 
     * @param file 分享的文件
     * @return 分享链接数量
     */
    long countByFile(FileEntity file);

    /**
     * 统计文件夹的分享链接数量
     * 
     * @param folder 分享的文件夹
     * @return 分享链接数量
     */
    long countByFolder(Folder folder);

    /**
     * 统计活跃的分享链接数量
     * 
     * @return 活跃分享链接数量
     */
    long countByIsActiveTrue();

    /**
     * 统计空间下活跃的分享链接数量
     * 
     * @param space 所属空间
     * @return 活跃分享链接数量
     */
    long countBySpaceAndIsActiveTrue(Space space);

    /**
     * 查询已过期的分享链接
     * 
     * @param now 当前时间
     * @return 已过期的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt < :now")
    List<ShareLink> findExpiredLinks(@Param("now") LocalDateTime now);

    /**
     * 查询即将过期的分享链接（指定时间内过期）
     * 
     * @param threshold 过期时间阈值
     * @return 即将过期的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt BETWEEN :now AND :threshold")
    List<ShareLink> findLinksExpiringBefore(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    /**
     * 查询永不过期的分享链接
     * 
     * @return 永不过期的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.expiresAt IS NULL")
    List<ShareLink> findNeverExpiringLinks();

    /**
     * 查询需要密码的分享链接
     * 
     * @return 需要密码的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.passwordHash IS NOT NULL AND sl.passwordHash != ''")
    List<ShareLink> findPasswordProtectedLinks();

    /**
     * 查询空间下需要密码的分享链接
     * 
     * @param space 所属空间
     * @return 需要密码的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.passwordHash IS NOT NULL AND sl.passwordHash != ''")
    List<ShareLink> findPasswordProtectedLinksBySpace(@Param("space") Space space);

    /**
     * 查询有下载限制的分享链接
     * 
     * @return 有下载限制的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.downloadLimit IS NOT NULL")
    List<ShareLink> findLinksWithDownloadLimit();

    /**
     * 查询达到下载限制的分享链接
     * 
     * @return 达到下载限制的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.downloadLimit IS NOT NULL AND sl.downloadCount >= sl.downloadLimit")
    List<ShareLink> findLinksReachedDownloadLimit();

    /**
     * 查询热门分享链接（下载次数超过阈值）
     * 
     * @param downloadThreshold 下载次数阈值
     * @return 热门分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.downloadCount > :downloadThreshold ORDER BY sl.downloadCount DESC")
    List<ShareLink> findPopularLinks(@Param("downloadThreshold") Integer downloadThreshold);

    /**
     * 查询空间下的热门分享链接
     * 
     * @param space 所属空间
     * @param downloadThreshold 下载次数阈值
     * @return 热门分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.downloadCount > :downloadThreshold ORDER BY sl.downloadCount DESC")
    List<ShareLink> findPopularLinksBySpace(@Param("space") Space space, @Param("downloadThreshold") Integer downloadThreshold);

    /**
     * 查询最近创建的分享链接
     * 
     * @param since 起始时间
     * @return 最近创建的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.createdAt > :since ORDER BY sl.createdAt DESC")
    List<ShareLink> findRecentLinks(@Param("since") LocalDateTime since);

    /**
     * 查询空间下最近创建的分享链接
     * 
     * @param space 所属空间
     * @param since 起始时间
     * @return 最近创建的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.createdAt > :since ORDER BY sl.createdAt DESC")
    List<ShareLink> findRecentLinksBySpace(@Param("space") Space space, @Param("since") LocalDateTime since);

    /**
     * 查询文件分享链接
     * 
     * @return 文件分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.file IS NOT NULL")
    List<ShareLink> findFileShareLinks();

    /**
     * 查询文件夹分享链接
     * 
     * @return 文件夹分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.folder IS NOT NULL")
    List<ShareLink> findFolderShareLinks();

    /**
     * 查询空间下的文件分享链接
     * 
     * @param space 所属空间
     * @return 文件分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.file IS NOT NULL")
    List<ShareLink> findFileShareLinksBySpace(@Param("space") Space space);

    /**
     * 查询空间下的文件夹分享链接
     * 
     * @param space 所属空间
     * @return 文件夹分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.folder IS NOT NULL")
    List<ShareLink> findFolderShareLinksBySpace(@Param("space") Space space);

    /**
     * 计算空间下所有分享链接的总下载次数
     * 
     * @param space 所属空间
     * @return 总下载次数
     */
    @Query("SELECT COALESCE(SUM(sl.downloadCount), 0) FROM ShareLink sl WHERE sl.space = :space")
    Long sumDownloadCountBySpace(@Param("space") Space space);

    /**
     * 批量更新分享链接状态
     * 
     * @param linkIds 分享链接ID列表
     * @param isActive 新状态
     * @return 更新的记录数
     */
    @Query("UPDATE ShareLink sl SET sl.isActive = :isActive WHERE sl.id IN :linkIds")
    int updateStatusByIds(@Param("linkIds") List<Long> linkIds, @Param("isActive") Boolean isActive);

    /**
     * 批量删除过期的分享链接
     * 
     * @param now 当前时间
     * @return 删除的记录数
     */
    @Query("DELETE FROM ShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt < :now")
    int deleteExpiredLinks(@Param("now") LocalDateTime now);

    /**
     * 根据空间ID查找分享链接列表（用于级联查询）
     * 
     * @param spaceId 空间ID
     * @return 分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space.id = :spaceId")
    List<ShareLink> findBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 根据文件ID查找分享链接列表（用于级联查询）
     * 
     * @param fileId 文件ID
     * @return 分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.file.id = :fileId")
    List<ShareLink> findByFileId(@Param("fileId") Long fileId);

    /**
     * 根据文件夹ID查找分享链接列表（用于级联查询）
     * 
     * @param folderId 文件夹ID
     * @return 分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.folder.id = :folderId")
    List<ShareLink> findByFolderId(@Param("folderId") Long folderId);

    /**
     * 查询有效的分享链接（活跃且未过期且未达到下载限制）
     * 
     * @param now 当前时间
     * @return 有效的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.isActive = true " +
           "AND (sl.expiresAt IS NULL OR sl.expiresAt > :now) " +
           "AND (sl.downloadLimit IS NULL OR sl.downloadCount < sl.downloadLimit)")
    List<ShareLink> findValidLinks(@Param("now") LocalDateTime now);

    /**
     * 查询空间下有效的分享链接
     * 
     * @param space 所属空间
     * @param now 当前时间
     * @return 有效的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE sl.space = :space AND sl.isActive = true " +
           "AND (sl.expiresAt IS NULL OR sl.expiresAt > :now) " +
           "AND (sl.downloadLimit IS NULL OR sl.downloadCount < sl.downloadLimit)")
    List<ShareLink> findValidLinksBySpace(@Param("space") Space space, @Param("now") LocalDateTime now);

    /**
     * 查询需要清理的分享链接（过期或达到下载限制）
     * 
     * @param now 当前时间
     * @return 需要清理的分享链接列表
     */
    @Query("SELECT sl FROM ShareLink sl WHERE " +
           "(sl.expiresAt IS NOT NULL AND sl.expiresAt < :now) OR " +
           "(sl.downloadLimit IS NOT NULL AND sl.downloadCount >= sl.downloadLimit)")
    List<ShareLink> findLinksToCleanup(@Param("now") LocalDateTime now);
} 