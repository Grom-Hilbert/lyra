package tslc.beihaiyun.lyra.service.impl;

import tslc.beihaiyun.lyra.dto.SearchRequest;
import tslc.beihaiyun.lyra.dto.SearchResponse;
import tslc.beihaiyun.lyra.entity.*;
import tslc.beihaiyun.lyra.repository.*;
import tslc.beihaiyun.lyra.service.SearchService;
import tslc.beihaiyun.lyra.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * 
 * @author Lyra Team
 */
@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    @Autowired
    private FileEntityRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private EntityManager entityManager;

    /**
     * 搜索文件和文件夹
     */
    @Override
    public Page<SearchResponse.SearchResult> search(SearchRequest request, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        
        // 执行搜索
        Page<SearchResponse.SearchResult> results = performSearch(request, pageable);
        
        // 记录搜索历史（异步执行）
        saveSearchHistoryAsync(request, results.getTotalElements(), System.currentTimeMillis() - startTime);
        
        return results;
    }

    /**
     * 快速搜索文件名
     */
    @Override
    public List<SearchResponse.SearchResult> quickSearch(String keyword, Long spaceId, Long userId, int limit) {
        // 创建简单的搜索请求
        SearchRequest request = new SearchRequest(keyword, spaceId);
        request.setExactMatch(false);
        request.setCaseSensitive(false);
        
        // 执行搜索并限制结果数量
        List<SearchResponse.SearchResult> results = performQuickSearch(request, userId, limit);
        
        // 记录快速搜索历史
        saveSearchHistoryAsync(request, results.size(), 0L);
        
        return results;
    }

    /**
     * 高级搜索
     */
    @Override
    public Page<SearchResponse.SearchResult> advancedSearch(SearchRequest request, Pageable pageable) {
        return search(request, pageable);
    }

    /**
     * 保存搜索历史
     */
    @Override
    @Transactional
    public void saveSearchHistory(Long userId, String keyword, int resultCount) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setResultCount(resultCount);
        history.setSearchType("MANUAL");
        
        searchHistoryRepository.save(history);
    }

    /**
     * 获取用户搜索历史
     */
    @Override
    public List<SearchResponse.SearchHistory> getSearchHistory(Long userId, int limit) {
        List<SearchHistory> histories = searchHistoryRepository.findRecentByUserId(userId, PageRequest.of(0, limit));
        
        return histories.stream()
                .map(this::convertToSearchHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取搜索建议
     */
    @Override
    public List<String> getSearchSuggestions(String keyword, Long userId, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        
        // 基于用户历史的建议
        List<String> userSuggestions = searchHistoryRepository
                .findKeywordSuggestionsByUserIdAndPrefix(userId, keyword, PageRequest.of(0, limit / 2));
        
        // 基于全局热门的建议
        List<String> globalSuggestions = getPopularKeywords(limit / 2);
        
        // 合并去重
        Set<String> allSuggestions = new LinkedHashSet<>(userSuggestions);
        globalSuggestions.stream()
                .filter(s -> s.toLowerCase().contains(keyword.toLowerCase()))
                .forEach(allSuggestions::add);
        
        return allSuggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 清除用户搜索历史
     */
    @Override
    @Transactional
    public void clearSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    /**
     * 删除特定搜索历史记录
     */
    @Override
    @Transactional
    public void deleteSearchHistory(Long userId, Long historyId) {
        searchHistoryRepository.deleteByUserIdAndId(userId, historyId);
    }

    /**
     * 获取热门搜索关键词
     */
    @Override
    public List<String> getPopularKeywords(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(30); // 最近30天的热门搜索
        return searchHistoryRepository.findPopularKeywords(since, PageRequest.of(0, limit));
    }

    /**
     * 执行实际搜索
     */
    private Page<SearchResponse.SearchResult> performSearch(SearchRequest request, Pageable pageable) {
        List<SearchResponse.SearchResult> allResults = new ArrayList<>();
        
        // 搜索文件
        if (request.getSearchType() == SearchRequest.SearchType.ALL || 
            request.getSearchType() == SearchRequest.SearchType.FILE) {
            allResults.addAll(searchFiles(request));
        }
        
        // 搜索文件夹
        if (request.getSearchType() == SearchRequest.SearchType.ALL || 
            request.getSearchType() == SearchRequest.SearchType.FOLDER) {
            allResults.addAll(searchFolders(request));
        }
        
        // 排序和分页
        List<SearchResponse.SearchResult> sortedResults = sortResults(allResults, request);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedResults.size());
        List<SearchResponse.SearchResult> pageResults = start < sortedResults.size() ? 
                sortedResults.subList(start, end) : Collections.emptyList();
        
        return new PageImpl<>(pageResults, pageable, sortedResults.size());
    }

    /**
     * 执行快速搜索
     */
    private List<SearchResponse.SearchResult> performQuickSearch(SearchRequest request, Long userId, int limit) {
        List<SearchResponse.SearchResult> results = new ArrayList<>();
        
        // 搜索文件
        List<FileEntity> files = findFilesByKeyword(request.getKeyword(), request.getSpaceId(), limit / 2);
        results.addAll(convertFilesToSearchResults(files));
        
        // 搜索文件夹
        List<Folder> folders = findFoldersByKeyword(request.getKeyword(), request.getSpaceId(), limit / 2);
        results.addAll(convertFoldersToSearchResults(folders));
        
        // 计算相关度并排序
        return results.stream()
                .peek(result -> result.setRelevanceScore(calculateRelevanceScore(result, request.getKeyword())))
                .sorted((r1, r2) -> Double.compare(r2.getRelevanceScore(), r1.getRelevanceScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 搜索文件
     */
    private List<SearchResponse.SearchResult> searchFiles(SearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FileEntity> query = cb.createQuery(FileEntity.class);
        Root<FileEntity> root = query.from(FileEntity.class);
        
        List<Predicate> predicates = buildFileSearchPredicates(cb, root, request);
        
        query.where(predicates.toArray(new Predicate[0]));
        
        TypedQuery<FileEntity> typedQuery = entityManager.createQuery(query);
        List<FileEntity> files = typedQuery.getResultList();
        
        return convertFilesToSearchResults(files);
    }

    /**
     * 搜索文件夹
     */
    private List<SearchResponse.SearchResult> searchFolders(SearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Folder> query = cb.createQuery(Folder.class);
        Root<Folder> root = query.from(Folder.class);
        
        List<Predicate> predicates = buildFolderSearchPredicates(cb, root, request);
        
        query.where(predicates.toArray(new Predicate[0]));
        
        TypedQuery<Folder> typedQuery = entityManager.createQuery(query);
        List<Folder> folders = typedQuery.getResultList();
        
        return convertFoldersToSearchResults(folders);
    }

    /**
     * 构建文件搜索条件
     */
    private List<Predicate> buildFileSearchPredicates(CriteriaBuilder cb, Root<FileEntity> root, SearchRequest request) {
        List<Predicate> predicates = new ArrayList<>();
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            Expression<String> nameExpression = request.getCaseSensitive() ? 
                    root.get("originalName") : cb.lower(root.get("originalName"));
            String keyword = request.getCaseSensitive() ? 
                    request.getKeyword() : request.getKeyword().toLowerCase();
            
            if (request.getExactMatch()) {
                predicates.add(cb.equal(nameExpression, keyword));
            } else {
                predicates.add(cb.like(nameExpression, "%" + keyword + "%"));
            }
        }
        
        // 空间限制
        if (request.getSpaceId() != null) {
            predicates.add(cb.equal(root.get("spaceId"), request.getSpaceId()));
        }
        
        // 文件夹限制
        if (request.getFolderId() != null) {
            if (request.getIncludeSubfolders()) {
                // TODO: 实现子文件夹搜索逻辑
                predicates.add(cb.equal(root.get("folderId"), request.getFolderId()));
            } else {
                predicates.add(cb.equal(root.get("folderId"), request.getFolderId()));
            }
        }
        
        // 文件类型过滤
        if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
            predicates.add(root.get("mimeType").in(request.getFileTypes()));
        }
        
        // 文件大小过滤
        if (request.getMinSize() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("size"), request.getMinSize()));
        }
        if (request.getMaxSize() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("size"), request.getMaxSize()));
        }
        
        // 时间过滤
        if (request.getModifiedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), request.getModifiedAfter()));
        }
        if (request.getModifiedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), request.getModifiedBefore()));
        }
        if (request.getCreatedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getCreatedAfter()));
        }
        if (request.getCreatedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getCreatedBefore()));
        }
        
        // 排除已删除的文件
        predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.equal(root.get("deleted"), false)));
        
        return predicates;
    }

    /**
     * 构建文件夹搜索条件
     */
    private List<Predicate> buildFolderSearchPredicates(CriteriaBuilder cb, Root<Folder> root, SearchRequest request) {
        List<Predicate> predicates = new ArrayList<>();
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            Expression<String> nameExpression = request.getCaseSensitive() ? 
                    root.get("name") : cb.lower(root.get("name"));
            String keyword = request.getCaseSensitive() ? 
                    request.getKeyword() : request.getKeyword().toLowerCase();
            
            if (request.getExactMatch()) {
                predicates.add(cb.equal(nameExpression, keyword));
            } else {
                predicates.add(cb.like(nameExpression, "%" + keyword + "%"));
            }
        }
        
        // 空间限制
        if (request.getSpaceId() != null) {
            predicates.add(cb.equal(root.get("spaceId"), request.getSpaceId()));
        }
        
        // 父文件夹限制
        if (request.getFolderId() != null) {
            predicates.add(cb.equal(root.get("parentId"), request.getFolderId()));
        }
        
        // 时间过滤
        if (request.getModifiedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), request.getModifiedAfter()));
        }
        if (request.getModifiedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), request.getModifiedBefore()));
        }
        if (request.getCreatedAfter() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getCreatedAfter()));
        }
        if (request.getCreatedBefore() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getCreatedBefore()));
        }
        
        // 排除已删除的文件夹
        predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.equal(root.get("deleted"), false)));
        
        return predicates;
    }

    /**
     * 简单的文件名关键词搜索
     */
    private List<FileEntity> findFilesByKeyword(String keyword, Long spaceId, int limit) {
        // 使用简单的JPA方法名查询，暂时不过滤删除状态
        org.springframework.data.domain.PageRequest pageRequest = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        
        if (spaceId != null) {
            // 使用Criteria API或自定义查询方法
            return fileRepository.findAll(pageRequest).getContent().stream()
                    .filter(f -> f.getOriginalName().toLowerCase().contains(keyword.toLowerCase()))
                    .filter(f -> spaceId.equals(f.getSpace() != null ? f.getSpace().getId() : null))
                    .filter(f -> f.getDeleted() == null || !f.getDeleted())
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            return fileRepository.findAll(pageRequest).getContent().stream()
                    .filter(f -> f.getOriginalName().toLowerCase().contains(keyword.toLowerCase()))
                    .filter(f -> f.getDeleted() == null || !f.getDeleted())
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * 简单的文件夹名关键词搜索
     */
    private List<Folder> findFoldersByKeyword(String keyword, Long spaceId, int limit) {
        // 使用简单的JPA方法名查询，暂时不过滤删除状态
        org.springframework.data.domain.PageRequest pageRequest = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        
        if (spaceId != null) {
            return folderRepository.findAll(pageRequest).getContent().stream()
                    .filter(f -> f.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .filter(f -> spaceId.equals(f.getSpace() != null ? f.getSpace().getId() : null))
                    .filter(f -> f.getDeleted() == null || !f.getDeleted())
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            return folderRepository.findAll(pageRequest).getContent().stream()
                    .filter(f -> f.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .filter(f -> f.getDeleted() == null || !f.getDeleted())
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * 转换文件实体为搜索结果
     */
    private List<SearchResponse.SearchResult> convertFilesToSearchResults(List<FileEntity> files) {
        return files.stream()
                .map(this::convertFileToSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 转换文件夹实体为搜索结果
     */
    private List<SearchResponse.SearchResult> convertFoldersToSearchResults(List<Folder> folders) {
        return folders.stream()
                .map(this::convertFolderToSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个文件为搜索结果
     */
    private SearchResponse.SearchResult convertFileToSearchResult(FileEntity file) {
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setId(file.getId());
        result.setName(file.getOriginalName());
        result.setType("FILE");
        result.setPath(file.getStoragePath());
        result.setSize(file.getSizeBytes());
        result.setMimeType(file.getMimeType());
        result.setModifiedDate(file.getUpdatedAt());
        result.setCreatedDate(file.getCreatedAt());
        result.setSpaceId(file.getSpace() != null ? file.getSpace().getId() : null);
        result.setFolderId(file.getFolder() != null ? file.getFolder().getId() : null);
        return result;
    }

    /**
     * 转换单个文件夹为搜索结果
     */
    private SearchResponse.SearchResult convertFolderToSearchResult(Folder folder) {
        SearchResponse.SearchResult result = new SearchResponse.SearchResult();
        result.setId(folder.getId());
        result.setName(folder.getName());
        result.setType("FOLDER");
        result.setPath(folder.getPath());
        result.setModifiedDate(folder.getUpdatedAt());
        result.setCreatedDate(folder.getCreatedAt());
        result.setSpaceId(folder.getSpace() != null ? folder.getSpace().getId() : null);
        result.setFolderId(folder.getParent() != null ? folder.getParent().getId() : null);
        return result;
    }

    /**
     * 转换搜索历史实体为DTO
     */
    private SearchResponse.SearchHistory convertToSearchHistoryDto(SearchHistory history) {
        SearchResponse.SearchHistory dto = new SearchResponse.SearchHistory();
        dto.setId(history.getId());
        dto.setKeyword(history.getKeyword());
        dto.setSearchTime(history.getCreatedAt());
        dto.setResultCount(history.getResultCount());
        dto.setSearchType(history.getSearchType());
        return dto;
    }

    /**
     * 对搜索结果进行排序
     */
    private List<SearchResponse.SearchResult> sortResults(List<SearchResponse.SearchResult> results, SearchRequest request) {
        Comparator<SearchResponse.SearchResult> comparator;
        
        switch (request.getSortBy()) {
            case NAME:
                comparator = Comparator.comparing(SearchResponse.SearchResult::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case SIZE:
                comparator = Comparator.comparing(r -> r.getSize() != null ? r.getSize() : 0L);
                break;
            case MODIFIED_DATE:
                comparator = Comparator.comparing(SearchResponse.SearchResult::getModifiedDate);
                break;
            case CREATED_DATE:
                comparator = Comparator.comparing(SearchResponse.SearchResult::getCreatedDate);
                break;
            case RELEVANCE:
            default:
                // 计算相关度
                results.forEach(result -> {
                    result.setRelevanceScore(calculateRelevanceScore(result, request.getKeyword()));
                });
                comparator = Comparator.comparing(SearchResponse.SearchResult::getRelevanceScore);
                break;
        }
        
        if (request.getSortDirection() == SearchRequest.SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        
        return results.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 计算搜索结果的相关度评分
     */
    private Double calculateRelevanceScore(SearchResponse.SearchResult result, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return 0.0;
        }
        
        String name = result.getName().toLowerCase();
        keyword = keyword.toLowerCase();
        
        double score = 0.0;
        
        // 完全匹配得分最高
        if (name.equals(keyword)) {
            score += 100.0;
        }
        // 开头匹配
        else if (name.startsWith(keyword)) {
            score += 80.0;
        }
        // 包含匹配
        else if (name.contains(keyword)) {
            score += 60.0;
        }
        
        // 根据匹配长度调整分数
        double lengthRatio = (double) keyword.length() / name.length();
        score *= (0.5 + lengthRatio * 0.5);
        
        // 文件类型加权（文件夹权重稍低）
        if ("FOLDER".equals(result.getType())) {
            score *= 0.9;
        }
        
        return score;
    }

    /**
     * 异步保存搜索历史
     */
    private void saveSearchHistoryAsync(SearchRequest request, long resultCount, long responseTime) {
        // TODO: 这里应该使用异步处理，暂时直接执行
        // 可以使用 @Async 注解或消息队列来实现异步处理
        try {
            SearchHistory history = new SearchHistory();
            history.setUserId(1L); // TODO: 从当前用户上下文获取用户ID
            history.setKeyword(request.getKeyword());
            history.setSearchType(request.getSearchType().name());
            history.setResultCount((int) resultCount);
            history.setResponseTime(responseTime);
            
            searchHistoryRepository.save(history);
        } catch (Exception e) {
            // 记录日志但不影响搜索功能
            // logger.warn("Failed to save search history", e);
        }
    }
} 