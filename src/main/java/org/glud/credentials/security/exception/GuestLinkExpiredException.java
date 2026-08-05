package org.glud.credentials.security.exception;

public class GuestLinkExpiredException extends RuntimeException {
    public GuestLinkExpiredException() {
        super("El enlace de acceso del invitado ha expirado");
    }
}
