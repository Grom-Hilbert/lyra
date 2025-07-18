package tslc.beihaiyun.lyra.storage;

import org.junit.jupiter.api.Test;
import tslc.beihaiyun.lyra.storage.impl.NFSStorageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFS存储服务集成测试
 * 测试NFS存储服务的基本功能
 */
class NFSStorageIntegrationTest {
    
    @Test
    void shouldSupportNFSStorageType() {
        // 验证NFS存储类型在枚举中存在
        assertThat(StorageType.NFS).isNotNull();
        assertThat(StorageType.NFS.name()).isEqualTo("NFS");
    }
    
    @Test
    void shouldCreateNFSStorageServiceInstance() {
        // 验证能够创建NFS存储服务实例
        NFSStorageService nfsService = new NFSStorageService(
            "test-server", 
            "/test/export", 
            "/tmp/test-mount",
            "rw,sync,hard,intr",
            1024 * 1024, // 1MB
            30000,
            60000,
            3
        );
        
        assertThat(nfsService).isNotNull();
        assertThat(nfsService.getStorageType()).isEqualTo(StorageType.NFS);
    }
    
    @Test
    void shouldHaveCorrectStorageTypeConstants() {
        // 验证所有存储类型常量都存在
        assertThat(StorageType.LOCAL_FILESYSTEM).isNotNull();
        assertThat(StorageType.NFS).isNotNull();
        assertThat(StorageType.S3_COMPATIBLE).isNotNull();
        assertThat(StorageType.SMB_CIFS).isNotNull();
        assertThat(StorageType.WEBDAV).isNotNull();
        assertThat(StorageType.IN_MEMORY).isNotNull();
    }
}