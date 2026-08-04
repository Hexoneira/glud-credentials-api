package org.glud.credentials.security.authorization;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.SuperAdminRequiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminGuard {

    public void assertSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isSuperAdmin(authentication)) {
            throw new SuperAdminRequiredException();
        }
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserDetailsImpl principal
                && principal.getRoleId() == Rol.SUPER_ADMIN;
    }
}
