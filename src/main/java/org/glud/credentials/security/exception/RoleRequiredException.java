package org.glud.credentials.security.exception;

import org.glud.credentials.auth.model.Rol;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RoleRequiredException extends RuntimeException {
    public RoleRequiredException(Rol... requiredRoles) {
        super("Se requieren permisos de rol: " + Arrays.stream(requiredRoles)
                .map(Rol::name)
                .collect(Collectors.joining(", ")));
    }
}
