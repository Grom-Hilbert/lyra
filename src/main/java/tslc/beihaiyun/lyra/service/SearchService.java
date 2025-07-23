package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.dto.SearchRequest;
import tslc.beihaiyun.lyra.dto.SearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * 搜索服务接口
 * 提供文件和文件夹的搜索功能，包括模糊搜索、过滤、历史记录等
 * 
 * @author Lyra Team
 */
public interface SearchService {

    /**
     * 搜索文件和文件夹
     * 
     * @param request 搜索请求
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<SearchResponse.SearchResult> search(SearchRequest request, Pageable pageable);

    /**
     * 快速搜索文件名
     * 
     * @param keyword 关键词
     * @param spaceId 空间ID
     * @param userId 用户ID
     * @param limit 结果限制数量
     * @return 搜索结果列表
     */
    List<SearchResponse.SearchResult> quickSearch(String keyword, Long spaceId, Long userId, int limit);

    /**
     * 高级搜索，支持多种过滤条件
     * 
     * @param request 搜索请求
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<SearchResponse.SearchResult> advancedSearch(SearchRequest request, Pageable pageable);

    /**
     * 保存搜索历史
     * 
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param resultCount 结果数量
     */
    void saveSearchHistory(Long userId, String keyword, int resultCount);

    /**
     * 获取用户搜索历史
     * 
     * @param userId 用户ID
     * @param limit 历史记录数量限制
     * @return 搜索历史列表
     */
    List<SearchResponse.SearchHistory> getSearchHistory(Long userId, int limit);

    /**
     * 获取搜索建议
     * 
     * @param keyword 关键词前缀
     * @param userId 用户ID
     * @param limit 建议数量限制
     * @return 搜索建议列表
     */
    List<String> getSearchSuggestions(String keyword, Long userId, int limit);

    /**
     * 清除用户搜索历史
     * 
     * @param userId 用户ID
     */
    void clearSearchHistory(Long userId);

    /**
     * 删除特定搜索历史记录
     * 
     * @param userId 用户ID
     * @param historyId 历史记录ID
     */
    void deleteSearchHistory(Long userId, Long historyId);

    /**
     * 获取热门搜索关键词
     * 
     * @param limit 关键词数量限制
     * @return 热门关键词列表
     */
    List<String> getPopularKeywords(int limit);
} 