package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.service.EditorService;
import tslc.beihaiyun.lyra.service.FileService;
import tslc.beihaiyun.lyra.service.PermissionService;
import tslc.beihaiyun.lyra.service.VersionService;
import tslc.beihaiyun.lyra.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 在线编辑服务实现
 * 提供文本文件在线编辑功能，包括实时保存、编辑历史、语法高亮等
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-27
 */
@Service
@Transactional
public class EditorServiceImpl implements EditorService {

    private static final Logger logger = LoggerFactory.getLogger(EditorServiceImpl.class);

    // 配置常量
    private static final int MAX_HISTORY_COUNT = 10;
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;
    private static final int AUTO_SAVE_INTERVAL_SECONDS = 30;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // 支持的编程语言
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        "java", "javascript", "typescript", "python", "go", "rust", "c", "cpp", "csharp",
        "php", "ruby", "kotlin", "scala", "swift", "dart", "sql", "html", "css", "scss",
        "json", "xml", "yaml", "toml", "markdown", "shell", "bash", "powershell", "dockerfile",
        "text"
    );

    // 支持编辑的文件扩展名
    private static final Set<String> SUPPORTED_EDIT_EXTENSIONS = Set.of(
        "txt", "md", "json", "xml", "yaml", "yml", "toml", "csv", "log", "properties",
        "java", "js", "ts", "jsx", "tsx", "py", "go", "rs", "c", "cpp", "h", "hpp",
        "cs", "php", "rb", "kt", "scala", "swift", "dart", "sql", "html", "htm", "css",
        "scss", "sass", "less", "sh", "bash", "ps1", "dockerfile", "gitignore", "editorconfig"
    );

    // 文件扩展名到语言的映射
    private static final Map<String, String> EXTENSION_TO_LANGUAGE_MAP;
    
    static {
        Map<String, String> map = new HashMap<>();
        map.put("java", "java");
        map.put("js", "javascript");
        map.put("ts", "typescript");
        map.put("jsx", "javascript");
        map.put("tsx", "typescript");
        map.put("py", "python");
        map.put("go", "go");
        map.put("rs", "rust");
        map.put("c", "c");
        map.put("cpp", "cpp");
        map.put("h", "c");
        map.put("hpp", "cpp");
        map.put("cs", "csharp");
        map.put("php", "php");
        map.put("rb", "ruby");
        map.put("kt", "kotlin");
        map.put("scala", "scala");
        map.put("swift", "swift");
        map.put("dart", "dart");
        map.put("sql", "sql");
        map.put("html", "html");
        map.put("htm", "html");
        map.put("css", "css");
        map.put("scss", "scss");
        map.put("sass", "scss");
        map.put("less", "css");
        map.put("md", "markdown");
        map.put("sh", "shell");
        map.put("bash", "bash");
        map.put("ps1", "powershell");
        map.put("json", "json");
        map.put("xml", "xml");
        map.put("yaml", "yaml");
        map.put("yml", "yaml");
        map.put("toml", "toml");
        map.put("dockerfile", "dockerfile");
        EXTENSION_TO_LANGUAGE_MAP = Collections.unmodifiableMap(map);
    }

    private final FileService fileService;
    private final PermissionService permissionService;
    private final VersionService versionService;

    // 内存存储（生产环境应使用Redis或数据库）
    private final Map<String, EditSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<EditHistory>> sessionHistories = new ConcurrentHashMap<>();
    private final Map<Long, FileLockInfo> fileLocks = new ConcurrentHashMap<>();
    private final AtomicInteger sessionCounter = new AtomicInteger(0);

    @Autowired
    public EditorServiceImpl(FileService fileService, PermissionService permissionService, VersionService versionService) {
        this.fileService = fileService;
        this.permissionService = permissionService;
        this.versionService = versionService;
    }

    // ==================== 编辑会话管理 ====================

    @Override
    public EditResult startEditSession(Long fileId, Long userId) {
        try {
            // 检查文件是否存在
            Optional<FileEntity> fileOptional = fileService.getFileById(fileId);
            if (!fileOptional.isPresent()) {
                return createErrorResult("文件不存在");
            }

            FileEntity file = fileOptional.get();

            // 检查权限
            if (!checkEditPermission(file, userId)) {
                return createErrorResult("没有编辑权限");
            }

            // 检查文件是否支持编辑
            if (!isEditSupported(file.getName(), file.getMimeType())) {
                return createErrorResult("文件类型不支持编辑");
            }

            // 检查文件大小
            if (file.getSizeBytes() > MAX_FILE_SIZE) {
                return createErrorResult("文件过大，不支持在线编辑");
            }

            // 检查文件是否被锁定
            if (isFileLocked(fileId, userId)) {
                Optional<FileLockInfo> lockInfo = getFileLockInfo(fileId);
                if (lockInfo.isPresent()) {
                    return createErrorResult("文件正在被用户 " + lockInfo.get().getUserName() + " 编辑");
                }
            }

            // 读取文件内容
            String content = readFileContent(file);
            
            // 生成会话ID
            String sessionId = generateSessionId();
            
            // 创建编辑会话
            EditSession session = new EditSession(sessionId, fileId, userId, content);
            activeSessions.put(sessionId, session);

            // 锁定文件
            lockFile(fileId, userId, sessionId);

            // 创建初始历史记录
            createHistoryRecord(sessionId, content, "开始编辑", 1);

            logger.info("开始编辑会话: sessionId={}, fileId={}, userId={}", sessionId, fileId, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("content", content);
            data.put("language", detectLanguage(file.getName(), content).orElse("text"));
            data.put("filename", file.getName());

            return createSuccessResult("编辑会话已开始", data);

        } catch (Exception e) {
            logger.error("开始编辑会话失败: fileId={}, userId={}", fileId, userId, e);
            return createErrorResult("开始编辑会话失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<EditSession> getEditSession(String sessionId) {
        EditSession session = activeSessions.get(sessionId);
        if (session != null) {
            // 更新最后活动时间
            session.setLastActivityTime(LocalDateTime.now());
        }
        return Optional.ofNullable(session);
    }

    @Override
    public EditResult updateContent(String sessionId, String content, boolean autoSave) {
        try {
            Optional<EditSession> sessionOptional = getEditSession(sessionId);
            if (!sessionOptional.isPresent()) {
                return createErrorResult("编辑会话不存在或已过期");
            }

            EditSession session = sessionOptional.get();
            
            // 更新内容
            String oldContent = session.getCurrentContent();
            session.setCurrentContent(content);
            session.setHasUnsavedChanges(!content.equals(oldContent));
            session.setVersion(session.getVersion() + 1);

            // 如果内容有变化，创建历史记录
            if (!content.equals(oldContent)) {
                createHistoryRecord(sessionId, content, "内容更新", session.getVersion());
            }

            // 自动保存
            if (autoSave && session.hasUnsavedChanges()) {
                saveContentInternal(session, "自动保存");
            }

            logger.debug("更新编辑内容: sessionId={}, autoSave={}, hasChanges={}", 
                        sessionId, autoSave, session.hasUnsavedChanges());

            Map<String, Object> data = new HashMap<>();
            data.put("version", session.getVersion());
            data.put("hasUnsavedChanges", session.hasUnsavedChanges());

            return createSuccessResult("内容已更新", data);

        } catch (Exception e) {
            logger.error("更新编辑内容失败: sessionId={}", sessionId, e);
            return createErrorResult("更新内容失败: " + e.getMessage());
        }
    }

    @Override
    public EditResult saveContent(String sessionId, String saveComment) {
        try {
            Optional<EditSession> sessionOptional = getEditSession(sessionId);
            if (!sessionOptional.isPresent()) {
                return createErrorResult("编辑会话不存在或已过期");
            }

            EditSession session = sessionOptional.get();
            
            if (!session.hasUnsavedChanges()) {
                return createSuccessResult("没有需要保存的更改", null);
            }

            // 保存到文件
            saveContentInternal(session, saveComment);

            logger.info("保存编辑内容: sessionId={}, fileId={}", sessionId, session.getFileId());

            Map<String, Object> data = new HashMap<>();
            data.put("version", session.getVersion());
            data.put("saved", true);

            return createSuccessResult("内容已保存", data);

        } catch (Exception e) {
            logger.error("保存编辑内容失败: sessionId={}", sessionId, e);
            return createErrorResult("保存内容失败: " + e.getMessage());
        }
    }

    @Override
    public EditResult closeEditSession(String sessionId, boolean forceClose) {
        try {
            Optional<EditSession> sessionOptional = getEditSession(sessionId);
            if (!sessionOptional.isPresent()) {
                return createErrorResult("编辑会话不存在");
            }

            EditSession session = sessionOptional.get();

            // 检查是否有未保存的更改
            if (session.hasUnsavedChanges() && !forceClose) {
                return createErrorResult("有未保存的更改，请先保存或使用强制关闭");
            }

            // 解锁文件
            unlockFile(session.getFileId(), session.getUserId(), sessionId);

            // 更新会话状态
            session.setStatus(SessionStatus.CLOSED);

            // 移除活跃会话
            activeSessions.remove(sessionId);

            logger.info("关闭编辑会话: sessionId={}, fileId={}, forceClose={}", 
                       sessionId, session.getFileId(), forceClose);

            return createSuccessResult("编辑会话已关闭", null);

        } catch (Exception e) {
            logger.error("关闭编辑会话失败: sessionId={}", sessionId, e);
            return createErrorResult("关闭会话失败: " + e.getMessage());
        }
    }

    @Override
    public List<EditSession> getUserActiveSessions(Long userId) {
        return activeSessions.values().stream()
                .filter(session -> session.getUserId().equals(userId))
                .filter(session -> session.getStatus() == SessionStatus.ACTIVE || session.getStatus() == SessionStatus.IDLE)
                .collect(Collectors.toList());
    }

    @Override
    public int cleanupExpiredSessions(int timeoutMinutes) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int cleanedCount = 0;

        Iterator<Map.Entry<String, EditSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, EditSession> entry = iterator.next();
            EditSession session = entry.getValue();

            if (session.getLastActivityTime().isBefore(expireTime)) {
                // 解锁文件
                unlockFile(session.getFileId(), session.getUserId(), session.getSessionId());

                // 标记为过期
                session.setStatus(SessionStatus.EXPIRED);

                // 移除会话
                iterator.remove();
                cleanedCount++;

                logger.info("清理过期编辑会话: sessionId={}, fileId={}",
                           session.getSessionId(), session.getFileId());
            }
        }

        if (cleanedCount > 0) {
            logger.info("清理了 {} 个过期编辑会话", cleanedCount);
        }

        return cleanedCount;
    }

    /**
     * 定时清理过期会话（无参数方法，供Spring调度使用）
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledCleanupExpiredSessions() {
        cleanupExpiredSessions(DEFAULT_SESSION_TIMEOUT_MINUTES);
    }

    // ==================== 编辑历史管理 ====================

    @Override
    public List<EditHistory> getEditHistory(String sessionId, int limit) {
        List<EditHistory> histories = sessionHistories.getOrDefault(sessionId, new ArrayList<>());
        return histories.stream()
                .sorted((h1, h2) -> h2.getTimestamp().compareTo(h1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<EditHistory> getFileEditHistory(Long fileId, int limit) {
        return activeSessions.values().stream()
                .filter(session -> session.getFileId().equals(fileId))
                .flatMap(session -> sessionHistories.getOrDefault(session.getSessionId(), new ArrayList<>()).stream())
                .sorted((h1, h2) -> h2.getTimestamp().compareTo(h1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DiffResult> compareVersions(String oldHistoryId, String newHistoryId) {
        try {
            EditHistory oldHistory = findHistoryById(oldHistoryId);
            EditHistory newHistory = findHistoryById(newHistoryId);

            if (oldHistory == null || newHistory == null) {
                return Optional.empty();
            }

            DiffResult diffResult = calculateDiff(oldHistory.getContent(), newHistory.getContent());
            return Optional.of(diffResult);

        } catch (Exception e) {
            logger.error("版本对比失败: oldHistoryId={}, newHistoryId={}", oldHistoryId, newHistoryId, e);
            return Optional.empty();
        }
    }

    @Override
    public EditResult restoreVersion(String sessionId, String historyId) {
        try {
            Optional<EditSession> sessionOptional = getEditSession(sessionId);
            if (!sessionOptional.isPresent()) {
                return createErrorResult("编辑会话不存在");
            }

            EditHistory history = findHistoryById(historyId);
            if (history == null) {
                return createErrorResult("历史版本不存在");
            }

            EditSession session = sessionOptional.get();
            session.setCurrentContent(history.getContent());
            session.setHasUnsavedChanges(true);
            session.setVersion(session.getVersion() + 1);

            // 创建恢复记录
            createHistoryRecord(sessionId, history.getContent(), "恢复到版本 " + history.getVersion(), session.getVersion());

            logger.info("恢复版本: sessionId={}, historyId={}", sessionId, historyId);

            Map<String, Object> data = new HashMap<>();
            data.put("content", history.getContent());
            data.put("version", session.getVersion());

            return createSuccessResult("版本已恢复", data);

        } catch (Exception e) {
            logger.error("恢复版本失败: sessionId={}, historyId={}", sessionId, historyId, e);
            return createErrorResult("恢复版本失败: " + e.getMessage());
        }
    }

    // ==================== 语法高亮支持 ====================

    @Override
    public Set<String> getSupportedLanguages() {
        return Collections.unmodifiableSet(SUPPORTED_LANGUAGES);
    }

    @Override
    public Optional<String> detectLanguage(String filename, String content) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        
        // 首先根据文件扩展名检测
        String language = EXTENSION_TO_LANGUAGE_MAP.get(extension);
        if (language != null) {
            return Optional.of(language);
        }

        // 根据内容进行简单检测
        if (content != null && !content.isEmpty()) {
            if (content.trim().startsWith("<?xml") || content.trim().startsWith("<")) {
                return Optional.of("xml");
            } else if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                return Optional.of("json");
            } else if (content.contains("#!/bin/bash") || content.contains("#!/bin/sh")) {
                return Optional.of("bash");
            }
        }

        return Optional.of("text");
    }

    @Override
    public boolean isEditSupported(String filename, String mimeType) {
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        
        // 检查扩展名
        if (SUPPORTED_EDIT_EXTENSIONS.contains(extension)) {
            return true;
        }

        // 检查MIME类型
        if (mimeType != null) {
            return mimeType.startsWith("text/") || 
                   mimeType.equals("application/json") ||
                   mimeType.equals("application/xml") ||
                   mimeType.equals("application/javascript");
        }

        return false;
    }

    @Override
    public Set<String> getSupportedEditExtensions() {
        return Collections.unmodifiableSet(SUPPORTED_EDIT_EXTENSIONS);
    }

    // ==================== 文件锁定管理 ====================

    @Override
    public boolean isFileLocked(Long fileId, Long userId) {
        FileLockInfo lockInfo = fileLocks.get(fileId);
        return lockInfo != null && !lockInfo.getUserId().equals(userId);
    }

    @Override
    public EditResult lockFile(Long fileId, Long userId, String sessionId) {
        if (isFileLocked(fileId, userId)) {
            FileLockInfo lockInfo = fileLocks.get(fileId);
            return createErrorResult("文件已被用户 " + lockInfo.getUserName() + " 锁定");
        }

        // 获取用户名（这里简化处理）
        String userName = "User" + userId;
        
        FileLockInfo lockInfo = new FileLockInfo(fileId, userId, sessionId, LocalDateTime.now(), userName);
        fileLocks.put(fileId, lockInfo);

        logger.debug("锁定文件: fileId={}, userId={}, sessionId={}", fileId, userId, sessionId);
        return createSuccessResult("文件已锁定", null);
    }

    @Override
    public EditResult unlockFile(Long fileId, Long userId, String sessionId) {
        FileLockInfo lockInfo = fileLocks.get(fileId);
        
        if (lockInfo == null) {
            return createSuccessResult("文件未被锁定", null);
        }

        if (!lockInfo.getUserId().equals(userId) || !lockInfo.getSessionId().equals(sessionId)) {
            return createErrorResult("无权解锁此文件");
        }

        fileLocks.remove(fileId);
        logger.debug("解锁文件: fileId={}, userId={}, sessionId={}", fileId, userId, sessionId);
        return createSuccessResult("文件已解锁", null);
    }

    @Override
    public Optional<FileLockInfo> getFileLockInfo(Long fileId) {
        return Optional.ofNullable(fileLocks.get(fileId));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查编辑权限
     */
    private boolean checkEditPermission(FileEntity file, Long userId) {
        try {
            // 使用权限服务检查文件写入权限
            return permissionService.hasPermission(userId, "file.write");
        } catch (Exception e) {
            logger.error("检查编辑权限失败: fileId={}, userId={}", file.getId(), userId, e);
            return false;
        }
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(FileEntity file) throws IOException {
        Optional<InputStream> inputStreamOpt = fileService.getFileContent(file.getId());
        if (!inputStreamOpt.isPresent()) {
            throw new IOException("无法读取文件内容");
        }

        try (InputStream inputStream = inputStreamOpt.get()) {
            return new String(inputStream.readAllBytes(), "UTF-8");
        }
    }

    /**
     * 保存内容到文件
     */
    private void saveContentInternal(EditSession session, String comment) throws IOException {
        String content = session.getCurrentContent();
        InputStream inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        
        FileService.FileOperationResult result = fileService.updateFileContent(
            session.getFileId(), inputStream, session.getUserId());
        
        if (!result.isSuccess()) {
            throw new IOException("保存文件失败: " + result.getMessage());
        }

        // 创建版本记录
        try {
            versionService.createVersion(result.getFileEntity(), 
                new ByteArrayInputStream(content.getBytes("UTF-8")), 
                comment, session.getUserId());
        } catch (Exception e) {
            logger.warn("创建版本记录失败: fileId={}", session.getFileId(), e);
        }

        session.setHasUnsavedChanges(false);
        session.setStatus(SessionStatus.SAVED);
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "edit_" + System.currentTimeMillis() + "_" + sessionCounter.incrementAndGet();
    }

    /**
     * 创建历史记录
     */
    private void createHistoryRecord(String sessionId, String content, String description, int version) {
        String historyId = "history_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        EditHistory history = new EditHistory(historyId, sessionId, content, description, version);
        
        sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(history);
        
        // 限制历史记录数量
        List<EditHistory> histories = sessionHistories.get(sessionId);
        if (histories.size() > MAX_HISTORY_COUNT) {
            histories.remove(0);
        }
    }

    /**
     * 根据ID查找历史记录
     */
    private EditHistory findHistoryById(String historyId) {
        return sessionHistories.values().stream()
                .flatMap(List::stream)
                .filter(history -> history.getHistoryId().equals(historyId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 计算内容差异
     */
    private DiffResult calculateDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");
        
        List<DiffLine> differences = new ArrayList<>();
        int addedLines = 0, deletedLines = 0, modifiedLines = 0;

        // 简单的行级差异算法
        int maxLines = Math.max(oldLines.length, newLines.length);
        for (int i = 0; i < maxLines; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;

            if (oldLine == null) {
                differences.add(new DiffLine(i + 1, newLine, DiffType.ADDED));
                addedLines++;
            } else if (newLine == null) {
                differences.add(new DiffLine(i + 1, oldLine, DiffType.DELETED));
                deletedLines++;
            } else if (!oldLine.equals(newLine)) {
                differences.add(new DiffLine(i + 1, newLine, DiffType.MODIFIED));
                modifiedLines++;
            } else {
                differences.add(new DiffLine(i + 1, newLine, DiffType.UNCHANGED));
            }
        }

        DiffStatistics statistics = new DiffStatistics(addedLines, deletedLines, modifiedLines);
        return new DiffResult(oldContent, newContent, differences, statistics);
    }

    /**
     * 创建成功结果
     */
    private EditResult createSuccessResult(String message, Object data) {
        return new EditResult(true, message, data, new HashMap<>());
    }

    /**
     * 创建错误结果
     */
    private EditResult createErrorResult(String message) {
        return new EditResult(false, message, null, new HashMap<>());
    }
} 