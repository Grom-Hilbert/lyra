package tslc.beihaiyun.lyra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import tslc.beihaiyun.lyra.entity.Permission;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LyraUserDetailsService 单元测试
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户详情服务测试")
class LyraUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private LyraUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new LyraUserDetailsService(userRepository);
    }

         @Test
     @DisplayName("成功加载有效用户")
     void testLoadUserByUsernameSuccess() {
         // 准备测试数据
         String username = "testuser";
         User user = createTestUser(username);
         
         when(userRepository.findByUsernameAndDeletedFalse(username))
             .thenReturn(Optional.of(user));
         
         // 执行测试
         UserDetails userDetails = userDetailsService.loadUserByUsername(username);
         
         // 验证结果
         assertNotNull(userDetails, "用户详情不应该为空");
         assertEquals(username, userDetails.getUsername(), "用户名应该匹配");
         assertEquals("encoded-password", userDetails.getPassword(), "密码应该匹配");
         assertTrue(userDetails.isEnabled(), "用户应该是启用状态");
         assertTrue(userDetails.isAccountNonLocked(), "账户应该未锁定");
         assertTrue(userDetails.isAccountNonExpired(), "账户应该未过期");
         assertTrue(userDetails.isCredentialsNonExpired(), "凭证应该未过期");
         
         // 验证权限（空角色应该有空权限列表）
         Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
         assertNotNull(authorities, "权限不应该为空");
         
         // 验证调用
         verify(userRepository, times(1)).findByUsernameAndDeletedFalse(username);
     }

    @Test
    @DisplayName("用户不存在时抛出异常")
    void testLoadUserByUsernameNotFound() {
        String username = "nonexistent";
        
        when(userRepository.findByUsernameAndDeletedFalse(username))
            .thenReturn(Optional.empty());
        
        // 验证抛出异常
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(username),
            "应该抛出UsernameNotFoundException"
        );
        
        assertTrue(exception.getMessage().contains(username), 
            "异常消息应该包含用户名");
        
        verify(userRepository, times(1)).findByUsernameAndDeletedFalse(username);
    }

    @Test
    @DisplayName("用户被禁用时抛出异常")
    void testLoadUserByUsernameDisabled() {
        String username = "disableduser";
        User user = createTestUser(username);
        user.setEnabled(false);
        
        when(userRepository.findByUsernameAndDeletedFalse(username))
            .thenReturn(Optional.of(user));
        
        // 验证抛出异常
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(username),
            "应该抛出UsernameNotFoundException"
        );
        
        assertTrue(exception.getMessage().contains("禁用"), 
            "异常消息应该包含禁用信息");
    }

    @Test
    @DisplayName("用户被锁定时抛出异常")
    void testLoadUserByUsernameLocked() {
        String username = "lockeduser";
        User user = createTestUser(username);
        user.setLockedAt(LocalDateTime.now());
        
        when(userRepository.findByUsernameAndDeletedFalse(username))
            .thenReturn(Optional.of(user));
        
        // 验证抛出异常
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(username),
            "应该抛出UsernameNotFoundException"
        );
        
        assertTrue(exception.getMessage().contains("锁定"), 
            "异常消息应该包含锁定信息");
    }

    @Test
    @DisplayName("正确加载用户权限")
    void testLoadUserAuthorities() {
        String username = "adminuser";
        User user = createTestUserWithRole(username, "ADMIN", "USER_CREATE", "USER_DELETE");
        
        when(userRepository.findByUsernameAndDeletedFalse(username))
            .thenReturn(Optional.of(user));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        
        // 验证角色权限
        assertTrue(containsAuthority(authorities, "ROLE_ADMIN"), 
            "应该包含ROLE_ADMIN权限");
        
        // 验证具体权限
        assertTrue(containsAuthority(authorities, "USER_CREATE"), 
            "应该包含USER_CREATE权限");
        assertTrue(containsAuthority(authorities, "USER_DELETE"), 
            "应该包含USER_DELETE权限");
    }

    @Test
    @DisplayName("返回的UserDetails应该是LyraUserPrincipal类型")
    void testReturnedUserDetailsType() {
        String username = "testuser";
        User user = createTestUser(username);
        
        when(userRepository.findByUsernameAndDeletedFalse(username))
            .thenReturn(Optional.of(user));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        assertInstanceOf(LyraUserPrincipal.class, userDetails, 
            "返回的UserDetails应该是LyraUserPrincipal类型");
        
        LyraUserPrincipal principal = (LyraUserPrincipal) userDetails;
        assertEquals(user.getId(), principal.getId(), "用户ID应该匹配");
        assertEquals(user.getEmail(), principal.getEmail(), "邮箱应该匹配");
    }

    /**
     * 创建测试用户
     */
    private User createTestUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        user.setUserRoles(new HashSet<>());
        return user;
    }

    /**
     * 创建带角色的测试用户
     */
    private User createTestUserWithRole(String username, String roleName, String... permissions) {
        User user = createTestUser(username);
        
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        role.setPermissions(new HashSet<>());
        
        // 添加权限
        for (String permCode : permissions) {
            Permission permission = new Permission();
            permission.setId((long) permCode.hashCode());
            permission.setCode(permCode);
            permission.setName(permCode + " Permission");
            role.getPermissions().add(permission);
        }
        
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        
        user.getUserRoles().add(userRole);
        
        return user;
    }

    /**
     * 检查权限集合是否包含指定权限
     */
    private boolean containsAuthority(Collection<? extends GrantedAuthority> authorities, String authority) {
        return authorities.stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }
} 