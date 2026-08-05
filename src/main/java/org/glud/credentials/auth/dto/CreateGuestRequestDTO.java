package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGuestRequestDTO(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$")
        String codigo,
        @NotBlank
        @Size(max = 255)
        String name,
        @Email
        String email
) {
}
