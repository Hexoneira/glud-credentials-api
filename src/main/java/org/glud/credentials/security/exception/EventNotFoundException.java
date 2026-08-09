package org.glud.credentials.security.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long eventId) {
        super("Evento no encontrado: " + eventId);
    }
}
