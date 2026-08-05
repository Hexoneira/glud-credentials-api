package org.glud.credentials.auth.dto;

import org.glud.credentials.auth.model.Guest;
import org.glud.credentials.auth.model.GuestStatus;
import org.glud.credentials.auth.model.Tenant;

import java.time.LocalDateTime;

public record GuestResponseDTO(
        Long id,
        String codigo,
        String name,
        String email,
        GuestStatus status,
        Long tenantId,
        String tenantName,
        String tenantCode,
        String primaryColor,
        String logoUrl,
        Long createdById,
        String createdByCodigo,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        String accessToken,
        String totpSecret
) {
    public static GuestResponseDTO from(Guest guest) {
        return from(guest, null);
    }

    public static GuestResponseDTO from(Guest guest, String totpSecret) {
        Tenant tenant = guest.getTenant();
        return new GuestResponseDTO(
                guest.getGuestId(),
                guest.getCodigo(),
                guest.getName(),
                guest.getEmail(),
                guest.getStatus(),
                tenant.getTenantId(),
                tenant.getName(),
                tenant.getTenantCode(),
                tenant.getPrimaryColor(),
                tenant.getLogoUrl(),
                guest.getCreatedBy().getUserId(),
                guest.getCreatedBy().getCodigo(),
                guest.getCreatedAt(),
                guest.getExpiresAt(),
                guest.getAccessToken(),
                totpSecret
        );
    }
}
