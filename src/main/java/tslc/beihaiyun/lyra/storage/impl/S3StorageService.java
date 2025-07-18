package tslc.beihaiyun.lyra.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import tslc.beihaiyun.lyra.storage.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * S3兼容对象存储服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "lyra.storage.s3.enabled", havingValue = "true")
public class S3StorageService implements StorageService {
    
    private final S3Client s3Client;
    private final String bucketName;
    private final long maxFileSize;
    
    public S3StorageService(
            @Value("${lyra.storage.s3.endpoint}") String endpoint,
            @Value("${lyra.storage.s3.region:us-east-1}") String region,
            @Value("${lyra.storage.s3.access-key}") String accessKey,
            @Value("${lyra.storage.s3.secret-key}") String secretKey,
            @Value("${lyra.storage.s3.bucket}") String bucketName,
            @Value("${lyra.storage.s3.max-file-size:104857600}") long maxFileSize) {
        
        this.bucketName = bucketName;
        this.maxFileSize = maxFileSize;
        
        var credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey));
        
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentialsProvider)
            .build();
            
        initializeStorage();
    }
    
    private void initializeStorage() {
        try {
            // 检查bucket是否存在，不存在则创建
            if (!bucketExists()) {
                createBucket();
            }
            log.info("S3存储初始化完成，bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("S3存储初始化失败", e);
            throw new RuntimeException("无法初始化S3存储", e);
        }
    }
    
    private boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
    
    private void createBucket() {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        log.info("创建S3 bucket: {}", bucketName);
    }
    
    @Override
    public StorageResult store(String key, InputStream inputStream, long contentLength, String contentType) throws StorageException {
        validateKey(key);
        validateContentLength(contentLength);
        
        try {
            // 读取输入流到字节数组（用于计算校验和）
            byte[] data = inputStream.readAllBytes();
            
            if (data.length > maxFileSize) {
                throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                    "文件大小超过限制: " + maxFileSize + " 字节");
            }
            
            var putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();
            
            var response = s3Client.putObject(putRequest, RequestBody.fromBytes(data));
            
            log.debug("文件存储成功: key={}, size={}, etag={}", key, data.length, response.eTag());
            
            return StorageResult.builder()
                .key(key)
                .size(data.length)
                .checksum(response.eTag().replace("\"", "")) // ETag通常是MD5
                .contentType(contentType)
                .storedAt(LocalDateTime.now())
                .storagePath("s3://" + bucketName + "/" + key)
                .success(true)
                .build();
                
        } catch (IOException e) {
            log.error("读取输入流失败: key={}", key, e);
            throw new StorageException(StorageErrorType.IO_ERROR, "读取输入流失败: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("存储文件到S3失败: key={}", key, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "存储文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<InputStream> retrieve(String key) throws StorageException {
        validateKey(key);
        
        try {
            var getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            var response = s3Client.getObject(getRequest);
            log.debug("文件检索成功: key={}", key);
            return Optional.of(response);
            
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            log.error("从S3检索文件失败: key={}", key, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "检索文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean delete(String key) throws StorageException {
        validateKey(key);
        
        try {
            var deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            s3Client.deleteObject(deleteRequest);
            log.debug("文件删除成功: key={}", key);
            return true;
            
        } catch (S3Exception e) {
            log.error("从S3删除文件失败: key={}", key, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "删除文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(String key) throws StorageException {
        validateKey(key);
        
        try {
            var headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("检查S3文件存在性失败: key={}", key, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "检查文件存在性失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<StorageMetadata> getMetadata(String key) throws StorageException {
        validateKey(key);
        
        try {
            var headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            var response = s3Client.headObject(headRequest);
            
            Map<String, String> customMetadata = new HashMap<>();
            if (response.metadata() != null) {
                customMetadata.putAll(response.metadata());
            }
            
            return Optional.of(StorageMetadata.builder()
                .key(key)
                .size(response.contentLength())
                .contentType(response.contentType())
                .checksum(response.eTag().replace("\"", ""))
                .lastModified(LocalDateTime.ofInstant(response.lastModified(), ZoneId.systemDefault()))
                .storagePath("s3://" + bucketName + "/" + key)
                .storageType(StorageType.S3_COMPATIBLE)
                .customMetadata(customMetadata)
                .build());
                
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            log.error("获取S3文件元数据失败: key={}", key, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "获取文件元数据失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult copy(String sourceKey, String targetKey) throws StorageException {
        validateKey(sourceKey);
        validateKey(targetKey);
        
        try {
            var copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(targetKey)
                .build();
            
            var response = s3Client.copyObject(copyRequest);
            
            // 获取目标文件的元数据
            var metadata = getMetadata(targetKey);
            long size = metadata.map(StorageMetadata::getSize).orElse(0L);
            
            log.debug("文件复制成功: {} -> {}", sourceKey, targetKey);
            
            return StorageResult.builder()
                .key(targetKey)
                .size(size)
                .checksum(response.copyObjectResult().eTag().replace("\"", ""))
                .storedAt(LocalDateTime.now())
                .storagePath("s3://" + bucketName + "/" + targetKey)
                .success(true)
                .build();
                
        } catch (S3Exception e) {
            log.error("S3文件复制失败: {} -> {}", sourceKey, targetKey, e);
            throw new StorageException(StorageErrorType.NETWORK_ERROR, "复制文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public StorageResult move(String sourceKey, String targetKey) throws StorageException {
        // S3没有原生的移动操作，需要先复制再删除
        StorageResult copyResult = copy(sourceKey, targetKey);
        if (copyResult.isSuccess()) {
            delete(sourceKey);
            log.debug("文件移动成功: {} -> {}", sourceKey, targetKey);
        }
        return copyResult;
    }
    
    @Override
    public StorageType getStorageType() {
        return StorageType.S3_COMPATIBLE;
    }
    
    @Override
    public StorageStats getStats() {
        try {
            // 获取bucket统计信息
            var listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
            
            long fileCount = 0;
            long totalSize = 0;
            
            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(listRequest);
                fileCount += response.contents().size();
                totalSize += response.contents().stream()
                    .mapToLong(S3Object::size)
                    .sum();
                
                listRequest = listRequest.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
                    
            } while (response.isTruncated());
            
            return StorageStats.builder()
                .totalSpace(-1) // S3没有固定的总空间限制
                .usedSpace(totalSize)
                .availableSpace(-1)
                .fileCount(fileCount)
                .storageType(StorageType.S3_COMPATIBLE)
                .healthy(true)
                .healthMessage("S3存储正常")
                .build();
                
        } catch (S3Exception e) {
            log.error("获取S3存储统计信息失败", e);
            return StorageStats.builder()
                .storageType(StorageType.S3_COMPATIBLE)
                .healthy(false)
                .healthMessage("获取存储统计信息失败: " + e.getMessage())
                .build();
        }
    }
    
    private void validateKey(String key) throws StorageException {
        if (key == null || key.trim().isEmpty()) {
            throw new StorageException(StorageErrorType.CONFIGURATION_ERROR, "存储键值不能为空");
        }
        
        // S3对象键的基本验证
        if (key.length() > 1024) {
            throw new StorageException(StorageErrorType.CONFIGURATION_ERROR, "存储键值过长");
        }
    }
    
    private void validateContentLength(long contentLength) throws StorageException {
        if (contentLength > maxFileSize) {
            throw new StorageException(StorageErrorType.FILE_SIZE_EXCEEDED, 
                "文件大小超过限制: " + maxFileSize + " 字节");
        }
    }
}