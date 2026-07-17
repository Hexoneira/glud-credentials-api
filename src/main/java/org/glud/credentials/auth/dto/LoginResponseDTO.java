package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginResponseDTO(
        @NotBlank
        String token
) {
}
