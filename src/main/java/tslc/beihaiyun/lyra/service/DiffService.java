package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 版本差异对比服务接口
 * 提供文本文件差异对比、二进制文件版本管理和版本历史展示功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public interface DiffService {

    /**
     * 差异类型枚举
     */
    enum DiffType {
        /** 文本差异 */
        TEXT,
        /** 二进制差异 */
        BINARY,
        /** 不支持对比 */
        UNSUPPORTED
    }

    /**
     * 差异操作类型
     */
    enum DiffOperation {
        /** 添加 */
        INSERT,
        /** 删除 */
        DELETE,
        /** 相等（无变化） */
        EQUAL,
        /** 替换 */
        REPLACE
    }

    /**
     * 差异行结果
     */
    class DiffLine {
        private final int lineNumber;
        private final DiffOperation operation;
        private final String content;
        private final String originalContent;

        public DiffLine(int lineNumber, DiffOperation operation, String content, String originalContent) {
            this.lineNumber = lineNumber;
            this.operation = operation;
            this.content = content;
            this.originalContent = originalContent;
        }

        public int getLineNumber() { return lineNumber; }
        public DiffOperation getOperation() { return operation; }
        public String getContent() { return content; }
        public String getOriginalContent() { return originalContent; }
    }

    /**
     * 差异对比结果
     */
    class DiffResult {
        private final DiffType diffType;
        private final List<DiffLine> diffLines;
        private final String unifiedDiff;
        private final int addedLines;
        private final int deletedLines;
        private final int modifiedLines;
        private final double similarity;
        private final String errorMessage;

        public DiffResult(DiffType diffType, List<DiffLine> diffLines, String unifiedDiff,
                         int addedLines, int deletedLines, int modifiedLines, double similarity) {
            this.diffType = diffType;
            this.diffLines = diffLines;
            this.unifiedDiff = unifiedDiff;
            this.addedLines = addedLines;
            this.deletedLines = deletedLines;
            this.modifiedLines = modifiedLines;
            this.similarity = similarity;
            this.errorMessage = null;
        }

        public DiffResult(DiffType diffType, String errorMessage) {
            this.diffType = diffType;
            this.diffLines = null;
            this.unifiedDiff = null;
            this.addedLines = 0;
            this.deletedLines = 0;
            this.modifiedLines = 0;
            this.similarity = 0.0;
            this.errorMessage = errorMessage;
        }

        public DiffType getDiffType() { return diffType; }
        public List<DiffLine> getDiffLines() { return diffLines; }
        public String getUnifiedDiff() { return unifiedDiff; }
        public int getAddedLines() { return addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public int getModifiedLines() { return modifiedLines; }
        public double getSimilarity() { return similarity; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return errorMessage == null; }
        public boolean hasChanges() { return addedLines > 0 || deletedLines > 0 || modifiedLines > 0; }
    }

    /**
     * 版本历史记录
     */
    class VersionHistory {
        private final FileVersion version;
        private final String changeType;
        private final String changeSummary;
        private final long sizeChange;
        private final double contentSimilarity;
        private final LocalDateTime timestamp;
        private final String creator;

        public VersionHistory(FileVersion version, String changeType, String changeSummary,
                            long sizeChange, double contentSimilarity, LocalDateTime timestamp, String creator) {
            this.version = version;
            this.changeType = changeType;
            this.changeSummary = changeSummary;
            this.sizeChange = sizeChange;
            this.contentSimilarity = contentSimilarity;
            this.timestamp = timestamp;
            this.creator = creator;
        }

        public FileVersion getVersion() { return version; }
        public String getChangeType() { return changeType; }
        public String getChangeSummary() { return changeSummary; }
        public long getSizeChange() { return sizeChange; }
        public double getContentSimilarity() { return contentSimilarity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getCreator() { return creator; }
    }

    /**
     * 版本历史展示配置
     */
    class HistoryDisplayConfig {
        private final boolean showDeleted;
        private final boolean showContentDiff;
        private final boolean calculateSimilarity;
        private final int maxHistoryCount;
        private final LocalDateTime fromDate;
        private final LocalDateTime toDate;

        public HistoryDisplayConfig(boolean showDeleted, boolean showContentDiff, boolean calculateSimilarity,
                                  int maxHistoryCount, LocalDateTime fromDate, LocalDateTime toDate) {
            this.showDeleted = showDeleted;
            this.showContentDiff = showContentDiff;
            this.calculateSimilarity = calculateSimilarity;
            this.maxHistoryCount = maxHistoryCount;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public boolean isShowDeleted() { return showDeleted; }
        public boolean isShowContentDiff() { return showContentDiff; }
        public boolean isCalculateSimilarity() { return calculateSimilarity; }
        public int getMaxHistoryCount() { return maxHistoryCount; }
        public LocalDateTime getFromDate() { return fromDate; }
        public LocalDateTime getToDate() { return toDate; }

        public static HistoryDisplayConfig getDefault() {
            return new HistoryDisplayConfig(false, true, true, 50, null, null);
        }
    }

    // ==================== 文本文件差异对比 ====================

    /**
     * 对比两个版本的差异
     * 
     * @param file 文件实体
     * @param fromVersionNumber 源版本号
     * @param toVersionNumber 目标版本号
     * @return 差异对比结果
     */
    DiffResult compareVersions(FileEntity file, Integer fromVersionNumber, Integer toVersionNumber);

    /**
     * 对比版本与当前文件的差异
     * 
     * @param file 文件实体
     * @param versionNumber 要对比的版本号
     * @return 差异对比结果
     */
    DiffResult compareWithCurrentVersion(FileEntity file, Integer versionNumber);

    /**
     * 对比文本内容差异
     * 
     * @param originalText 原始文本
     * @param newText 新文本
     * @param fileName 文件名（用于上下文）
     * @return 差异对比结果
     */
    DiffResult compareText(String originalText, String newText, String fileName);

    /**
     * 对比文件流内容差异
     * 
     * @param originalStream 原始文件流
     * @param newStream 新文件流
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @return 差异对比结果
     */
    DiffResult compareStreams(InputStream originalStream, InputStream newStream, String fileName, String mimeType);

    /**
     * 生成统一差异格式
     * 
     * @param originalText 原始文本
     * @param newText 新文本
     * @param fileName 文件名
     * @param contextLines 上下文行数
     * @return 统一差异格式字符串
     */
    String generateUnifiedDiff(String originalText, String newText, String fileName, int contextLines);

    // ==================== 二进制文件版本管理 ====================

    /**
     * 检测文件类型
     * 
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @return 差异类型
     */
    DiffType detectDiffType(String fileName, String mimeType);

    /**
     * 对比二进制文件
     * 
     * @param originalStream 原始文件流
     * @param newStream 新文件流
     * @param fileName 文件名
     * @return 差异对比结果
     */
    DiffResult compareBinaryFiles(InputStream originalStream, InputStream newStream, String fileName);

    /**
     * 计算文件相似度
     * 
     * @param originalStream 原始文件流
     * @param newStream 新文件流
     * @return 相似度（0.0-1.0）
     */
    double calculateSimilarity(InputStream originalStream, InputStream newStream);

    /**
     * 计算文本相似度
     * 
     * @param originalText 原始文本
     * @param newText 新文本
     * @return 相似度（0.0-1.0）
     */
    double calculateTextSimilarity(String originalText, String newText);

    /**
     * 生成二进制文件变更摘要
     * 
     * @param originalSize 原始文件大小
     * @param newSize 新文件大小
     * @param similarity 相似度
     * @return 变更摘要
     */
    String generateBinaryChangeSummary(long originalSize, long newSize, double similarity);

    // ==================== 版本历史查询和展示 ====================

    /**
     * 获取文件版本历史
     * 
     * @param file 文件实体
     * @param config 展示配置
     * @return 版本历史列表
     */
    List<VersionHistory> getVersionHistory(FileEntity file, HistoryDisplayConfig config);

    /**
     * 获取版本变更摘要
     * 
     * @param file 文件实体
     * @param versionNumber 版本号
     * @return 变更摘要
     */
    String getVersionChangeSummary(FileEntity file, Integer versionNumber);

    /**
     * 获取版本间的详细差异
     * 
     * @param file 文件实体
     * @param fromVersion 起始版本号
     * @param toVersion 结束版本号
     * @param includeContent 是否包含内容差异
     * @return 详细差异信息
     */
    String getDetailedDifference(FileEntity file, Integer fromVersion, Integer toVersion, boolean includeContent);

    /**
     * 生成版本历史报告
     * 
     * @param file 文件实体
     * @param config 展示配置
     * @return 版本历史报告
     */
    String generateHistoryReport(FileEntity file, HistoryDisplayConfig config);

    /**
     * 获取最近的版本变更
     * 
     * @param file 文件实体
     * @param limit 限制数量
     * @return 最近的版本历史
     */
    List<VersionHistory> getRecentChanges(FileEntity file, int limit);

    /**
     * 分析版本变更趋势
     * 
     * @param file 文件实体
     * @param days 分析天数
     * @return 变更趋势分析
     */
    String analyzeChangeTrend(FileEntity file, int days);

    // ==================== 高级功能 ====================

    /**
     * 智能合并版本差异
     * 
     * @param file 文件实体
     * @param baseVersion 基础版本号
     * @param branchVersion1 分支版本1
     * @param branchVersion2 分支版本2
     * @return 合并结果
     */
    Optional<String> mergeVersions(FileEntity file, Integer baseVersion, Integer branchVersion1, Integer branchVersion2);

    /**
     * 检测版本冲突
     * 
     * @param file 文件实体
     * @param version1 版本1
     * @param version2 版本2
     * @return 是否存在冲突
     */
    boolean detectConflicts(FileEntity file, Integer version1, Integer version2);

    /**
     * 生成版本关系图
     * 
     * @param file 文件实体
     * @return 版本关系图数据
     */
    String generateVersionGraph(FileEntity file);

    /**
     * 搜索包含特定内容变更的版本
     * 
     * @param file 文件实体
     * @param searchText 搜索文本
     * @param isRegex 是否为正则表达式
     * @return 匹配的版本列表
     */
    List<FileVersion> searchVersionsByChange(FileEntity file, String searchText, boolean isRegex);

    /**
     * 获取版本差异统计
     * 
     * @param file 文件实体
     * @param fromVersion 起始版本
     * @param toVersion 结束版本
     * @return 差异统计信息
     */
    DiffStats getDiffStatistics(FileEntity file, Integer fromVersion, Integer toVersion);

    /**
     * 差异统计信息
     */
    class DiffStats {
        private final int totalLines;
        private final int addedLines;
        private final int deletedLines;
        private final int modifiedLines;
        private final int unchangedLines;
        private final double changeRatio;

        public DiffStats(int totalLines, int addedLines, int deletedLines, int modifiedLines, int unchangedLines) {
            this.totalLines = totalLines;
            this.addedLines = addedLines;
            this.deletedLines = deletedLines;
            this.modifiedLines = modifiedLines;
            this.unchangedLines = unchangedLines;
            this.changeRatio = totalLines > 0 ? (double) (addedLines + deletedLines + modifiedLines) / totalLines : 0.0;
        }

        public int getTotalLines() { return totalLines; }
        public int getAddedLines() { return addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public int getModifiedLines() { return modifiedLines; }
        public int getUnchangedLines() { return unchangedLines; }
        public double getChangeRatio() { return changeRatio; }
    }
} 