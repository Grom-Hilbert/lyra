package tslc.beihaiyun.lyra.repository;

import tslc.beihaiyun.lyra.entity.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索历史Repository接口
 * 
 * @author Lyra Team
 */
@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    /**
     * 根据用户ID查询搜索历史（按时间降序）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 搜索历史列表
     */
    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID查询最近的搜索历史
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 搜索历史列表
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.userId = :userId ORDER BY sh.createdAt DESC LIMIT :limit")
    List<SearchHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 根据用户ID和关键词查询搜索历史
     * 
     * @param userId 用户ID
     * @param keyword 关键词
     * @return 搜索历史列表
     */
    List<SearchHistory> findByUserIdAndKeywordContainingIgnoreCaseOrderByCreatedAtDesc(Long userId, String keyword);

    /**
     * 获取用户的搜索建议（基于历史关键词）
     * 
     * @param userId 用户ID
     * @param prefix 关键词前缀
     * @param limit 限制数量
     * @return 关键词列表
     */
    @Query("SELECT DISTINCT sh.keyword FROM SearchHistory sh WHERE sh.userId = :userId " +
           "AND LOWER(sh.keyword) LIKE LOWER(CONCAT(:prefix, '%')) " +
           "ORDER BY sh.createdAt DESC LIMIT :limit")
    List<String> findKeywordSuggestionsByUserIdAndPrefix(@Param("userId") Long userId, 
                                                         @Param("prefix") String prefix,
                                                         @Param("limit") int limit);

    /**
     * 获取全局热门搜索关键词
     * 
     * @param limit 限制数量
     * @return 关键词列表
     */
    @Query("SELECT sh.keyword, COUNT(sh.keyword) as cnt FROM SearchHistory sh " +
           "WHERE sh.createdAt >= :since " +
           "GROUP BY sh.keyword " +
           "ORDER BY cnt DESC LIMIT :limit")
    List<String> findPopularKeywords(@Param("since") LocalDateTime since, @Param("limit") int limit);

    /**
     * 根据用户ID删除搜索历史
     * 
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和历史记录ID删除特定搜索历史
     * 
     * @param userId 用户ID
     * @param historyId 历史记录ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.userId = :userId AND sh.id = :historyId")
    int deleteByUserIdAndId(@Param("userId") Long userId, @Param("historyId") Long historyId);

    /**
     * 清理过期的搜索历史（超过指定天数）
     * 
     * @param before 时间界限
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.createdAt < :before")
    int deleteOldHistory(@Param("before") LocalDateTime before);

    /**
     * 统计用户搜索历史总数
     * 
     * @param userId 用户ID
     * @return 历史记录总数
     */
    long countByUserId(Long userId);

    /**
     * 查询用户最频繁搜索的关键词
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 关键词列表
     */
    @Query("SELECT sh.keyword, COUNT(sh.keyword) as cnt FROM SearchHistory sh " +
           "WHERE sh.userId = :userId " +
           "GROUP BY sh.keyword " +
           "ORDER BY cnt DESC LIMIT :limit")
    List<String> findMostSearchedKeywordsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 检查用户是否搜索过某个关键词
     * 
     * @param userId 用户ID
     * @param keyword 关键词
     * @return 是否存在
     */
    boolean existsByUserIdAndKeyword(Long userId, String keyword);
} 