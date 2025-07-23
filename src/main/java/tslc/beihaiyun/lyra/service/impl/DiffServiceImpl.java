package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FileVersionRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.service.DiffService;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.service.VersionService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 版本差异对比服务实现类
 * 提供文本文件差异对比、二进制文件版本管理和版本历史展示功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
@Transactional
public class DiffServiceImpl implements DiffService {

    private static final Logger logger = LoggerFactory.getLogger(DiffServiceImpl.class);

    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private VersionService versionService;

    // 支持的文本文件扩展名
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        ".txt", ".md", ".java", ".js", ".ts", ".html", ".css", ".xml", ".json", 
        ".yml", ".yaml", ".properties", ".conf", ".ini", ".sql", ".py", ".c", 
        ".cpp", ".h", ".sh", ".bat", ".log", ".csv"
    );

    // 支持的二进制文件扩展名
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".ico", ".pdf", ".doc", ".docx",
        ".xls", ".xlsx", ".ppt", ".pptx", ".zip", ".tar", ".gz", ".rar", ".7z",
        ".exe", ".dll", ".so", ".dylib", ".mp3", ".mp4", ".avi", ".mov", ".wav"
    );

    // ==================== 文本文件差异对比 ====================

    @Override
    public DiffResult compareVersions(FileEntity file, Integer fromVersionNumber, Integer toVersionNumber) {
        try {
            logger.debug("比较文件 {} 的版本 {} 和 {}", file.getName(), fromVersionNumber, toVersionNumber);

            Optional<FileVersion> fromVersion = fileVersionRepository.findByFileAndVersionNumber(file, fromVersionNumber);
            Optional<FileVersion> toVersion = fileVersionRepository.findByFileAndVersionNumber(file, toVersionNumber);

            if (fromVersion.isEmpty()) {
                return new DiffResult(DiffType.TEXT, "源版本 " + fromVersionNumber + " 不存在");
            }
            if (toVersion.isEmpty()) {
                return new DiffResult(DiffType.TEXT, "目标版本 " + toVersionNumber + " 不存在");
            }

            String fromContent = loadVersionContent(fromVersion.get());
            String toContent = loadVersionContent(toVersion.get());

            if (fromContent == null || toContent == null) {
                return new DiffResult(DiffType.TEXT, "无法加载版本内容");
            }

            DiffType diffType = detectDiffType(file.getName(), file.getMimeType());
            if (diffType == DiffType.TEXT) {
                return compareText(fromContent, toContent, file.getName());
            } else {
                return compareBinaryContent(fromContent.getBytes(), toContent.getBytes(), file.getName());
            }

        } catch (Exception e) {
            logger.error("比较版本时发生错误: {}", e.getMessage(), e);
            return new DiffResult(DiffType.TEXT, "比较版本时发生错误: " + e.getMessage());
        }
    }

    @Override
    public DiffResult compareWithCurrentVersion(FileEntity file, Integer versionNumber) {
        try {
            Optional<FileVersion> currentVersion = fileVersionRepository.findLatestByFile(file);
            if (currentVersion.isEmpty()) {
                return new DiffResult(DiffType.TEXT, "当前文件没有版本");
            }

            return compareVersions(file, versionNumber, currentVersion.get().getVersionNumber());

        } catch (Exception e) {
            logger.error("与当前版本比较时发生错误: {}", e.getMessage(), e);
            return new DiffResult(DiffType.TEXT, "与当前版本比较时发生错误: " + e.getMessage());
        }
    }

    @Override
    public DiffResult compareText(String originalText, String newText, String fileName) {
        try {
            logger.debug("比较文本文件差异: {}", fileName);

            if (originalText == null || newText == null) {
                return new DiffResult(DiffType.TEXT, "文本内容不能为空");
            }

            // 按行分割文本
            List<String> originalLines = Arrays.asList(originalText.split("\\r?\\n"));
            List<String> newLines = Arrays.asList(newText.split("\\r?\\n"));

            // 执行差异算法
            List<DiffLine> diffLines = performTextDiff(originalLines, newLines);

            // 计算统计信息
            int addedLines = (int) diffLines.stream().filter(line -> line.getOperation() == DiffOperation.INSERT).count();
            int deletedLines = (int) diffLines.stream().filter(line -> line.getOperation() == DiffOperation.DELETE).count();
            int modifiedLines = (int) diffLines.stream().filter(line -> line.getOperation() == DiffOperation.REPLACE).count();

            // 生成统一差异格式
            String unifiedDiff = generateUnifiedDiff(originalText, newText, fileName, 3);

            // 计算相似度
            double similarity = calculateTextSimilarity(originalText, newText);

            return new DiffResult(DiffType.TEXT, diffLines, unifiedDiff, addedLines, deletedLines, modifiedLines, similarity);

        } catch (Exception e) {
            logger.error("文本差异比较时发生错误: {}", e.getMessage(), e);
            return new DiffResult(DiffType.TEXT, "文本差异比较时发生错误: " + e.getMessage());
        }
    }

    @Override
    public DiffResult compareStreams(InputStream originalStream, InputStream newStream, String fileName, String mimeType) {
        try {
            DiffType diffType = detectDiffType(fileName, mimeType);

            if (diffType == DiffType.TEXT) {
                String originalText = readStreamAsText(originalStream);
                String newText = readStreamAsText(newStream);
                return compareText(originalText, newText, fileName);
            } else {
                return compareBinaryFiles(originalStream, newStream, fileName);
            }

        } catch (Exception e) {
            logger.error("文件流差异比较时发生错误: {}", e.getMessage(), e);
            DiffType resultType = detectDiffType(fileName, mimeType);
            return new DiffResult(resultType, "文件流差异比较时发生错误: " + e.getMessage());
        }
    }

    @Override
    public String generateUnifiedDiff(String originalText, String newText, String fileName, int contextLines) {
        try {
            List<String> originalLines = Arrays.asList(originalText.split("\\r?\\n"));
            List<String> newLines = Arrays.asList(newText.split("\\r?\\n"));

            StringBuilder diff = new StringBuilder();
            diff.append("--- a/").append(fileName).append("\n");
            diff.append("+++ b/").append(fileName).append("\n");

            List<DiffLine> diffLines = performTextDiff(originalLines, newLines);
            
            // 生成差异块
            List<DiffHunk> hunks = generateDiffHunks(diffLines, contextLines);
            
            for (DiffHunk hunk : hunks) {
                diff.append(hunk.getHeader()).append("\n");
                for (String line : hunk.getLines()) {
                    diff.append(line).append("\n");
                }
            }

            return diff.toString();

        } catch (Exception e) {
            logger.error("生成统一差异格式时发生错误: {}", e.getMessage(), e);
            return "生成统一差异格式时发生错误: " + e.getMessage();
        }
    }

    // ==================== 二进制文件版本管理 ====================

    @Override
    public DiffType detectDiffType(String fileName, String mimeType) {
        if (fileName == null) {
            return DiffType.UNSUPPORTED;
        }

        String extension = getFileExtension(fileName).toLowerCase();
        
        if (TEXT_EXTENSIONS.contains(extension)) {
            return DiffType.TEXT;
        } else if (BINARY_EXTENSIONS.contains(extension)) {
            return DiffType.BINARY;
        }

        // 通过MIME类型判断
        if (mimeType != null) {
            if (mimeType.startsWith("text/") || 
                mimeType.equals("application/json") ||
                mimeType.equals("application/xml") ||
                mimeType.equals("application/javascript")) {
                return DiffType.TEXT;
            } else if (mimeType.startsWith("image/") ||
                       mimeType.startsWith("video/") ||
                       mimeType.startsWith("audio/") ||
                       mimeType.equals("application/octet-stream")) {
                return DiffType.BINARY;
            }
        }

        return DiffType.UNSUPPORTED;
    }

    @Override
    public DiffResult compareBinaryFiles(InputStream originalStream, InputStream newStream, String fileName) {
        try {
            byte[] originalBytes = readStreamAsBytes(originalStream);
            byte[] newBytes = readStreamAsBytes(newStream);

            return compareBinaryContent(originalBytes, newBytes, fileName);

        } catch (Exception e) {
            logger.error("二进制文件比较时发生错误: {}", e.getMessage(), e);
            return new DiffResult(DiffType.BINARY, "二进制文件比较时发生错误: " + e.getMessage());
        }
    }

    @Override
    public double calculateSimilarity(InputStream originalStream, InputStream newStream) {
        try {
            byte[] originalBytes = readStreamAsBytes(originalStream);
            byte[] newBytes = readStreamAsBytes(newStream);

            return calculateBinarySimilarity(originalBytes, newBytes);

        } catch (Exception e) {
            logger.error("计算文件相似度时发生错误: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    @Override
    public double calculateTextSimilarity(String originalText, String newText) {
        if (originalText == null || newText == null) {
            return 0.0;
        }

        if (originalText.equals(newText)) {
            return 1.0;
        }

        // 使用编辑距离算法计算相似度
        int editDistance = calculateEditDistance(originalText, newText);
        int maxLength = Math.max(originalText.length(), newText.length());
        
        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - (double) editDistance / maxLength;
    }

    @Override
    public String generateBinaryChangeSummary(long originalSize, long newSize, double similarity) {
        StringBuilder summary = new StringBuilder();
        
        long sizeDiff = newSize - originalSize;
        if (sizeDiff > 0) {
            summary.append("文件大小增加 ").append(formatFileSize(sizeDiff));
        } else if (sizeDiff < 0) {
            summary.append("文件大小减少 ").append(formatFileSize(-sizeDiff));
        } else {
            summary.append("文件大小无变化");
        }

        summary.append("，相似度 ").append(String.format("%.1f%%", similarity * 100));

        if (similarity >= 0.8) {
            summary.append("（小幅修改）");
        } else if (similarity >= 0.5) {
            summary.append("（中等修改）");
        } else {
            summary.append("（重大修改）");
        }

        return summary.toString();
    }

    // ==================== 版本历史查询和展示 ====================

    @Override
    public List<VersionHistory> getVersionHistory(FileEntity file, HistoryDisplayConfig config) {
        try {
            List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
            
            if (config.getFromDate() != null || config.getToDate() != null) {
                versions = versions.stream()
                    .filter(version -> {
                        LocalDateTime timestamp = version.getCreatedAt();
                        return (config.getFromDate() == null || timestamp.isAfter(config.getFromDate())) &&
                               (config.getToDate() == null || timestamp.isBefore(config.getToDate()));
                    })
                    .collect(Collectors.toList());
            }

            if (!config.isShowDeleted()) {
                versions = versions.stream()
                    .filter(version -> !version.isDeleted())
                    .collect(Collectors.toList());
            }

            if (config.getMaxHistoryCount() > 0 && versions.size() > config.getMaxHistoryCount()) {
                versions = versions.subList(0, config.getMaxHistoryCount());
            }

            List<VersionHistory> histories = new ArrayList<>();
            for (int i = 0; i < versions.size(); i++) {
                FileVersion version = versions.get(i);
                FileVersion previousVersion = i < versions.size() - 1 ? versions.get(i + 1) : null;

                String changeType = determineChangeType(version, previousVersion);
                String changeSummary = generateChangeSummary(version, previousVersion, config.isShowContentDiff());
                long sizeChange = calculateSizeChange(version, previousVersion);
                double contentSimilarity = config.isCalculateSimilarity() ? 
                    calculateVersionSimilarity(version, previousVersion) : 0.0;

                User creator = null;
                if (version.getCreatedBy() != null) {
                    creator = userRepository.findById(Long.valueOf(version.getCreatedBy())).orElse(null);
                }
                String creatorName = creator != null ? creator.getUsername() : "未知用户";

                histories.add(new VersionHistory(
                    version, changeType, changeSummary, sizeChange, 
                    contentSimilarity, version.getCreatedAt(), creatorName
                ));
            }

            return histories;

        } catch (Exception e) {
            logger.error("获取版本历史时发生错误: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getVersionChangeSummary(FileEntity file, Integer versionNumber) {
        try {
            Optional<FileVersion> version = fileVersionRepository.findByFileAndVersionNumber(file, versionNumber);
            if (version.isEmpty()) {
                return "版本不存在";
            }

            Optional<FileVersion> previousVersion = fileVersionRepository.findByFileAndVersionNumber(file, versionNumber - 1);
            return generateChangeSummary(version.get(), previousVersion.orElse(null), false);

        } catch (Exception e) {
            logger.error("获取版本变更摘要时发生错误: {}", e.getMessage(), e);
            return "获取版本变更摘要时发生错误: " + e.getMessage();
        }
    }

    @Override
    public String getDetailedDifference(FileEntity file, Integer fromVersion, Integer toVersion, boolean includeContent) {
        try {
            DiffResult diffResult = compareVersions(file, fromVersion, toVersion);
            
            StringBuilder detail = new StringBuilder();
            detail.append("版本 ").append(fromVersion).append(" → ").append(toVersion).append(" 差异详情\n");
            detail.append("=====================================\n\n");

            if (!diffResult.isSuccess()) {
                detail.append("错误: ").append(diffResult.getErrorMessage());
                return detail.toString();
            }

            detail.append("统计信息:\n");
            detail.append("- 新增行数: ").append(diffResult.getAddedLines()).append("\n");
            detail.append("- 删除行数: ").append(diffResult.getDeletedLines()).append("\n");
            detail.append("- 修改行数: ").append(diffResult.getModifiedLines()).append("\n");
            detail.append("- 相似度: ").append(String.format("%.1f%%", diffResult.getSimilarity() * 100)).append("\n\n");

            if (includeContent && diffResult.getUnifiedDiff() != null) {
                detail.append("详细差异:\n");
                detail.append(diffResult.getUnifiedDiff());
            }

            return detail.toString();

        } catch (Exception e) {
            logger.error("获取详细差异时发生错误: {}", e.getMessage(), e);
            return "获取详细差异时发生错误: " + e.getMessage();
        }
    }

    @Override
    public String generateHistoryReport(FileEntity file, HistoryDisplayConfig config) {
        try {
            List<VersionHistory> histories = getVersionHistory(file, config);
            
            StringBuilder report = new StringBuilder();
            report.append("文件版本历史报告\n");
            report.append("=====================================\n");
            report.append("文件名: ").append(file.getName()).append("\n");
            report.append("文件路径: ").append(file.getPath()).append("\n");
            report.append("总版本数: ").append(histories.size()).append("\n");
            report.append("生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

            for (VersionHistory history : histories) {
                report.append("版本 ").append(history.getVersion().getVersionNumber()).append("\n");
                report.append("- 时间: ").append(history.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                report.append("- 创建者: ").append(history.getCreator()).append("\n");
                report.append("- 变更类型: ").append(history.getChangeType()).append("\n");
                report.append("- 变更摘要: ").append(history.getChangeSummary()).append("\n");
                if (history.getSizeChange() != 0) {
                    report.append("- 大小变化: ").append(formatSizeChange(history.getSizeChange())).append("\n");
                }
                if (config.isCalculateSimilarity()) {
                    report.append("- 内容相似度: ").append(String.format("%.1f%%", history.getContentSimilarity() * 100)).append("\n");
                }
                report.append("- 备注: ").append(history.getVersion().getChangeComment() != null ? history.getVersion().getChangeComment() : "无").append("\n\n");
            }

            return report.toString();

        } catch (Exception e) {
            logger.error("生成历史报告时发生错误: {}", e.getMessage(), e);
            return "生成历史报告时发生错误: " + e.getMessage();
        }
    }

    @Override
    public List<VersionHistory> getRecentChanges(FileEntity file, int limit) {
        HistoryDisplayConfig config = new HistoryDisplayConfig(
            false, false, false, limit, null, null
        );
        return getVersionHistory(file, config);
    }

    @Override
    public String analyzeChangeTrend(FileEntity file, int days) {
        try {
            LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
            HistoryDisplayConfig config = new HistoryDisplayConfig(
                false, false, true, 0, fromDate, null
            );

            List<VersionHistory> histories = getVersionHistory(file, config);
            
            if (histories.isEmpty()) {
                return "在过去 " + days + " 天内没有版本变更";
            }

            StringBuilder analysis = new StringBuilder();
            analysis.append("过去 ").append(days).append(" 天变更趋势分析\n");
            analysis.append("=====================================\n");
            analysis.append("总变更次数: ").append(histories.size()).append("\n");

            // 按天统计
            Map<String, Integer> dailyChanges = new HashMap<>();
            for (VersionHistory history : histories) {
                String date = history.getTimestamp().toLocalDate().toString();
                dailyChanges.put(date, dailyChanges.getOrDefault(date, 0) + 1);
            }

            analysis.append("日平均变更次数: ").append(String.format("%.1f", (double) histories.size() / days)).append("\n");
            analysis.append("最活跃日期: ").append(findMostActiveDate(dailyChanges)).append("\n");

            // 变更类型统计
            Map<String, Integer> changeTypes = new HashMap<>();
            for (VersionHistory history : histories) {
                String type = history.getChangeType();
                changeTypes.put(type, changeTypes.getOrDefault(type, 0) + 1);
            }

            analysis.append("\n变更类型分布:\n");
            for (Map.Entry<String, Integer> entry : changeTypes.entrySet()) {
                double percentage = (double) entry.getValue() / histories.size() * 100;
                analysis.append("- ").append(entry.getKey()).append(": ").append(entry.getValue())
                        .append(" 次 (").append(String.format("%.1f%%", percentage)).append(")\n");
            }

            return analysis.toString();

        } catch (Exception e) {
            logger.error("分析变更趋势时发生错误: {}", e.getMessage(), e);
            return "分析变更趋势时发生错误: " + e.getMessage();
        }
    }

    // ==================== 高级功能 ====================

    @Override
    public Optional<String> mergeVersions(FileEntity file, Integer baseVersion, Integer branchVersion1, Integer branchVersion2) {
        try {
            logger.debug("合并版本: base={}, branch1={}, branch2={}", baseVersion, branchVersion1, branchVersion2);

            // 加载三个版本的内容
            Optional<FileVersion> base = fileVersionRepository.findByFileAndVersionNumber(file, baseVersion);
            Optional<FileVersion> branch1 = fileVersionRepository.findByFileAndVersionNumber(file, branchVersion1);
            Optional<FileVersion> branch2 = fileVersionRepository.findByFileAndVersionNumber(file, branchVersion2);

            if (base.isEmpty() || branch1.isEmpty() || branch2.isEmpty()) {
                return Optional.empty();
            }

            String baseContent = loadVersionContent(base.get());
            String branch1Content = loadVersionContent(branch1.get());
            String branch2Content = loadVersionContent(branch2.get());

            // 执行三路合并
            String mergedContent = performThreeWayMerge(baseContent, branch1Content, branch2Content);
            return Optional.ofNullable(mergedContent);

        } catch (Exception e) {
            logger.error("合并版本时发生错误: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean detectConflicts(FileEntity file, Integer version1, Integer version2) {
        try {
            DiffResult diffResult = compareVersions(file, version1, version2);
            return diffResult.isSuccess() && diffResult.hasChanges();

        } catch (Exception e) {
            logger.error("检测版本冲突时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateVersionGraph(FileEntity file) {
        try {
            List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberAsc(file);
            
            StringBuilder graph = new StringBuilder();
            graph.append("digraph VersionGraph {\n");
            graph.append("  rankdir=TB;\n");
            graph.append("  node [shape=box];\n\n");

            for (int i = 0; i < versions.size(); i++) {
                FileVersion version = versions.get(i);
                graph.append("  v").append(version.getVersionNumber())
                     .append(" [label=\"v").append(version.getVersionNumber())
                     .append("\\n").append(version.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                     .append("\\n").append(formatFileSize(version.getSizeBytes()))
                     .append("\"];\n");

                if (i > 0) {
                    graph.append("  v").append(versions.get(i-1).getVersionNumber())
                         .append(" -> v").append(version.getVersionNumber()).append(";\n");
                }
            }

            graph.append("}\n");
            return graph.toString();

        } catch (Exception e) {
            logger.error("生成版本关系图时发生错误: {}", e.getMessage(), e);
            return "生成版本关系图时发生错误: " + e.getMessage();
        }
    }

    @Override
    public List<FileVersion> searchVersionsByChange(FileEntity file, String searchText, boolean isRegex) {
        try {
            List<FileVersion> versions = fileVersionRepository.findByFileOrderByVersionNumberDesc(file);
            List<FileVersion> matchingVersions = new ArrayList<>();

            Pattern pattern = isRegex ? Pattern.compile(searchText) : Pattern.compile(Pattern.quote(searchText));

            for (FileVersion version : versions) {
                String content = loadVersionContent(version);
                if (content != null) {
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        matchingVersions.add(version);
                    }
                }
            }

            return matchingVersions;

        } catch (Exception e) {
            logger.error("按变更内容搜索版本时发生错误: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public DiffStats getDiffStatistics(FileEntity file, Integer fromVersion, Integer toVersion) {
        try {
            DiffResult diffResult = compareVersions(file, fromVersion, toVersion);
            
            if (!diffResult.isSuccess()) {
                return new DiffStats(0, 0, 0, 0, 0);
            }

            int totalLines = 0;
            int unchangedLines = 0;
            
            if (diffResult.getDiffLines() != null) {
                totalLines = diffResult.getDiffLines().size();
                unchangedLines = (int) diffResult.getDiffLines().stream()
                    .filter(line -> line.getOperation() == DiffOperation.EQUAL)
                    .count();
            }

            return new DiffStats(
                totalLines,
                diffResult.getAddedLines(),
                diffResult.getDeletedLines(),
                diffResult.getModifiedLines(),
                unchangedLines
            );

        } catch (Exception e) {
            logger.error("获取差异统计时发生错误: {}", e.getMessage(), e);
            return new DiffStats(0, 0, 0, 0, 0);
        }
    }

    // ==================== 私有辅助方法 ====================

    private String loadVersionContent(FileVersion version) {
        try {
            // 模拟从存储路径读取内容，实际实现时需要调用具体的存储服务
            // 这里假设存储服务有相应的方法来获取内容
            return "模拟内容 - 版本 " + version.getVersionNumber();
        } catch (Exception e) {
            logger.error("加载版本内容失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String readStreamAsText(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private byte[] readStreamAsBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    private List<DiffLine> performTextDiff(List<String> originalLines, List<String> newLines) {
        // 简化的差异算法实现 (实际项目中可以使用更复杂的算法如Myers算法)
        List<DiffLine> diffLines = new ArrayList<>();
        
        int originalIndex = 0;
        int newIndex = 0;
        
        while (originalIndex < originalLines.size() || newIndex < newLines.size()) {
            if (originalIndex >= originalLines.size()) {
                // 剩余的都是新增行
                diffLines.add(new DiffLine(newIndex + 1, DiffOperation.INSERT, 
                    newLines.get(newIndex), null));
                newIndex++;
            } else if (newIndex >= newLines.size()) {
                // 剩余的都是删除行
                diffLines.add(new DiffLine(originalIndex + 1, DiffOperation.DELETE, 
                    null, originalLines.get(originalIndex)));
                originalIndex++;
            } else {
                String originalLine = originalLines.get(originalIndex);
                String newLine = newLines.get(newIndex);
                
                if (originalLine.equals(newLine)) {
                    // 相同行
                    diffLines.add(new DiffLine(originalIndex + 1, DiffOperation.EQUAL, 
                        newLine, originalLine));
                    originalIndex++;
                    newIndex++;
                } else {
                    // 查找最优匹配 (简化处理)
                    if (findLineInSubsequent(newLines, newIndex, originalLine) != -1) {
                        // 原行在后续新行中找到，当前新行是插入
                        diffLines.add(new DiffLine(newIndex + 1, DiffOperation.INSERT, 
                            newLine, null));
                        newIndex++;
                    } else if (findLineInSubsequent(originalLines, originalIndex, newLine) != -1) {
                        // 新行在后续原行中找到，当前原行是删除
                        diffLines.add(new DiffLine(originalIndex + 1, DiffOperation.DELETE, 
                            null, originalLine));
                        originalIndex++;
                    } else {
                        // 替换行
                        diffLines.add(new DiffLine(originalIndex + 1, DiffOperation.REPLACE, 
                            newLine, originalLine));
                        originalIndex++;
                        newIndex++;
                    }
                }
            }
        }
        
        return diffLines;
    }

    private int findLineInSubsequent(List<String> lines, int startIndex, String targetLine) {
        for (int i = startIndex; i < Math.min(lines.size(), startIndex + 10); i++) {
            if (lines.get(i).equals(targetLine)) {
                return i;
            }
        }
        return -1;
    }

    private List<DiffHunk> generateDiffHunks(List<DiffLine> diffLines, int contextLines) {
        List<DiffHunk> hunks = new ArrayList<>();
        // 简化实现，实际项目中需要更复杂的逻辑来生成差异块
        DiffHunk hunk = new DiffHunk();
        hunk.setHeader("@@ -1," + diffLines.size() + " +1," + diffLines.size() + " @@");
        
        List<String> hunkLines = new ArrayList<>();
        for (DiffLine diffLine : diffLines) {
            switch (diffLine.getOperation()) {
                case INSERT -> hunkLines.add("+" + diffLine.getContent());
                case DELETE -> hunkLines.add("-" + diffLine.getOriginalContent());
                case EQUAL -> hunkLines.add(" " + diffLine.getContent());
                case REPLACE -> {
                    hunkLines.add("-" + diffLine.getOriginalContent());
                    hunkLines.add("+" + diffLine.getContent());
                }
            }
        }
        
        hunk.setLines(hunkLines);
        hunks.add(hunk);
        return hunks;
    }

    private DiffResult compareBinaryContent(byte[] originalBytes, byte[] newBytes, String fileName) {
        boolean areEqual = Arrays.equals(originalBytes, newBytes);
        
        if (areEqual) {
            return new DiffResult(DiffType.BINARY, new ArrayList<>(), 
                "二进制文件无变化", 0, 0, 0, 1.0);
        }

        double similarity = calculateBinarySimilarity(originalBytes, newBytes);
        String summary = generateBinaryChangeSummary(originalBytes.length, newBytes.length, similarity);

        return new DiffResult(DiffType.BINARY, new ArrayList<>(), 
            summary, 0, 0, 1, similarity);
    }

    private double calculateBinarySimilarity(byte[] originalBytes, byte[] newBytes) {
        if (Arrays.equals(originalBytes, newBytes)) {
            return 1.0;
        }

        // 使用简单的字节匹配算法
        int matches = 0;
        int maxLength = Math.max(originalBytes.length, newBytes.length);
        int minLength = Math.min(originalBytes.length, newBytes.length);

        for (int i = 0; i < minLength; i++) {
            if (originalBytes[i] == newBytes[i]) {
                matches++;
            }
        }

        return (double) matches / maxLength;
    }

    private int calculateEditDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatSizeChange(long sizeChange) {
        if (sizeChange > 0) {
            return "+" + formatFileSize(sizeChange);
        } else {
            return "-" + formatFileSize(-sizeChange);
        }
    }

    private String determineChangeType(FileVersion version, FileVersion previousVersion) {
        if (previousVersion == null) {
            return "首次创建";
        }

        if (version.getSizeBytes().equals(previousVersion.getSizeBytes())) {
            return "内容修改";
        } else if (version.getSizeBytes() > previousVersion.getSizeBytes()) {
            return "内容增加";
        } else {
            return "内容减少";
        }
    }

    private String generateChangeSummary(FileVersion version, FileVersion previousVersion, boolean includeContentDiff) {
        if (previousVersion == null) {
            return "首次创建，大小: " + formatFileSize(version.getSizeBytes());
        }

        StringBuilder summary = new StringBuilder();
        long sizeDiff = version.getSizeBytes() - previousVersion.getSizeBytes();
        
        if (sizeDiff != 0) {
            summary.append("大小变化: ").append(formatSizeChange(sizeDiff)).append("；");
        }

        if (version.getChangeComment() != null && !version.getChangeComment().trim().isEmpty()) {
            summary.append("备注: ").append(version.getChangeComment());
        } else {
            summary.append("无备注说明");
        }

        return summary.toString();
    }

    private long calculateSizeChange(FileVersion version, FileVersion previousVersion) {
        if (previousVersion == null) {
            return version.getSizeBytes();
        }
        return version.getSizeBytes() - previousVersion.getSizeBytes();
    }

    private double calculateVersionSimilarity(FileVersion version, FileVersion previousVersion) {
        if (previousVersion == null) {
            return 0.0;
        }

        try {
            String currentContent = loadVersionContent(version);
            String previousContent = loadVersionContent(previousVersion);
            
            if (currentContent != null && previousContent != null) {
                return calculateTextSimilarity(previousContent, currentContent);
            }
        } catch (Exception e) {
            logger.error("计算版本相似度时发生错误: {}", e.getMessage(), e);
        }

        return 0.0;
    }

    private String findMostActiveDate(Map<String, Integer> dailyChanges) {
        return dailyChanges.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("无");
    }

    private String performThreeWayMerge(String baseContent, String branch1Content, String branch2Content) {
        // 简化的三路合并实现
        if (branch1Content.equals(branch2Content)) {
            return branch1Content;
        }

        if (baseContent.equals(branch1Content)) {
            return branch2Content;
        }

        if (baseContent.equals(branch2Content)) {
            return branch1Content;
        }

        // 存在冲突，返回冲突标记的内容
        StringBuilder merged = new StringBuilder();
        merged.append("<<<<<<< branch1\n");
        merged.append(branch1Content);
        merged.append("\n=======\n");
        merged.append(branch2Content);
        merged.append("\n>>>>>>> branch2\n");

        return merged.toString();
    }

    /**
     * 差异块类
     */
    private static class DiffHunk {
        private String header;
        private List<String> lines;

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public List<String> getLines() { return lines; }
        public void setLines(List<String> lines) { this.lines = lines; }
    }
} 