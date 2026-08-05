package org.glud.credentials.security.exception;

public class GuestLimitExceededException extends RuntimeException {
    public GuestLimitExceededException(int limit) {
        super("Límite de invitados activos alcanzado (" + limit + ")");
    }
}
