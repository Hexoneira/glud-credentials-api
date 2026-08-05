package org.glud.credentials.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AccessRequestDTO(
        @NotNull
        Long userId,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "El código TOTP debe tener 6 dígitos")
        String totpCode,
        String deviceId,
        String location
) {
}
