package tslc.beihaiyun.lyra.controller;

import tslc.beihaiyun.lyra.dto.SearchRequest;
import tslc.beihaiyun.lyra.dto.SearchResponse;
import tslc.beihaiyun.lyra.service.SearchService;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * 搜索控制器
 * 提供文件和文件夹搜索相关的REST API
 * 
 * @author Lyra Team
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 执行搜索
     * 
     * @param request 搜索请求
     * @param pageable 分页参数
     * @param user 当前用户
     * @return 搜索结果
     */
    @PostMapping
    public ResponseEntity<Page<SearchResponse.SearchResult>> search(
            @Valid @RequestBody SearchRequest request,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        Page<SearchResponse.SearchResult> results = searchService.search(request, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 快速搜索（用于搜索框自动完成）
     * 
     * @param keyword 搜索关键词
     * @param spaceId 空间ID（可选）
     * @param limit 结果限制数量
     * @param user 当前用户
     * @return 搜索结果列表
     */
    @GetMapping("/quick")
    public ResponseEntity<List<SearchResponse.SearchResult>> quickSearch(
            @RequestParam @NotBlank String keyword,
            @RequestParam(required = false) Long spaceId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        List<SearchResponse.SearchResult> results = searchService.quickSearch(
                keyword, spaceId, user.getId(), limit);
        return ResponseEntity.ok(results);
    }

    /**
     * 高级搜索
     * 
     * @param request 搜索请求
     * @param pageable 分页参数
     * @param user 当前用户
     * @return 搜索结果
     */
    @PostMapping("/advanced")
    public ResponseEntity<Page<SearchResponse.SearchResult>> advancedSearch(
            @Valid @RequestBody SearchRequest request,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        Page<SearchResponse.SearchResult> results = searchService.advancedSearch(request, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取搜索建议
     * 
     * @param keyword 关键词前缀
     * @param limit 建议数量限制
     * @param user 当前用户
     * @return 搜索建议列表
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        List<String> suggestions = searchService.getSearchSuggestions(keyword, user.getId(), limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * 获取用户搜索历史
     * 
     * @param limit 历史记录数量限制
     * @param user 当前用户
     * @return 搜索历史列表
     */
    @GetMapping("/history")
    public ResponseEntity<List<SearchResponse.SearchHistory>> getSearchHistory(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        List<SearchResponse.SearchHistory> history = searchService.getSearchHistory(user.getId(), limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 清除搜索历史
     * 
     * @param user 当前用户
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearSearchHistory(
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        searchService.clearSearchHistory(user.getId());
        return ResponseEntity.ok(Map.of("message", "搜索历史已清除"));
    }

    /**
     * 删除特定搜索历史记录
     * 
     * @param historyId 历史记录ID
     * @param user 当前用户
     * @return 操作结果
     */
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<Map<String, String>> deleteSearchHistory(
            @PathVariable Long historyId,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        searchService.deleteSearchHistory(user.getId(), historyId);
        return ResponseEntity.ok(Map.of("message", "搜索历史记录已删除"));
    }

    /**
     * 获取热门搜索关键词
     * 
     * @param limit 关键词数量限制
     * @return 热门关键词列表
     */
    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularKeywords(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        
        List<String> keywords = searchService.getPopularKeywords(limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 手动保存搜索历史（用于客户端主动保存重要搜索）
     * 
     * @param keyword 搜索关键词
     * @param resultCount 结果数量
     * @param user 当前用户
     * @return 操作结果
     */
    @PostMapping("/history")
    public ResponseEntity<Map<String, String>> saveSearchHistory(
            @RequestParam @NotBlank String keyword,
            @RequestParam @Min(0) int resultCount,
            @AuthenticationPrincipal LyraUserPrincipal user) {
        
        searchService.saveSearchHistory(user.getId(), keyword, resultCount);
        return ResponseEntity.ok(Map.of("message", "搜索历史已保存"));
    }

    /**
     * 搜索统计信息（管理员功能）
     * 
     * @return 搜索统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        // TODO: 实现搜索统计功能
        return ResponseEntity.ok(Map.of(
                "totalSearches", 0,
                "uniqueKeywords", 0,
                "avgResultsPerSearch", 0.0
        ));
    }
} 