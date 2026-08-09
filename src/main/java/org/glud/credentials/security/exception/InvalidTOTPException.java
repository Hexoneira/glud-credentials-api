package org.glud.credentials.security.exception;

public class InvalidTOTPException extends RuntimeException {

    public InvalidTOTPException() {
        super("El código TOTP del carnet no es válido o está vencido");
    }
}
