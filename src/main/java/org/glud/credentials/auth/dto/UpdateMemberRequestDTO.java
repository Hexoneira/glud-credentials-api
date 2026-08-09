package org.glud.credentials.auth.dto;

import jakarta.validation.constraints.Size;
import org.glud.credentials.auth.model.Rol;

public record UpdateMemberRequestDTO(
        @Size(max = 120)
        String name,
        Rol rol
) {
}
