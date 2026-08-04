package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequestDTO(
        @Size(min = 3)
        String name,
        @Size(min = 3)
        String director,
        @Min(1)
        @Max(1000)
        Integer memberLimit
) {
}
