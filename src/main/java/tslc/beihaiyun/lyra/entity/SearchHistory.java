package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 搜索历史实体
 * 
 * @author Lyra Team
 */
@Entity
@Table(name = "search_history", indexes = {
    @Index(name = "idx_search_history_user_time", columnList = "userId, created_at"),
    @Index(name = "idx_search_history_keyword", columnList = "keyword"),
    @Index(name = "idx_search_history_user_keyword", columnList = "userId, keyword")
})
public class SearchHistory extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Column(nullable = false)
    private Long userId;

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 200, message = "搜索关键词长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    private String keyword;

    /**
     * 搜索类型
     */
    @Size(max = 20, message = "搜索类型长度不能超过20个字符")
    @Column(length = 20)
    private String searchType;

    /**
     * 搜索结果数量
     */
    private Integer resultCount;

    /**
     * 搜索响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 搜索参数（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String searchParams;

    // Constructors
    public SearchHistory() {}

    public SearchHistory(Long userId, String keyword, String searchType, Integer resultCount) {
        this.userId = userId;
        this.keyword = keyword;
        this.searchType = searchType;
        this.resultCount = resultCount;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public String getSearchParams() {
        return searchParams;
    }

    public void setSearchParams(String searchParams) {
        this.searchParams = searchParams;
    }

    // ID field getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SearchHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", keyword='" + keyword + '\'' +
                ", searchType='" + searchType + '\'' +
                ", resultCount=" + resultCount +
                ", responseTime=" + responseTime +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
} 