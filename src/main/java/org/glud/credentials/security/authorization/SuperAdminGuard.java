package org.glud.credentials.security.authorization;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.model.Rol;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final RoleGuard roleGuard;

    public void assertSuperAdmin() {
        roleGuard.assertRole(Rol.SUPER_ADMIN);
    }
}
