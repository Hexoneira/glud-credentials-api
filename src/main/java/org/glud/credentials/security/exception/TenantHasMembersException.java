package org.glud.credentials.security.exception;

public class TenantHasMembersException extends RuntimeException {
    public TenantHasMembersException(Long tenantId) {
        super("El grupo de trabajo no se puede eliminar porque aún tiene miembros: " + tenantId);
    }
}
