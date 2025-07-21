package tslc.beihaiyun.lyra.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 文件工具类
 * 提供文件操作、哈希计算、MIME类型检测等实用方法
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 文件大小单位
     */
    private static final Map<String, Long> SIZE_UNITS = Map.of(
        "B", 1L,
        "KB", 1024L,
        "MB", 1024L * 1024L,
        "GB", 1024L * 1024L * 1024L,
        "TB", 1024L * 1024L * 1024L * 1024L
    );

    /**
     * 常见MIME类型映射
     */
    private static final Map<String, String> MIME_TYPES;
    
    static {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("xls", "application/vnd.ms-excel");
        mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("bmp", "image/bmp");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("avi", "video/x-msvideo");
        mimeTypes.put("mov", "video/quicktime");
        mimeTypes.put("mp3", "audio/mpeg");
        mimeTypes.put("wav", "audio/wav");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("rar", "application/vnd.rar");
        mimeTypes.put("7z", "application/x-7z-compressed");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        MIME_TYPES = Collections.unmodifiableMap(mimeTypes);
    }

    /**
     * 非法文件名字符模式
     */
    private static final Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * 计算输入流的SHA-256哈希值
     * 
     * @param inputStream 输入流
     * @return SHA-256哈希值（小写十六进制）
     * @throws IOException 读取异常
     */
    public static String calculateSHA256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 计算文件的SHA-256哈希值
     * 
     * @param filePath 文件路径
     * @return SHA-256哈希值
     * @throws IOException 读取异常
     */
    public static String calculateSHA256(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return calculateSHA256(inputStream);
        }
    }

    /**
     * 根据文件扩展名获取MIME类型
     * 
     * @param filename 文件名
     * @return MIME类型，未知类型返回application/octet-stream
     */
    public static String getMimeType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        
        // 先尝试使用Java内置方法
        try {
            String mimeType = Files.probeContentType(Paths.get(filename));
            if (mimeType != null) {
                return mimeType;
            }
        } catch (Exception e) {
            logger.debug("使用Files.probeContentType检测MIME类型失败: {}", e.getMessage());
        }
        
        // 使用自定义映射
        String extension = getFileExtension(filename).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名（不包含点号），没有扩展名返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            String extension = filename.substring(lastDotIndex + 1).trim();
            // 确保扩展名不为空且不只包含空格
            if (!extension.isEmpty()) {
                return extension;
            }
        }
        return "";
    }

    /**
     * 获取不带扩展名的文件名
     * 
     * @param filename 文件名
     * @return 不带扩展名的文件名
     */
    public static String getNameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        } else if (lastDotIndex == 0) {
            // 隐藏文件如.hiddenfile，没有扩展名，返回空字符串
            return "";
        }
        return filename;
    }

    /**
     * 清理文件名，移除非法字符
     * 
     * @param filename 原始文件名
     * @return 清理后的文件名
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        
        // 先提取扩展名，避免扩展名中的点号被误替换
        String extension = getFileExtension(filename);
        String nameWithoutExt = getNameWithoutExtension(filename);
        
                 // 移除非法字符（只处理文件名部分，不包括扩展名）
         String sanitizedName = INVALID_FILENAME_PATTERN.matcher(nameWithoutExt).replaceAll("_");
         
         // 合并连续的下划线
         sanitizedName = sanitizedName.replaceAll("_{2,}", "_");
         
         // 去除前后空格、点号和下划线
         sanitizedName = sanitizedName.trim().replaceAll("^[.\\s_]+|[.\\s_]+$", "");
        
        // 确保文件名不为空
        if (sanitizedName.isEmpty()) {
            sanitizedName = "unnamed";
        }
        
        // 重新组合文件名和扩展名
        String result = extension.isEmpty() ? sanitizedName : sanitizedName + "." + extension;
        
        // 限制长度
        if (result.length() > 200) {
            int maxNameLength = 200 - (extension.isEmpty() ? 0 : extension.length() + 1);
            sanitizedName = sanitizedName.substring(0, Math.max(1, maxNameLength));
            result = extension.isEmpty() ? sanitizedName : sanitizedName + "." + extension;
        }
        
        return result;
    }

    /**
     * 解析文件大小字符串
     * 
     * @param sizeStr 文件大小字符串（如"100MB", "1GB"）
     * @return 文件大小（字节）
     * @throws IllegalArgumentException 格式错误
     */
    public static long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("文件大小字符串不能为空");
        }
        
        sizeStr = sizeStr.trim().toUpperCase();
        
        // 提取数字和单位
        String numberPart = sizeStr.replaceAll("[^0-9.]", "");
        String unitPart = sizeStr.replaceAll("[0-9.]", "");
        
        if (numberPart.isEmpty()) {
            throw new IllegalArgumentException("无效的文件大小格式: " + sizeStr);
        }
        
        try {
            double number = Double.parseDouble(numberPart);
            
            // 如果没有单位，默认为字节
            if (unitPart.isEmpty()) {
                return (long) number;
            }
            
            // 标准化单位
            if (unitPart.equals("K") || unitPart.equals("KB")) {
                unitPart = "KB";
            } else if (unitPart.equals("M") || unitPart.equals("MB")) {
                unitPart = "MB";
            } else if (unitPart.equals("G") || unitPart.equals("GB")) {
                unitPart = "GB";
            } else if (unitPart.equals("T") || unitPart.equals("TB")) {
                unitPart = "TB";
            } else if (!unitPart.equals("B")) {
                throw new IllegalArgumentException("不支持的文件大小单位: " + unitPart);
            }
            
            Long multiplier = SIZE_UNITS.get(unitPart);
            if (multiplier == null) {
                throw new IllegalArgumentException("不支持的文件大小单位: " + unitPart);
            }
            
            return (long) (number * multiplier);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的文件大小格式: " + sizeStr, e);
        }
    }

    /**
     * 格式化文件大小为人类可读格式
     * 
     * @param sizeBytes 文件大小（字节）
     * @return 格式化的大小字符串
     */
    public static String formatFileSize(long sizeBytes) {
        if (sizeBytes < 0) {
            return "0 B";
        }
        
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        
        int exp = (int) (Math.log(sizeBytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", sizeBytes / Math.pow(1024, exp), pre);
    }

    /**
     * 生成基于时间和哈希的存储路径
     * 
     * @param fileHash 文件哈希值
     * @param originalFilename 原始文件名
     * @return 存储路径
     */
    public static String generateStoragePath(String fileHash, String originalFilename) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getFileExtension(originalFilename);
        
        StringBuilder path = new StringBuilder();
        path.append(datePath).append("/");
        
        // 使用哈希值的前几位作为子目录
        if (fileHash != null && fileHash.length() >= 4) {
            path.append(fileHash.substring(0, 2)).append("/");
            path.append(fileHash.substring(2, 4)).append("/");
            path.append(fileHash);
        } else {
            // 降级方案：使用时间戳
            path.append(now.format(DateTimeFormatter.ofPattern("HHmmss"))).append("_");
            path.append(UUID.randomUUID().toString().replace("-", ""));
        }
        
        if (!extension.isEmpty()) {
            path.append(".").append(extension);
        }
        
        return path.toString();
    }

    /**
     * 生成唯一的文件名
     * 
     * @param originalFilename 原始文件名
     * @param existingNames 已存在的文件名集合
     * @return 唯一的文件名
     */
    public static String generateUniqueFilename(String originalFilename, Set<String> existingNames) {
        if (!existingNames.contains(originalFilename)) {
            return originalFilename;
        }
        
        String nameWithoutExt = getNameWithoutExtension(originalFilename);
        String extension = getFileExtension(originalFilename);
        
        int counter = 1;
        String newFilename;
        do {
            newFilename = nameWithoutExt + "(" + counter + ")";
            if (!extension.isEmpty()) {
                newFilename += "." + extension;
            }
            counter++;
        } while (existingNames.contains(newFilename) && counter < 1000);
        
        return newFilename;
    }

    /**
     * 检查文件类型是否被允许
     * 
     * @param filename 文件名
     * @param allowedTypes 允许的类型配置（*表示全部允许）
     * @return 是否允许
     */
    public static boolean isFileTypeAllowed(String filename, String allowedTypes) {
        if (allowedTypes == null || allowedTypes.trim().isEmpty() || "*".equals(allowedTypes.trim())) {
            return true;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        String[] allowed = allowedTypes.toLowerCase().split("[,;\\s]+");
        
        for (String type : allowed) {
            type = type.trim();
            if (type.startsWith(".")) {
                type = type.substring(1);
            }
            if (type.equals(extension)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 创建目录（如果不存在）
     * 
     * @param dirPath 目录路径
     * @throws IOException 创建失败
     */
    public static void ensureDirectoryExists(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            logger.debug("创建目录: {}", dirPath);
        }
    }

    /**
     * 安全地删除文件
     * 
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean safeDelete(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.debug("删除文件: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.warn("删除文件失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 复制输入流到输出流
     * 
     * @param input 输入流
     * @param output 输出流
     * @return 复制的字节数
     * @throws IOException 复制异常
     */
    public static long copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int bytesRead;
        
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            totalBytes += bytesRead;
        }
        
        return totalBytes;
    }

    /**
     * 检查文件是否为文本文件
     * 
     * @param mimeType MIME类型
     * @return 是否为文本文件
     */
    public static boolean isTextFile(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("text/") ||
               mimeType.equals("application/json") ||
               mimeType.equals("application/xml") ||
               mimeType.equals("application/javascript");
    }

    /**
     * 检查文件是否为图片文件
     * 
     * @param mimeType MIME类型
     * @return 是否为图片文件
     */
    public static boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 检查文件是否为视频文件
     * 
     * @param mimeType MIME类型
     * @return 是否为视频文件
     */
    public static boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * 检查文件是否为音频文件
     * 
     * @param mimeType MIME类型
     * @return 是否为音频文件
     */
    public static boolean isAudioFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }
} 