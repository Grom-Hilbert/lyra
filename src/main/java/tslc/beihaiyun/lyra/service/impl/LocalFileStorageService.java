package tslc.beihaiyun.lyra.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.repository.FileEntityRepository;
import tslc.beihaiyun.lyra.service.StorageService;
import tslc.beihaiyun.lyra.util.FileUtils;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地文件系统存储服务实现
 * 提供基于本地文件系统的文件存储、读取、删除等操作
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
public class LocalFileStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final LyraProperties lyraProperties;
    private final FileEntityRepository fileEntityRepository;

    private Path basePath;
    private Path tempPath;
    private long maxFileSize;
    private String allowedTypes;
    private boolean enableDeduplication;

    @Autowired
    public LocalFileStorageService(LyraProperties lyraProperties, FileEntityRepository fileEntityRepository) {
        this.lyraProperties = lyraProperties;
        this.fileEntityRepository = fileEntityRepository;
    }

    /**
     * 初始化存储服务
     */
    @PostConstruct
    public void init() {
        LyraProperties.StorageConfig config = lyraProperties.getStorage();
        
        this.basePath = Paths.get(config.getBasePath()).toAbsolutePath();
        this.tempPath = Paths.get(config.getTempPath()).toAbsolutePath();
        this.maxFileSize = FileUtils.parseFileSize(config.getMaxFileSize());
        this.allowedTypes = config.getAllowedTypes();
        this.enableDeduplication = config.getEnableDeduplication();

        try {
            // 确保存储目录存在
            FileUtils.ensureDirectoryExists(basePath);
            FileUtils.ensureDirectoryExists(tempPath);
            logger.info("文件存储服务初始化完成 - 基础路径: {}, 临时路径: {}", basePath, tempPath);
        } catch (IOException e) {
            throw new RuntimeException("文件存储服务初始化失败", e);
        }
    }

    @Override
    public StorageResult store(MultipartFile file) throws IOException {
        validateFile(file);
        
        try (InputStream inputStream = file.getInputStream()) {
            return store(inputStream, file.getOriginalFilename(), file.getContentType());
        }
    }

    @Override
    public StorageResult store(InputStream inputStream, String filename, String contentType) throws IOException {
        // 创建临时文件来计算哈希值
        Path tempFile = createTempFile(filename);
        String fileHash;
        long fileSize;
        
        try {
            // 将输入流写入临时文件并计算哈希值
            try (FileOutputStream tempOutput = new FileOutputStream(tempFile.toFile());
                 BufferedInputStream bufferedInput = new BufferedInputStream(inputStream)) {
                
                fileSize = FileUtils.copyStream(bufferedInput, tempOutput);
            }
            
            // 计算文件哈希值
            fileHash = FileUtils.calculateSHA256(tempFile);
            
            // 检查文件去重
            if (enableDeduplication) {
                Optional<String> existingPath = findDuplicateFile(fileHash);
                if (existingPath.isPresent()) {
                    // 删除临时文件
                    FileUtils.safeDelete(tempFile);
                    return new StorageResult(existingPath.get(), fileHash, fileSize, true);
                }
            }
            
            // 生成存储路径
            String storagePath = FileUtils.generateStoragePath(fileHash, filename);
            Path targetPath = basePath.resolve(storagePath);
            
            // 确保目标目录存在
            FileUtils.ensureDirectoryExists(targetPath.getParent());
            
            // 移动临时文件到最终位置
            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.debug("文件存储成功: {} -> {}", filename, storagePath);
            return new StorageResult(storagePath, fileHash, fileSize, false);
            
        } catch (Exception e) {
            // 清理临时文件
            FileUtils.safeDelete(tempFile);
            throw new IOException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageResult store(byte[] content, String filename, String contentType) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            return store(inputStream, filename, contentType);
        }
    }

    @Override
    public Optional<InputStream> load(String storagePath) throws IOException {
        Path filePath = basePath.resolve(storagePath);
        
        if (!Files.exists(filePath)) {
            logger.warn("文件不存在: {}", storagePath);
            return Optional.empty();
        }
        
        if (!Files.isReadable(filePath)) {
            logger.warn("文件不可读: {}", storagePath);
            return Optional.empty();
        }
        
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            return Optional.of(inputStream);
        } catch (IOException e) {
            logger.error("读取文件失败: {}", storagePath, e);
            throw new IOException("读取文件失败: " + storagePath, e);
        }
    }

    @Override
    public Optional<Path> getPath(String storagePath) {
        Path filePath = basePath.resolve(storagePath);
        return Files.exists(filePath) ? Optional.of(filePath) : Optional.empty();
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = basePath.resolve(storagePath);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    public boolean delete(String storagePath) {
        Path filePath = basePath.resolve(storagePath);
        boolean result = FileUtils.safeDelete(filePath);
        
        if (result) {
            logger.debug("文件删除成功: {}", storagePath);
        } else {
            logger.warn("文件删除失败: {}", storagePath);
        }
        
        return result;
    }

    @Override
    public long getFileSize(String storagePath) {
        Path filePath = basePath.resolve(storagePath);
        
        try {
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            logger.warn("获取文件大小失败: {}", storagePath, e);
        }
        
        return -1;
    }

    @Override
    public String calculateHash(InputStream inputStream) throws IOException {
        return FileUtils.calculateSHA256(inputStream);
    }

    @Override
    public boolean verifyIntegrity(String storagePath, String expectedHash) {
        try {
            Path filePath = basePath.resolve(storagePath);
            if (!Files.exists(filePath)) {
                return false;
            }
            
            String actualHash = FileUtils.calculateSHA256(filePath);
            return actualHash.equals(expectedHash);
        } catch (IOException e) {
            logger.warn("验证文件完整性失败: {}", storagePath, e);
            return false;
        }
    }

    @Override
    public Optional<String> findDuplicateFile(String fileHash) {
        if (!enableDeduplication || fileHash == null || fileHash.isEmpty()) {
            return Optional.empty();
        }
        
        // 查询数据库中是否存在相同哈希值的文件
        return fileEntityRepository.findByFileHash(fileHash)
                .stream()
                .filter(file -> exists(file.getStoragePath()))
                .map(file -> file.getStoragePath())
                .findFirst();
    }

    @Override
    public boolean copy(String sourcePath, String targetPath) throws IOException {
        Path source = basePath.resolve(sourcePath);
        Path target = basePath.resolve(targetPath);
        
        if (!Files.exists(source)) {
            logger.warn("源文件不存在: {}", sourcePath);
            return false;
        }
        
        try {
            // 确保目标目录存在
            FileUtils.ensureDirectoryExists(target.getParent());
            
            // 复制文件
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("文件复制成功: {} -> {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            logger.error("文件复制失败: {} -> {}", sourcePath, targetPath, e);
            throw new IOException("文件复制失败", e);
        }
    }

    @Override
    public boolean move(String sourcePath, String targetPath) throws IOException {
        Path source = basePath.resolve(sourcePath);
        Path target = basePath.resolve(targetPath);
        
        if (!Files.exists(source)) {
            logger.warn("源文件不存在: {}", sourcePath);
            return false;
        }
        
        try {
            // 确保目标目录存在
            FileUtils.ensureDirectoryExists(target.getParent());
            
            // 移动文件
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("文件移动成功: {} -> {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            logger.error("文件移动失败: {} -> {}", sourcePath, targetPath, e);
            throw new IOException("文件移动失败", e);
        }
    }

    @Override
    public int cleanupTempFiles() {
        int cleanedCount = 0;
        LocalDateTime cutoffTime = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempPath)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    try {
                        LocalDateTime lastModified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        
                        if (lastModified.isBefore(cutoffTime)) {
                            if (FileUtils.safeDelete(path)) {
                                cleanedCount++;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("清理临时文件失败: {}", path, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("清理临时文件目录失败", e);
        }
        
        if (cleanedCount > 0) {
            logger.info("清理了 {} 个临时文件", cleanedCount);
        }
        
        return cleanedCount;
    }

    @Override
    public StorageStats getStorageStats() {
        try {
            FileStore fileStore = Files.getFileStore(basePath);
            long totalSpace = fileStore.getTotalSpace();
            long freeSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;
            
            // 统计文件数量
            AtomicLong fileCount = new AtomicLong(0);
            Files.walk(basePath)
                .filter(Files::isRegularFile)
                .forEach(path -> fileCount.incrementAndGet());
            
            return new StorageStats(totalSpace, usedSpace, freeSpace, fileCount.get());
        } catch (IOException e) {
            logger.error("获取存储统计信息失败", e);
            return new StorageStats(0, 0, 0, 0);
        }
    }

    /**
     * 验证上传的文件
     * 
     * @param file 上传文件
     * @throws IOException 验证失败
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("文件名不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new IOException("文件大小超过限制: " + FileUtils.formatFileSize(maxFileSize));
        }
        
        // 检查文件类型
        if (!FileUtils.isFileTypeAllowed(filename, allowedTypes)) {
            throw new IOException("不支持的文件类型: " + FileUtils.getFileExtension(filename));
        }
    }

    /**
     * 创建临时文件
     * 
     * @param originalFilename 原始文件名
     * @return 临时文件路径
     * @throws IOException 创建失败
     */
    private Path createTempFile(String originalFilename) throws IOException {
        String sanitizedName = FileUtils.sanitizeFilename(originalFilename);
        String extension = FileUtils.getFileExtension(sanitizedName);
        String prefix = FileUtils.getNameWithoutExtension(sanitizedName);
        
        if (prefix.length() < 3) {
            prefix = "temp_" + prefix;
        }
        
        String suffix = extension.isEmpty() ? ".tmp" : "." + extension;
        
        return Files.createTempFile(tempPath, prefix, suffix);
    }
} 