package tslc.beihaiyun.lyra.webdav;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

/**
 * WebDAV MultipartFile 包装器
 * 
 * 将WebDAV上传的InputStream包装为Spring MultipartFile接口
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
public class WebDavMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;
    private final long size;

    /**
     * 构造器
     * 
     * @param filename 文件名
     * @param inputStream 输入流
     * @param contentLength 内容长度
     * @throws IOException 读取异常
     */
    public WebDavMultipartFile(String filename, InputStream inputStream, long contentLength) throws IOException {
        this.name = "file";
        this.originalFilename = filename;
        this.contentType = "application/octet-stream";
        this.size = contentLength;
        
        // 读取输入流内容
        if (contentLength > 0 && contentLength <= Integer.MAX_VALUE) {
            this.content = inputStream.readAllBytes();
        } else {
            // 对于未知长度或过大的文件，使用流式读取
            this.content = inputStream.readAllBytes();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content.clone();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo not supported for WebDAV files");
    }
} 