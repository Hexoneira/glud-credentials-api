package org.glud.credentials.security.components;

import lombok.Getter;
import lombok.Setter;
import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NullMarked
public class UserDetailsImpl implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final Long tenantId;
    private final Rol roleId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long userId, String username, String password,
                           Long tenantId, Rol roleId,
                           Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.authorities = (authorities != null) ? authorities : Collections.emptyList();
    }

    public static UserDetailsImpl build(User user) {
        // Por el momento, los roles y permisos no se han implementado en esta versión.
        // Devuelve una colección de autorizaciones vacía.
        Collection<? extends GrantedAuthority> authorities = Collections.emptyList();

        return new UserDetailsImpl(user.getUserId(), user.getUsername(), user.getPassword(),
                user.getTenant().getTenantId(), user.getRol(), authorities);
    }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
