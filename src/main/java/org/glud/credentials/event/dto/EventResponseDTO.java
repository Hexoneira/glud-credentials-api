package org.glud.credentials.event.dto;

import org.glud.credentials.event.model.Event;

import java.time.LocalDateTime;

public record EventResponseDTO(
        Long eventId,
        String title,
        LocalDateTime startsAt,
        EventStatus status,
        long attendeesCount,
        Long tenantId,
        String tenantName,
        String createdByCodigo
) {
    public static EventResponseDTO from(Event event, long attendeesCount, EventStatus status) {
        return new EventResponseDTO(
                event.getEventId(),
                event.getTitle(),
                event.getStartsAt(),
                status,
                attendeesCount,
                event.getTenant().getTenantId(),
                event.getTenant().getName(),
                event.getCreatedBy().getCodigo()
        );
    }
}
