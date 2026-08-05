package org.glud.credentials.security.exception;

public class ActiveGuestAlreadyExistsException extends RuntimeException {
    public ActiveGuestAlreadyExistsException() {
        super("El miembro ya tiene un invitado activo");
    }
}
