package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.auth.model.Tenant;
import org.glud.credentials.auth.model.User;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsImplTest {

    @Test
    void constructor_setsAllFieldsCorrectly() {
        UserDetailsImpl details = new UserDetailsImpl(
                1L, "john", "hashedPass", 10L, Rol.MIEMBRO, Collections.emptyList()
        );

        assertEquals(1L, details.getUserId());
        assertEquals("john", details.getUsername());
        assertEquals("hashedPass", details.getPassword());
        assertEquals(10L, details.getTenantId());
        assertEquals(Rol.MIEMBRO, details.getRoleId());
        assertTrue(details.getAuthorities().isEmpty());
    }

    @Test
    void build_createsUserDetailsFromUser() {
        User user = mock(User.class);
        Tenant tenant = mock(Tenant.class);

        when(user.getUserId()).thenReturn(2L);
        when(user.getUsername()).thenReturn("mary");
        when(user.getPassword()).thenReturn("hash2");
        when(user.getTenant()).thenReturn(tenant);
        when(tenant.getTenantId()).thenReturn(20L);
        when(user.getRol()).thenReturn(Rol.INVITADO);

        UserDetailsImpl details = UserDetailsImpl.build(user);

        assertEquals(2L, details.getUserId());
        assertEquals("mary", details.getUsername());
        assertEquals(20L, details.getTenantId());
        assertEquals(Rol.INVITADO, details.getRoleId());
    }

    @Test
    void accountStatusFlags_areAlwaysTrue() {
        UserDetailsImpl details = new UserDetailsImpl(
                1L, "john", "pass", 10L, Rol.MIEMBRO, Collections.emptyList()
        );
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
    }
}