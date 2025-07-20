package tslc.beihaiyun.lyra.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * 用户主体类
 * 实现Spring Security的UserDetails接口
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
public class LyraUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    private LyraUserPrincipal(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.enabled = builder.enabled;
        this.accountNonLocked = builder.accountNonLocked;
        this.authorities = builder.authorities;
    }

    public static Builder builder() {
        return new Builder();
    }

    // UserDetails接口实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 账户永不过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 密码永不过期
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // 额外的属性getter
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    // equals和hashCode基于用户ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LyraUserPrincipal that = (LyraUserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LyraUserPrincipal{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", accountNonLocked=" + accountNonLocked +
                ", authorities=" + authorities +
                '}';
    }

    // Builder模式
    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private boolean enabled = true;
        private boolean accountNonLocked = true;
        private Collection<? extends GrantedAuthority> authorities;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder accountNonLocked(boolean accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
            return this;
        }

        public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public LyraUserPrincipal build() {
            Objects.requireNonNull(id, "ID不能为空");
            Objects.requireNonNull(username, "用户名不能为空");
            Objects.requireNonNull(password, "密码不能为空");
            Objects.requireNonNull(authorities, "权限不能为空");
            return new LyraUserPrincipal(this);
        }
    }
} 