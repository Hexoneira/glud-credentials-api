package org.glud.credentials.security.authorization;

import org.glud.credentials.auth.model.Rol;
import org.glud.credentials.security.components.UserDetailsImpl;
import org.glud.credentials.security.exception.RoleRequiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RoleGuard {

    public void assertRole(Rol... allowedRoles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasAnyRole(authentication, allowedRoles)) {
            throw new RoleRequiredException(allowedRoles);
        }
    }

    private boolean hasAnyRole(Authentication authentication, Rol... allowedRoles) {
        return authentication != null
                && authentication.getPrincipal() instanceof UserDetailsImpl principal
                && principal.getRoleId() != null
                && Arrays.asList(allowedRoles).contains(principal.getRoleId());
    }
}
