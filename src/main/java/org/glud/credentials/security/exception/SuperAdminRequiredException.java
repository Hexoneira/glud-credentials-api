package org.glud.credentials.security.exception;

public class SuperAdminRequiredException extends RuntimeException {
    public SuperAdminRequiredException() {
        super("Se requieren permisos de administrador");
    }
}
