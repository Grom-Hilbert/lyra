package tslc.beihaiyun.lyra.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件版本Repository接口
 * 提供文件版本数据访问操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    /**
     * 根据文件查找所有版本
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> findByFile(FileEntity file);

    /**
     * 根据文件和版本号查找特定版本
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 文件版本（可选）
     */
    Optional<FileVersion> findByFileAndVersionNumber(FileEntity file, Integer versionNumber);

    /**
     * 根据文件查找最新版本
     *
     * @param file 文件实体
     * @return 最新版本（可选）
     */
    Optional<FileVersion> findFirstByFileOrderByVersionNumberDesc(FileEntity file);

    /**
     * 根据文件查找第一个版本
     *
     * @param file 文件实体
     * @return 第一个版本（可选）
     */
    Optional<FileVersion> findFirstByFileOrderByVersionNumberAsc(FileEntity file);

    /**
     * 根据文件分页查询版本列表
     * 
     * @param file 文件实体
     * @param pageable 分页参数
     * @return 版本分页结果
     */
    Page<FileVersion> findByFile(FileEntity file, Pageable pageable);

    /**
     * 根据文件查找版本列表（按版本号倒序）
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> findByFileOrderByVersionNumberDesc(FileEntity file);

    /**
     * 根据文件查找版本列表（按版本号正序）
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> findByFileOrderByVersionNumberAsc(FileEntity file);

    /**
     * 根据文件查找版本列表（按创建时间倒序）
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    List<FileVersion> findByFileOrderByCreatedAtDesc(FileEntity file);

    /**
     * 根据文件哈希值查找版本
     * 
     * @param fileHash 文件哈希值
     * @return 版本列表
     */
    List<FileVersion> findByFileHash(String fileHash);

    /**
     * 检查文件和版本号是否已存在
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 是否存在
     */
    boolean existsByFileAndVersionNumber(FileEntity file, Integer versionNumber);

    /**
     * 统计文件的版本数量
     * 
     * @param file 文件实体
     * @return 版本数量
     */
    long countByFile(FileEntity file);

    /**
     * 计算文件所有版本的总大小
     * 
     * @param file 文件实体
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(fv.sizeBytes), 0) FROM FileVersion fv WHERE fv.file = :file")
    Long sumSizeBytesByFile(@Param("file") FileEntity file);

    /**
     * 查询文件的指定数量的最新版本
     *
     * @param file 文件实体
     * @param pageable 分页参数
     * @return 最新版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file ORDER BY fv.versionNumber DESC")
    List<FileVersion> findLatestVersionsByFile(@Param("file") FileEntity file, Pageable pageable);

    /**
     * 查询文件指定版本号范围的版本
     * 
     * @param file 文件实体
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.versionNumber BETWEEN :fromVersion AND :toVersion ORDER BY fv.versionNumber")
    List<FileVersion> findByFileAndVersionNumberBetween(@Param("file") FileEntity file, 
                                                       @Param("fromVersion") Integer fromVersion, 
                                                       @Param("toVersion") Integer toVersion);

    /**
     * 查询大版本文件（超过指定大小阈值）
     * 
     * @param file 文件实体
     * @param sizeThreshold 大小阈值（字节）
     * @return 大版本文件列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.sizeBytes > :sizeThreshold ORDER BY fv.sizeBytes DESC")
    List<FileVersion> findLargeVersionsByFile(@Param("file") FileEntity file, @Param("sizeThreshold") Long sizeThreshold);

    /**
     * 查询指定时间范围内的文件版本
     * 
     * @param file 文件实体
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.createdAt BETWEEN :startTime AND :endTime ORDER BY fv.createdAt DESC")
    List<FileVersion> findByFileAndCreatedAtBetween(@Param("file") FileEntity file,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近创建的文件版本
     * 
     * @param file 文件实体
     * @param since 起始时间
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.createdAt > :since ORDER BY fv.createdAt DESC")
    List<FileVersion> findRecentVersionsByFile(@Param("file") FileEntity file, @Param("since") LocalDateTime since);

    /**
     * 查询有变更注释的版本
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.changeComment IS NOT NULL AND fv.changeComment != '' ORDER BY fv.versionNumber DESC")
    List<FileVersion> findVersionsWithCommentByFile(@Param("file") FileEntity file);

    /**
     * 查询没有变更注释的版本
     * 
     * @param file 文件实体
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND (fv.changeComment IS NULL OR fv.changeComment = '') ORDER BY fv.versionNumber DESC")
    List<FileVersion> findVersionsWithoutCommentByFile(@Param("file") FileEntity file);

    /**
     * 根据变更注释模糊查询版本
     * 
     * @param file 文件实体
     * @param comment 注释关键字
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.changeComment LIKE %:comment% ORDER BY fv.versionNumber DESC")
    List<FileVersion> findByFileAndChangeCommentContaining(@Param("file") FileEntity file, @Param("comment") String comment);

    /**
     * 查询可以清理的旧版本（保留最新N个版本）
     * 
     * @param file 文件实体
     * @param keepCount 保留的版本数量
     * @return 可清理的版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.versionNumber <= " +
           "(SELECT MAX(fv2.versionNumber) - :keepCount FROM FileVersion fv2 WHERE fv2.file = :file) " +
           "ORDER BY fv.versionNumber")
    List<FileVersion> findCleanableVersionsByFile(@Param("file") FileEntity file, @Param("keepCount") int keepCount);

    /**
     * 查询超过指定天数的旧版本
     * 
     * @param file 文件实体
     * @param daysAgo 天数前
     * @return 旧版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.createdAt < :daysAgo ORDER BY fv.versionNumber")
    List<FileVersion> findOldVersionsByFile(@Param("file") FileEntity file, @Param("daysAgo") LocalDateTime daysAgo);

    /**
     * 批量删除文件的指定版本号范围
     * 
     * @param file 文件实体
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @return 删除的记录数
     */
    @Query("DELETE FROM FileVersion fv WHERE fv.file = :file AND fv.versionNumber BETWEEN :fromVersion AND :toVersion")
    int deleteByFileAndVersionNumberBetween(@Param("file") FileEntity file,
                                          @Param("fromVersion") Integer fromVersion,
                                          @Param("toVersion") Integer toVersion);

    /**
     * 批量删除文件的旧版本（保留最新N个版本）
     * 
     * @param file 文件实体
     * @param keepCount 保留的版本数量
     * @return 删除的记录数
     */
    @Query("DELETE FROM FileVersion fv WHERE fv.file = :file AND fv.versionNumber <= " +
           "(SELECT MAX(fv2.versionNumber) - :keepCount FROM FileVersion fv2 WHERE fv2.file = :file)")
    int deleteOldVersionsByFile(@Param("file") FileEntity file, @Param("keepCount") int keepCount);

    /**
     * 根据文件ID查找版本列表（用于级联查询）
     * 
     * @param fileId 文件ID
     * @return 版本列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file.id = :fileId")
    List<FileVersion> findByFileId(@Param("fileId") Long fileId);

    /**
     * 查询系统中所有的大版本文件（超过指定大小阈值）
     * 
     * @param sizeThreshold 大小阈值（字节）
     * @return 大版本文件列表
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.sizeBytes > :sizeThreshold ORDER BY fv.sizeBytes DESC")
    List<FileVersion> findAllLargeVersions(@Param("sizeThreshold") Long sizeThreshold);

    /**
     * 统计系统中所有版本的总大小
     * 
     * @return 总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(fv.sizeBytes), 0) FROM FileVersion fv")
    Long sumAllVersionsSize();

    /**
     * 查询系统中最大的版本文件
     *
     * @param pageable 分页参数
     * @return 最大版本文件列表
     */
    @Query("SELECT fv FROM FileVersion fv ORDER BY fv.sizeBytes DESC")
    List<FileVersion> findLargestVersions(Pageable pageable);

    /**
     * 查询重复的版本（相同哈希值）
     * 
     * @return 重复版本组（按哈希值分组）
     */
    @Query("SELECT fv.fileHash, COUNT(fv) FROM FileVersion fv WHERE fv.fileHash IS NOT NULL GROUP BY fv.fileHash HAVING COUNT(fv) > 1")
    List<Object[]> findDuplicateVersionsByHash();
} 