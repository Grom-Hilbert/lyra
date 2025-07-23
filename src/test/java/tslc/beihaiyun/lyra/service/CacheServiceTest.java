package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import tslc.beihaiyun.lyra.config.CacheConfig;
import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.RoleRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * CacheService 单元测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private LyraProperties lyraProperties;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private SpaceRepository spaceRepository;
    
    @Mock
    private PermissionService permissionService;
    
    @Mock
    private RoleService roleService;

    @InjectMocks
    private CacheService cacheService;

    private LyraProperties.CacheConfig cacheConfig;
    private Cache mockCache;

    @BeforeEach
    void setUp() {
        cacheConfig = new LyraProperties.CacheConfig();
        cacheConfig.setEnableWarmup(true);

        mockCache = new ConcurrentMapCache("test-cache");
    }

    @Test
    void testWarmUpCache_Success() {
        // 准备测试数据
        List<User> activeUsers = Arrays.asList(
            createTestUser(1L, "user1"),
            createTestUser(2L, "user2")
        );

        List<Role> enabledRoles = Arrays.asList(
            createTestRole(1L, "ADMIN"),
            createTestRole(2L, "USER")
        );

        List<Space> spaces = Arrays.asList(
            createTestSpace(1L, "space1"),
            createTestSpace(2L, "space2")
        );

        when(lyraProperties.getCache()).thenReturn(cacheConfig);
        when(userRepository.findActiveUsers(anyInt())).thenReturn(activeUsers);
        when(roleRepository.findByEnabledTrue()).thenReturn(enabledRoles);
        when(spaceRepository.findTop100ByOrderByUpdatedAtDesc()).thenReturn(spaces);
        when(cacheManager.getCache(any(String.class))).thenReturn(mockCache);

        when(permissionService.getUserPermissions(any(Long.class))).thenReturn(Collections.emptySet());
        when(roleService.getUserRoleInfo(any(Long.class))).thenReturn(Collections.emptyMap());
        when(roleService.hasPermissionThroughRoles(any(Long.class), any(String.class))).thenReturn(false);

        // 执行测试
        assertDoesNotThrow(() -> cacheService.warmUpCache());

        // 验证调用
        verify(userRepository).findActiveUsers(30);
        verify(roleRepository).findByEnabledTrue();
        verify(spaceRepository).findTop100ByOrderByUpdatedAtDesc();
        verify(permissionService, times(2)).getUserPermissions(any(Long.class));
        verify(roleService, times(2)).getUserRoleInfo(any(Long.class));
    }

    @Test
    void testWarmUpCache_Disabled() {
        // 禁用缓存预热
        cacheConfig.setEnableWarmup(false);
        when(lyraProperties.getCache()).thenReturn(cacheConfig);

        // 执行测试
        assertDoesNotThrow(() -> cacheService.warmUpCache());

        // 验证没有调用预热相关方法
        verify(userRepository, never()).findActiveUsers(anyInt());
        verify(roleRepository, never()).findByEnabledTrue();
        verify(spaceRepository, never()).findTop100ByOrderByUpdatedAtDesc();
    }

    @Test
    void testEvictCache() {
        String cacheName = CacheConfig.USER_PERMISSIONS_CACHE;
        when(cacheManager.getCache(cacheName)).thenReturn(mockCache);

        // 执行测试
        cacheService.evictCache(cacheName);

        // 验证缓存被清理
        verify(cacheManager).getCache(cacheName);
    }

    @Test
    void testEvictCacheKey() {
        String cacheName = CacheConfig.USER_PERMISSIONS_CACHE;
        String key = "user:123";
        when(cacheManager.getCache(cacheName)).thenReturn(mockCache);

        // 执行测试
        cacheService.evictCacheKey(cacheName, key);

        // 验证缓存键被清理
        verify(cacheManager).getCache(cacheName);
    }

    @Test
    void testEvictAllCaches() {
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList(
            CacheConfig.USER_PERMISSIONS_CACHE,
            CacheConfig.USER_ROLES_CACHE,
            CacheConfig.FILE_METADATA_CACHE
        ));
        when(cacheManager.getCache(any(String.class))).thenReturn(mockCache);

        // 执行测试
        cacheService.evictAllCaches();

        // 验证所有缓存被清理
        verify(cacheManager).getCacheNames();
        verify(cacheManager, times(3)).getCache(any(String.class));
    }

    @Test
    void testGetCacheStatistics() {
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList(
            CacheConfig.USER_PERMISSIONS_CACHE,
            CacheConfig.USER_ROLES_CACHE,
            CacheConfig.FILE_METADATA_CACHE
        ));
        when(cacheManager.getCache(any(String.class))).thenReturn(mockCache);

        // 执行测试
        Map<String, Object> statistics = cacheService.getCacheStatistics();

        // 验证统计信息
        assertNotNull(statistics);
        assertEquals(3, statistics.size());
        assertTrue(statistics.containsKey(CacheConfig.USER_PERMISSIONS_CACHE));
        assertTrue(statistics.containsKey(CacheConfig.USER_ROLES_CACHE));
        assertTrue(statistics.containsKey(CacheConfig.FILE_METADATA_CACHE));
    }

    @Test
    void testRecordCacheHit() {
        String cacheName = "test-cache";
        
        // 执行测试
        cacheService.recordCacheHit(cacheName);
        
        // 验证统计信息被记录
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        assertNotNull(statistics);
    }

    @Test
    void testRecordCacheMiss() {
        String cacheName = "test-cache";
        
        // 执行测试
        cacheService.recordCacheMiss(cacheName);
        
        // 验证统计信息被记录
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        assertNotNull(statistics);
    }

    @Test
    void testRecordCacheEviction() {
        String cacheName = "test-cache";
        
        // 执行测试
        cacheService.recordCacheEviction(cacheName);
        
        // 验证统计信息被记录
        Map<String, Object> statistics = cacheService.getCacheStatistics();
        assertNotNull(statistics);
    }

    @Test
    void testCleanupExpiredStats() {
        // 执行测试
        assertDoesNotThrow(() -> cacheService.cleanupExpiredStats());
    }

    // 辅助方法
    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setEnabled(true);
        return user;
    }

    private Role createTestRole(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(code);
        role.setEnabled(true);
        return role;
    }

    private Space createTestSpace(Long id, String name) {
        Space space = new Space();
        space.setId(id);
        space.setName(name);
        return space;
    }
}
