package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequestDTO(
        @NotBlank
        @Size(min = 3)
        String name,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$")
        String tenantCode,
        @NotBlank
        String director,
        @NotNull
        @Min(1)
        @Max(1000)
        Integer memberLimit
) {
}
