package org.glud.credentials.security.exception;

public class GuestNotFoundException extends RuntimeException {
    public GuestNotFoundException(Long userId) {
        super("Invitado activo no encontrado para el usuario: " + userId);
    }
}
