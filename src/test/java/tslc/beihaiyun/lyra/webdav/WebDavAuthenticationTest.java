package tslc.beihaiyun.lyra.webdav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.JwtService;
import tslc.beihaiyun.lyra.service.PermissionService;
import tslc.beihaiyun.lyra.service.UserService;

/**
 * WebDAV 认证单元测试
 * 
 * 测试WebDAV认证处理器、权限服务和锁定服务的核心功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WebDavAuthenticationTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private WebDavAuthenticationHandler authHandler;
    private WebDavPermissionService permissionServiceImpl;
    private WebDavLockService lockService;

    private User testUser;
    private Space testSpace;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() throws IOException {
        authHandler = new WebDavAuthenticationHandler(jwtService, userDetailsService, authenticationManager);
        permissionServiceImpl = new WebDavPermissionService(permissionService, userService);
        lockService = new WebDavLockService();
        
        passwordEncoder = new BCryptPasswordEncoder();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("test-space");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);
        
        // 设置Mock response的getWriter方法
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("WebDAV认证处理器应该拒绝没有Authorization头的请求")
    void testAuthenticationWithoutHeader() throws IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/webdav/test");

        boolean result = authHandler.handleAuthentication(request, response);

        assertThat(result).isFalse();
        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Lyra WebDAV\", Bearer realm=\"Lyra WebDAV\"");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("WebDAV认证处理器应该处理有效的Basic认证")
    void testBasicAuthentication() throws IOException {
        String credentials = testUser.getUsername() + ":password123";
        String encodedCredentials = Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8));

        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);
        when(request.getRequestURI()).thenReturn("/webdav/test");

        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authToken);

        boolean result = authHandler.handleAuthentication(request, response);

        assertThat(result).isTrue();
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("WebDAV认证处理器应该处理有效的JWT Bearer认证")
    void testBearerAuthentication() throws IOException {
        String token = "valid-jwt-token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/webdav/test");
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(testUser.getUsername());

        LyraUserPrincipal principal = LyraUserPrincipal.fromUser(testUser);
        when(userDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(principal);
        when(jwtService.isTokenValid(token, principal)).thenReturn(true);

        boolean result = authHandler.handleAuthentication(request, response);

        assertThat(result).isTrue();
        verify(jwtService).isTokenValid(token);
        verify(jwtService).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(testUser.getUsername());
        verify(jwtService).isTokenValid(token, principal);
    }

    @Test
    @DisplayName("WebDAV认证处理器应该拒绝无效的JWT令牌")
    void testInvalidBearerToken() throws IOException {
        String token = "invalid-jwt-token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/webdav/test");
        when(jwtService.isTokenValid(token)).thenReturn(false);

        boolean result = authHandler.handleAuthentication(request, response);

        assertThat(result).isFalse();
        verify(jwtService).isTokenValid(token);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("WebDAV锁定服务应该成功获取独占锁")
    void testAcquireExclusiveLock() {
        String resourcePath = "/webdav/personal/test-file.txt";
        
        WebDavLockService.WebDavLock lock = lockService.acquireLock(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        assertThat(lock).isNotNull();
        assertThat(lock.getResourcePath()).isEqualTo(resourcePath);
        assertThat(lock.getLockType()).isEqualTo(WebDavLockService.LockType.EXCLUSIVE);
        assertThat(lock.getUserId()).isEqualTo(testUser.getId());
        assertThat(lock.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(lock.getLockToken()).startsWith("opaquelocktoken:");
    }

    @Test
    @DisplayName("WebDAV锁定服务应该检测锁定冲突")
    void testLockConflict() {
        String resourcePath = "/webdav/personal/test-file.txt";
        
        // 第一个用户获取锁定
        WebDavLockService.WebDavLock firstLock = lockService.acquireLock(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "first-user",
            testUser
        );
        
        assertThat(firstLock).isNotNull();
        
        // 创建第二个用户
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        
        // 第二个用户尝试获取同一资源的锁定应该失败
        WebDavLockService.WebDavLock conflictLock = lockService.acquireLock(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "second-user",
            anotherUser
        );
        
        assertThat(conflictLock).isNull();
    }

    @Test
    @DisplayName("WebDAV锁定服务应该允许同一用户获取多个锁定")
    void testSameUserMultipleLocks() {
        String resourcePath1 = "/webdav/personal/file1.txt";
        String resourcePath2 = "/webdav/personal/file2.txt";
        
        WebDavLockService.WebDavLock lock1 = lockService.acquireLock(
            resourcePath1, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        WebDavLockService.WebDavLock lock2 = lockService.acquireLock(
            resourcePath2, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        assertThat(lock1).isNotNull();
        assertThat(lock2).isNotNull();
        assertThat(lock1.getLockToken()).isNotEqualTo(lock2.getLockToken());
    }

    @Test
    @DisplayName("WebDAV锁定服务应该成功释放锁定")
    void testReleaseLock() {
        String resourcePath = "/webdav/personal/test-file.txt";
        
        WebDavLockService.WebDavLock lock = lockService.acquireLock(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        assertThat(lock).isNotNull();
        
        boolean released = lockService.releaseLock(lock.getLockToken(), testUser);
        assertThat(released).isTrue();
        
        // 验证锁定已被释放
        WebDavLockService.WebDavLock retrievedLock = lockService.getLock(lock.getLockToken());
        assertThat(retrievedLock).isNull();
    }

    @Test
    @DisplayName("WebDAV锁定服务应该刷新锁定")
    void testRefreshLock() {
        String resourcePath = "/webdav/personal/test-file.txt";
        
        WebDavLockService.WebDavLock lock = lockService.acquireLock(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            1800, // 30分钟
            "test-owner",
            testUser
        );
        
        assertThat(lock).isNotNull();
        
        WebDavLockService.WebDavLock refreshedLock = lockService.refreshLock(
            lock.getLockToken(), 
            3600, // 刷新到1小时
            testUser
        );
        
        assertThat(refreshedLock).isNotNull();
        assertThat(refreshedLock.getTimeout()).isEqualTo(3600);
        assertThat(refreshedLock.getLockToken()).isEqualTo(lock.getLockToken());
    }

    @Test
    @DisplayName("WebDAV锁定服务应该正确统计锁定信息")
    void testLockStatistics() {
        // 初始状态
        WebDavLockService.LockStatistics stats = lockService.getLockStatistics();
        assertThat(stats.getTotalLocks()).isZero();
        
        // 创建独占锁
        lockService.acquireLock(
            "/webdav/personal/file1.txt",
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        // 创建共享锁
        lockService.acquireLock(
            "/webdav/personal/file2.txt",
            WebDavLockService.LockType.SHARED,
            "shared",
            0,
            3600,
            "test-owner",
            testUser
        );
        
        stats = lockService.getLockStatistics();
        assertThat(stats.getTotalLocks()).isEqualTo(2);
        assertThat(stats.getExclusiveLocks()).isEqualTo(1);
        assertThat(stats.getSharedLocks()).isEqualTo(1);
    }

    @Test
    @DisplayName("WebDAV锁定服务应该清理过期锁定")
    void testExpiredLockCleanup() {
        String resourcePath = "/webdav/personal/test-file.txt";
        
        // 使用测试辅助方法创建已过期的锁定
        WebDavLockService.WebDavLock lock = lockService.createExpiredLockForTesting(
            resourcePath, 
            WebDavLockService.LockType.EXCLUSIVE,
            "exclusive",
            testUser
        );
        
        assertThat(lock).isNotNull();
        assertThat(lock.isExpired()).isTrue();
        
        // 验证清理前有锁定
        WebDavLockService.LockStatistics statsBefore = lockService.getLockStatistics();
        assertThat(statsBefore.getTotalLocks()).isEqualTo(1);
        
        // 清理过期锁定
        lockService.cleanupExpiredLocks();
        
        // 验证锁定已被清理
        WebDavLockService.LockStatistics statsAfter = lockService.getLockStatistics();
        assertThat(statsAfter.getTotalLocks()).isZero();
    }
} 