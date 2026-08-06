package org.glud.credentials.security.exception;

public class InvalidScannedCodeException extends RuntimeException {

    public InvalidScannedCodeException() {
        super("El código escaneado no es válido para toma de asistencia");
    }
}
