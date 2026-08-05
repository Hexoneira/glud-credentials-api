package org.glud.credentials.security.exception;

public class CrossTenantAccessException extends RuntimeException {
    public CrossTenantAccessException() {
        super("No tiene acceso a los datos de este miembro");
    }
}
