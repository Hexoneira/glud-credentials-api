package org.glud.credentials.access.dto;

import java.time.LocalDateTime;

public record AccessValidationResponseDTO(
        boolean allowed,
        String message,
        LocalDateTime timestamp
) {
}
