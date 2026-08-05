package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;

import java.time.LocalDateTime;

public record GuestResponseDTO(
        Long id,
        String codigo,
        String name,
        String email,
        GuestStatus status,
        Long tenantId,
        String tenantName,
        Long createdById,
        String createdByCodigo,
        LocalDateTime createdAt
) {
    public static GuestResponseDTO from(Guest guest) {
        return new GuestResponseDTO(
                guest.getGuestId(),
                guest.getCodigo(),
                guest.getName(),
                guest.getEmail(),
                guest.getStatus(),
                guest.getTenant().getTenantId(),
                guest.getTenant().getName(),
                guest.getCreatedBy().getUserId(),
                guest.getCreatedBy().getCodigo(),
                guest.getCreatedAt()
        );
    }
}
