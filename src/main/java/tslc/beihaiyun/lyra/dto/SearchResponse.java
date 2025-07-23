package tslc.beihaiyun.lyra.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索响应DTO
 * 
 * @author Lyra Team
 */
public class SearchResponse {

    /**
     * 搜索结果项
     */
    public static class SearchResult {
        private Long id;
        private String name;
        private String type; // FILE 或 FOLDER
        private String path;
        private Long size;
        private String mimeType;
        private LocalDateTime modifiedDate;
        private LocalDateTime createdDate;
        private Long spaceId;
        private String spaceName;
        private Long folderId;
        private String folderPath;
        private Double relevanceScore; // 相关度评分
        private List<String> highlightSnippets; // 高亮片段

        // Constructors
        public SearchResult() {}

        public SearchResult(Long id, String name, String type, String path) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.path = path;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public LocalDateTime getModifiedDate() {
            return modifiedDate;
        }

        public void setModifiedDate(LocalDateTime modifiedDate) {
            this.modifiedDate = modifiedDate;
        }

        public LocalDateTime getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
        }

        public Long getSpaceId() {
            return spaceId;
        }

        public void setSpaceId(Long spaceId) {
            this.spaceId = spaceId;
        }

        public String getSpaceName() {
            return spaceName;
        }

        public void setSpaceName(String spaceName) {
            this.spaceName = spaceName;
        }

        public Long getFolderId() {
            return folderId;
        }

        public void setFolderId(Long folderId) {
            this.folderId = folderId;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public void setFolderPath(String folderPath) {
            this.folderPath = folderPath;
        }

        public Double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }

        public List<String> getHighlightSnippets() {
            return highlightSnippets;
        }

        public void setHighlightSnippets(List<String> highlightSnippets) {
            this.highlightSnippets = highlightSnippets;
        }
    }

    /**
     * 搜索历史记录
     */
    public static class SearchHistory {
        private Long id;
        private String keyword;
        private LocalDateTime searchTime;
        private Integer resultCount;
        private String searchType;

        // Constructors
        public SearchHistory() {}

        public SearchHistory(Long id, String keyword, LocalDateTime searchTime, Integer resultCount) {
            this.id = id;
            this.keyword = keyword;
            this.searchTime = searchTime;
            this.resultCount = resultCount;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public LocalDateTime getSearchTime() {
            return searchTime;
        }

        public void setSearchTime(LocalDateTime searchTime) {
            this.searchTime = searchTime;
        }

        public Integer getResultCount() {
            return resultCount;
        }

        public void setResultCount(Integer resultCount) {
            this.resultCount = resultCount;
        }

        public String getSearchType() {
            return searchType;
        }

        public void setSearchType(String searchType) {
            this.searchType = searchType;
        }
    }

    /**
     * 搜索统计信息
     */
    public static class SearchStats {
        private Long totalResults;
        private Long fileCount;
        private Long folderCount;
        private Long searchTime; // 搜索耗时（毫秒）
        private List<String> searchedSpaces;

        // Constructors
        public SearchStats() {}

        public SearchStats(Long totalResults, Long fileCount, Long folderCount, Long searchTime) {
            this.totalResults = totalResults;
            this.fileCount = fileCount;
            this.folderCount = folderCount;
            this.searchTime = searchTime;
        }

        // Getters and Setters
        public Long getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(Long totalResults) {
            this.totalResults = totalResults;
        }

        public Long getFileCount() {
            return fileCount;
        }

        public void setFileCount(Long fileCount) {
            this.fileCount = fileCount;
        }

        public Long getFolderCount() {
            return folderCount;
        }

        public void setFolderCount(Long folderCount) {
            this.folderCount = folderCount;
        }

        public Long getSearchTime() {
            return searchTime;
        }

        public void setSearchTime(Long searchTime) {
            this.searchTime = searchTime;
        }

        public List<String> getSearchedSpaces() {
            return searchedSpaces;
        }

        public void setSearchedSpaces(List<String> searchedSpaces) {
            this.searchedSpaces = searchedSpaces;
        }
    }
} 