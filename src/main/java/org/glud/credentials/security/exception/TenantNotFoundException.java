package org.glud.credentials.security.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long tenantId) {
        super("Grupo de trabajo no encontrado: " + tenantId);
    }
}
