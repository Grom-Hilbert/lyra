package tslc.beihaiyun.lyra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索请求DTO
 * 
 * @author Lyra Team
 */
public class SearchRequest {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 200, message = "搜索关键词长度不能超过200个字符")
    private String keyword;

    /**
     * 空间ID，null表示搜索所有有权限的空间
     */
    private Long spaceId;

    /**
     * 文件夹ID，null表示搜索整个空间
     */
    private Long folderId;

    /**
     * 搜索类型：ALL(全部)、FILE(文件)、FOLDER(文件夹)
     */
    private SearchType searchType = SearchType.ALL;

    /**
     * 文件类型过滤
     */
    private List<String> fileTypes;

    /**
     * 文件大小范围过滤（字节）
     */
    private Long minSize;
    private Long maxSize;

    /**
     * 修改时间范围过滤
     */
    private LocalDateTime modifiedAfter;
    private LocalDateTime modifiedBefore;

    /**
     * 创建时间范围过滤
     */
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    /**
     * 是否包含子文件夹
     */
    private Boolean includeSubfolders = true;

    /**
     * 是否区分大小写
     */
    private Boolean caseSensitive = false;

    /**
     * 是否精确匹配
     */
    private Boolean exactMatch = false;

    /**
     * 搜索排序方式
     */
    private SortBy sortBy = SortBy.RELEVANCE;

    /**
     * 搜索排序方向
     */
    private SortDirection sortDirection = SortDirection.DESC;

    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        ALL, FILE, FOLDER
    }

    /**
     * 排序方式枚举
     */
    public enum SortBy {
        RELEVANCE,      // 相关度
        NAME,           // 名称
        SIZE,           // 大小
        MODIFIED_DATE,  // 修改时间
        CREATED_DATE    // 创建时间
    }

    /**
     * 排序方向枚举
     */
    public enum SortDirection {
        ASC, DESC
    }

    // Constructors
    public SearchRequest() {}

    public SearchRequest(String keyword) {
        this.keyword = keyword;
    }

    public SearchRequest(String keyword, Long spaceId) {
        this.keyword = keyword;
        this.spaceId = spaceId;
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }

    public Long getMinSize() {
        return minSize;
    }

    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public LocalDateTime getModifiedAfter() {
        return modifiedAfter;
    }

    public void setModifiedAfter(LocalDateTime modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
    }

    public LocalDateTime getModifiedBefore() {
        return modifiedBefore;
    }

    public void setModifiedBefore(LocalDateTime modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
    }

    public LocalDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Boolean getIncludeSubfolders() {
        return includeSubfolders;
    }

    public void setIncludeSubfolders(Boolean includeSubfolders) {
        this.includeSubfolders = includeSubfolders;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public Boolean getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }
} 