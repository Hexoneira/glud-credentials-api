package org.glud.credentials.event.dto;

/**
 * Estado derivado del evento según su hora de inicio:
 * SCHEDULED  -> la fecha aún no llega
 * IN_PROGRESS -> ocurrió hace menos de la ventana de reunión (2h)
 * FINISHED   -> pasó la ventana
 */
public enum EventStatus {
    SCHEDULED,
    IN_PROGRESS,
    FINISHED
}
