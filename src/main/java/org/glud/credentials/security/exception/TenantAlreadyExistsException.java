package org.glud.credentials.security.exception;

public class TenantAlreadyExistsException extends RuntimeException {
    public TenantAlreadyExistsException(String tenantCode) {
        super("Ya existe un grupo de trabajo con el código: " + tenantCode);
    }
}
