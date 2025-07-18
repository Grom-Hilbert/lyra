package tslc.beihaiyun.lyra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;

import java.util.List;
import java.util.Optional;

/**
 * 文件版本数据访问接口
 * 提供文件版本相关的数据库操作
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    /**
     * 根据文件查找所有版本
     */
    List<FileVersion> findByFileOrderByVersionNumberDesc(FileEntity file);

    /**
     * 根据文件ID查找所有版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file.id = :fileId ORDER BY fv.versionNumber DESC")
    List<FileVersion> findByFileIdOrderByVersionNumberDesc(@Param("fileId") Long fileId);

    /**
     * 查找文件的当前版本
     */
    Optional<FileVersion> findByFileAndIsCurrentTrue(FileEntity file);

    /**
     * 根据文件和版本号查找版本
     */
    Optional<FileVersion> findByFileAndVersionNumber(FileEntity file, Integer versionNumber);

    /**
     * 查找文件的最新版本号
     */
    @Query("SELECT MAX(fv.versionNumber) FROM FileVersion fv WHERE fv.file = :file")
    Optional<Integer> findMaxVersionNumberByFile(@Param("file") FileEntity file);

    /**
     * 根据Git提交哈希查找版本
     */
    Optional<FileVersion> findByGitCommitHash(String gitCommitHash);

    /**
     * 统计文件的版本数量
     */
    @Query("SELECT COUNT(fv) FROM FileVersion fv WHERE fv.file = :file")
    Long countByFile(@Param("file") FileEntity file);

    /**
     * 查找特定校验和的版本
     */
    List<FileVersion> findByChecksum(String checksum);

    /**
     * 查找文件的前N个版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file ORDER BY fv.versionNumber DESC LIMIT :limit")
    List<FileVersion> findTopVersionsByFile(@Param("file") FileEntity file, @Param("limit") int limit);
}