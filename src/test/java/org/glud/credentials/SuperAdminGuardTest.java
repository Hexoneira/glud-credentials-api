package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.authorization.SuperAdminGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperAdminGuardTest {

    private final SuperAdminGuard guard = new SuperAdminGuard(new RoleGuard());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assertSuperAdmin_doesNotThrow_whenPrincipalIsSuperAdmin() {
        setPrincipal(new UserDetailsImpl(1L, "admin", "pw", 1L, Rol.SUPER_ADMIN, Collections.emptyList()));

        assertDoesNotThrow(guard::assertSuperAdmin);
    }

    @Test
    void assertSuperAdmin_throws_whenPrincipalIsNotSuperAdmin() {
        setPrincipal(new UserDetailsImpl(2L, "member", "pw", 1L, Rol.MIEMBRO, Collections.emptyList()));

        assertThrows(RoleRequiredException.class, guard::assertSuperAdmin);
    }

    @Test
    void assertSuperAdmin_throws_whenPrincipalIsNotUserDetailsImpl() {
        setPrincipal("anonymous");

        assertThrows(RoleRequiredException.class, guard::assertSuperAdmin);
    }

    @Test
    void assertSuperAdmin_throws_whenNoAuthentication() {
        assertThrows(RoleRequiredException.class, guard::assertSuperAdmin);
    }

    private void setPrincipal(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "pw", Collections.emptyList())
        );
    }
}
