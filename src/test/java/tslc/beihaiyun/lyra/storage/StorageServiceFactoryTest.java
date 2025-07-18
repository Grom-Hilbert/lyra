package tslc.beihaiyun.lyra.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 存储服务工厂测试
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceFactoryTest {
    
    @Mock
    private StorageService localStorageService;
    
    @Mock
    private StorageService s3StorageService;
    
    private StorageServiceFactory factory;
    
    @BeforeEach
    void setUp() {
        // Mockito stubs will be configured in individual tests as needed
    }
    
    @Test
    void shouldReturnPrimaryStorageServiceWhenConfigured() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "s3");
        
        // When
        StorageService primary = factory.getPrimaryStorageService();
        
        // Then
        assertThat(primary).isEqualTo(s3StorageService);
        assertThat(primary.getStorageType()).isEqualTo(StorageType.S3_COMPATIBLE);
    }
    
    @Test
    void shouldReturnLocalStorageWhenConfiguredAsLocal() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "local");
        
        // When
        StorageService primary = factory.getPrimaryStorageService();
        
        // Then
        assertThat(primary).isEqualTo(localStorageService);
        assertThat(primary.getStorageType()).isEqualTo(StorageType.LOCAL_FILESYSTEM);
    }
    
    @Test
    void shouldReturnFirstServiceWhenPrimaryNotFound() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "unknown");
        
        // When
        StorageService primary = factory.getPrimaryStorageService();
        
        // Then
        assertThat(primary).isEqualTo(localStorageService); // 第一个服务
    }
    
    @Test
    void shouldThrowExceptionWhenNoServicesAvailable() {
        // Given
        List<StorageService> services = Collections.emptyList();
        factory = new StorageServiceFactory(services, "local");
        
        // When & Then
        assertThatThrownBy(() -> factory.getPrimaryStorageService())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("没有可用的存储服务");
    }
    
    @Test
    void shouldGetStorageServiceByType() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "local");
        
        // When
        Optional<StorageService> localService = factory.getStorageService(StorageType.LOCAL_FILESYSTEM);
        Optional<StorageService> s3Service = factory.getStorageService(StorageType.S3_COMPATIBLE);
        Optional<StorageService> nfsService = factory.getStorageService(StorageType.NFS);
        
        // Then
        assertThat(localService).isPresent().contains(localStorageService);
        assertThat(s3Service).isPresent().contains(s3StorageService);
        assertThat(nfsService).isEmpty();
    }
    
    @Test
    void shouldReturnAllStorageServices() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "local");
        
        // When
        List<StorageService> allServices = factory.getAllStorageServices();
        
        // Then
        assertThat(allServices).hasSize(2);
        assertThat(allServices).contains(localStorageService, s3StorageService);
    }
    
    @Test
    void shouldCheckStorageServiceAvailability() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(localStorageService, s3StorageService);
        factory = new StorageServiceFactory(services, "local");
        
        // When & Then
        assertThat(factory.isStorageServiceAvailable(StorageType.LOCAL_FILESYSTEM)).isTrue();
        assertThat(factory.isStorageServiceAvailable(StorageType.S3_COMPATIBLE)).isTrue();
        assertThat(factory.isStorageServiceAvailable(StorageType.NFS)).isFalse();
        assertThat(factory.isStorageServiceAvailable(StorageType.WEBDAV)).isFalse();
    }
    
    @Test
    void shouldHandleFilesystemAlias() {
        // Given
        when(localStorageService.getStorageType()).thenReturn(StorageType.LOCAL_FILESYSTEM);
        List<StorageService> services = Arrays.asList(localStorageService);
        factory = new StorageServiceFactory(services, "filesystem");
        
        // When
        StorageService primary = factory.getPrimaryStorageService();
        
        // Then
        assertThat(primary).isEqualTo(localStorageService);
    }
    
    @Test
    void shouldHandleObjectAlias() {
        // Given
        when(s3StorageService.getStorageType()).thenReturn(StorageType.S3_COMPATIBLE);
        List<StorageService> services = Arrays.asList(s3StorageService);
        factory = new StorageServiceFactory(services, "object");
        
        // When
        StorageService primary = factory.getPrimaryStorageService();
        
        // Then
        assertThat(primary).isEqualTo(s3StorageService);
    }
}