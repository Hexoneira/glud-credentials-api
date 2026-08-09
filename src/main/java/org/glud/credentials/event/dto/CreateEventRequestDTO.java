package org.glud.credentials.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventRequestDTO(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotNull
        LocalDateTime startsAt,
        // Solo lo usa el super admin para elegir el grupo; el admin de grupo usa el suyo
        Long tenantId
) {
}
