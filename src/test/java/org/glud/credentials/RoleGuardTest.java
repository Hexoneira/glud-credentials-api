package org.glud.credentials;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.authorization.RoleGuard;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleGuardTest {

    private final RoleGuard roleGuard = new RoleGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(Rol rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserDetailsImpl(1L, "user", "pw", 1L, rol, Collections.emptyList()),
                        "pw",
                        Collections.emptyList()
                )
        );
    }

    @Test
    void assertRole_allowsPrincipalWithAnyAllowedRole() {
        setPrincipal(Rol.TENANT_ADMIN);

        assertDoesNotThrow(() -> roleGuard.assertRole(Rol.SUPER_ADMIN, Rol.TENANT_ADMIN));
    }

    @Test
    void assertRole_allowsPrincipalWithSingleAllowedRole() {
        setPrincipal(Rol.SUPER_ADMIN);

        assertDoesNotThrow(() -> roleGuard.assertRole(Rol.SUPER_ADMIN));
    }

    @Test
    void assertRole_throws_whenRoleNotAllowed() {
        setPrincipal(Rol.MIEMBRO);

        assertThrows(RoleRequiredException.class, () -> roleGuard.assertRole(Rol.SUPER_ADMIN, Rol.TENANT_ADMIN));
    }

    @Test
    void assertRole_throws_whenPrincipalIsNotUserDetailsImpl() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", "pw", Collections.emptyList())
        );

        assertThrows(RoleRequiredException.class, () -> roleGuard.assertRole(Rol.SUPER_ADMIN));
    }

    @Test
    void assertRole_throws_whenNoAuthentication() {
        assertThrows(RoleRequiredException.class, () -> roleGuard.assertRole(Rol.SUPER_ADMIN));
    }
}
