package tslc.beihaiyun.lyra.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.entity.UserRole;
import tslc.beihaiyun.lyra.repository.UserRepository;

/**
 * 自定义用户详情服务
 * 从数据库加载用户信息和权限
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@Service
public class LyraUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(LyraUserDetailsService.class);

    private final UserRepository userRepository;

    @Autowired
    public LyraUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("正在加载用户信息：{}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));

        if (!user.getEnabled()) {
            logger.warn("用户已被禁用：{}", username);
            throw new UsernameNotFoundException("用户已被禁用：" + username);
        }

        if (user.getLocked()) {
            logger.warn("用户已被锁定：{}", username);
            throw new UsernameNotFoundException("用户已被锁定：" + username);
        }

        Collection<GrantedAuthority> authorities = getAuthorities(user);
        
        logger.debug("用户 {} 的权限：{}", username, authorities);

        return LyraUserPrincipal.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .password(user.getPassword())
            .enabled(user.getEnabled())
            .accountNonLocked(!user.getLocked())
            .authorities(authorities)
            .build();
    }

    /**
     * 获取用户权限列表
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 添加角色权限
        for (UserRole userRole : user.getUserRoles()) {
            if (userRole.getRole() != null) {
                String roleName = userRole.getRole().getName();
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName;
                }
                authorities.add(new SimpleGrantedAuthority(roleName));
                
                // 添加角色相关的权限
                userRole.getRole().getPermissions().forEach(permission -> {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                });
            }
        }
        
        return authorities;
    }
} 