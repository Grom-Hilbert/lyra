package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FileVersion;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FileVersionRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;
import tslc.beihaiyun.lyra.service.impl.DiffServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DiffService单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiffService单元测试")
class DiffServiceTest {

    @Mock
    private FileVersionRepository fileVersionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private VersionService versionService;

    @InjectMocks
    private DiffServiceImpl diffService;

    private FileEntity testFile;
    private FileVersion testVersion1;
    private FileVersion testVersion2;
    private User testUser;
    private Space testSpace;

    @BeforeEach
    void setUp() {
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("测试空间");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setPath("/test/test.txt");
        testFile.setMimeType("text/plain");
        testFile.setSizeBytes(100L);
        testFile.setSpace(testSpace);

        testVersion1 = new FileVersion();
        testVersion1.setId(1L);
        testVersion1.setFile(testFile);
        testVersion1.setVersionNumber(1);
        testVersion1.setSizeBytes(50L);
        testVersion1.setStoragePath("storage/v1");
        testVersion1.setChangeComment("初始版本");
        testVersion1.setCreatedBy("1");
        testVersion1.setCreatedAt(LocalDateTime.now().minusDays(1));

        testVersion2 = new FileVersion();
        testVersion2.setId(2L);
        testVersion2.setFile(testFile);
        testVersion2.setVersionNumber(2);
        testVersion2.setSizeBytes(75L);
        testVersion2.setStoragePath("storage/v2");
        testVersion2.setChangeComment("修改版本");
        testVersion2.setCreatedBy("1");
        testVersion2.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 文本文件差异对比测试 ====================

    @Test
    @DisplayName("成功比较两个版本的差异")
    void should_CompareVersionsSuccessfully_When_BothVersionsExist() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.of(testVersion1));
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 2)).thenReturn(Optional.of(testVersion2));

        // When
        DiffService.DiffResult result = diffService.compareVersions(testFile, 1, 2);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiffType()).isEqualTo(DiffService.DiffType.TEXT);
        verify(fileVersionRepository, times(2)).findByFileAndVersionNumber(any(), anyInt());
    }

    @Test
    @DisplayName("比较版本失败 - 源版本不存在")
    void should_FailToCompareVersions_When_FromVersionNotExist() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.empty());

        // When
        DiffService.DiffResult result = diffService.compareVersions(testFile, 1, 2);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("源版本 1 不存在");
    }

    @Test
    @DisplayName("比较版本失败 - 目标版本不存在")
    void should_FailToCompareVersions_When_ToVersionNotExist() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.of(testVersion1));
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 2)).thenReturn(Optional.empty());

        // When
        DiffService.DiffResult result = diffService.compareVersions(testFile, 1, 2);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("目标版本 2 不存在");
    }

    @Test
    @DisplayName("成功比较文本内容差异")
    void should_CompareTextSuccessfully_When_ValidInput() {
        // Given
        String originalText = "line1\nline2\nline3";
        String newText = "line1\nmodified line2\nline3\nline4";

        // When
        DiffService.DiffResult result = diffService.compareText(originalText, newText, "test.txt");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiffType()).isEqualTo(DiffService.DiffType.TEXT);
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.getAddedLines()).isGreaterThan(0);
        assertThat(result.getDiffLines()).isNotNull();
        assertThat(result.getUnifiedDiff()).isNotNull();
    }

    @Test
    @DisplayName("比较文本失败 - 空内容")
    void should_FailToCompareText_When_NullContent() {
        // When
        DiffService.DiffResult result = diffService.compareText(null, "content", "test.txt");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("文本内容不能为空");
    }

    @Test
    @DisplayName("成功生成统一差异格式")
    void should_GenerateUnifiedDiffSuccessfully_When_ValidInput() {
        // Given
        String originalText = "line1\nline2";
        String newText = "line1\nmodified line2";

        // When
        String unifiedDiff = diffService.generateUnifiedDiff(originalText, newText, "test.txt", 3);

        // Then
        assertThat(unifiedDiff).isNotNull();
        assertThat(unifiedDiff).contains("--- a/test.txt");
        assertThat(unifiedDiff).contains("+++ b/test.txt");
        assertThat(unifiedDiff).contains("@@");
    }

    // ==================== 二进制文件版本管理测试 ====================

    @Test
    @DisplayName("正确检测文本文件类型")
    void should_DetectTextFile_When_TextExtension() {
        // When & Then
        assertThat(diffService.detectDiffType("test.txt", "text/plain")).isEqualTo(DiffService.DiffType.TEXT);
        assertThat(diffService.detectDiffType("test.java", "text/plain")).isEqualTo(DiffService.DiffType.TEXT);
        assertThat(diffService.detectDiffType("test.json", "application/json")).isEqualTo(DiffService.DiffType.TEXT);
    }

    @Test
    @DisplayName("正确检测二进制文件类型")
    void should_DetectBinaryFile_When_BinaryExtension() {
        // When & Then
        assertThat(diffService.detectDiffType("test.jpg", "image/jpeg")).isEqualTo(DiffService.DiffType.BINARY);
        assertThat(diffService.detectDiffType("test.pdf", "application/pdf")).isEqualTo(DiffService.DiffType.BINARY);
        assertThat(diffService.detectDiffType("test.exe", "application/octet-stream")).isEqualTo(DiffService.DiffType.BINARY);
    }

    @Test
    @DisplayName("检测不支持的文件类型")
    void should_DetectUnsupportedFile_When_UnknownExtension() {
        // When & Then
        assertThat(diffService.detectDiffType("test.unknown", null)).isEqualTo(DiffService.DiffType.UNSUPPORTED);
        assertThat(diffService.detectDiffType(null, null)).isEqualTo(DiffService.DiffType.UNSUPPORTED);
    }

    @Test
    @DisplayName("成功比较二进制文件")
    void should_CompareBinaryFilesSuccessfully_When_ValidInput() {
        // Given
        byte[] originalBytes = "binary content 1".getBytes();
        byte[] newBytes = "binary content 2".getBytes();
        
        InputStream originalStream = new ByteArrayInputStream(originalBytes);
        InputStream newStream = new ByteArrayInputStream(newBytes);

        // When
        DiffService.DiffResult result = diffService.compareBinaryFiles(originalStream, newStream, "test.jpg");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDiffType()).isEqualTo(DiffService.DiffType.BINARY);
        assertThat(result.getSimilarity()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("计算文本相似度")
    void should_CalculateTextSimilarity_When_ValidInput() {
        // Given
        String text1 = "Hello World";
        String text2 = "Hello Java";

        // When
        double similarity = diffService.calculateTextSimilarity(text1, text2);

        // Then
        assertThat(similarity).isBetween(0.0, 1.0);
        assertThat(similarity).isLessThan(1.0); // 不完全相同
    }

    @Test
    @DisplayName("计算相同文本的相似度为1.0")
    void should_CalculatePerfectSimilarity_When_IdenticalTexts() {
        // Given
        String text = "Hello World";

        // When
        double similarity = diffService.calculateTextSimilarity(text, text);

        // Then
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("生成二进制文件变更摘要")
    void should_GenerateBinaryChangeSummary_When_ValidInput() {
        // Given
        long originalSize = 1024;
        long newSize = 2048;
        double similarity = 0.8;

        // When
        String summary = diffService.generateBinaryChangeSummary(originalSize, newSize, similarity);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary).contains("文件大小增加");
        assertThat(summary).contains("80.0%");
        assertThat(summary).contains("小幅修改");
    }

    // ==================== 版本历史查询和展示测试 ====================

    @Test
    @DisplayName("成功获取版本历史")
    void should_GetVersionHistorySuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion2, testVersion1);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        DiffService.HistoryDisplayConfig config = DiffService.HistoryDisplayConfig.getDefault();

        // When
        List<DiffService.VersionHistory> histories = diffService.getVersionHistory(testFile, config);

        // Then
        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getVersion().getVersionNumber()).isEqualTo(2);
        assertThat(histories.get(1).getVersion().getVersionNumber()).isEqualTo(1);
        assertThat(histories.get(0).getCreator()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("获取版本变更摘要")
    void should_GetVersionChangeSummary_When_ValidInput() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 2)).thenReturn(Optional.of(testVersion2));
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.of(testVersion1));

        // When
        String summary = diffService.getVersionChangeSummary(testFile, 2);

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary).contains("大小变化");
    }

    @Test
    @DisplayName("获取版本变更摘要失败 - 版本不存在")
    void should_ReturnErrorMessage_When_VersionNotExists() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 999)).thenReturn(Optional.empty());

        // When
        String summary = diffService.getVersionChangeSummary(testFile, 999);

        // Then
        assertThat(summary).isEqualTo("版本不存在");
    }

    @Test
    @DisplayName("生成版本历史报告")
    void should_GenerateHistoryReportSuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion2, testVersion1);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        DiffService.HistoryDisplayConfig config = DiffService.HistoryDisplayConfig.getDefault();

        // When
        String report = diffService.generateHistoryReport(testFile, config);

        // Then
        assertThat(report).isNotNull();
        assertThat(report).contains("文件版本历史报告");
        assertThat(report).contains("test.txt");
        assertThat(report).contains("总版本数: 2");
        assertThat(report).contains("版本 1");
        assertThat(report).contains("版本 2");
    }

    @Test
    @DisplayName("获取最近的版本变更")
    void should_GetRecentChangesSuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion2);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        List<DiffService.VersionHistory> recentChanges = diffService.getRecentChanges(testFile, 1);

        // Then
        assertThat(recentChanges).hasSize(1);
        assertThat(recentChanges.get(0).getVersion().getVersionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("分析版本变更趋势")
    void should_AnalyzeChangeTrendSuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion2, testVersion1);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        String trend = diffService.analyzeChangeTrend(testFile, 7);

        // Then
        assertThat(trend).isNotNull();
        assertThat(trend).contains("变更趋势分析");
        assertThat(trend).contains("总变更次数");
    }

    // ==================== 高级功能测试 ====================

    @Test
    @DisplayName("检测版本冲突")
    void should_DetectConflictsSuccessfully_When_ValidInput() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.of(testVersion1));
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 2)).thenReturn(Optional.of(testVersion2));

        // When
        boolean hasConflicts = diffService.detectConflicts(testFile, 1, 2);

        // Then
        assertThat(hasConflicts).isTrue();
    }

    @Test
    @DisplayName("生成版本关系图")
    void should_GenerateVersionGraphSuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion1, testVersion2);
        when(fileVersionRepository.findByFileOrderByVersionNumberAsc(testFile)).thenReturn(versions);

        // When
        String graph = diffService.generateVersionGraph(testFile);

        // Then
        assertThat(graph).isNotNull();
        assertThat(graph).contains("digraph VersionGraph");
        assertThat(graph).contains("v1");
        assertThat(graph).contains("v2");
        assertThat(graph).contains("->");
    }

    @Test
    @DisplayName("搜索包含特定内容变更的版本")
    void should_SearchVersionsByChangeSuccessfully_When_ValidInput() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion1, testVersion2);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);

        // When
        List<FileVersion> matchingVersions = diffService.searchVersionsByChange(testFile, "版本", false);

        // Then
        assertThat(matchingVersions).isNotNull();
    }

    @Test
    @DisplayName("获取版本差异统计")
    void should_GetDiffStatisticsSuccessfully_When_ValidInput() {
        // Given
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 1)).thenReturn(Optional.of(testVersion1));
        when(fileVersionRepository.findByFileAndVersionNumber(testFile, 2)).thenReturn(Optional.of(testVersion2));

        // When
        DiffService.DiffStats stats = diffService.getDiffStatistics(testFile, 1, 2);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalLines()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getChangeRatio()).isBetween(0.0, 1.0);
    }

    // ==================== 边界条件和异常处理测试 ====================

    @Test
    @DisplayName("处理空的版本历史")
    void should_HandleEmptyVersionHistory_When_NoVersions() {
        // Given
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(Arrays.asList());

        DiffService.HistoryDisplayConfig config = DiffService.HistoryDisplayConfig.getDefault();

        // When
        List<DiffService.VersionHistory> histories = diffService.getVersionHistory(testFile, config);

        // Then
        assertThat(histories).isEmpty();
    }

    @Test
    @DisplayName("处理用户不存在的情况")
    void should_HandleMissingUser_When_UserNotFound() {
        // Given
        List<FileVersion> versions = Arrays.asList(testVersion1);
        when(fileVersionRepository.findByFileOrderByVersionNumberDesc(testFile)).thenReturn(versions);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        DiffService.HistoryDisplayConfig config = DiffService.HistoryDisplayConfig.getDefault();

        // When
        List<DiffService.VersionHistory> histories = diffService.getVersionHistory(testFile, config);

        // Then
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getCreator()).isEqualTo("未知用户");
    }
} 