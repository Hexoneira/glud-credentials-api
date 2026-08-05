package org.glud.credentials.security.exception;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(Long userId) {
        super("Miembro no encontrado: " + userId);
    }
}
