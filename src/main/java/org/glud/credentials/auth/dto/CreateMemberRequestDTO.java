package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.glud.credentials.auth.model.Rol;

public record CreateMemberRequestDTO(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$")
        String codigo,
        @NotBlank
        @Size(min = 6)
        String password,
        @NotBlank
        @Size(max = 120)
        String name,
        @NotNull
        Rol rol,
        Long tenantId
) {
}
